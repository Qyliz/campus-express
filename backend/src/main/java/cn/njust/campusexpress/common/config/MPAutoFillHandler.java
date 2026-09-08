package cn.njust.campusexpress.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MPAutoFillHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        //实体 updateTime 为 java.util.Date，类型需与字段一致 strictUpdateFill 才会生效
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
