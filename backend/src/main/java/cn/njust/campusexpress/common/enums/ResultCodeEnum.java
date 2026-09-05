package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCodeEnum {
    //成功
    SUCCESS(0, "操作成功"),
    //参数
    PARAM_ERROR(1, "参数校验失败"),
    PARAM_MISSING(2, "缺少必要参数"),
    //认证
    UNAUTHORIZED(1001, "未登录或登录已过期"),
    BE_REPLACED(1002, "您的账号在其他设备登录"),
    KICKED_OUT(1003, "您已被强制下线"),
    NO_PERMISSION(1004, "没有操作权限"),
    //用户模块
    USER_NOT_FOUND(2001, "用户不存在"),
    LOGIN_ERROR(2002, "用户不存在或密码错误"),
    PHONE_ALREADY_BIND(2003, "该手机号已被注册"),
    EMAIL_ALREADY_BIND(2004, "该邮箱已被注册"),
    ACCOUNT_REVIEWING(2005, "账号申请中"),
    ACCOUNT_REJECTED(2006, "账号申请被驳回，请联系管理员"),
    ACCOUNT_DISABLED(2007, "账号已被禁用"),
    ACCOUNT_REVIEWED(2008, "账号已审核"),
    // 文件模块
    FILE_EMPTY(3001, "请选择文件"),
    FILE_TOO_LARGE(3002, "文件过大，请重试"),
    FILE_TYPE_ERROR(3003, "文件类型不支持"),
    FILE_UPLOAD_ERROR(3004, "文件上传失败"),
    //系统错误
    SYSTEM_ERROR(9999, "系统繁忙，请稍候再试");

    private final Integer code;
    private final String message;
}
