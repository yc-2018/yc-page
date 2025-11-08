package ikun.yc.ycpage.entity.enumeration;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ikun.yc.ycpage.common.exception.EnumNotFoundException;
import ikun.yc.ycpage.entity.UserConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 链接类型
 *
 * @author cgl
 * @since 2025/11/07 00:39:07
 */
@Getter
@AllArgsConstructor
public enum LinkType {
    SEARCH(0, UserConfig::getSearchSort),
    LOW_SEARCH(1, UserConfig::getLowSearchSort),
    HOME_LINK(2, UserConfig::getHomeBookmarkSort);

    @EnumValue
    @JsonValue  // 序列化时使用这个值
    private final int code;
    private final SFunction<UserConfig, String> fieldMapper;

    // Jackson 反序列化时使用
    @JsonCreator
    public static LinkType fromCode(int code) {
        return of(code);
    }

    public static LinkType of(Integer code) {
        if (code == null) return SEARCH;
        for (LinkType type : values()) {
            if (type.code == code) return type;
        }
        throw new EnumNotFoundException("链接类型不存在");
    }

}
