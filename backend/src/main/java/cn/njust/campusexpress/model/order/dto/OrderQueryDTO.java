package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class OrderQueryDTO {
    @Min(1) private Integer currentPage = 1;
    @Min(0) @Max(6) private Integer orderStatus;
    @Positive private Long orderId;
    @NotNull @Pattern(regexp="all|created|received") private String relation = "all";
}
