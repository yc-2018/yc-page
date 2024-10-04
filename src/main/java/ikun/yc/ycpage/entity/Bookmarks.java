package ikun.yc.ycpage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 书签 实体类
 *
 * @author yc
 * @since 2023-12-03 22:31:22
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)    //chain = true 生成setter方法返回this //https://blog.csdn.net/qs_xf/article/details/123876948
@EqualsAndHashCode(callSuper = false)
@TableName("bookmarks")
public class Bookmarks extends Model<Bookmarks> implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 书签ID */
  @TableId(type = IdType.AUTO)
  private Integer id;

  /** 用户ID */
  @JsonIgnore
  private String userId;

  /** 名称 */
  private String name;

  /** URL */
  private String url;

  /** 排序  书签组：所属书签排序，如：id/id/id    书签：书签组id */
  private String sort;

  /** 类型 状态0:正常书签(默认)  1:排序书签组(一个账号一个)  2:快捷图标书签 */
  private Integer type;

  /** 图标 */
  private String icon;

}