package cn.njust.campusexpress.model.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置他人密码。密码存于 user 主表、由该用户名下所有角色共享，因此不需要指定角色。
 */
@SuppressWarnings("unused")
@Data
public class AdminResetPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String newPassword;

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword == null ? null : newPassword.trim();
    }
}
