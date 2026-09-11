package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import cn.njust.campusexpress.common.enums.OrderStatusEnum;
@Data
public class OrderQueryDTO {
    @Min(1) private Integer currentPage = 1;
    private OrderStatusEnum orderStatus;
    @Positive private Long orderId;
    @NotNull @Pattern(regexp="all|created|received") private String relation = "all";
}
