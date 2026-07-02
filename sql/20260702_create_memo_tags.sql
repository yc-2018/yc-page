CREATE TABLE IF NOT EXISTS `memo_tag` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID',
  `item_type` int NOT NULL COMMENT '备忘类型',
  `name` varchar(32) NOT NULL COMMENT '标签名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memo_tag_user_type_name` (`user_id`, `item_type`, `name`),
  KEY `idx_memo_tag_user_type` (`user_id`, `item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备忘类型标签';

CREATE TABLE IF NOT EXISTS `memo_tag_relation` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `memo_id` int NOT NULL COMMENT '备忘主键',
  `tag_id` int NOT NULL COMMENT '标签主键',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_memo_tag_relation_memo_tag` (`memo_id`, `tag_id`),
  KEY `idx_memo_tag_relation_tag_memo` (`tag_id`, `memo_id`),
  KEY `idx_memo_tag_relation_memo` (`memo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备忘标签关联';
