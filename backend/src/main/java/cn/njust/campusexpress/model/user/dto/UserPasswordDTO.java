package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@SuppressWarnings("unused")
@Data
public class UserPasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;

    public void setOldPassword(String oldPassword) {
        this.oldPassword = StringUtil.clean(oldPassword);
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = StringUtil.clean(newPassword);
    }
}
