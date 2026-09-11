package cn.njust.campusexpress.model.user.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserRoleEnum;
import lombok.Data;

import java.util.Date;

@Data
public class UserBanRecordVO {
    private Long userBanRecordId;
    private Long userId;
    private UserRoleEnum role;
    private String username;
    private UserGenderEnum gender;
    private String phone;
    private String email;
    private Boolean unbanned;
    private String reason;
    private Date createTime;
}
