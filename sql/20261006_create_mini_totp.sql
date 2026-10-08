CREATE TABLE IF NOT EXISTS `mini_totp` (
    `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '2FA条目ID',
    `user_openid` varchar(64) NOT NULL COMMENT '所属用户openid',
    `name` varchar(64) NOT NULL COMMENT '名称，导出链接时作为label前缀和issuer',
    `account` varchar(128) NOT NULL DEFAULT '' COMMENT '账号标签，导出链接时放在label冒号之后',
    `secret` varchar(128) NOT NULL COMMENT 'Base32密钥',
    `sort_order` bigint unsigned NOT NULL DEFAULT 0 COMMENT '置顶时间戳(毫秒)，0表示未置顶，越大越靠前',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_totp_user_secret` (`user_openid`, `secret`),
    KEY `idx_totp_user_sort` (`user_openid`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序2FA验证码表';
