package cn.njust.campusexpress.model.order.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ReportExceptionDTO {
    @NotNull @Pattern(regexp="CONTACT|ADDRESS|ITEM|COURIER|OTHER") private String type;
    @NotBlank @Size(max=255) private String description;
}
