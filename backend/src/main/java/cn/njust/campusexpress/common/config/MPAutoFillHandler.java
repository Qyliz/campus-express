package cn.njust.campusexpress.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MPAutoFillHandler implements MetaObjectHandler {
    // 插入时间统一交给数据库的 DEFAULT CURRENT_TIMESTAMP（schema.sql 的约定），
    // MyBatis-Plus 插入时 null 字段不会出现在 INSERT 列清单里，所以这里不需要填充。
    @Override
    public void insertFill(MetaObject metaObject) {

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //实体 updateTime 为 java.util.Date，类型需与字段一致 strictUpdateFill 才会生效
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
