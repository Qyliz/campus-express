package cn.njust.campusexpress.model.user.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import lombok.Data;

import java.util.Date;

@Data
public class UserBanRecordVO {
    private Long userBanRecordId;
    private Long userRoleId;
    private String username;
    private UserGenderEnum gender;
    private String phone;
    private String email;
    //0=封禁中，1=已解封
    private Integer unbanned;
    private String reason;
    private Date createTime;
}
