package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.UserRoleEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserBanDTO {
    @NotNull(message = "用户id不能为空")
    private Long userId;

    @NotNull(message = "角色不能为空")
    private UserRoleEnum role;

    private String reason;
}
