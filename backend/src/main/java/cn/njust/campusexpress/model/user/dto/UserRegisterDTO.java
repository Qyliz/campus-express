package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{1,10}$", message = "用户名必须是1-10位的中文、字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    @NotNull(message = "角色不能为空")
    private UserRoleEnum role;

    @NotNull(message = "性别不能为空")
    private UserGenderEnum gender;

    /** 手机号必填；空串会被归一成 null，保证「只填空格」稳定地由 @NotBlank 拒绝而不是撞上 @Pattern。 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱选填；留空时归一成 null，VerifyCodeService.verifyRegistration 才会跳过邮箱那一项。 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱长度不能超过254个字符")
    private String email;

    @Pattern(regexp = "^\\d{6}$", message = "手机验证码必须为6位数字")
    private String phoneCode;

    @Pattern(regexp = "^\\d{6}$", message = "邮箱验证码必须为6位数字")
    private String emailCode;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public void setPhone(String phone) {
        this.phone = clean(phone);
    }

    public void setEmail(String email) {
        this.email = clean(email);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
