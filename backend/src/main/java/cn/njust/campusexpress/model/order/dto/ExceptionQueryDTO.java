package cn.njust.campusexpress.model.order.dto;
import cn.njust.campusexpress.common.enums.ExceptionStatusEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ExceptionQueryDTO {
    @NotNull @Min(1) private Integer currentPage = 1;
    /** 为 null 表示「全部状态」。取值范围由枚举本身限定，不再需要 @Min/@Max。 */
    private ExceptionStatusEnum status;
    @Positive private Long orderId;
}
