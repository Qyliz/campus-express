package cn.njust.campusexpress;

import java.util.concurrent.ThreadLocalRandom;

//生成满足手机号格式和唯一约束的测试手机号
public final class TestPhones {

    private TestPhones() {
    }

    public static String next() {
        return "14" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }
}
