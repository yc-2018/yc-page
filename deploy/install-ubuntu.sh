#!/usr/bin/env bash

# Ubuntu 一键安装 yc-page 后端所需基础环境。
# 支持 Ubuntu 20.04、22.04 和 24.04；已有服务不会被覆盖。

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=install-stack-common.sh
source "${SCRIPT_DIR}/install-stack-common.sh"

check_operating_system() {
    [[ -r /etc/os-release ]] || die "无法识别操作系统"
    # shellcheck source=/dev/null
    source /etc/os-release
    [[ "${ID:-}" == "ubuntu" ]] || die "此脚本只支持 Ubuntu，当前为 ${PRETTY_NAME:-未知系统}"
    case "${VERSION_ID:-}" in
        20.04|22.04|24.04) ;;
        *) die "仅验证 Ubuntu 20.04、22.04、24.04，当前为 ${VERSION_ID:-未知版本}" ;;
    esac
}

install_os_packages() {
    local packages
    check_operating_system
    export DEBIAN_FRONTEND=noninteractive
    apt-get update

    packages=(
        ca-certificates curl tar gzip xz-utils unzip openssl git python3-minimal
        procps iproute2 libnuma1 build-essential tcl
    )

    # Ubuntu 24.04 把 libaio1 政名为 libaio1t64，其他版本仍使用 libaio1。
    if apt-cache show libaio1 >/dev/null 2>&1; then
        packages+=(libaio1)
    else
        packages+=(libaio1t64)
    fi
    # 部分 Ubuntu 版本仍提供 MySQL 5.7 可能需要的 ncurses 兼容库。
    apt-cache show libncurses5 >/dev/null 2>&1 && packages+=(libncurses5)

    log "安装 Ubuntu 基础工具和 MySQL 运行库"
    apt-get install -y --no-install-recommends "${packages[@]}"
}

install_redis_package() {
    apt-get install -y redis-server || return 1
}

main "$@"
