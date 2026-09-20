package cn.njust.campusexpress.model.order.dto;

import cn.njust.campusexpress.common.enums.ExceptionResolutionEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

//处理异常DTO
@Data
public class ResolveExceptionDTO {

    @NotNull
    private ExceptionResolutionEnum resolution;
    
    @NotBlank
    @Size(max = 255)
    private String description;
}
