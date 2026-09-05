package cn.njust.campusexpress.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class UserProfileAdminVO {

    private String username;
    private UserRoleEnum role;
    private UserGenderEnum gender;
    private String phone;
    private String email;
    private String avatar;
    private UserStatusEnum status;
    private Date createTime;

}
