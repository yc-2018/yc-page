package ikun.yc.ycpage.common;

import ikun.yc.ycpage.common.exception.EnumNotFoundException;
import ikun.yc.ycpage.entity.enumeration.LinkType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * [枚举转换器]字符串到链接类型转换器
 *
 * @author cgl
 * @since 2025/11/06 22:06:06
 */
@Component
public class StringToLinkTypeConverter implements Converter<String, LinkType> {
    
    @Override
    public LinkType convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return LinkType.SEARCH; // 默认值
        }
        try {
            Integer code = Integer.parseInt(source);
            return LinkType.of(code);
        } catch (NumberFormatException e) {
            // 如果传的是枚举名称而不是数字，也支持
            try {
                return LinkType.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new EnumNotFoundException("链接类型不存在！");
            }
        }
    }
}