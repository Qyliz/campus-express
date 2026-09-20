package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//管理员重置账号密码DTO
@Data
public class AdminResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;

    public void setNewPassword(String newPassword) {
        this.newPassword = StringUtil.clean(newPassword);
    }
}
