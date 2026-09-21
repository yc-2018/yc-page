# yc-page 协作说明

本文档是 `yc-page` 后端目录下的 AI 编码代理和开发协作者规则。执行任务时，优先以当前代码、配置、测试和数据库脚本为准；如果文档与实际代码不一致，以实际代码为准，并在合适时更新文档。

## 项目概览

- 项目类型：单模块 Maven Spring Boot 后端服务。
- 主包：`src/main/java/ikun/yc/ycpage`。
- 资源：`src/main/resources`，包括 `application.properties`、`initial-engines.json` 和 MyBatis XML 映射文件。
- 测试：`src/test/java`，当前以 JUnit 5、Mockito、Spring MockMvc 和少量资源回归测试为主。
- 技术栈：Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.8、MySQL、Redis、JJWT、Spring AOP、Validation、Lombok、FastJSON。
- 当前没有 Spring Security 依赖；接口鉴权由 `LoginInterceptor`、JWT 工具和 `@PassToken` 配合完成。

### 主要分层

- `controller`：HTTP 接口入口，按用户、书签、备忘录、搜索引擎、微信/小程序等业务划分。
- `service`：业务接口；`service/impl` 使用 MyBatis-Plus 的 `ServiceImpl` 承载业务实现。
- `mapper`：MyBatis-Plus `BaseMapper` 接口和自定义数据访问接口。
- `entity`：数据库实体；`entity/dto` 存放接口请求/响应 DTO；`entity/enumeration` 存放业务枚举。
- `common`：统一返回值、当前用户上下文、自动填充、微信认证、转换器、AOP 注解及切面、异常处理。
- `config`：MVC、Redis、MyBatis-Plus 等配置。
- `interceptor`：请求级 JWT 鉴权和请求上下文清理。
- `utils`：JWT、验证码、字符串等通用工具。

## 常用命令

在 `yc-page` 目录执行：

```text
mvn -DskipTests=false test
mvn package
mvn spring-boot:run
```

- 代码变更后，优先运行与改动相关的测试；无法确定影响范围时运行 `mvn -DskipTests=false test`。
- `pom.xml` 声明了 `skipTests` 项目属性，测试命令显式传入 `-DskipTests=false`，避免验证时误跳过测试。
- `mvn package` 用于编译和打包；不要把跳过测试当成完整验证。
- 启动服务前确认 Java 21、Maven、MySQL、Redis 和配置项均可用；不要在提交内容中写入真实密钥、Token 或密码。

## 配置与敏感信息

配置入口是 `src/main/resources/application.properties`。运行环境通常需要提供：

- 微信公众号：`YC_WECHAT_TOKEN`、`YC_WECHAT_ENCODINGAESKEY`。
- 微信小程序：`YC_WECHAT_MINI_APPID`、`YC_WECHAT_MINI_APPSECRET`。
- Redis：`YC_REDIS_HOST`、`YC_REDIS_PASSWORD`、`YC_REDIS_DATABASE`；端口当前由配置固定为 `6379`。
- MySQL：`YC_MYSQL_HOST`、`YC_MYSQL_USERNAME`、`YC_MYSQL_PASSWORD`。
- JWT：生产环境设置至少 32 个 UTF-8 字节的 `YC_JWT_SECRET`；测试或本地 JVM 可使用 `-Dyc.jwt.secret=...`。

不要把密钥、数据库密码、微信配置或线上 Token 写入 Java、Properties、测试提交、日志和文档。新增配置时优先使用环境变量占位符，并同步检查默认值是否适合本地开发。

## 鉴权、上下文与接口约定

- `WebMvcConfig` 注册 `LoginInterceptor`，默认拦截映射到方法的请求。
- 类或方法标注 `@PassToken` 时跳过 JWT 校验；其他接口从 `Authorization` 请求头解析 JWT。
- 解析出的 `userId` 写入 `BaseContext` 的 `ThreadLocal`，请求完成后由拦截器清理。业务代码获取当前用户使用 `BaseContext.getCurrentId()`，不要自行创建新的用户上下文。
- 需要把当前用户 ID 写入请求实体时，复用 `@UserId` 和 `UserIdAspect`；小程序实体常使用 `fieldName = "userOpenid"`，不要混用网页用户 ID 和小程序 openid。
- Controller 通常返回统一的 `R<T>`；参数校验、运行时异常和 SQL 完整性异常交给 `GlobalExceptionHandler` 统一转换。
- 新增或修改路由前检查现有 Controller 的类级和方法级映射，避免冲突，并明确接口是否需要登录、用户隔离和限流。

## AOP 与数据访问规则

- `@CountControl` 通过 Redis 限制接口访问频率，默认窗口和禁用时间由注解属性决定；新增高频或容易滥用的写接口时评估是否需要限流。
- `@RedisCache` 只用于稳定且适合缓存的查询；写操作使用匹配的 `@DelCache` 清理当前用户对应缓存区域，缓存区域名称必须保持一致。
- `@Log` 记录重要业务操作到 `operate_log`；只记录有审计价值的新增、修改、删除行为，避免给高频查询造成日志表膨胀。
- 需要跨表保持一致的业务操作，沿用现有 `@Transactional` 使用方式，并检查事务边界和缓存清理时机。
- Mapper XML 位于 `src/main/resources/mapper` 或其现有子目录中，`namespace` 必须与 Mapper 接口全限定名一致；修改 XML 时同步检查接口方法、参数和结果映射。
- 继续使用 MyBatis-Plus 的实体映射、链式查询、自动填充和项目已有逻辑删除/删除策略，不要为单次需求引入新的 ORM 或缓存框架。

## 代码风格与注释要求

1. 生成或修改代码必须使用 UTF-8 编码。Markdown、Java、XML、Properties、JSON 和 SQL 文件都按 UTF-8 读写；写入后检查中文没有变成问号、乱码或 `\\u` 转义文本。
2. 编写类或方法都要写注释。Java 类和公开方法优先使用 JavaDoc，说明职责、关键参数、返回值和异常/边界；私有方法也要有能解释目的的简洁注释。
3. 生成的变量也要在变量尾部加注释，说明变量用途。例如：`String token = ...; // JWT 登录令牌`。
4. 如果变量声明换行，无法在声明尾部清晰放置注释，则把注释放在变量声明顶部；不要为了放注释破坏格式或把注释放在无关行。
5. 注释解释业务意图、约束、兼容性和非显然逻辑，避免为每一行写没有信息量的注释。修改旧代码时只补充与本次变更直接相关的注释，不进行无关的全量注释或格式化。
6. 优先遵循项目现有风格、架构和测试方式：沿用四空格缩进、Lombok 构造注入、`R<T>` 返回结构、MyBatis-Plus Mapper/Service、现有异常类型和现有注解。不要为了小需求新增抽象、框架或平行实现。

## 业务修改注意事项

- 用户、书签、备忘录、标签、搜索引擎和小程序模块的数据必须保持当前用户隔离；涉及排序、统计、循环备忘录、分享过期、乐观锁或逻辑删除时，先阅读同模块完整调用链。
- `initial-engines.json` 用于新用户初始化搜索引擎；修改搜索引擎结构时同步检查 `LinkType`、转换器、用户配置和排序读写。
- 微信公众号入口和小程序登录/打卡/分享依赖外部配置、Redis 和 openid，修改前检查过期时间、幂等性、权限、频控和失败兜底。
- 新增数据库表或字段时，补充 `sql` 目录迁移脚本，并同步实体、Mapper、Service、Controller、校验和测试。不要只修改实体而遗漏 XML 或数据库脚本。
- 只修改完成用户需求所需的文件；不要顺手重构无关代码、统一无关格式或删除原有废弃代码。发现文档与代码不一致时，以代码为准，并在变更涉及的范围内更新文档。

## 测试要求

- 测试使用 JUnit 5；服务单元测试优先 Mockito，Controller 参数绑定或路由测试可使用独立 `MockMvc`，资源映射回归测试可按现有方式读取 UTF-8 文件。
- 新增行为至少覆盖成功、失败和边界输入；涉及鉴权、用户隔离、缓存、频控、排序、事务、乐观锁或微信登录时，优先补充对应测试。
- 测试需要设置 JWT 时使用测试专用密钥，并在测试结束清理系统属性或 `BaseContext`，避免污染其他用例。
- 如果因为本地缺少 MySQL、Redis、外部服务或环境变量导致测试不能完整执行，在最终说明中列出未验证项和原因，不要声称测试已通过。

## 变更前后检查清单

1. 先定位所属业务模块和现有实现，阅读 Controller、Service、Mapper、Entity/DTO、配置和相关测试。
2. 确认鉴权、当前用户隔离、限流、缓存、日志、事务和数据库变更是否受到影响。
3. 按现有风格实现最小改动，并为新增类、方法和变量补充符合本文件要求的注释。
4. 检查 UTF-8 编码、中文显示、路由映射、Mapper XML namespace、环境变量和敏感信息。
5. 运行与改动匹配的测试，必要时运行完整 Maven 测试，并如实报告验证结果。
