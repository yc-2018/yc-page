package ikun.yc.ycpage.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 本地日期时间自定义序列化器
 *
 * @author cgl
 * @since  2025/03/09 22:38:09
 */
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 你可以在这里自定义逻辑，例如根据某些条件决定是否输出字段
        if (value != null) {
            gen.writeString(value.toString().replace("T", " "));
        }
    }
}