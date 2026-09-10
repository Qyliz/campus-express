package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ResolveExceptionDTO {
    @NotNull @Pattern(regexp="RESUME|CANCEL") private String resolution;
    @NotBlank @Size(max=255) private String description;
}
