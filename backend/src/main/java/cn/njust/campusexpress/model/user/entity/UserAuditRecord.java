package cn.njust.campusexpress.model.user.entity;

import cn.njust.campusexpress.common.enums.AuditStatusEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class UserAuditRecord {
    private Long id;
    private Long courierId;
    private AuditStatusEnum status;
    private String reason;
    private String material;
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;
}
