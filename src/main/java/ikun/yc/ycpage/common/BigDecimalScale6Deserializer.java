package ikun.yc.ycpage.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalScale6Deserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            // 关键点：统一读取为 String，再转为 BigDecimal
            String rawValue = p.getText(); // 处理数值和字符串类型
            BigDecimal value = new BigDecimal(rawValue);

            // 直接截断 （若需四舍五入到 6 位小数，使用 RoundingMode.HALF_UP）
            return value.setScale(6, RoundingMode.DOWN);
        } catch (NumberFormatException  e) {
            // 抛出明确错误信息，帮助前端调试
            throw new JsonProcessingException("无效的数值格式: " + p.getText(), e) {};
        }
    }
}