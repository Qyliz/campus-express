package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCodeEnum {
    //成功
    SUCCESS(0, "操作成功"),
    //通用错误
    PARAM_ERROR(1001, "参数校验失败"),
    PARAM_MISSING(1002, "缺少必要参数"),
    UNAUTHORIZED(1003, "未登录或登录已过期"),
    //用户模块
    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    ACCOUNT_DISABLED(2003, "账号已被禁用"),
    PHONE_ALREADY_BIND(2004, "该手机号已被注册"),
    EMAIL_ALREADY_BIND(2005, "该手机号已被注册"),
    //系统错误
    SYSTEM_ERROR(9999, "系统繁忙，请稍候再试");

    private final Integer code;
    private final String message;
}
