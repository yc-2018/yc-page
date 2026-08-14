#!/usr/bin/env bash

# CentOS 7 一键安装 yc-page 后端所需基础环境。
# 已存在的软件会报告版本并跳过，不会覆盖现有 MySQL/Redis 数据。

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=install-stack-common.sh
source "${SCRIPT_DIR}/install-stack-common.sh"

check_operating_system() {
    [[ -r /etc/os-release ]] || die "无法识别操作系统"
    # shellcheck source=/dev/null
    source /etc/os-release
    [[ "${ID:-}" == "centos" && "${VERSION_ID:-}" == 7* ]] \
        || die "此脚本只支持 CentOS 7，当前为 ${PRETTY_NAME:-未知系统}"
}

repair_centos_repositories_if_needed() {
    local backup_dir
    local repo

    if yum -q makecache >/dev/null 2>&1; then
        return
    fi

    # CentOS 7 已停止维护，公共镜像不可用时切换到已验证可访问的归档镜像。
    warn "当前 CentOS 7 软件源不可用，切换到 archive.kernel.org 的 CentOS 7.9.2009 归档"
    backup_dir="/etc/yum.repos.d/backup-before-yc-$(date +%Y%m%d%H%M%S)"
    mkdir -p "${backup_dir}"

    for repo in /etc/yum.repos.d/CentOS-*.repo; do
        [[ -f "${repo}" ]] || continue
        cp -a "${repo}" "${backup_dir}/"
        sed -i \
            -e 's|^mirrorlist=|#mirrorlist=|' \
            -e 's|^#baseurl=http://mirror.centos.org/centos/\$releasever|baseurl=https://archive.kernel.org/centos-vault/7.9.2009|' \
            -e 's|^#baseurl=https://mirror.centos.org/centos/\$releasever|baseurl=https://archive.kernel.org/centos-vault/7.9.2009|' \
            "${repo}"
    done

    yum clean all
    yum -y makecache || die "CentOS Vault 软件源仍不可用，请检查服务器网络和 DNS"
}

install_os_packages() {
    check_operating_system
    repair_centos_repositories_if_needed
    log "安装 CentOS 7 基础工具和 MySQL 运行库"
    yum -y install \
        ca-certificates curl tar gzip xz unzip openssl git python \
        procps-ng iproute libaio numactl-libs ncurses-libs \
        gcc gcc-c++ make tcl
    update-ca-trust
}

install_redis_package() {
    # 优先使用 EPEL；CentOS 7 的 EPEL 已归档，失败时公共脚本会改用源码安装。
    yum -y install epel-release >/dev/null 2>&1 || return 1
    yum -y install redis || return 1
}

main "$@"
