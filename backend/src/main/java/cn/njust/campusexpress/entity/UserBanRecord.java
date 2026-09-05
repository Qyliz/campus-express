package cn.njust.campusexpress.entity;

import lombok.Data;

import java.util.Date;


@Data
public class UserBanRecord {
    private Long id;
    private Long userRoleId;
    private Integer unbanned;
    private String reason;
    private Date createTime;
}