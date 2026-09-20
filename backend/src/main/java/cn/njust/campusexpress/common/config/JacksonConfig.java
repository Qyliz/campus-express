package cn.njust.campusexpress.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    //将Long类型雪花ID转化为字符串
    @Bean
    public JsonMapperBuilderCustomizer longToStringSerializer() {
        return builder -> builder.addModule(
                new SimpleModule().addSerializer(Long.class, ToStringSerializer.instance));
    }
}
