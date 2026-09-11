package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class CreateOrderDTO {
    @NotBlank @Size(max=255) private String pickupAddress;
    @NotBlank @Size(max=50) private String pickupName;
    @NotBlank @Pattern(regexp="^1[3-9]\\d{9}$", message="取件手机号格式不正确") private String pickupPhone;
    @NotBlank @Size(max=255) private String deliveryAddress;
    @NotBlank @Size(max=50) private String deliveryName;
    @NotBlank @Pattern(regexp="^1[3-9]\\d{9}$", message="收件手机号格式不正确") private String deliveryPhone;
    //空串归一成 null，保证「只填空格」稳定地由 @NotBlank 拒绝，而不是撞上 @Pattern 报格式错误。
    public void setDeliveryPhone(String value) { deliveryPhone = clean(value); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    @NotBlank @Size(max=255) private String itemDescription;
    @Size(max=255) private String remark;
    @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") @Digits(integer=4, fraction=2)
    private BigDecimal fee;
}
