package cn.njust.campusexpress.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("unused")
@Getter
@AllArgsConstructor
public enum UserRoleEnum {
    ADMIN(0, "管理员"),
    CUSTOMER(1, "收寄件人"),
    COURIER(2, "配送员");

    @EnumValue
    private final Integer code;
    private final String description;

    //用于Sa-Token鉴权的静态常量
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_COURIER = "COURIER";
}
