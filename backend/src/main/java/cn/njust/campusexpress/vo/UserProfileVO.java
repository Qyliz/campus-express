package cn.njust.campusexpress.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

@Data
public class UserProfileVO {
    String username;
    UserRoleEnum role;
    UserGenderEnum gender;
    String phone;
    String email;
    String avatar;
}
