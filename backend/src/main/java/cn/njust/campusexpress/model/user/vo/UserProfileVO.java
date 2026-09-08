package cn.njust.campusexpress.model.user.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

@Data
public class UserProfileVO {
    private String username;
    private UserRoleEnum role;
    private UserGenderEnum gender;
    private String phone;
    private String email;
    private String avatar;
}
