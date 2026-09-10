package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class CancelOrderDTO {
    @NotBlank(message="请填写取消原因") @Size(max=255) private String reason;
}

