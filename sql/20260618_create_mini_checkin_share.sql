CREATE TABLE IF NOT EXISTS `mini_checkin_share` (
    `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '分享ID',
    `user_openid` varchar(255) NOT NULL COMMENT '创建人openid',
    `record_id` int unsigned NOT NULL COMMENT '原打卡记录ID',
    `expire_time` datetime NOT NULL COMMENT '有效截止时间',
    `share_type` varchar(10) NOT NULL DEFAULT 'dynamic' COMMENT '分享类型 dynamic-动态 static-静态',
    `include_remark` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT '是否包含备注 0-否 1-是',
    `include_imgs` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT '是否包含图片 0-否 1-是',
    `content` longtext NOT NULL COMMENT '动态分享存记录ID，静态分享存记录JSON',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_openid` (`user_openid`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_expire_time` (`expire_time`)
) COMMENT='小程序打卡分享表';
