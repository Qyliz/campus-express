package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//换绑邮箱DTO
@Data
public class ChangeEmailDTO {

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱长度不能超过254个字符")
    private String newEmail;

    @NotBlank(message = "验证码不能为空")
    private String code;

    public void setNewEmail(String newEmail) {
        this.newEmail = StringUtil.clean(newEmail);
    }

    public void setCode(String code) {
        this.code = StringUtil.clean(code);
    }
}
