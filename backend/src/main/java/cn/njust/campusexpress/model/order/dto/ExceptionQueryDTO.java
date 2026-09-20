package cn.njust.campusexpress.model.order.dto;

import cn.njust.campusexpress.common.enums.ExceptionStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

//异常查询DTO
@Data
public class ExceptionQueryDTO {

    @NotNull
    @Min(1)
    private Integer currentPage = 1;
    
    private ExceptionStatusEnum status;

    @Positive
    private Long orderId;
}
