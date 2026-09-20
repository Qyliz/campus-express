package cn.njust.campusexpress.model.order.dto;

import cn.njust.campusexpress.common.enums.ExceptionTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

//上报异常DTO
@Data
public class ReportExceptionDTO {

    @NotNull
    private ExceptionTypeEnum type;

    @NotBlank
    @Size(max = 255)
    private String description;
}
