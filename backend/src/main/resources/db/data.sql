-- 用户模块种子数据
-- 注意：spring.sql.init.mode=always 会让本脚本在应用每次启动时都执行，因此必须幂等（可重复运行）。
-- 用途：预置一个管理员账号，供 UpdateInfoTest / AdminUserTest 等测试登录（admin@email.com / IamADMIN）。
-- 策略：仅当对应记录不存在时才插入，并且按邮箱回查真实 user.id，
--       避免与现网已存在的管理员冲突，也不会触发外键错误。
-- id 约定：种子管理员固定使用 id=0（user 与 admin 各自的表主键都置 0，互不冲突）。
--          MyBatis-Plus 雪花算法不会生成 0，因此 0 是安全的保留种子值，与本机现有管理员 id 一致。

-- 1) 管理员 user 行：同邮箱的活跃账号不存在时插入，固定 id=0
--    username/gender 已上移到 user 表且均为 NOT NULL，必须显式提供（gender=0 即 UNKNOWN）
INSERT INTO `user` (`id`, `username`, `gender`, `email`, `password`, `deleted`)
SELECT 0, 'admin', 0, 'admin@email.com', '$2a$10$65nnhWnpd/AxlmDIhawipuSIlrSb2ZL57C5Li86.hsDSkU1fVcUBu', 0
WHERE NOT EXISTS (
    SELECT 1 FROM `user` WHERE `email` = 'admin@email.com' AND `deleted` = 0
);

-- 2) 管理员 admin 账户行：id=0，user_id 按邮箱回查真实值；status=1(NORMAL)
INSERT INTO `admin` (`id`, `user_id`, `status`, `deleted`)
SELECT 0, u.`id`, 1, 0
FROM `user` u
WHERE u.`email` = 'admin@email.com'
  AND u.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1 FROM `admin` x WHERE x.`user_id` = u.`id` AND x.`deleted` = 0
  );
