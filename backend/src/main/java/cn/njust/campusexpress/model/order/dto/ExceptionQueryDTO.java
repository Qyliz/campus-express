package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ExceptionQueryDTO {
    @NotNull @Min(1) private Integer currentPage = 1;
    @Min(0) @Max(1) private Integer status;
    @Positive private Long orderId;
}
