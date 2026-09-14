-- 种子数据：一个管理员 + 一批便于手工测试的账号、订单、异常、评价与申诉。
--
-- 注意：spring.sql.init.mode=always 会让本脚本在应用每次启动、每次跑测试时都执行，
--       因此必须幂等（可重复运行），也绝不能在这里写 TRUNCATE/DELETE —— 那会变成每次启动清库。
--       清空数据是手工一次性操作，不属于本脚本。
--
-- 幂等做法：每张表一条 INSERT ... SELECT，派生表(UNION ALL)提供行数据，
--           LEFT JOIN 目标表反连接按主键逐行判重，只插入当前缺失的行。
--           所以手工删掉某几条种子数据后重启，只有那几条会被补回来。
--
-- id 约定：MyBatis-Plus 雪花算法(ASSIGN_ID)生成的是 19 位大整数，永远不会落在 0~99，
--          因此种子数据固定占用这个保留区段，与运行时新建的数据不会冲突。
--
-- id 区段分配总表（改种子必须同步维护这张表）：
--   表                   占用id
--   user                 0(管理员)、1~8(种子用户)
--   customer             1~4
--   courier              1~5
--   admin                0
--   user_audit_record    1~4
--   user_ban_record      1~2
--   express_order        1~11
--   order_status_record  1~51
--   delivery_exception   1~3
--   service_review       1~4
--   review_appeal        1~2
--
-- 密码：所有种子账号统一为 IamADMIN，复用下面这个已验证的 BCrypt($2a$) 哈希。
--
-- 时间列：全部表都有 DEFAULT CURRENT_TIMESTAMP，账号类行(user/customer/courier/admin/
--         审核/封禁)不写时间由默认值填充；订单/流转/异常/评价/申诉类行的 create_time
--         承载业务时间线，必须显式给出并保证每张订单内部单调递增。
--
-- 联系方式号段规划（user.phone 是 NOT NULL + 唯一键，撞号就会静默少插一行种子数据）：
--   135 段：管理员，固定 13500000000
--   136 段：本文件的种子用户 13600000001～13600000008，以及种子订单里出现的未注册收件人 1361111xxxx
--   137/138/139 段：测试代码自己用的固定号，本文件一律避开
--   14  段：测试里随机生成的 fixture 手机号（ThreadLocalRandom），本文件一律避开
-- 邮箱同理避开 @test.com / @example.com / @email.com / @review.test，
-- 唯一沿用的是管理员的 admin@email.com —— 现有测试要凭它登录。

-- ============================================================================
-- 1) 管理员 user 行：同邮箱的活跃账号不存在时插入，固定 id=0
--    username/gender/phone 均为 NOT NULL，必须显式提供（gender=0 即 UNKNOWN）
-- ============================================================================
INSERT INTO `user` (`id`, `username`, `gender`, `phone`, `email`, `password`, `deleted`)
SELECT s.`id`, s.`username`, s.`gender`, s.`phone`, s.`email`, s.`password`, s.`deleted`
FROM (SELECT 0                                                              AS `id`,
             'admin'                                                        AS `username`,
             0                                                              AS `gender`,
             '13500000000'                                                  AS `phone`,
             'admin@email.com'                                              AS `email`,
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu' AS `password`,
             0                                                              AS `deleted`) s
         LEFT JOIN `user` t ON t.`email` = s.`email` AND t.`deleted` = 0
WHERE t.`id` IS NULL;

-- 2) 管理员 admin 账户行：id=0，user_id 按邮箱回查真实值；status=1(NORMAL)
INSERT INTO `admin` (`id`, `user_id`, `status`, `deleted`)
SELECT 0, u.`id`, 1, 0
FROM `user` u
         LEFT JOIN `admin` x ON x.`user_id` = u.`id` AND x.`deleted` = 0
WHERE u.`email` = 'admin@email.com'
  AND u.`deleted` = 0
  AND x.`id` IS NULL;

-- ============================================================================
-- 3) 测试用 user 行。gender：0未知 1男 2女。
--    覆盖：多角色（吴九同时是收寄件人和配送员）、被封禁账号（郑十）、
--          审核中与已驳回的配送员（孙七、周八 —— 用于验证登录时的驳回原因提示）。
-- ============================================================================
INSERT INTO `user` (`id`, `username`, `gender`, `phone`, `email`, `password`, `deleted`)
SELECT s.`id`, s.`username`, s.`gender`, s.`phone`, s.`email`, s.`password`, s.`deleted`
FROM (SELECT 1                                                              AS `id`,
             '张三'                                                         AS `username`,
             1                                                              AS `gender`,
             '13600000001'                                                  AS `phone`,
             'zhangsan@campus.com'                                          AS `email`,
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu' AS `password`,
             0                                                              AS `deleted`
      UNION ALL
      SELECT 2, '李四', 2, '13600000002', 'lisi@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 3, '王五', 1, '13600000003', 'wangwu@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 4, '赵六', 2, '13600000004', 'zhaoliu@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 5, '孙七', 1, '13600000005', 'sunqi@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 6, '周八', 2, '13600000006', 'zhouba@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 7, '吴九', 1, '13600000007', 'wujiu@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
      UNION ALL
      SELECT 8, '郑十', 2, '13600000008', 'zhengshi@campus.com',
             '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0) s
         LEFT JOIN `user` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- 4) 收寄件人账户。status：0禁用 1正常 2审核中 3审核驳回。
--    郑十(user 8)是 DISABLED，与下面 user_ban_record 里未解封的那条对应。
INSERT INTO `customer` (`id`, `user_id`, `status`, `deleted`)
SELECT s.`id`, s.`user_id`, s.`status`, s.`deleted`
FROM (SELECT 1 AS `id`, 1 AS `user_id`, 1 AS `status`, 0 AS `deleted`
      UNION ALL
      SELECT 2, 2, 1, 0
      UNION ALL
      SELECT 3, 7, 1, 0
      UNION ALL
      SELECT 4, 8, 0, 0) s
         LEFT JOIN `customer` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- 5) 配送员账户。孙七审核中、周八已驳回、吴九与收寄件人身份并存（多角色）。
INSERT INTO `courier` (`id`, `user_id`, `status`, `deleted`)
SELECT s.`id`, s.`user_id`, s.`status`, s.`deleted`
FROM (SELECT 1 AS `id`, 3 AS `user_id`, 1 AS `status`, 0 AS `deleted`
      UNION ALL
      SELECT 2, 4, 1, 0
      UNION ALL
      SELECT 3, 5, 2, 0
      UNION ALL
      SELECT 4, 6, 3, 0
      UNION ALL
      SELECT 5, 7, 1, 0) s
         LEFT JOIN `courier` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- 6) 配送员审核记录。status 用 AuditStatusEnum：1已通过 2审核中 3已驳回。
--    周八那条已驳回并写了原因 —— 用他的账号登录时应看到「审核被驳回，原因：…」。
--    material 指向 backend/upload/audit/ 下的图片，前端用 el-image 加 #error 兜底，
--    文件不存在时显示占位图而不是碎图标。
INSERT INTO `user_audit_record` (`id`, `courier_id`, `status`, `reason`, `material`)
SELECT s.`id`, s.`courier_id`, s.`status`, s.`reason`, s.`material`
FROM (SELECT 1                               AS `id`,
             1                               AS `courier_id`,
             1                               AS `status`,
             '材料齐全，审核通过'             AS `reason`,
             '/upload/audit/seed-wangwu.png' AS `material`
      UNION ALL
      SELECT 2, 2, 1, '材料齐全，审核通过', '/upload/audit/seed-zhaoliu.png'
      UNION ALL
      SELECT 3, 3, 2, NULL, '/upload/audit/seed-sunqi.png'
      UNION ALL
      SELECT 4, 4, 3, '审核材料照片模糊，请重新上传清晰的学生证', '/upload/audit/seed-zhouba.png') s
         LEFT JOIN `user_audit_record` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- 7) 封禁记录。role 用 UserRoleEnum：0管理员 1收寄件人 2配送员。
--    一条封禁中(unbanned=0)、一条已解封(unbanned=1)，让管理端两种筛选都有数据。
INSERT INTO `user_ban_record` (`id`, `user_id`, `role`, `unbanned`, `reason`)
SELECT s.`id`, s.`user_id`, s.`role`, s.`unbanned`, s.`reason`
FROM (SELECT 1 AS `id`, 8 AS `user_id`, 1 AS `role`, 0 AS `unbanned`, '发布违禁物品配送订单' AS `reason`
      UNION ALL
      SELECT 2, 1, 1, 1, '多次恶意取消订单，已教育后解封') s
         LEFT JOIN `user_ban_record` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- ============================================================================
-- 8) 订单。十一张单覆盖全部七个状态，version 与该单实际经历的更新次数一致
--    （每次 updateById 都会让 @Version 递增），这样乐观锁的行为和真实数据一样。
--    order_status/payment_status 的列注释里记着枚举 code 对照表，本文件的种子值就是那些数字。
--
--    收件手机号刻意分两种，用于验证「收件人不必注册也能被匹配到」：
--      · 单1/3/6/7/8 收件手机号属于已注册用户 ⇒ 该用户可在「我的订单」按 RECEIVED 看到
--        其中单6 的收件人是张三(user 1)而不是下单人李四，用于演示「非下单人确认取件」
--      · 单2 用未注册的 13611112222、单5 用未注册的 13611113333 ⇒ 无人能凭联系方式认领
--    单9/10/11 是完整走完流程的已完成订单，专门给 service_review / review_appeal 当载体。
-- ============================================================================
INSERT INTO `express_order` (`id`, `customer_id`, `courier_id`, `pickup_address`, `pickup_name`, `pickup_phone`,
                             `delivery_address`, `delivery_name`, `delivery_phone`,
                             `item_description`, `remark`, `fee`, `order_status`, `payment_status`, `version`,
                             `create_time`, `update_time`)
SELECT s.`id`, s.`customer_id`, s.`courier_id`, s.`pickup_address`, s.`pickup_name`, s.`pickup_phone`,
       s.`delivery_address`, s.`delivery_name`, s.`delivery_phone`,
       s.`item_description`, s.`remark`, s.`fee`, s.`order_status`, s.`payment_status`, s.`version`,
       s.`create_time`, s.`update_time`
FROM (SELECT 1                     AS `id`,
             1                     AS `customer_id`,
             CAST(NULL AS SIGNED)  AS `courier_id`,
             '紫金校区3号宿舍楼'   AS `pickup_address`,
             '张三'                AS `pickup_name`,
             '13600000001'         AS `pickup_phone`,
             '一食堂二楼'          AS `delivery_address`,
             '李四'                AS `delivery_name`,
             '13600000002'         AS `delivery_phone`,
             '一箱课本（约5kg）'    AS `item_description`,
             CAST(NULL AS CHAR)    AS `remark`,
             8.00                  AS `fee`,
             0                     AS `order_status`,
             0                     AS `payment_status`,
             0                     AS `version`,
             '2026-09-10 09:00:00' AS `create_time`,
             '2026-09-10 09:00:00' AS `update_time`
      UNION ALL
      -- 单2：已支付待接单，收件人未注册
      SELECT 2, 1, NULL, '紫金校区快递驿站', '张三', '13600000001',
             '图书馆自习室', '陈老师', '13611112222',
             '一袋打印资料', NULL, 5.50, 1, 1, 1,
             '2026-09-09 14:20:00', '2026-09-09 14:25:00'
      UNION ALL
      -- 单3：配送员接单后上报异常，停在待揽收
      SELECT 3, 2, 1, '梅园小区5栋', '李四', '13600000002',
             '紫金校区3号宿舍楼', '张三', '13600000001',
             '一台笔记本电脑', '贵重物品，请轻拿轻放', 15.00, 2, 1, 3,
             '2026-09-09 10:00:00', '2026-09-09 16:40:00'
      UNION ALL
      -- 单4：赵六配送中
      SELECT 4, 1, 2, '校医院门口', '张三', '13600000001',
             '体育馆器材室', '王五', '13600000003',
             '一箱矿泉水', NULL, 10.00, 3, 1, 3,
             '2026-09-08 16:00:00', '2026-09-09 09:30:00'
      UNION ALL
      -- 单5：待取件，收件人未注册所以没人能确认取件
      SELECT 5, 3, 1, '南门菜鸟驿站', '吴九', '13600000007',
             '实验楼A301', '同学', '13611113333',
             '一个快递包裹', '到了打电话', 6.00, 4, 1, 4,
             '2026-09-08 11:00:00', '2026-09-08 18:00:00'
      UNION ALL
      -- 单6：完整走完全流程，中途异常被管理员恢复，最后由收件人张三（非下单人）确认取件
      SELECT 6, 2, 2, '紫金校区2号宿舍楼', '李四', '13600000002',
             '教学楼B305', '张三', '13600000001',
             '一份实验报告', NULL, 4.00, 5, 1, 7,
             '2026-09-07 08:30:00', '2026-09-08 10:00:00'
      UNION ALL
      -- 单7：已支付后取消，同步退款
      SELECT 7, 1, NULL, '紫金校区3号宿舍楼', '张三', '13600000001',
             '一食堂', '李四', '13600000002',
             '一杯奶茶', NULL, 3.00, 6, 2, 2,
             '2026-09-06 15:00:00', '2026-09-06 15:20:00'
      UNION ALL
      -- 单8：配送员上报违禁品异常，管理员处理为取消订单
      SELECT 8, 3, 1, '北门快递柜', '吴九', '13600000007',
             '行政楼201', '李四', '13600000002',
             '一箱实验试剂', NULL, 20.00, 6, 2, 5,
             '2026-09-05 09:00:00', '2026-09-06 11:00:00'
      UNION ALL
      -- 单9：张三下单、王五配送，顺利完成 —— 承载评价2
      SELECT 9, 1, 1, '紫金校区快递驿站', '张三', '13600000001',
             '研究生楼A栋', '陈同学', '13611114444',
             '一袋水果', '放到楼下快递柜', 6.50, 5, 1, 5,
             '2026-09-04 09:00:00', '2026-09-04 19:00:00'
      UNION ALL
      -- 单10：李四下单、赵六配送，顺利完成 —— 承载评价3（已作废）与申诉1（申诉成立）
      SELECT 10, 2, 2, '梅园小区5栋', '李四', '13600000002',
             '紫金校区3号宿舍楼', '张三', '13600000001',
             '一双运动鞋', NULL, 5.00, 5, 1, 5,
             '2026-09-05 10:00:00', '2026-09-05 20:00:00'
      UNION ALL
      -- 单11：吴九下单、王五配送，顺利完成 —— 承载评价4与申诉2（待处理）
      SELECT 11, 3, 1, '南门菜鸟驿站', '吴九', '13600000007',
             '实验楼B402', '郑十', '13600000008',
             '一个键盘', '易碎，轻拿轻放', 7.00, 5, 1, 5,
             '2026-09-06 09:00:00', '2026-09-06 16:00:00') s
         LEFT JOIN `express_order` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- ============================================================================
-- 9) 订单状态流转记录。节点时间统一从这里读，所以每张单的时间线必须完整且单调递增。
--    operator_id 引用 user 表主键（不是角色账户主键），operator_role 用 UserRoleEnum 码。
--    上报异常和处理异常这两类记录的 from_status 与 to_status 相同 ——
--    它们不推进配送阶段，只是暂停或恢复，代码里就是这么写的。
--    单9/10/11 是无异常的完整时间线：创建->支付->接单->揽收->送达->确认取件。
-- ============================================================================
INSERT INTO `order_status_record` (`id`, `order_id`, `from_status`, `to_status`, `operator_id`, `operator_role`,
                                   `description`, `create_time`)
SELECT s.`id`, s.`order_id`, s.`from_status`, s.`to_status`, s.`operator_id`, s.`operator_role`,
       s.`description`, s.`create_time`
FROM (SELECT 1                     AS `id`,
             1                     AS `order_id`,
             CAST(NULL AS SIGNED)  AS `from_status`,
             0                     AS `to_status`,
             1                     AS `operator_id`,
             1                     AS `operator_role`,
             '创建订单'            AS `description`,
             '2026-09-10 09:00:00' AS `create_time`
      -- 单2：创建 -> 模拟支付，进入待接单
      UNION ALL
      SELECT 2, 2, NULL, 0, 1, 1, '创建订单', '2026-09-09 14:20:00'
      UNION ALL
      SELECT 3, 2, 0, 1, 1, 1, '支付成功', '2026-09-09 14:25:00'
      -- 单3：李四下单并支付，王五接单后上报异常，停在待揽收
      UNION ALL
      SELECT 4, 3, NULL, 0, 2, 1, '创建订单', '2026-09-09 10:00:00'
      UNION ALL
      SELECT 5, 3, 0, 1, 2, 1, '支付成功', '2026-09-09 10:05:00'
      UNION ALL
      SELECT 6, 3, 1, 2, 3, 2, '配送员接单', '2026-09-09 11:30:00'
      UNION ALL
      SELECT 7, 3, 2, 2, 3, 2, '配送员上报异常，配送暂停', '2026-09-09 16:40:00'
      -- 单4：张三下单，赵六接单并揽收，配送中
      UNION ALL
      SELECT 8, 4, NULL, 0, 1, 1, '创建订单', '2026-09-08 16:00:00'
      UNION ALL
      SELECT 9, 4, 0, 1, 1, 1, '支付成功', '2026-09-08 16:02:00'
      UNION ALL
      SELECT 10, 4, 1, 2, 4, 2, '配送员接单', '2026-09-08 17:10:00'
      UNION ALL
      SELECT 11, 4, 2, 3, 4, 2, '配送员确认揽收', '2026-09-09 09:30:00'
      -- 单5：吴九下单，王五一路送到待取件，收件人未注册所以没人能确认取件
      UNION ALL
      SELECT 12, 5, NULL, 0, 7, 1, '创建订单', '2026-09-08 11:00:00'
      UNION ALL
      SELECT 13, 5, 0, 1, 7, 1, '支付成功', '2026-09-08 11:03:00'
      UNION ALL
      SELECT 14, 5, 1, 2, 3, 2, '配送员接单', '2026-09-08 12:00:00'
      UNION ALL
      SELECT 15, 5, 2, 3, 3, 2, '配送员确认揽收', '2026-09-08 14:30:00'
      UNION ALL
      SELECT 16, 5, 3, 4, 3, 2, '配送员确认送达', '2026-09-08 18:00:00'
      -- 单6：完整走完全流程，中途上报异常又被管理员恢复，最后由收件人张三（非下单人）确认取件
      UNION ALL
      SELECT 17, 6, NULL, 0, 2, 1, '创建订单', '2026-09-07 08:30:00'
      UNION ALL
      SELECT 18, 6, 0, 1, 2, 1, '支付成功', '2026-09-07 08:35:00'
      UNION ALL
      SELECT 19, 6, 1, 2, 4, 2, '配送员接单', '2026-09-07 09:20:00'
      UNION ALL
      SELECT 20, 6, 2, 3, 4, 2, '配送员确认揽收', '2026-09-07 14:00:00'
      UNION ALL
      SELECT 21, 6, 3, 3, 4, 2, '配送员上报异常，配送暂停', '2026-09-07 15:30:00'
      UNION ALL
      SELECT 22, 6, 3, 3, 0, 0, '异常处理：恢复配送', '2026-09-07 17:00:00'
      UNION ALL
      SELECT 23, 6, 3, 4, 4, 2, '配送员确认送达', '2026-09-08 09:00:00'
      UNION ALL
      SELECT 24, 6, 4, 5, 1, 1, '收寄件人确认取件', '2026-09-08 10:00:00'
      -- 单7：张三支付后在接单前自行取消，已支付所以标记退款
      UNION ALL
      SELECT 25, 7, NULL, 0, 1, 1, '创建订单', '2026-09-06 15:00:00'
      UNION ALL
      SELECT 26, 7, 0, 1, 1, 1, '支付成功', '2026-09-06 15:05:00'
      UNION ALL
      SELECT 27, 7, 1, 6, 1, 1, '临时有事，不需要配送了', '2026-09-06 15:20:00'
      -- 单8：王五揽收后发现违禁品上报异常，管理员处理为取消订单
      UNION ALL
      SELECT 28, 8, NULL, 0, 7, 1, '创建订单', '2026-09-05 09:00:00'
      UNION ALL
      SELECT 29, 8, 0, 1, 7, 1, '支付成功', '2026-09-05 09:05:00'
      UNION ALL
      SELECT 30, 8, 1, 2, 3, 2, '配送员接单', '2026-09-05 10:30:00'
      UNION ALL
      SELECT 31, 8, 2, 3, 3, 2, '配送员确认揽收', '2026-09-05 16:00:00'
      UNION ALL
      SELECT 32, 8, 3, 3, 3, 2, '配送员上报异常，配送暂停', '2026-09-06 10:00:00'
      UNION ALL
      SELECT 33, 8, 3, 6, 0, 0, '异常处理：取消订单', '2026-09-06 11:00:00'
      -- 单9：张三下单，王五配送，当天完成
      UNION ALL
      SELECT 34, 9, NULL, 0, 1, 1, '创建订单', '2026-09-04 09:00:00'
      UNION ALL
      SELECT 35, 9, 0, 1, 1, 1, '支付成功', '2026-09-04 09:05:00'
      UNION ALL
      SELECT 36, 9, 1, 2, 3, 2, '配送员接单', '2026-09-04 10:00:00'
      UNION ALL
      SELECT 37, 9, 2, 3, 3, 2, '配送员确认揽收', '2026-09-04 14:00:00'
      UNION ALL
      SELECT 38, 9, 3, 4, 3, 2, '配送员确认送达', '2026-09-04 18:00:00'
      UNION ALL
      SELECT 39, 9, 4, 5, 1, 1, '收寄件人确认取件', '2026-09-04 19:00:00'
      -- 单10：李四下单，赵六配送，当天完成
      UNION ALL
      SELECT 40, 10, NULL, 0, 2, 1, '创建订单', '2026-09-05 10:00:00'
      UNION ALL
      SELECT 41, 10, 0, 1, 2, 1, '支付成功', '2026-09-05 10:10:00'
      UNION ALL
      SELECT 42, 10, 1, 2, 4, 2, '配送员接单', '2026-09-05 11:00:00'
      UNION ALL
      SELECT 43, 10, 2, 3, 4, 2, '配送员确认揽收', '2026-09-05 15:00:00'
      UNION ALL
      SELECT 44, 10, 3, 4, 4, 2, '配送员确认送达', '2026-09-05 18:30:00'
      UNION ALL
      SELECT 45, 10, 4, 5, 2, 1, '收寄件人确认取件', '2026-09-05 20:00:00'
      -- 单11：吴九下单，王五配送，当天完成
      UNION ALL
      SELECT 46, 11, NULL, 0, 7, 1, '创建订单', '2026-09-06 09:00:00'
      UNION ALL
      SELECT 47, 11, 0, 1, 7, 1, '支付成功', '2026-09-06 09:02:00'
      UNION ALL
      SELECT 48, 11, 1, 2, 3, 2, '配送员接单', '2026-09-06 09:30:00'
      UNION ALL
      SELECT 49, 11, 2, 3, 3, 2, '配送员确认揽收', '2026-09-06 11:00:00'
      UNION ALL
      SELECT 50, 11, 3, 4, 3, 2, '配送员确认送达', '2026-09-06 15:00:00'
      UNION ALL
      SELECT 51, 11, 4, 5, 7, 1, '收寄件人确认取件', '2026-09-06 16:00:00') s
         LEFT JOIN `order_status_record` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- ============================================================================
-- 10) 配送异常。type 用 ExceptionTypeEnum 的 code：0联系不上 1地址问题 2物品问题 3配送员突发 4其他
--     status 用 ExceptionStatusEnum：0待处理 1已处理
--     resolution 用 ExceptionResolutionEnum：0恢复配送 1取消订单（未处理时为 NULL）
--     一条待处理（单3，卡住揽收和送达）、一条恢复（单6，后续走完了全流程）、
--     一条取消（单8，同步退款）。
--     admin_id 引用 admin 表主键，种子管理员是 0。
-- ============================================================================
INSERT INTO `delivery_exception` (`id`, `order_id`, `courier_id`, `type`, `description`, `status`,
                                  `admin_id`, `resolution`, `resolution_description`, `create_time`, `resolved_time`)
SELECT s.`id`, s.`order_id`, s.`courier_id`, s.`type`, s.`description`, s.`status`,
       s.`admin_id`, s.`resolution`, s.`resolution_description`, s.`create_time`, s.`resolved_time`
FROM (SELECT 1                                   AS `id`,
             3                                   AS `order_id`,
             1                                   AS `courier_id`,
             0                                   AS `type`,
             '收件人电话一直无人接听，已尝试三次' AS `description`,
             0                                   AS `status`,
             CAST(NULL AS SIGNED)                AS `admin_id`,
             CAST(NULL AS SIGNED)                AS `resolution`,
             CAST(NULL AS CHAR)                  AS `resolution_description`,
             '2026-09-09 16:40:00'               AS `create_time`,
             CAST(NULL AS DATETIME)              AS `resolved_time`
      UNION ALL
      SELECT 2, 6, 2, 1, '教学楼B305门禁需要刷卡，无法进入', 1, 0, 0,
             '已联系收件人下楼取件，恢复配送', '2026-09-07 15:30:00', '2026-09-07 17:00:00'
      UNION ALL
      SELECT 3, 8, 1, 2, '开箱发现是实验试剂，疑似易燃易爆违禁品', 1, 0, 1,
             '确认为违禁物品，取消订单并退款', '2026-09-06 10:00:00', '2026-09-06 11:00:00') s
         LEFT JOIN `delivery_exception` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- ============================================================================
-- 11) 服务评价。status 用 ReviewStatusEnum：0有效 1已作废（申诉成立时作废）。
--     同一张已完成订单上，下单人和收件人可以各评一次（唯一键 order_id+user_id）。
--     单6 的评价1、单9 的评价2、单11 的评价4 保持有效；
--     单10 的评价3 因申诉成立已被作废，但内容和申诉处理记录都保留。
--     create_time 必须晚于对应订单的完成时间（见第 9 节时间线）。
-- ============================================================================
INSERT INTO `service_review` (`id`, `order_id`, `user_id`, `courier_id`, `rating`, `content`, `status`, `create_time`)
SELECT s.`id`, s.`order_id`, s.`user_id`, s.`courier_id`, s.`rating`, s.`content`, s.`status`, s.`create_time`
FROM (SELECT 1                          AS `id`,
             6                          AS `order_id`,
             2                          AS `user_id`,
             2                          AS `courier_id`,
             5                          AS `rating`,
             '配送很及时，实验报告完好无损，谢谢！' AS `content`,
             0                          AS `status`,
             '2026-09-08 12:00:00'      AS `create_time`
      UNION ALL
      SELECT 2, 9, 1, 1, 4, '准时放到了快递柜，就是比约定晚了一点', 0, '2026-09-04 19:30:00'
      UNION ALL
      SELECT 3, 10, 2, 2, 2, '鞋盒有明显压痕，配送过程恐怕不太小心', 1, '2026-09-05 20:30:00'
      UNION ALL
      SELECT 4, 11, 7, 1, 3, '键盘包装完好，但配送耗时比预期长', 0, '2026-09-06 16:30:00') s
         LEFT JOIN `service_review` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;

-- ============================================================================
-- 12) 评价申诉。status 用 AppealStatusEnum：0待处理 1申诉成立 2已驳回。
--     申诉1 对评价3（单10）：申诉成立，评价已作废（与第 11 节的 status=1 自洽），
--     处理人是种子管理员 admin_id=0；
--     申诉2 对评价4（单11）：待处理 —— 给管理端申诉页留一条待办。
--     admin_id/resolution_reason/resolved_time 在未处理时为 NULL。
-- ============================================================================
INSERT INTO `review_appeal` (`id`, `review_id`, `order_id`, `courier_id`, `reason`, `status`,
                             `admin_id`, `resolution_reason`, `create_time`, `resolved_time`)
SELECT s.`id`, s.`review_id`, s.`order_id`, s.`courier_id`, s.`reason`, s.`status`,
       s.`admin_id`, s.`resolution_reason`, s.`create_time`, s.`resolved_time`
FROM (SELECT 1                          AS `id`,
             3                          AS `review_id`,
             10                         AS `order_id`,
             2                          AS `courier_id`,
             '鞋盒压痕是驿站堆压所致，揽收时有照片为证，并非配送问题' AS `reason`,
             1                          AS `status`,
             0                          AS `admin_id`,
             '核实揽收照片后认定评价描述不实，申诉成立，评价作废'     AS `resolution_reason`,
             '2026-09-05 21:00:00'      AS `create_time`,
             '2026-09-06 09:00:00'      AS `resolved_time`
      UNION ALL
      SELECT 2, 4, 11, 1,
             '当天订单量较大，已尽力在承诺时间内送达，希望核实评价',
             0, CAST(NULL AS SIGNED), CAST(NULL AS CHAR),
             '2026-09-06 17:00:00', CAST(NULL AS DATETIME)) s
         LEFT JOIN `review_appeal` t ON t.`id` = s.`id`
WHERE t.`id` IS NULL;
