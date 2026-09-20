package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;

    public void setAccount(String account) {
        this.account = StringUtil.clean(account);
    }

    public void setCode(String code) {
        this.code = StringUtil.clean(code);
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = StringUtil.clean(newPassword);
    }
}
