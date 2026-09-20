package cn.njust.campusexpress.model.review.dto;

import cn.njust.campusexpress.common.enums.AppealStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

//申诉查询DTO
@Data
public class AppealQueryDTO {

    @NotNull
    @Min(1)
    private Integer currentPage = 1;
    private AppealStatusEnum status;

    @Positive
    private Long orderId;
}
