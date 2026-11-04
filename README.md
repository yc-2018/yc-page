# 欢迎来到 YcPage 项目

YcPage 是一个基于 Spring Boot 的多功能后端服务项目，旨在为用户提供便捷的个人数据管理功能。该项目集成了多种常用功能模块，包括书签管理、备忘录、微信小程序登录、操作日志记录等，适用于个人工具类应用的开发。

---

## 🌟 主要功能模块

- **书签管理**：支持添加、编辑、删除和拖拽排序书签，支持自定义图标和分类。
- **备忘录系统**：支持创建、更新、删除备忘录，支持循环备忘录和打卡记录。
- **微信小程序集成**：提供微信登录、用户信息管理、打卡记录、个人常用语、打卡分享、2FA 验证码保管、WiFi 小程序码生成等功能，WiFi 码按用户每天最多生成 10 次。
- **搜索引擎配置**：支持自定义搜索引擎及其排序，适用于个性化搜索需求。
- **用户配置管理**：支持用户自定义背景、书签排序等个性化设置。
- **操作日志记录**：记录用户操作行为，便于审计和追踪。

---

## 🛠 技术栈

- **后端框架**：Spring Boot + MyBatis Plus
- **数据库**：MySQL
- **缓存**：Redis
- **安全**：JWT + Spring Security
- **日志记录**：AOP + Logback
- **其他工具**：Lombok、Validation、RedisTemplate、RestTemplate

---

## 📦 核心组件说明

### 控制器（Controller）
- `BookmarksController`：书签管理接口
- `MemoController`：备忘录管理接口
- `MiniController`：微信小程序相关接口
- `MiniCommonPhraseController`：微信小程序打卡常用语接口
- `MiniCheckinShareController`：微信小程序打卡分享接口
- `MiniSubscribeMessageController`：微信小程序一次性订阅消息授权记录接口
- `MiniTotpController`：微信小程序 2FA 验证码接口
- `MiniWifiCodeController`：微信小程序 WiFi 码生成接口
- `SearchEnginesController`：搜索引擎配置接口
- `UserController`：用户管理接口

### 服务层（Service）
- `BookmarksService`：书签业务逻辑
- `MemoService`：备忘录业务逻辑
- `MiniUserService`：微信用户管理
- `MiniCommonPhraseService`：按当前用户管理打卡常用语及置顶顺序
- `MiniCheckinShareService`：微信小程序打卡分享业务逻辑
- `MiniSubscribeMessageService`：按用户记录一次性订阅额度，并在新评论、新回复时发送订阅消息
- `MiniTotpService`：按当前用户管理 2FA 条目，名称/账号/密钥均可修改，排序值只能通过置顶接口变更
- `SearchEnginesService`：搜索引擎业务逻辑
- `UserService`：用户管理业务逻辑

### 数据访问层（Mapper）
- `BookmarksMapper`、`MemoMapper`、`MiniUserMapper`、`SearchEnginesMapper` 等：数据库操作接口

### 实体类（Entity）
- `Bookmarks`、`Memo`、`MiniUser`、`SearchEngines` 等：数据库映射实体类

### 工具类与配置
- `JwtUtils`：JWT 生成与解析工具
- `RedisConfig`：Redis 缓存配置
- `WebMvcConfig`：Web 请求拦截与格式配置
- `GlobalExceptionHandler`：全局异常处理
- `MyMetaObjectHandler`：MyBatis Plus 自动填充字段

---

## 🧪 测试与部署

### 单元测试
- `YcPageApplicationTests`：Spring Boot 应用上下文测试

### 部署建议
- 使用 `application.properties` 配置数据库、Redis、JWT 等参数
- 使用 `pom.xml` 构建项目，支持 Maven 打包部署
- 运行和构建均需要 Java 21 或更高版本；部署前必须配置至少 32 字节的 `YC_JWT_SECRET`
- CentOS 7 和 Ubuntu 的基础环境安装方式见 `deploy/INSTALL.md`
- 首次部署、环境变量配置和后续更新方式见 `deploy/DEPLOY.md`

---

## 📚 开发文档

- **日志记录**：所有操作日志记录在 `operate_log` 表中
- **缓存机制**：使用 Redis 缓存书签和搜索引擎数据，提升访问效率
- **权限控制**：通过 JWT 实现 Token 认证，部分接口需登录访问
- **打卡分享**：创建分享需登录，静态分享创建请求可通过 `staticRemark` 和 `staticImgs` 覆盖本次快照的备注与图片；空字符串表示不展示对应内容，字段缺失时兼容使用原记录内容。动态分享仍读取原记录最新内容。分享详情接口 `/mini/checkinShare/{id}` 对好友公开读取，过期后不返回打卡内容；作者详情接口 `/mini/checkinShare/owner/{id}` 支持作者查看已过期分享；详情和管理列表会返回地点类型；管理接口支持列表、续期、停用和删除自己的分享记录；管理列表可通过可选的 `shareType` 查询参数筛选 dynamic/static/multi，`/mini/checkinShare/manage/deleteExpired` 可一键清除当前用户已停用或已过期的分享并返回清除数量。多地打卡分享通过 `/mini/checkinShare/multi` 创建，只支持动态分享，请求体提交按行程排序的 `recordIds`（2~100 条）、`durationType` 和可选的 `title`（分享名称，最多 30 字），记录以 `share_type=multi`、`content` 存 `{"title":名称,"recordIds":记录ID数组}` JSON 对象保存；查看时按数组顺序实时拼接仍存在的打卡记录放到详情的 `points`，详情和管理列表名称优先使用 `title`、未命名时展示“多地打卡分享”，管理列表返回 `pointCount`
- **打卡常用语**：`/mini/commonPhrase` 提供当前登录用户的列表、新增、编辑、置顶和删除接口；内容最长 255 字且同一用户不能重复，建表脚本为 `sql/20260816_create_mini_common_phrase.sql`
- **评论订阅通知**：小程序调用 `wx.requestSubscribeMessage` 获得用户同意后，通过 `POST /mini/subscribeMessage/record` 提交已同意的模板 ID，后端按“用户 + 模板”把剩余可发送次数写入 Redis（单模板最多累计 20 次；微信一次性订阅本身不限时间，本地记录不设过期）。Redis 只作辅助额度记录，不作为发送硬门槛：即使本地没有额度也会尝试发送，以微信实际订阅状态为准。有新评论时使用“评论留言通知”模板提醒记录主人，有回复时使用“评论回复通知”模板提醒被回复人；发送成功扣减一次额度，微信返回 `43101`（未订阅/已取消订阅）时清除额度并暂停该模板 1 天，用户重新授权后立即恢复，其他失败只记录日志，不影响评论和分享创建。点击消息按分享类型跳转 `pages/checkinShare/checkinShare?id=...` 或 `pages/multiCheckinShare/multiCheckinShare?id=...`，无分享 ID 时回退 `pages/records/records`。模板 ID 和打开版本通过 `YC_MINI_SUBSCRIBE_COMMENT_TEMPLATE_ID`、`YC_MINI_SUBSCRIBE_REPLY_TEMPLATE_ID`、`YC_MINI_SUBSCRIBE_STATE` 配置

---

## 📌 注意事项

- 项目中使用了大量 Spring Boot 注解（如 `@Component`, `@Service`, `@RestController`），建议熟悉 Spring Boot 基础知识
- 所有数据库操作基于 MyBatis Plus，支持自动分页、自动填充等功能
- 微信相关接口需配置 AppID 和 Secret，详见 `application.properties`
- Redis 缓存策略通过自定义注解（如 `@RedisCache`, `@CacheEvict`）实现，便于扩展和维护

---

## 📝 许可证

本项目采用 [MIT License](https://opensource.org/licenses/MIT)，欢迎自由使用和修改。如需贡献代码，请提交 PR 或 Issue。

---

## 📬 联系方式

如需进一步了解项目或提出建议，请访问 [Gitee 项目主页](https://gitee.com/yc556/yc-page) 或联系作者。
