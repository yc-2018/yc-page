ALTER TABLE `memo` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE `loop_memo_item` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE `loop_memo_item_comment` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE `bookmarks` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE `search_engines` ADD COLUMN `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE `user_config` ADD COLUMN `sort_version` int NOT NULL DEFAULT 0 COMMENT '搜索排序版本号';
