#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="yc-page"
APP_PORT="${APP_PORT:-8080}"
SOURCE_DIR="/var/javaCode/yc-page"
DEPLOY_DIR="/var/java/yc-page"
RELEASE_DIR="${DEPLOY_DIR}/releases"
LOG_DIR="${DEPLOY_DIR}/logs"
CURRENT_JAR="${DEPLOY_DIR}/current.jar"
PID_FILE="${DEPLOY_DIR}/${APP_NAME}.pid"
ENV_FILE="${ENV_FILE:-/etc/yc-page/yc-page.env}"

if [[ ! -r "${ENV_FILE}" ]]; then
    echo "缺少环境变量文件: ${ENV_FILE}"
    exit 1
fi

set -a
# shellcheck source=/dev/null
source "${ENV_FILE}"
set +a

: "${JAVA_HOME:?请在 ${ENV_FILE} 中配置 JDK 21 的 JAVA_HOME}"
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

JAVA_VERSION="$("${JAVA_BIN}" -version 2>&1)"
JAVA_MAJOR="$(awk -F '"' 'NR == 1 {split($2, version, "."); print version[1]}' <<< "${JAVA_VERSION}")"
if [[ "${JAVA_MAJOR}" != "21" ]]; then
    echo "当前项目必须使用 JDK 21，实际为: ${JAVA_VERSION}"
    exit 1
fi

export PATH="${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
command -v git >/dev/null || { echo "未找到 git"; exit 1; }
command -v mvn >/dev/null || { echo "未找到 mvn"; exit 1; }
command -v ss >/dev/null || { echo "未找到 ss"; exit 1; }

mkdir -p "${RELEASE_DIR}" "${LOG_DIR}"

PREVIOUS_JAR=""
if [[ -L "${CURRENT_JAR}" ]]; then
    PREVIOUS_JAR="$(readlink -f "${CURRENT_JAR}")"
else
    OLD_JAR="$(find "${SOURCE_DIR}/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' -print -quit 2>/dev/null || true)"
    if [[ -n "${OLD_JAR}" ]]; then
        PREVIOUS_JAR="${RELEASE_DIR}/${APP_NAME}-before-java21-$(date +%Y%m%d%H%M%S).jar"
        cp "${OLD_JAR}" "${PREVIOUS_JAR}"
    fi
fi

echo "拉取最新代码并打包（此阶段旧服务继续运行）"
cd "${SOURCE_DIR}"
git pull --ff-only
mvn -B clean package -DskipTests

BUILD_JAR="$(find target -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' -print -quit)"
if [[ -z "${BUILD_JAR}" ]]; then
    echo "打包完成但没有找到可运行的 jar"
    exit 1
fi

REVISION="$(git rev-parse --short HEAD)"
NEW_JAR="${RELEASE_DIR}/${APP_NAME}-${REVISION}-$(date +%Y%m%d%H%M%S).jar"
cp "${BUILD_JAR}" "${NEW_JAR}"

find_app_pids() {
    ps -eo pid=,args= | awk '$0 ~ /[j]ava/ && $0 ~ /yc-page.*[.]jar/ {print $1}'
}

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

start_app() {
    nohup "${JAVA_BIN}" ${JAVA_OPTS:-} -jar "${CURRENT_JAR}" >> "${LOG_DIR}/${APP_NAME}.log" 2>&1 &
    echo $! > "${PID_FILE}"
}

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

stop_app
ln -sfn "${NEW_JAR}" "${CURRENT_JAR}"
start_app

if wait_for_start; then
    echo "部署成功: ${NEW_JAR}，PID $(cat "${PID_FILE}")，端口 ${APP_PORT}"
    exit 0
fi

echo "新版本启动失败，最近日志如下："
tail -n 80 "${LOG_DIR}/${APP_NAME}.log" || true
stop_app

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
