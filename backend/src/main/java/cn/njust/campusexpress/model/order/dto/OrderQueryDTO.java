package cn.njust.campusexpress.model.order.dto;

import cn.njust.campusexpress.common.enums.OrderRelationEnum;
import cn.njust.campusexpress.common.enums.OrderStatusEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

//订单查询DTO
@Data
public class OrderQueryDTO {

    @Min(1)
    private Integer currentPage = 1;

    private OrderStatusEnum orderStatus;

    @Positive
    private Long orderId;

    @NotNull
    private OrderRelationEnum relation = OrderRelationEnum.ALL;
}
