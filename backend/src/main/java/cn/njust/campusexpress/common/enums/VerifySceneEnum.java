package cn.njust.campusexpress.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 验证码使用场景。不同场景的验证码相互隔离，不可跨场景复用。
 */
@Getter
@AllArgsConstructor
public enum VerifySceneEnum {
    FORGOT_PASSWORD("忘记密码"),
    CHANGE_PHONE("换绑手机号"),
    CHANGE_EMAIL("换绑邮箱");

    private final String description;
}
