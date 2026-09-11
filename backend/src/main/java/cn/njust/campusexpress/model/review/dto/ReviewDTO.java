package cn.njust.campusexpress.model.review.dto;
import jakarta.validation.constraints.*;
public record ReviewDTO(@NotNull @Min(1) @Max(5) Integer rating, @NotBlank String content) {}
