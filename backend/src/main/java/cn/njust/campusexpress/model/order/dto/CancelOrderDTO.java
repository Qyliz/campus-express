package cn.njust.campusexpress.model.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//取消订单DTO
@Data
public class CancelOrderDTO {
    @NotBlank(message = "请填写取消原因")
    @Size(max = 255)
    private String reason;
}
