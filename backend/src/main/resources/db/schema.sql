-- 用户模块建表脚本（角色账户三表拆分版）
-- 幂等：统一使用 CREATE TABLE IF NOT EXISTS，应用重复启动不会报错。
-- 说明：
--   1) 主键 id 为 BIGINT 且非自增，由 MyBatis-Plus 雪花算法(ASSIGN_ID)在应用侧生成。
--   2) 建表顺序遵循外键依赖：user -> customer/courier/admin -> user_audit_record / user_ban_record。
--   3) username/gender/avatar 属于「人」的共有资料，统一放在 user 主表；
--      三张角色账户表结构对称，只保留该身份的账户状态，一个 user 在每张表至多一行有效数据。
--   4) 逻辑删除列 deleted 参与唯一索引，配合 @TableLogic(delval="id") 实现软删后可重新注册。
--   5) 外键约束名在 MySQL 中是库级唯一的，三张角色表的约束名必须互不相同；索引名是表级的，可以重复。

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL COMMENT '主键id',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `gender` tinyint NOT NULL COMMENT '性别',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`,`deleted`),
  UNIQUE KEY `uk_email` (`email`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `customer` (
  `id` bigint NOT NULL COMMENT '主键id',
  `user_id` bigint NOT NULL COMMENT 'user表id',
  `status` tinyint NOT NULL COMMENT '账户状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`,`deleted`),
  CONSTRAINT `fk_customer_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收寄件人账户表';

CREATE TABLE IF NOT EXISTS `courier` (
  `id` bigint NOT NULL COMMENT '主键id',
  `user_id` bigint NOT NULL COMMENT 'user表id',
  `status` tinyint NOT NULL COMMENT '账户状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`,`deleted`),
  CONSTRAINT `fk_courier_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配送员账户表';

CREATE TABLE IF NOT EXISTS `admin` (
  `id` bigint NOT NULL COMMENT '主键id',
  `user_id` bigint NOT NULL COMMENT 'user表id',
  `status` tinyint NOT NULL COMMENT '账户状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`,`deleted`),
  CONSTRAINT `fk_admin_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员账户表';

CREATE TABLE IF NOT EXISTS `user_audit_record` (
  `id` bigint NOT NULL COMMENT '主键id',
  `courier_id` bigint NOT NULL COMMENT '配送员账户id',
  `status` tinyint NOT NULL DEFAULT '2' COMMENT '审核状态',
  `reason` varchar(100) DEFAULT NULL COMMENT '审核意见',
  `material` varchar(255) DEFAULT NULL COMMENT '审核材料图片',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_user_audit_record` (`courier_id`),
  CONSTRAINT `fk_user_audit_record` FOREIGN KEY (`courier_id`) REFERENCES `courier` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户审核记录表';

CREATE TABLE IF NOT EXISTS `user_ban_record` (
  `id` bigint NOT NULL COMMENT '主键id',
  `user_id` bigint NOT NULL COMMENT '被封禁用户的id',
  `role` tinyint NOT NULL COMMENT '被封禁账户的角色',
  `unbanned` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否解封',
  `reason` varchar(100) DEFAULT NULL COMMENT '封禁原因',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁时间',
  PRIMARY KEY (`id`),
  KEY `fk_user_ban_record` (`user_id`),
  CONSTRAINT `fk_user_ban_record` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户封禁记录表';
