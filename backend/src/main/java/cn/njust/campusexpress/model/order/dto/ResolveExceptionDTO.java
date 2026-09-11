package cn.njust.campusexpress.model.order.dto;
import cn.njust.campusexpress.common.enums.ExceptionResolutionEnum;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ResolveExceptionDTO {
    /** 取值由枚举限定，非法处理结果会在参数绑定阶段被拒，不需要再写 @Pattern 白名单。 */
    @NotNull private ExceptionResolutionEnum resolution;
    @NotBlank @Size(max=255) private String description;
}
