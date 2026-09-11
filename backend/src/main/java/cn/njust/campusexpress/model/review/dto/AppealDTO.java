package cn.njust.campusexpress.model.review.dto;
import jakarta.validation.constraints.*;
public record AppealDTO(@NotBlank String reason) {}
