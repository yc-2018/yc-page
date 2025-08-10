package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * (search_engines)实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@TableName("search_engines")
@EqualsAndHashCode(callSuper = false)
public class SearchEngines extends Model<SearchEngines> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 搜索引擎ID  */
    @TableId(type = IdType.AUTO)
	private Integer id;

    /** 搜索引擎URL  */
    @Pattern(regexp = "^https?://.*$", message = "搜索引擎URL格式不正确")
    private String engineUrl;

    /** 不常用 1是 0否  */
    private Integer lowUsage;

    /** 名称  */
    @NotNull(message = "名称不可为空")  // 验证非空且长度 > 0（去空格后）
    private String name;

    /** 图标URL  */
    private String iconUrl;

    /** 用户id  */
    @JsonIgnore
    private String userId;

    @JsonIgnore
    @TableField(fill = FieldFill.UPDATE, select = false)
    private LocalDateTime updateTime;
}