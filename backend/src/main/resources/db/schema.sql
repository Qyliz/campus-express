-- 用户模块建表脚本（角色账户三表拆分版）
-- 订单表附于本文件末尾，启动时幂等创建。
-- 幂等：统一使用 CREATE TABLE IF NOT EXISTS，应用重复启动不会报错。
-- 说明：
--   1) 主键 id 为 BIGINT 且非自增，由 MyBatis-Plus 雪花算法(ASSIGN_ID)在应用侧生成。
--   2) 建表顺序遵循外键依赖：user -> customer/courier/admin -> user_audit_record / user_ban_record。
--   3) username/gender/avatar 属于「人」的共有资料，统一放在 user 主表；
--      三张角色账户表结构对称，只保留该身份的账户状态，一个 user 在每张表至多一行有效数据。
--   4) 逻辑删除列 deleted 参与唯一索引，配合 @TableLogic(delval="id") 实现软删后可重新注册。
--   5) 外键约束名在 MySQL 中是库级唯一的，三张角色表的约束名必须互不相同；索引名是表级的，可以重复。
--   6) 所有状态/类型列都是「数据库存 TINYINT 数字码、接口传枚举名」，由 MyBatis-Plus 的 @EnumValue 负责翻译。
--      列注释里的数字与枚举常量的 code 一一对应，改枚举 code 必须同步改这张表和 db/data.sql 的种子值。

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL COMMENT '主键id',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `gender` tinyint NOT NULL COMMENT '性别',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
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

-- 配送订单：地址和联系人保留下单时的值。节点时间统一保存在状态记录中。
-- 收件人只留手机号一种联系方式，绑定该手机号的收寄件人即可在「我的订单」里认领这张单。
CREATE TABLE IF NOT EXISTS express_order (
  id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  courier_id BIGINT DEFAULT NULL,
  pickup_address VARCHAR(255) NOT NULL,
  pickup_name VARCHAR(50) NOT NULL,
  pickup_phone VARCHAR(20) NOT NULL,
  delivery_address VARCHAR(255) NOT NULL,
  delivery_name VARCHAR(50) NOT NULL,
  delivery_phone VARCHAR(20) NOT NULL,
  item_description VARCHAR(255) NOT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  fee DECIMAL(6,2) NOT NULL,
  order_status TINYINT NOT NULL DEFAULT 0 COMMENT 'OrderStatusEnum：0待支付 1待接单 2待揽收 3配送中 4待取件 5已完成 6已取消',
  payment_status TINYINT NOT NULL DEFAULT 0 COMMENT 'PaymentStatusEnum：0未支付 1已支付 2已退款',
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_customer_time (customer_id, create_time),
  KEY idx_order_courier_time (courier_id, create_time),
  KEY idx_order_status_time (order_status, create_time),
  KEY idx_order_delivery_phone (delivery_phone, create_time),
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
  CONSTRAINT fk_order_courier FOREIGN KEY (courier_id) REFERENCES courier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS delivery_exception (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  courier_id BIGINT NOT NULL,
  type TINYINT NOT NULL COMMENT 'ExceptionTypeEnum：0联系不上 1地址问题 2物品问题 3配送员突发情况 4其他',
  description VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT 'ExceptionStatusEnum：0待处理 1已处理',
  admin_id BIGINT DEFAULT NULL,
  resolution TINYINT DEFAULT NULL COMMENT 'ExceptionResolutionEnum：0恢复配送 1取消订单',
  resolution_description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL,
  resolved_time DATETIME DEFAULT NULL,
  KEY idx_exception_order (order_id, create_time, id),
  KEY idx_exception_status (status, create_time, id),
  CONSTRAINT fk_exception_order FOREIGN KEY (order_id) REFERENCES express_order(id),
  CONSTRAINT fk_exception_courier FOREIGN KEY (courier_id) REFERENCES courier(id),
  CONSTRAINT fk_exception_admin FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_status_record (
  id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  from_status TINYINT DEFAULT NULL,
  to_status TINYINT NOT NULL,
  operator_id BIGINT NOT NULL,
  operator_role TINYINT NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_order_record_time (order_id, create_time, id),
  CONSTRAINT fk_order_record_order FOREIGN KEY (order_id) REFERENCES express_order(id),
  CONSTRAINT fk_order_record_operator FOREIGN KEY (operator_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 服务评价：同一用户在同一订单最多评价一次。ReviewStatusEnum：0有效、1作废。
CREATE TABLE IF NOT EXISTS service_review (
  id BIGINT NOT NULL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  courier_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT 'ReviewStatusEnum：0有效 1已作废',
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_review_order_user (order_id, user_id),
  KEY idx_review_courier (courier_id),
  CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES express_order(id),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES user(id),
  CONSTRAINT fk_review_courier FOREIGN KEY (courier_id) REFERENCES courier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- AppealStatusEnum：0待处理、1申诉成立、2已驳回。保留原评价与全部处理信息。
CREATE TABLE IF NOT EXISTS review_appeal (
  id BIGINT NOT NULL PRIMARY KEY,
  review_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  courier_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT 'AppealStatusEnum：0待处理 1申诉成立 2已驳回',
  admin_id BIGINT DEFAULT NULL,
  resolution_reason VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL,
  resolved_time DATETIME DEFAULT NULL,
  UNIQUE KEY uk_appeal_review (review_id),
  KEY idx_appeal_order (order_id),
  KEY idx_appeal_status_time (status, create_time, id),
  CONSTRAINT fk_appeal_review FOREIGN KEY (review_id) REFERENCES service_review(id),
  CONSTRAINT fk_appeal_admin FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
