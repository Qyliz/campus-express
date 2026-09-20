package cn.njust.campusexpress.model.order.dto;

import cn.njust.campusexpress.common.util.StringUtil;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

//创建订单DTO
@Data
public class CreateOrderDTO {

    @NotBlank
    @Size(max = 255)
    private String pickupAddress;

    @NotBlank
    @Size(max = 50)
    private String pickupName;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "取件手机号格式不正确")
    private String pickupPhone;

    @NotBlank
    @Size(max = 255)
    private String deliveryAddress;

    @NotBlank
    @Size(max = 50)
    private String deliveryName;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收件手机号格式不正确")
    private String deliveryPhone;

    @NotBlank
    @Size(max = 255)
    private String itemDescription;

    @Size(max = 255)
    private String remark;

    @NotNull
    @DecimalMin("0.01")
    @DecimalMax("9999.99")
    @Digits(integer = 4, fraction = 2)
    private BigDecimal fee;

    public void setDeliveryPhone(String value) {
        deliveryPhone = StringUtil.clean(value);
    }
}
