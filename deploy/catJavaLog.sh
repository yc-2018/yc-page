#!/usr/bin/env bash

set -Eeuo pipefail

LOG_FILE="/var/java/yc-page/logs/yc-page.log"
LINES="${LINES:-200}"

if [[ ! -f "${LOG_FILE}" ]]; then
    echo "日志文件不存在: ${LOG_FILE}" >&2
    echo "请先确认 yc-page 已经启动，或执行: ls -l /var/java/yc-page/logs" >&2
    exit 1
fi

# 默认先显示最近 200 行，再持续跟踪；-F 可在日志文件被重建后继续读取。
tail -n "${LINES}" -F "${LOG_FILE}"
