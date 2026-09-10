package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.AuditStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserAuditDTO {
    @NotNull(message = "审核结果不能为空")
    private AuditStatusEnum status;

    private String reason;
}
