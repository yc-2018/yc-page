#!/usr/bin/env bash

# 使用 runJava.sh 的同一套停服、启动、端口检查和日志轮转逻辑回滚版本。
set -Eeuo pipefail

RUN_JAVA_SCRIPT="${RUN_JAVA_SCRIPT:-/root/runJava.sh}" # 已安装的主部署脚本

if [[ ! -x "${RUN_JAVA_SCRIPT}" ]]; then
    echo "部署脚本不存在或不可执行: ${RUN_JAVA_SCRIPT}" >&2
    echo "请先执行: install -m 700 deploy/runJava.sh /root/runJava.sh" >&2
    exit 1
fi

exec "${RUN_JAVA_SCRIPT}" --rollback
