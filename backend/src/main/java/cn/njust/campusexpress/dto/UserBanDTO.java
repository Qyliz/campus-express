package cn.njust.campusexpress.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserBanDTO {
    @NotNull(message = "id不能为空")
    private Long userRoleId;

    private String reason;
}
