package cn.njust.campusexpress;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 测试用的唯一手机号生成器。
 *
 * user.phone 是 NOT NULL 且有 uk_phone(phone, deleted) 唯一键，所以每个测试内新建的
 * User fixture 都必须带一个不重复的手机号 —— 直接 users.save() 绕过 bean validation，
 * 撞上的会是数据库约束而不是校验消息。
 *
 * 号段刻意用 14 开头：既满足 ^1[3-9]\d{9}$，又避开 db/data.sql 的 135(管理员)/136(种子用户)
 * 和各测试文件里固定的 137/138/139。
 * 必须随机而非固定：RecipientExceptionTest、ReviewFlowTest、OrderTransactionTest 不是事务测试、
 * 靠 @AfterEach 手工清理，一旦上次运行有残留，固定值就会撞唯一键。
 */
public final class TestPhones {

    private TestPhones() {
    }

    public static String next() {
        return "14" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
