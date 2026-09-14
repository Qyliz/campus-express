package cn.njust.campusexpress.model.user.dto;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserGenderDTO {
    @NotNull(message = "性别不能为空")
    private UserGenderEnum gender;
}
