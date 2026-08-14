#!/usr/bin/env bash

# CentOS 7 和 Ubuntu 安装脚本共用的实现。
# 请运行 install-centos7.sh 或 install-ubuntu.sh，不要直接运行本文件。

set -Eeuo pipefail

JDK_MAJOR="21"
MAVEN_VERSION="3.9.11"
MYSQL_VERSION="5.7.25"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_BIND_ADDRESS="${MYSQL_BIND_ADDRESS:-127.0.0.1}"
MYSQL_APP_DATABASE="${MYSQL_APP_DATABASE:-yc_page}"
MYSQL_APP_USER="${MYSQL_APP_USER:-yc_page}"
CREDENTIAL_DIR="/root/.yc-stack"
CREDENTIAL_FILE="${CREDENTIAL_DIR}/credentials.env"
CACHE_DIR="/var/cache/yc-stack"
JDK_MIRROR_BASE="${JDK_MIRROR_BASE:-https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux}"
MAVEN_MIRROR_BASE="${MAVEN_MIRROR_BASE:-https://mirrors.huaweicloud.com/apache/maven/maven-3}"
MAVEN_REPOSITORY_MIRROR_URL="${MAVEN_REPOSITORY_MIRROR_URL:-https://mirrors.huaweicloud.com/repository/maven/}"
REDIS_MIRROR_BASE="${REDIS_MIRROR_BASE:-https://mirrors.huaweicloud.com/redis}"
WORK_DIR=""

log() {
    printf '\n[%s] %s\n' "$(date '+%F %T')" "$*"
}

warn() {
    printf '\n[警告] %s\n' "$*" >&2
}

die() {
    printf '\n[错误] %s\n' "$*" >&2
    exit 1
}

cleanup() {
    # 只删除本脚本通过 mktemp 创建的明确目录，避免误删其他路径。
    if [[ -n "${WORK_DIR}" && "${WORK_DIR}" == /tmp/yc-stack.* && -d "${WORK_DIR}" ]]; then
        rm -rf -- "${WORK_DIR}"
    fi
}

require_root() {
    (( EUID == 0 )) || die "请使用 root 用户执行此脚本"
    command -v systemctl >/dev/null || die "当前系统没有 systemd，无法注册服务"
}

version_ge() {
    # 当第一个版本大于或等于第二个版本时返回成功。
    [[ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -n 1)" == "$2" ]]
}

download_file() {
    local url="$1"
    local destination="$2"
    log "下载 ${url}"
    curl --fail --location --retry 3 --connect-timeout 20 --output "${destination}" "${url}"
}

download_resumable() {
    local url="$1"
    local destination="$2"

    log "下载 ${url}"
    # -C - 会根据现有文件大小继续下载，网络中断后重新执行脚本不必从头开始。
    if curl --fail --location --retry 3 --connect-timeout 20 \
        --continue-at - --output "${destination}" "${url}"; then
        return
    fi

    # 少数服务器不接受续传请求；保留明确提示后再从头下载该文件。
    warn "断点续传失败，尝试从头下载 $(basename "${destination}")"
    rm -f -- "${destination}"
    curl --fail --location --retry 3 --connect-timeout 20 \
        --output "${destination}" "${url}"
}

link_directory() {
    local target="$1"
    local link_path="$2"

    if [[ -e "${link_path}" && ! -L "${link_path}" ]]; then
        die "${link_path} 已存在且不是软链接，请先人工确认"
    fi
    ln -sfn "${target}" "${link_path}"
}

credential_set() {
    local key="$1"
    local value="$2"
    local temporary

    mkdir -p "${CREDENTIAL_DIR}"
    chmod 700 "${CREDENTIAL_DIR}"
    touch "${CREDENTIAL_FILE}"
    chmod 600 "${CREDENTIAL_FILE}"

    temporary="$(mktemp "${CREDENTIAL_DIR}/credentials.XXXXXX")"
    grep -v "^${key}=" "${CREDENTIAL_FILE}" > "${temporary}" || true
    printf '%s=%q\n' "${key}" "${value}" >> "${temporary}"
    chmod 600 "${temporary}"
    mv "${temporary}" "${CREDENTIAL_FILE}"
}

java_major_from() {
    local java_bin="$1"
    local version_output
    version_output="$("${java_bin}" -version 2>&1)" || return 1
    awk -F '"' 'NR == 1 {split($2, version, "."); print version[1]}' <<< "${version_output}"
}

configure_java_home() {
    local java_home="$1"
    mkdir -p /usr/local/java

    if [[ "${java_home}" != "/usr/local/java/jdk-21" ]]; then
        link_directory "${java_home}" /usr/local/java/jdk-21
    fi

    cat > /etc/profile.d/yc-java.sh <<'EOF'
export JAVA_HOME='/usr/local/java/jdk-21'
export PATH="$JAVA_HOME/bin:$PATH"
EOF
    chmod 644 /etc/profile.d/yc-java.sh

    export JAVA_HOME="/usr/local/java/jdk-21"
    export PATH="${JAVA_HOME}/bin:${PATH}"
}

install_jdk() {
    local existing_java=""
    local existing_major=""
    local existing_home=""
    local metadata_file="${WORK_DIR}/temurin.json"
    local package_url
    local package_checksum
    local package_name
    local mirror_url
    local archive
    local extracted_root
    local installed_home

    if command -v java >/dev/null; then
        existing_java="$(readlink -f "$(command -v java)")"
        existing_major="$(java_major_from "${existing_java}" || true)"
    fi

    if [[ "${existing_major}" =~ ^[0-9]+$ ]] && (( existing_major >= JDK_MAJOR )); then
        existing_home="$(dirname "$(dirname "${existing_java}")")"
        log "已安装 JDK ${existing_major}，跳过下载：${existing_home}"
        configure_java_home "${existing_home}"
        return
    fi

    if [[ -x /usr/local/java/jdk-21/bin/java ]]; then
        existing_major="$(java_major_from /usr/local/java/jdk-21/bin/java || true)"
        if [[ "${existing_major}" =~ ^[0-9]+$ ]] && (( existing_major >= JDK_MAJOR )); then
            log "已安装 JDK ${existing_major}，跳过下载：/usr/local/java/jdk-21"
            configure_java_home /usr/local/java/jdk-21
            return
        fi
    fi

    log "未发现 JDK 21 或更高版本，安装 Eclipse Temurin JDK 21 最新补丁版"
    download_file "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=linux&vendor=eclipse" "${metadata_file}"
    package_url="$("${JSON_PYTHON}" -c 'import json,sys; print(json.load(open(sys.argv[1]))[0]["binary"]["package"]["link"])' "${metadata_file}")"
    package_checksum="$("${JSON_PYTHON}" -c 'import json,sys; print(json.load(open(sys.argv[1]))[0]["binary"]["package"]["checksum"])' "${metadata_file}")"
    [[ -n "${package_url}" && -n "${package_checksum}" ]] || die "无法从 Adoptium 响应中读取 JDK 下载信息"

    package_name="$(basename "${package_url}")"
    mirror_url="${JDK_MIRROR_BASE}/${package_name}"
    archive="${CACHE_DIR}/${package_name}"

    if [[ -f "${archive}" && "$(sha256sum "${archive}" | awk '{print $1}')" == "${package_checksum}" ]]; then
        log "使用已校验的 JDK 缓存：${archive}"
    else
        # 国内服务器优先使用清华镜像；镜像没有同步该版本时回退 Adoptium/GitHub。
        if ! download_resumable "${mirror_url}" "${archive}"; then
            warn "JDK 镜像下载失败，回退到 Adoptium 官方地址"
            download_resumable "${package_url}" "${archive}"
        fi
        if [[ "$(sha256sum "${archive}" | awk '{print $1}')" != "${package_checksum}" ]]; then
            warn "JDK 缓存校验失败，删除缓存后从官方地址重试"
            rm -f -- "${archive}"
            download_resumable "${package_url}" "${archive}"
        fi
        [[ "$(sha256sum "${archive}" | awk '{print $1}')" == "${package_checksum}" ]] || die "JDK 下载文件校验失败"
    fi

    mkdir -p "${WORK_DIR}/jdk"
    tar -xzf "${archive}" -C "${WORK_DIR}/jdk"
    extracted_root="$(find "${WORK_DIR}/jdk" -mindepth 1 -maxdepth 1 -type d -print -quit)"
    [[ -x "${extracted_root}/bin/java" ]] || die "JDK 压缩包目录结构不符合预期"

    installed_home="/opt/$(basename "${extracted_root}")"
    if [[ ! -d "${installed_home}" ]]; then
        mv "${extracted_root}" "${installed_home}"
    fi
    configure_java_home "${installed_home}"
    log "JDK 安装完成：$("${JAVA_HOME}/bin/java" -version 2>&1 | awk 'NR == 1 {first=$0} END {print first}')"
}

configure_maven_home() {
    cat > /etc/profile.d/yc-maven.sh <<'EOF'
export MAVEN_HOME='/usr/local/maven'
export PATH="$MAVEN_HOME/bin:$PATH"
EOF
    chmod 644 /etc/profile.d/yc-maven.sh
    export MAVEN_HOME="/usr/local/maven"
    export PATH="${MAVEN_HOME}/bin:${PATH}"
}

configure_maven_repository_mirror() {
    local settings_file="/root/.m2/settings.xml"

    if [[ -e "${settings_file}" ]]; then
        log "发现现有 Maven 用户配置，保留不修改：${settings_file}"
        return
    fi

    mkdir -p /root/.m2
    chmod 700 /root/.m2
    cat > "${settings_file}" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>huawei-cloud-central</id>
      <name>Huawei Cloud Maven Central Mirror</name>
      <url>${MAVEN_REPOSITORY_MIRROR_URL}</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
    chmod 600 "${settings_file}"
    log "已配置 Maven Central 国内镜像：${MAVEN_REPOSITORY_MIRROR_URL}"
}

install_maven() {
    local existing_version=""
    local existing_bin=""
    local nested_maven=""
    local archive="${CACHE_DIR}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
    local checksum_file="${WORK_DIR}/apache-maven-${MAVEN_VERSION}-bin.tar.gz.sha512"
    local expected_checksum
    local actual_checksum
    local target="/opt/apache-maven-${MAVEN_VERSION}"
    local base_url="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries"
    local mirror_url="${MAVEN_MIRROR_BASE}/${MAVEN_VERSION}/binaries/$(basename "${archive}")"
    local origin_url="${base_url}/$(basename "${archive}")"

    if command -v mvn >/dev/null; then
        existing_bin="$(command -v mvn)"
    elif [[ -x /usr/local/maven/bin/mvn ]]; then
        # 有些旧安装没有写入当前 Shell 的 PATH，但目录本身仍然可用。
        existing_bin="/usr/local/maven/bin/mvn"
    elif [[ -d /usr/local/maven ]]; then
        # 兼容 /usr/local/maven/apache-maven-x.y.z/bin/mvn 这种旧目录结构。
        nested_maven="$(find /usr/local/maven -mindepth 3 -maxdepth 3 -type f -path '*/apache-maven-*/bin/mvn' -print | sort -V | tail -n 1)"
        [[ -x "${nested_maven}" ]] && existing_bin="${nested_maven}"
    fi

    if [[ -n "${existing_bin}" ]]; then
        existing_version="$("${existing_bin}" -v 2>/dev/null | awk 'NR == 1 {print $3}' || true)"
    fi

    if [[ -n "${existing_version}" ]] && version_ge "${existing_version}" "3.6.3"; then
        if [[ "${existing_bin}" == "/usr/local/maven/bin/mvn" ]]; then
            configure_maven_home
        elif [[ -n "${nested_maven}" ]]; then
            # 不改动原目录，只在系统 PATH 中增加一个统一命令入口。
            ln -sfn "${nested_maven}" /usr/local/bin/mvn
        fi
        configure_maven_repository_mirror
        log "已安装可用的 Maven ${existing_version}，跳过下载：${existing_bin}"
        return
    fi

    if [[ ! -x "${target}/bin/mvn" ]]; then
        log "未发现 Maven 3.6.3 或更高版本，安装 Maven ${MAVEN_VERSION}"
        download_file "${base_url}/$(basename "${checksum_file}")" "${checksum_file}"
        expected_checksum="$(awk '{print $1}' "${checksum_file}")"
        actual_checksum="$(sha512sum "${archive}" 2>/dev/null | awk '{print $1}' || true)"
        if [[ "${actual_checksum}" != "${expected_checksum}" ]]; then
            if ! download_resumable "${mirror_url}" "${archive}"; then
                warn "Maven 镜像下载失败，回退到 Apache Archive"
                download_resumable "${origin_url}" "${archive}"
            fi
            actual_checksum="$(sha512sum "${archive}" | awk '{print $1}')"
        else
            log "使用已校验的 Maven 缓存：${archive}"
        fi
        if [[ "${actual_checksum}" != "${expected_checksum}" ]]; then
            warn "Maven 缓存校验失败，删除缓存后从 Apache Archive 重试"
            rm -f -- "${archive}"
            download_resumable "${origin_url}" "${archive}"
            actual_checksum="$(sha512sum "${archive}" | awk '{print $1}')"
        fi
        [[ "${actual_checksum}" == "${expected_checksum}" ]] || die "Maven 下载文件校验失败"
        tar -xzf "${archive}" -C /opt
    fi

    link_directory "${target}" /usr/local/maven
    configure_maven_home
    configure_maven_repository_mirror
    log "Maven 安装完成：$(mvn -v | awk 'NR == 1 {first=$0} END {print first}')"
}

mysql_existing_version() {
    if command -v mysqld >/dev/null; then
        mysqld --version 2>/dev/null || true
        return
    fi
    if command -v mysql >/dev/null; then
        mysql --version 2>/dev/null || true
        return
    fi
    if [[ -x /usr/local/mysql/bin/mysqld ]]; then
        /usr/local/mysql/bin/mysqld --version 2>/dev/null || true
    fi
}

validate_generated_password() {
    local name="$1"
    local value="$2"
    [[ "${value}" =~ ^[A-Za-z0-9._@%+=:-]{16,128}$ ]] || die "${name} 至少 16 位，且只能包含字母、数字和 ._@%+=:-"
}

install_mysql() {
    local existing_version
    local mysql_home="/opt/mysql-${MYSQL_VERSION}"
    local mysql_link="/usr/local/mysql"
    local archive="${CACHE_DIR}/mysql-${MYSQL_VERSION}-linux-glibc2.12-x86_64.tar.gz"
    local archive_url="https://downloads.mysql.com/archives/get/p/23/file/mysql-${MYSQL_VERSION}-linux-glibc2.12-x86_64.tar.gz"
    local extracted_root
    local missing_libraries
    local root_password="${MYSQL_ROOT_PASSWORD:-}"
    local app_password="${MYSQL_APP_PASSWORD:-}"
    local socket="/run/yc-mysql/mysql.sock"
    local install_marker="/var/lib/yc-mysql/.yc-install-complete"
    local command_path=""
    local service

    # 成功跑过本脚本的 MySQL 用标记文件识别；再次执行时只确保服务已启动。
    if [[ -f "${install_marker}" && -x "${mysql_home}/bin/mysqld" ]]; then
        log "发现本脚本已安装的 MySQL ${MYSQL_VERSION}，跳过安装和初始化"
        systemctl enable yc-mysql
        systemctl start yc-mysql
        return
    fi

    # 只把脚本安装目录以外的 mysql/mysqld 当作用户原有数据库。
    for service in mysqld mysql; do
        if command -v "${service}" >/dev/null; then
            command_path="$(readlink -f "$(command -v "${service}")")"
            if [[ "${command_path}" != "${mysql_home}/"* ]]; then
                existing_version="$(mysql_existing_version)"
                log "发现现有 MySQL/MariaDB，按保护策略跳过安装和配置：${existing_version}"
                warn "脚本不会修改现有数据库；请自行确认数据库名、账号和密码"
                return
            fi
        fi
    done

    # 某些包只注册服务但未把客户端放入 PATH，同样按现有数据库保护。
    for service in mysqld mysql mariadb; do
        if systemctl list-unit-files "${service}.service" --no-legend 2>/dev/null | grep -q "${service}.service"; then
            log "发现现有数据库服务 ${service}.service，按保护策略跳过安装和配置"
            return
        fi
    done

    [[ "$(uname -m)" == "x86_64" ]] || die "MySQL 5.7.25 官方通用包仅按 x86_64 服务器编写，当前架构为 $(uname -m)"
    [[ "${MYSQL_APP_DATABASE}" =~ ^[A-Za-z0-9_]+$ ]] || die "MYSQL_APP_DATABASE 只能包含字母、数字和下划线"
    [[ "${MYSQL_APP_USER}" =~ ^[A-Za-z0-9_]+$ ]] || die "MYSQL_APP_USER 只能包含字母、数字和下划线"
    root_password="${root_password:-$(openssl rand -hex 24)}"
    app_password="${app_password:-$(openssl rand -hex 24)}"
    validate_generated_password MYSQL_ROOT_PASSWORD "${root_password}"
    validate_generated_password MYSQL_APP_PASSWORD "${app_password}"

    if [[ ! -x "${mysql_home}/bin/mysqld" ]]; then
        log "未发现 MySQL，安装官方归档版 MySQL ${MYSQL_VERSION}"
        if [[ -f "${archive}" ]] && tar -tzf "${archive}" >/dev/null 2>&1; then
            log "使用已验证可解压的 MySQL 缓存：${archive}"
        else
            download_resumable "${archive_url}" "${archive}"
        fi
        # MySQL 归档站未提供独立校验文件；HTTPS 下载后至少先验证 gzip/tar 完整性。
        tar -tzf "${archive}" >/dev/null || die "MySQL 下载文件无法正常解压"
        mkdir -p "${WORK_DIR}/mysql"
        tar -xzf "${archive}" -C "${WORK_DIR}/mysql"
        extracted_root="$(find "${WORK_DIR}/mysql" -mindepth 1 -maxdepth 1 -type d -print -quit)"
        [[ -x "${extracted_root}/bin/mysqld" ]] || die "MySQL 压缩包目录结构不符合预期"
        mv "${extracted_root}" "${mysql_home}"
    fi

    missing_libraries="$(ldd "${mysql_home}/bin/mysqld" | awk '/not found/ {print $1}' | paste -sd ' ' -)"
    [[ -z "${missing_libraries}" ]] || die "MySQL 缺少系统动态库：${missing_libraries}"

    getent group mysql >/dev/null || groupadd --system mysql
    if ! id mysql >/dev/null 2>&1; then
        useradd --system --gid mysql --home-dir /nonexistent --shell "$(command -v nologin)" mysql
    fi

    link_directory "${mysql_home}" "${mysql_link}"
    mkdir -p /etc/yc-mysql /var/lib/yc-mysql /var/log/yc-mysql /run/yc-mysql
    chown -R mysql:mysql /var/lib/yc-mysql /var/log/yc-mysql /run/yc-mysql

    cat > /etc/yc-mysql/my.cnf <<EOF
[client]
port=${MYSQL_PORT}
socket=${socket}
default-character-set=utf8mb4

[mysqld]
user=mysql
basedir=${mysql_link}
datadir=/var/lib/yc-mysql
port=${MYSQL_PORT}
bind-address=${MYSQL_BIND_ADDRESS}
socket=${socket}
pid-file=/run/yc-mysql/mysql.pid
log-error=/var/log/yc-mysql/error.log
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
explicit_defaults_for_timestamp=1
skip-name-resolve=1
symbolic-links=0
EOF
    chmod 640 /etc/yc-mysql/my.cnf
    chown root:mysql /etc/yc-mysql/my.cnf

    cat > /etc/systemd/system/yc-mysql.service <<'EOF'
[Unit]
Description=YC MySQL Server
After=network.target

[Service]
Type=simple
User=mysql
Group=mysql
ExecStart=/usr/local/mysql/bin/mysqld --defaults-file=/etc/yc-mysql/my.cnf
Restart=on-failure
RestartSec=5
LimitNOFILE=65535
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

    if [[ ! -d /var/lib/yc-mysql/mysql ]]; then
        log "初始化 MySQL 数据目录"
        "${mysql_link}/bin/mysqld" --defaults-file=/etc/yc-mysql/my.cnf --initialize-insecure
        chown -R mysql:mysql /var/lib/yc-mysql /var/log/yc-mysql /run/yc-mysql
    elif [[ ! -f "${install_marker}" ]]; then
        die "发现未完成的 MySQL 安装：数据目录已初始化但缺少完成标记，请先检查 /var/log/yc-mysql/error.log"
    fi

    systemctl daemon-reload
    systemctl enable yc-mysql
    systemctl start yc-mysql

    for _ in {1..60}; do
        [[ -S "${socket}" ]] && "${mysql_link}/bin/mysqladmin" --protocol=socket --socket="${socket}" -uroot ping --silent && break
        sleep 1
    done
    "${mysql_link}/bin/mysqladmin" --protocol=socket --socket="${socket}" -uroot ping --silent || die "MySQL 启动失败，请检查 /var/log/yc-mysql/error.log"

    # 新安装使用无密码本地 socket 完成第一次配置；密码通过标准输入传入，不出现在进程参数中。
    printf "%s\n" \
        "ALTER USER 'root'@'localhost' IDENTIFIED BY '${root_password}';" \
        "CREATE DATABASE IF NOT EXISTS \`${MYSQL_APP_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" \
        "CREATE USER IF NOT EXISTS '${MYSQL_APP_USER}'@'127.0.0.1' IDENTIFIED BY '${app_password}';" \
        "GRANT ALL PRIVILEGES ON \`${MYSQL_APP_DATABASE}\`.* TO '${MYSQL_APP_USER}'@'127.0.0.1';" \
        "FLUSH PRIVILEGES;" \
        | "${mysql_link}/bin/mysql" --protocol=socket --socket="${socket}" -uroot

    credential_set MYSQL_ROOT_PASSWORD "${root_password}"
    credential_set YC_MYSQL_HOST "127.0.0.1"
    credential_set YC_MYSQL_USERNAME "${MYSQL_APP_USER}"
    credential_set YC_MYSQL_PASSWORD "${app_password}"

    cat > /etc/profile.d/yc-mysql.sh <<'EOF'
export PATH="/usr/local/mysql/bin:$PATH"
EOF
    chmod 644 /etc/profile.d/yc-mysql.sh
    export PATH="${mysql_link}/bin:${PATH}"
    touch "${install_marker}"
    chmod 640 "${install_marker}"
    chown root:mysql "${install_marker}"
    log "MySQL ${MYSQL_VERSION} 安装完成，应用数据库为 ${MYSQL_APP_DATABASE}"
}

redis_existing_version() {
    if command -v redis-server >/dev/null; then
        redis-server --version 2>/dev/null || true
        return
    fi
    if [[ -x /usr/local/redis/bin/redis-server ]]; then
        /usr/local/redis/bin/redis-server --version 2>/dev/null || true
    fi
}

enable_existing_redis_service() {
    local service
    for service in redis-server redis yc-redis; do
        if systemctl list-unit-files "${service}.service" --no-legend 2>/dev/null | grep -q "${service}.service"; then
            systemctl enable "${service}"
            systemctl start "${service}"
            return
        fi
    done
    warn "找到了 redis-server，但没有识别到 systemd 服务，请自行确认 Redis 是否已启动"
}

install_redis_from_source() {
    local redis_version="${REDIS_FALLBACK_VERSION:-7.2.4}"
    local archive="${CACHE_DIR}/redis-${redis_version}.tar.gz"
    local source_dir="${WORK_DIR}/redis-${redis_version}"
    local target="/opt/redis-${redis_version}"
    local mirror_url="${REDIS_MIRROR_BASE}/redis-${redis_version}.tar.gz"
    local origin_url="https://download.redis.io/releases/redis-${redis_version}.tar.gz"

    log "系统仓库无法提供 Redis，回退为源码安装 Redis ${redis_version}"
    if [[ -f "${archive}" ]] && tar -tzf "${archive}" >/dev/null 2>&1; then
        log "使用已验证可解压的 Redis 缓存：${archive}"
    else
        if ! download_resumable "${mirror_url}" "${archive}"; then
            warn "Redis 镜像下载失败，回退到 Redis 官方地址"
            download_resumable "${origin_url}" "${archive}"
        fi
        if ! tar -tzf "${archive}" >/dev/null 2>&1; then
            warn "Redis 镜像文件无法解压，删除缓存后从官方地址重试"
            rm -f -- "${archive}"
            download_resumable "${origin_url}" "${archive}"
        fi
    fi
    tar -xzf "${archive}" -C "${WORK_DIR}"
    make -C "${source_dir}" -j "$(nproc)"
    make -C "${source_dir}" PREFIX="${target}" install
    link_directory "${target}" /usr/local/redis

    cat > /etc/profile.d/yc-redis.sh <<'EOF'
export PATH="/usr/local/redis/bin:$PATH"
EOF
    chmod 644 /etc/profile.d/yc-redis.sh
    export PATH="/usr/local/redis/bin:${PATH}"

    getent group redis >/dev/null || groupadd --system redis
    if ! id redis >/dev/null 2>&1; then
        useradd --system --gid redis --home-dir /var/lib/redis --shell "$(command -v nologin)" redis
    fi
    mkdir -p /etc/redis /var/lib/redis /run/redis
    chown -R redis:redis /var/lib/redis /run/redis

    cat > /etc/redis/redis.conf <<'EOF'
bind 127.0.0.1 ::1
protected-mode yes
port 6379
daemonize no
supervised no
dir /var/lib/redis
appendonly yes
EOF

    cat > /etc/systemd/system/yc-redis.service <<EOF
[Unit]
Description=YC Redis Server
After=network.target

[Service]
Type=simple
User=redis
Group=redis
ExecStart=${target}/bin/redis-server /etc/redis/redis.conf
ExecStop=${target}/bin/redis-cli shutdown
Restart=on-failure
RestartSec=5
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
}

install_redis() {
    local existing_version
    existing_version="$(redis_existing_version)"
    if [[ -n "${existing_version}" ]]; then
        log "发现现有 Redis，按保护策略跳过安装：${existing_version}"
        enable_existing_redis_service
        return
    fi

    log "未发现 Redis，使用当前系统的软件包管理器安装"
    if ! install_redis_package; then
        install_redis_from_source
    fi

    existing_version="$(redis_existing_version)"
    [[ -n "${existing_version}" ]] || die "Redis 安装完成后仍找不到 redis-server"
    enable_existing_redis_service
    log "Redis 安装完成：${existing_version}"
}

print_summary() {
    log "基础环境安装检查完成"
    printf '%s\n' \
        "Java: $(java -version 2>&1 | awk 'NR == 1 {first=$0} END {print first}')" \
        "Maven: $(mvn -v 2>/dev/null | awk 'NR == 1 {first=$0} END {print first}')" \
        "MySQL: $(mysql_existing_version)" \
        "Redis: $(redis_existing_version)"

    if [[ -f "${CREDENTIAL_FILE}" ]]; then
        printf '\n新生成的数据库凭据保存在：%s\n' "${CREDENTIAL_FILE}"
        printf '请把其中的 YC_MYSQL_* 填入 /etc/yc-page/yc-page.env。\n'
    fi
    printf '重新登录 Shell 后 JAVA_HOME 和 Maven PATH 会自动生效。\n'
}

main() {
    require_root
    [[ "$(uname -s)" == "Linux" ]] || die "此脚本只能在 Linux 上运行"
    WORK_DIR="$(mktemp -d /tmp/yc-stack.XXXXXX)"
    trap cleanup EXIT
    mkdir -p "${CACHE_DIR}"
    chmod 700 "${CACHE_DIR}"

    install_os_packages
    if command -v python3 >/dev/null; then
        JSON_PYTHON="$(command -v python3)"
    elif command -v python >/dev/null; then
        JSON_PYTHON="$(command -v python)"
    else
        die "未找到 Python，无法解析 JDK 官方元数据"
    fi
    install_jdk
    install_maven
    install_mysql
    install_redis
    print_summary
}
