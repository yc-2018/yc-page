ALTER TABLE `mini_checkin_share`
  ADD COLUMN `comments_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT '是否允许评论 0-否 1-是' AFTER `include_imgs`,
  ADD COLUMN `comments_public` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '是否公开全部评论 0-否 1-是' AFTER `comments_enabled`,
  ADD COLUMN `share_title` varchar(30) NOT NULL DEFAULT '' COMMENT '多地分享名称，其他类型为空' AFTER `content`,
  ADD COLUMN `point_count` smallint unsigned NOT NULL DEFAULT 0 COMMENT '多地分享地点数量，其他类型为0' AFTER `share_title`;

UPDATE `mini_checkin_share`
SET `comments_enabled` = 0,
    `comments_public` = 0
WHERE `share_type` = 'static';

UPDATE `mini_checkin_share`
SET `share_title` = CASE
        WHEN JSON_VALID(`content`) THEN COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`content`, '$.title')), '')
        ELSE ''
    END,
    `point_count` = CASE
        WHEN JSON_VALID(`content`) THEN COALESCE(JSON_LENGTH(JSON_EXTRACT(`content`, '$.recordIds')), 0)
        ELSE 0
    END
WHERE `share_type` = 'multi';

CREATE TABLE IF NOT EXISTS `mini_checkin_comment` (
    `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `record_id` int unsigned NOT NULL COMMENT '打卡记录ID',
    `user_openid` varchar(255) NOT NULL COMMENT '评论人openid',
    `content` varchar(200) NOT NULL COMMENT '评论内容',
    `root_id` int unsigned NOT NULL DEFAULT 0 COMMENT '顶级评论ID，顶级评论为0',
    `reply_to_comment_id` int unsigned NOT NULL DEFAULT 0 COMMENT '直接回复的评论ID，顶级评论为0',
    `reply_to_openid` varchar(255) NOT NULL DEFAULT '' COMMENT '直接回复对象openid，顶级评论为空字符串',
    `is_deleted` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '软删除标记 0-正常 1-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_record_root_time` (`record_id`, `root_id`, `create_time`, `id`),
    KEY `idx_root_time` (`root_id`, `create_time`, `id`),
    KEY `idx_user_time` (`user_openid`, `create_time`),
    KEY `idx_reply_to_openid` (`reply_to_openid`, `is_deleted`, `root_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序打卡评论';
