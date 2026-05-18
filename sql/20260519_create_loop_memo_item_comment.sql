CREATE TABLE IF NOT EXISTS `loop_memo_item_comment` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `memo_id` int NOT NULL COMMENT '备忘主键',
  `loop_item_id` int NOT NULL COMMENT '循环记录主键',
  `comment_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `comment_text` text COMMENT '评论文本',
  `img_arr` text COMMENT '评论图片用逗号分割',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_loop_memo_item_comment_loop_item_date` (`loop_item_id`, `comment_date`, `id`),
  KEY `idx_loop_memo_item_comment_memo_id` (`memo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环备忘记录评论';
