ALTER TABLE `search_engines`
  ADD COLUMN `direct_url` varchar(255) DEFAULT NULL COMMENT '搜索框为空时直接访问的URL' AFTER `engine_url`;
