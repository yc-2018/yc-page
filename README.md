# 欢迎来到 YcPage 项目

YcPage 是一个基于 Spring Boot 的多功能后端服务项目，旨在为用户提供便捷的个人数据管理功能。该项目集成了多种常用功能模块，包括书签管理、备忘录、微信小程序登录、操作日志记录等，适用于个人工具类应用的开发。

---

## 🌟 主要功能模块

- **书签管理**：支持添加、编辑、删除和拖拽排序书签，支持自定义图标和分类。
- **备忘录系统**：支持创建、更新、删除备忘录，支持循环备忘录和打卡记录。
- **微信小程序集成**：提供微信登录、用户信息管理、打卡记录等功能。
- **搜索引擎配置**：支持自定义搜索引擎及其排序，适用于个性化搜索需求。
- **用户配置管理**：支持用户自定义背景、书签排序等个性化设置。
- **操作日志记录**：记录用户操作行为，便于审计和追踪。

---

## 🛠 技术栈

- **后端框架**：Spring Boot + MyBatis Plus
- **数据库**：MySQL
- **缓存**：Redis
- **安全**：JWT + Spring Security
- **接口文档**：Swagger UI
- **日志记录**：AOP + Logback
- **其他工具**：Lombok、Validation、RedisTemplate、RestTemplate

---

## 📦 核心组件说明

### 控制器（Controller）
- `BookmarksController`：书签管理接口
- `MemoController`：备忘录管理接口
- `MiniController`：微信小程序相关接口
- `SearchEnginesController`：搜索引擎配置接口
- `UserController`：用户管理接口

### 服务层（Service）
- `BookmarksService`：书签业务逻辑
- `MemoService`：备忘录业务逻辑
- `MiniUserService`：微信用户管理
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
- 可部署于任意支持 Java 11+ 的服务器环境（如 Tomcat、Jetty、Docker）

---

## 📚 开发文档

- **Swagger 接口文档**：启动项目后访问 `/swagger-ui.html` 查看所有 API 接口
- **日志记录**：所有操作日志记录在 `operate_log` 表中
- **缓存机制**：使用 Redis 缓存书签和搜索引擎数据，提升访问效率
- **权限控制**：通过 JWT 实现 Token 认证，部分接口需登录访问

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