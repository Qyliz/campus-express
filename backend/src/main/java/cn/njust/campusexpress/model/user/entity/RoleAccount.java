package cn.njust.campusexpress.model.user.entity;

import cn.njust.campusexpress.common.enums.UserStatusEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * 角色账户表的公共结构，对应 customer / courier / admin 三张同构表。
 * 抽象基类不能加 @TableName，表名由各具体子类按驼峰转下划线推导。
 */
@Data
public abstract class RoleAccount {
    private Long id;
    private Long userId;
    private UserStatusEnum status;
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableLogic(value = "0", delval = "id")
    private Long deleted;
}
