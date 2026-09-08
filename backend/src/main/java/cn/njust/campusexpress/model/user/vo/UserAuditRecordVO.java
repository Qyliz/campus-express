package cn.njust.campusexpress.model.user.vo;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import cn.njust.campusexpress.common.enums.UserStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class UserAuditRecordVO {
    private Long userAuditRecordId;
    private String username;
    private UserGenderEnum gender;
    private String phone;
    private String email;
    private UserStatusEnum status;
    private String reason;
    private String material;
    private Date createTime;
    private Date updateTime;
}
