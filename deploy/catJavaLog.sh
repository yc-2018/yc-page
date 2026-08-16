#!/usr/bin/env bash

set -Eeuo pipefail

LOG_FILE="/var/java/yc-page/logs/yc-page.log"
HISTORY_LOG_FILE="/var/java/yc-page/logs/yc-page-history.log"
LINES="${LINES:-200}"
MODE="${1:-current}"

case "${MODE}" in
    current)
        if [[ ! -f "${LOG_FILE}" ]]; then
            echo "当前日志不存在: ${LOG_FILE}" >&2
            echo "请先确认 yc-page 已经启动，或执行: ls -l /var/java/yc-page/logs" >&2
            exit 1
        fi

        # 默认先显示本次启动的最近 200 行，再持续跟踪；文件重建后也会自动跟随。
        tail -n "${LINES}" -F "${LOG_FILE}"
        ;;
    history)
        if [[ ! -f "${HISTORY_LOG_FILE}" ]]; then
            echo "历史日志不存在: ${HISTORY_LOG_FILE}" >&2
            echo "至少完成两次应用启动后才会产生历史日志" >&2
            exit 1
        fi

        # 历史日志只查看末尾，不持续等待后续内容。
        tail -n "${LINES}" "${HISTORY_LOG_FILE}"
        ;;
    *)
        echo "用法: $0 [current|history]" >&2
        exit 1
        ;;
esac
