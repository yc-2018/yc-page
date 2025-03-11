package ikun.yc.ycpage.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalScale6Deserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        BigDecimal value = p.getDecimalValue();
        // 直接截断 （若需四舍五入到 6 位小数，使用 RoundingMode.HALF_UP）
        return value.setScale(6, RoundingMode.DOWN);
    }
}