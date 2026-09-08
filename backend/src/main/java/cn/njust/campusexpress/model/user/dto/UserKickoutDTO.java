package cn.njust.campusexpress.model.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserKickoutDTO {
    @NotNull(message = "id不能为空")
    private Long userRoleId;
}
