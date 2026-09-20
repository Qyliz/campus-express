package cn.njust.campusexpress.model.review.dto;

import cn.njust.campusexpress.common.enums.AppealStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//处理申诉DTO
@Data
public class ResolveAppealDTO {
    @NotNull
    private AppealStatusEnum status;

    @NotBlank
    private String reason;
}
