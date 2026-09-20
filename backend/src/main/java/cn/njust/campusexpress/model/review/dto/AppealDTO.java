package cn.njust.campusexpress.model.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//申诉评价DTO
@Data
public class AppealDTO {
    @NotBlank
    private String reason;
}
