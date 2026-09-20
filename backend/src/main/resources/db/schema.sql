-- 建表脚本
-- 幂等：统一使用 CREATE TABLE IF NOT EXISTS
CREATE TABLE IF NOT EXISTS `user`
(
    `id`          bigint       NOT NULL COMMENT '主键id',
    `username`    varchar(50)  NOT NULL COMMENT '用户名',
    `gender`      tinyint      NOT NULL COMMENT 'UserGenderEnum：0未知 1男 2女',
    `avatar`      varchar(255)          DEFAULT NULL COMMENT '头像',
    `phone`       varchar(20)  NOT NULL COMMENT '手机号',
    `email`       varchar(255)          DEFAULT NULL COMMENT '邮箱',
    `password`    varchar(100) NOT NULL COMMENT 'BCrypt密码',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bigint       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`, `deleted`),
    UNIQUE KEY `uk_email` (`email`, `deleted`)
)
    COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS `customer`
(
    `id`          bigint   NOT NULL COMMENT '主键id',
    `user_id`     bigint   NOT NULL COMMENT 'user表id',
    `status`      tinyint  NOT NULL COMMENT 'UserStatusEnum：0禁用 1正常 2审核中 3审核驳回',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bigint   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    CONSTRAINT `fk_customer_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
)
    COMMENT ='收寄件人账户表';

CREATE TABLE IF NOT EXISTS `courier`
(
    `id`          bigint   NOT NULL COMMENT '主键id',
    `user_id`     bigint   NOT NULL COMMENT 'user表id',
    `status`      tinyint  NOT NULL COMMENT 'UserStatusEnum：0禁用 1正常 2审核中 3审核驳回',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bigint   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    CONSTRAINT `fk_courier_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
)
    COMMENT ='配送员账户表';

CREATE TABLE IF NOT EXISTS `admin`
(
    `id`          bigint   NOT NULL COMMENT '主键id',
    `user_id`     bigint   NOT NULL COMMENT 'user表id',
    `status`      tinyint  NOT NULL COMMENT 'UserStatusEnum：0禁用 1正常 2审核中 3审核驳回',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bigint   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`, `deleted`),
    CONSTRAINT `fk_admin_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
)
    COMMENT ='管理员账户表';

CREATE TABLE IF NOT EXISTS `user_audit_record`
(
    `id`          bigint   NOT NULL COMMENT '主键id',
    `courier_id`  bigint   NOT NULL COMMENT '配送员账户id',
    `status`      tinyint  NOT NULL DEFAULT 2 COMMENT 'AuditStatusEnum：1已通过 2审核中 3已驳回',
    `reason`      varchar(100)      DEFAULT NULL COMMENT '审核意见',
    `material`    varchar(255)      DEFAULT NULL COMMENT '审核材料图片',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_courier` (`courier_id`),
    CONSTRAINT `fk_user_audit_record` FOREIGN KEY (`courier_id`) REFERENCES `courier` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
)
    COMMENT ='账户审核记录表';

CREATE TABLE IF NOT EXISTS `user_ban_record`
(
    `id`          bigint     NOT NULL COMMENT '主键id',
    `user_id`     bigint     NOT NULL COMMENT '被封禁用户的id',
    `role`        tinyint    NOT NULL COMMENT 'UserRoleEnum：0管理员 1收寄件人 2配送员',
    `unbanned`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否解封：0封禁中 1已解封',
    `reason`      varchar(100)        DEFAULT NULL COMMENT '封禁原因',
    `create_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '封禁时间',
    `update_time` datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ban_user` (`user_id`),
    CONSTRAINT `fk_user_ban_record` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
)
    COMMENT ='账户封禁记录表';

-- 配送订单：地址和联系人保留下单时的值。节点时间统一保存在 order_status_record 中。
-- 收件人只留手机号一种联系方式，绑定该手机号的收寄件人即可在「我的订单」里认领这张单。
CREATE TABLE IF NOT EXISTS `express_order`
(
    `id`               bigint        NOT NULL COMMENT '主键id',
    `customer_id`      bigint        NOT NULL COMMENT '下单收寄件人账户id',
    `courier_id`       bigint                 DEFAULT NULL COMMENT '接单配送员账户id，接单前为NULL',
    `pickup_address`   varchar(255)  NOT NULL COMMENT '取件地址',
    `pickup_name`      varchar(50)   NOT NULL COMMENT '取件联系人',
    `pickup_phone`     varchar(20)   NOT NULL COMMENT '取件联系电话',
    `delivery_address` varchar(255)  NOT NULL COMMENT '收件地址',
    `delivery_name`    varchar(50)   NOT NULL COMMENT '收件联系人',
    `delivery_phone`   varchar(20)   NOT NULL COMMENT '收件联系电话',
    `item_description` varchar(255)  NOT NULL COMMENT '物品描述',
    `remark`           varchar(255)           DEFAULT NULL COMMENT '备注',
    `fee`              decimal(6, 2) NOT NULL COMMENT '配送费',
    `order_status`     tinyint       NOT NULL DEFAULT 0 COMMENT 'OrderStatusEnum：0待支付 1待接单 2待揽收 3配送中 4待取件 5已完成 6已取消',
    `payment_status`   tinyint       NOT NULL DEFAULT 0 COMMENT 'PaymentStatusEnum：0未支付 1已支付 2已退款',
    `version`          int           NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，每次成功更新加一',
    `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_customer_time` (`customer_id`, `create_time`),
    KEY `idx_order_courier_time` (`courier_id`, `create_time`),
    KEY `idx_order_status_time` (`order_status`, `create_time`),
    KEY `idx_order_delivery_phone` (`delivery_phone`, `create_time`),
    CONSTRAINT `fk_order_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_order_courier` FOREIGN KEY (`courier_id`) REFERENCES `courier` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
    COMMENT ='配送订单表';

CREATE TABLE IF NOT EXISTS `delivery_exception`
(
    `id`                     bigint       NOT NULL COMMENT '主键id',
    `order_id`               bigint       NOT NULL COMMENT '订单id',
    `courier_id`             bigint       NOT NULL COMMENT '上报异常的配送员账户id',
    `type`                   tinyint      NOT NULL COMMENT 'ExceptionTypeEnum：0联系不上 1地址问题 2物品问题 3配送员突发情况 4其他',
    `description`            varchar(255) NOT NULL COMMENT '异常描述',
    `status`                 tinyint      NOT NULL DEFAULT 0 COMMENT 'ExceptionStatusEnum：0待处理 1已处理',
    `admin_id`               bigint                DEFAULT NULL COMMENT '处理异常的管理员账户id，未处理为NULL',
    `resolution`             tinyint               DEFAULT NULL COMMENT 'ExceptionResolutionEnum：0恢复配送 1取消订单，未处理为NULL',
    `resolution_description` varchar(255)          DEFAULT NULL COMMENT '处理说明，未处理为NULL',
    `create_time`            datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    `resolved_time`          datetime              DEFAULT NULL COMMENT '处理时间，未处理为NULL',
    PRIMARY KEY (`id`),
    KEY `idx_exception_order` (`order_id`, `create_time`, `id`),
    KEY `idx_exception_status` (`status`, `create_time`, `id`),
    CONSTRAINT `fk_exception_order` FOREIGN KEY (`order_id`) REFERENCES `express_order` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `fk_exception_courier` FOREIGN KEY (`courier_id`) REFERENCES `courier` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_exception_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
    COMMENT ='配送异常表';

CREATE TABLE IF NOT EXISTS `order_status_record`
(
    `id`            bigint   NOT NULL COMMENT '主键id',
    `order_id`      bigint   NOT NULL COMMENT '订单id',
    `from_status`   tinyint           DEFAULT NULL COMMENT 'OrderStatusEnum，流转前状态；创建订单时为NULL',
    `to_status`     tinyint  NOT NULL COMMENT 'OrderStatusEnum，流转后状态',
    `operator_id`   bigint   NOT NULL COMMENT '操作人id（user表主键，非角色账户主键）',
    `operator_role` tinyint  NOT NULL COMMENT 'UserRoleEnum：0管理员 1收寄件人 2配送员',
    `description`   varchar(255)      DEFAULT NULL COMMENT '操作说明',
    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_record_time` (`order_id`, `create_time`, `id`),
    CONSTRAINT `fk_order_record_order` FOREIGN KEY (`order_id`) REFERENCES `express_order` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `fk_order_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
    COMMENT ='订单状态流转记录表';

-- 服务评价：同一用户在同一订单最多评价一次，下单人和收件人都可以评。
CREATE TABLE IF NOT EXISTS `service_review`
(
    `id`          bigint       NOT NULL COMMENT '主键id',
    `order_id`    bigint       NOT NULL COMMENT '订单id',
    `user_id`     bigint       NOT NULL COMMENT '评价人id（user表主键）',
    `courier_id`  bigint       NOT NULL COMMENT '被评价的配送员账户id，冗余自订单',
    `rating`      tinyint      NOT NULL COMMENT '评分1~5星',
    `content`     varchar(500) NOT NULL COMMENT '评价内容',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT 'ReviewStatusEnum：0有效 1已作废（申诉成立时作废）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_order_user` (`order_id`, `user_id`),
    KEY `idx_review_courier` (`courier_id`),
    CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `express_order` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `fk_review_courier` FOREIGN KEY (`courier_id`) REFERENCES `courier` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
    COMMENT ='服务评价表';

-- 配送员对评价的申诉：一条评价最多申诉一次。申诉成立时原评价作废，但保留评价与全部处理信息。
CREATE TABLE IF NOT EXISTS `review_appeal`
(
    `id`                bigint       NOT NULL COMMENT '主键id',
    `review_id`         bigint       NOT NULL COMMENT '被申诉的评价id',
    `order_id`          bigint       NOT NULL COMMENT '订单id，冗余自评价，仅用于查询（见文件头外键策略）',
    `courier_id`        bigint       NOT NULL COMMENT '申诉配送员账户id，冗余自评价，仅用于查询（见文件头外键策略）',
    `reason`            varchar(500) NOT NULL COMMENT '申诉理由',
    `status`            tinyint      NOT NULL DEFAULT 0 COMMENT 'AppealStatusEnum：0待处理 1申诉成立 2已驳回',
    `admin_id`          bigint                DEFAULT NULL COMMENT '处理申诉的管理员账户id，未处理为NULL',
    `resolution_reason` varchar(500)          DEFAULT NULL COMMENT '处理说明，未处理为NULL',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
    `resolved_time`     datetime              DEFAULT NULL COMMENT '处理时间，未处理为NULL',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_appeal_review` (`review_id`),
    KEY `idx_appeal_order` (`order_id`),
    KEY `idx_appeal_courier` (`courier_id`),
    KEY `idx_appeal_status_time` (`status`, `create_time`, `id`),
    CONSTRAINT `fk_appeal_review` FOREIGN KEY (`review_id`) REFERENCES `service_review` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
    CONSTRAINT `fk_appeal_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
)
    COMMENT ='评价申诉表';
