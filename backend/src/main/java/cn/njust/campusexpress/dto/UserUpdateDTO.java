package cn.njust.campusexpress.dto;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class UserUpdateDTO {
    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]{1,10}$", message = "用户名必须是1-10位的中文、字母、数字或下划线")
    private String username;

    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;

    @NotNull(message = "性别不能为空")
    private UserGenderEnum gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱长度不能超过254个字符")
    private String email;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
