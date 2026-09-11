package cn.njust.campusexpress.model.order.dto;
import cn.njust.campusexpress.common.enums.ExceptionTypeEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ReportExceptionDTO {
    /** 取值由枚举限定，非法类型会在参数绑定阶段被拒，不需要再写 @Pattern 白名单。 */
    @NotNull private ExceptionTypeEnum type;
    @NotBlank @Size(max=255) private String description;
}
