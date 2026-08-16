CREATE TABLE IF NOT EXISTS `mini_common_phrase` (
    `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '常用语ID',
    `user_openid` varchar(64) NOT NULL COMMENT '所属用户openid',
    `content` varchar(255) NOT NULL COMMENT '常用语内容',
    `sort_order` bigint unsigned NOT NULL DEFAULT 0 COMMENT '置顶排序值，越大越靠前',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_common_phrase_user_content` (`user_openid`, `content`),
    KEY `idx_common_phrase_user_sort` (`user_openid`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序打卡常用语表';
