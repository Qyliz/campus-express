package cn.njust.campusexpress.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    /**
     * 主键是 MyBatis-Plus 的雪花 ID（19 位 Long，约 1.9e18），超出 JS 的
     * Number.MAX_SAFE_INTEGER（2^53-1）。直接以 JSON 数字下发会被前端 JSON.parse 舍位，
     * 管理端就会拿着错的 id 去封禁/踢人/重置密码——操作到错误的行，或者报"用户不存在"。
     * 统一序列化成字符串，前端当不透明字符串原样回传即可（Jackson 能把字符串反序列化回 Long）。
     *
     * 只注册 Long.class：PageResult 的 total/current/size/pages 是原始 long，
     * 再注册 Long.TYPE 会把它们也变成字符串，前端分页组件会收到错的类型。
     */
    @Bean
    public JsonMapperBuilderCustomizer longToStringSerializer() {
        return builder -> builder.addModule(
                new SimpleModule().addSerializer(Long.class, ToStringSerializer.instance));
    }
}
