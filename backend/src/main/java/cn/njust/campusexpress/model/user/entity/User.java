package cn.njust.campusexpress.model.user.entity;

import cn.njust.campusexpress.common.enums.UserGenderEnum;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

/**
 * 用户主表：凭证（手机号/邮箱/密码）与共有资料（用户名/性别/头像）。
 * 一个用户可以在 customer / courier / admin 三张角色表中各持有一个账户。
 */
@Data
public class User {
    private Long id;
    private String username;
    private UserGenderEnum gender;
    private String avatar;
    private String phone;
    private String email;
    private String password;
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableLogic(value = "0", delval = "id")
    private Long deleted;
}
