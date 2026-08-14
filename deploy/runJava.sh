#!/usr/bin/env bash

# 任一命令失败、使用未定义变量或管道中间命令失败时，立即终止脚本。
# 这样可以避免 git pull 或 Maven 打包失败后仍继续停服、启动旧包。
set -Eeuo pipefail

# ==================== 基础配置 ====================
APP_NAME="yc-page"                                      # 用于识别 Java 进程和命名日志。
APP_PORT="${APP_PORT:-8080}"                            # 可在执行脚本前用 APP_PORT 覆盖。
SOURCE_DIR="/var/javaCode/yc-page"                      # Git 源码目录。
DEPLOY_DIR="/var/java/yc-page"                          # 独立的运行目录，避免直接运行 target 中的 jar。
RELEASE_DIR="${DEPLOY_DIR}/releases"                    # 保存每次构建的 jar，供失败回滚。
LOG_DIR="${DEPLOY_DIR}/logs"                            # 应用日志目录。
CURRENT_JAR="${DEPLOY_DIR}/current.jar"                 # 指向当前版本 jar 的软链接。
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"                # 记录本次启动的进程号。
ENV_FILE="${ENV_FILE:-/etc/yc-page/yc-page.env}"        # 密钥和数据库等环境变量文件。

# ==================== 读取并校验环境 ====================
# 环境变量文件包含密钥，建议权限设置为 600，仅允许 root 读取。
if [[ ! -r "${ENV_FILE}" ]]; then
    echo "缺少环境变量文件: ${ENV_FILE}"
    exit 1
fi

# set -a 会让 source 读到的变量自动 export，Java 进程才能读取这些变量。
set -a
# shellcheck source=/dev/null
source "${ENV_FILE}"
set +a

# ${变量:?错误信息} 表示变量不存在或为空时立即终止，并显示对应提示。
: "${JAVA_HOME:?请在 ${ENV_FILE} 中配置 JDK 21 或更高版本的 JAVA_HOME}"
: "${YC_WECHAT_TOKEN:?请在 ${ENV_FILE} 中配置 YC_WECHAT_TOKEN}"
: "${YC_WECHAT_ENCODINGAESKEY:?请在 ${ENV_FILE} 中配置 YC_WECHAT_ENCODINGAESKEY}"
: "${YC_WECHAT_MINI_APPID:?请在 ${ENV_FILE} 中配置 YC_WECHAT_MINI_APPID}"
: "${YC_WECHAT_MINI_APPSECRET:?请在 ${ENV_FILE} 中配置 YC_WECHAT_MINI_APPSECRET}"
: "${YC_JWT_SECRET:?请在 ${ENV_FILE} 中配置 YC_JWT_SECRET}"

if (( ${#YC_JWT_SECRET} < 32 )); then
    echo "YC_JWT_SECRET 不能少于 32 个字符"
    exit 1
fi

JAVA_BIN="${JAVA_HOME}/bin/java"
if [[ ! -x "${JAVA_BIN}" ]]; then
    echo "JAVA_HOME 无效: ${JAVA_HOME}"
    exit 1
fi

# 从 java -version 第一行提取主版本号，例如 21.0.12 会得到 21。
JAVA_VERSION="$("${JAVA_BIN}" -version 2>&1)"
JAVA_MAJOR="$(awk -F '"' 'NR == 1 {split($2, version, "."); print version[1]}' <<< "${JAVA_VERSION}")"
if [[ ! "${JAVA_MAJOR}" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 21 )); then
    echo "当前项目必须使用 JDK 21 或更高版本，实际为: ${JAVA_VERSION}"
    exit 1
fi

# 将所选 JDK 放到 PATH 最前面，确保 Maven 也使用这个 JDK，而不是系统里的 JDK 8。
export PATH="${JAVA_HOME}/bin:/usr/local/maven/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
# 部署依赖 git 拉代码、mvn 打包、ss 检查监听端口，缺少任一工具都不继续。
command -v git >/dev/null || { echo "未找到 git"; exit 1; }
command -v mvn >/dev/null || { echo "未找到 mvn"; exit 1; }
command -v ss >/dev/null || { echo "未找到 ss"; exit 1; }

mkdir -p "${RELEASE_DIR}" "${LOG_DIR}"

# ==================== 保存可回滚版本 ====================
# 正常部署后 current.jar 是软链接，读取它即可找到上一版 jar。
PREVIOUS_JAR=""
if [[ -L "${CURRENT_JAR}" ]]; then
    PREVIOUS_JAR="$(readlink -f "${CURRENT_JAR}")"
else
    # 第一次使用本脚本时还没有 current.jar，先备份 target 中原有的 jar。
    OLD_JAR="$(find "${SOURCE_DIR}/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' -print -quit 2>/dev/null || true)"
    if [[ -n "${OLD_JAR}" ]]; then
        PREVIOUS_JAR="${RELEASE_DIR}/${APP_NAME}-before-java21-$(date +%Y%m%d%H%M%S).jar"
        cp "${OLD_JAR}" "${PREVIOUS_JAR}"
    fi
fi

# ==================== 拉代码并构建新版本 ====================
# 这里尚未停止旧进程；拉取或构建失败时，线上旧服务不受影响。
echo "拉取最新代码并打包（此阶段旧服务继续运行）"
cd "${SOURCE_DIR}"
git pull --ff-only                         # 只允许快进更新，避免服务器自动产生合并提交。
mvn -B clean package -DskipTests           # 批处理模式构建；编译测试代码但不执行测试。

# Spring Boot 打包可能同时生成 original-*.jar，这里只选择可直接运行的 jar。
BUILD_JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' -print -quit)"
if [[ -z "${BUILD_JAR}" ]]; then
    echo "打包完成但没有找到可运行的 jar"
    exit 1
fi

# 发布文件名加入 Git 提交号和时间，方便确认版本，也避免覆盖旧包。
REVISION="$(git rev-parse --short HEAD)"
NEW_JAR="${RELEASE_DIR}/${APP_NAME}-${REVISION}-$(date +%Y%m%d%H%M%S).jar"
cp "${BUILD_JAR}" "${NEW_JAR}"

# 查找命令行中同时包含 java、yc-page 和 .jar 的进程号。
# [j]ava 的写法可避免 awk 把自己的命令行误认为 Java 进程。
find_app_pids() {
    ps -eo pid=,args= | awk '$0 ~ /[j]ava/ && $0 ~ /yc-page.*[.]jar/ {print $1}'
}

# 先发送 TERM，让应用有机会正常释放端口和连接；30 秒仍未退出才强制结束。
stop_app() {
    local pids
    pids="$(find_app_pids)"
    [[ -z "${pids}" ]] && return 0

    echo "停止旧进程: ${pids//$'\n'/ }"
    kill -TERM ${pids}
    for _ in {1..30}; do
        sleep 1
        pids="$(find_app_pids)"
        [[ -z "${pids}" ]] && return 0
    done

    echo "旧进程未正常退出，强制停止: ${pids//$'\n'/ }"
    kill -KILL ${pids}
}

# 后台启动 current.jar，将标准输出和错误输出追加到同一日志，并记录 PID。
start_app() {
    nohup "${JAVA_BIN}" ${JAVA_OPTS:-} -jar "${CURRENT_JAR}" >> "${LOG_DIR}/${APP_NAME}.log" 2>&1 &
    echo $! > "${PID_FILE}"
}

# 最多等待 30 秒：进程必须存活，并且应用端口已经进入监听状态。
wait_for_start() {
    local pid
    pid="$(cat "${PID_FILE}")"
    for _ in {1..30}; do
        kill -0 "${pid}" 2>/dev/null || return 1
        if ss -ltn | awk '{print $4}' | grep -Eq ":${APP_PORT}$"; then
            return 0
        fi
        sleep 1
    done
    return 1
}

# ==================== 切换到新版本 ====================
stop_app
# 只切换软链接，不覆盖历史 jar，因此后面可以快速回滚。
ln -sfn "${NEW_JAR}" "${CURRENT_JAR}"
start_app

if wait_for_start; then
    echo "部署成功: ${NEW_JAR}，PID $(cat "${PID_FILE}")，端口 ${APP_PORT}"
    exit 0
fi

# ==================== 启动失败时回滚 ====================
# 先打印最近 80 行日志帮助定位问题，再停止失败的新进程。
echo "新版本启动失败，最近日志如下："
tail -n 80 "${LOG_DIR}/${APP_NAME}.log" || true
stop_app

# 如果上一版 jar 存在，将 current.jar 重新指向上一版并启动。
if [[ -n "${PREVIOUS_JAR}" && -f "${PREVIOUS_JAR}" ]]; then
    echo "正在回滚到: ${PREVIOUS_JAR}"
    ln -sfn "${PREVIOUS_JAR}" "${CURRENT_JAR}"
    start_app
    if wait_for_start; then
        echo "回滚成功，旧版本已恢复"
    else
        echo "回滚版本也未能启动，请检查日志: ${LOG_DIR}/${APP_NAME}.log"
    fi
fi

exit 1
