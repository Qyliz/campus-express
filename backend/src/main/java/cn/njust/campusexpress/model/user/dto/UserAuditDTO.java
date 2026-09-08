package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.UserStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserAuditDTO {
    @NotNull(message = "id不能为空")
    private Long userAuditRecordId;

    @NotNull(message = "审核结果（用户状态）不能为空")
    private UserStatusEnum status;

    private String reason;
}
