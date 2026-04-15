# 项目阅读记录

## 1. 项目定位

`yc-page` 是一个基于 Spring Boot 2.7 的个人工具类后端项目，核心目标是给个人主页/个人工具箱提供统一 API。  
从代码和现有 `README.md` 来看，它目前承载的能力主要包括：

- 用户登录与基础资料维护
- 书签管理
- 备忘录/待办管理
- 循环备忘录明细
- 搜索引擎配置
- 微信公众号消息接入
- 微信小程序登录与打卡
- 一些个人统计类接口（抖音观看时长、设备使用日志等）

这个项目不是单一业务系统，而是“多个个人工具后端能力”的集合。

## 2. 技术栈与运行环境

### 2.1 技术栈

- Java 8
- Spring Boot 2.7.5
- MyBatis-Plus 3.5.4.1
- MySQL
- Redis
- JWT
- AOP
- Lombok
- Spring Validation
- RestTemplate
- Swagger / Springfox 3.0.0（代码存在，但当前配置类默认未启用）

### 2.2 关键配置

主配置文件是 `src/main/resources/application.properties`，主要依赖这些环境变量：

- `YC_WECHAT_TOKEN`
- `YC_WECHAT_ENCODINGAESKEY`
- `YC_WECHAT_MINI_APPID`
- `YC_WECHAT_MINI_APPSECRET`
- `YC_REDIS_HOST`
- `YC_REDIS_PASSWORD`
- `YC_REDIS_DATABASE`
- `YC_MYSQL_HOST`
- `YC_MYSQL_USERNAME`
- `YC_MYSQL_PASSWORD`

默认端口是 `8080`，数据库名默认是 `yc_page`。

## 3. 项目结构理解

源码主路径：`src/main/java/ikun/yc/ycpage`

按职责可以分成这些部分：

- `controller`：对外 REST 接口
- `service` / `service.impl`：业务逻辑
- `mapper`：数据库访问
- `entity`：数据库实体与 DTO / 枚举
- `common`：公共能力、注解、AOP、异常、上下文
- `config`：Spring / MyBatis / Redis / MVC 配置
- `interceptor`：登录拦截器

我本次阅读时大致统计到：

- `controller` 10 个
- `service` 24 个（含接口）
- `service/impl` 12 个
- `mapper` 12 个
- `entity` 21 个

## 4. 应用启动与请求链路

### 4.1 启动入口

启动类：`src/main/java/ikun/yc/ycpage/YcPageApplication.java`

职责很简单：

- 启动 Spring Boot
- 通过 `@MapperScan("ikun.yc.ycpage.mapper")` 扫描 MyBatis Mapper

### 4.2 认证链路

认证相关代码主要在：

- `src/main/java/ikun/yc/ycpage/interceptor/LoginInterceptor.java`
- `src/main/java/ikun/yc/ycpage/config/WebMvcConfig.java`
- `src/main/java/ikun/yc/ycpage/utils/JwtUtils.java`
- `src/main/java/ikun/yc/ycpage/common/BaseContext.java`

请求链路大致如下：

1. `WebMvcConfig` 注册 `LoginInterceptor`
2. 拦截器默认拦截大部分请求
3. 如果方法或类上标了 `@PassToken`，则跳过登录校验
4. 否则从 `Authorization` 头取 JWT
5. `JwtUtils.parseJWT()` 解析 token
6. 解析出的 `userId` 放入 `BaseContext`（`ThreadLocal`）
7. 业务层/切面从 `BaseContext` 获取当前用户
8. 请求结束后在 `afterCompletion` 清理线程变量

结论：  
这个项目的“当前用户”获取，不是靠 Spring Security 上下文，而是自定义拦截器 + `ThreadLocal`。

## 5. 横切能力

项目有一套比较完整的自定义注解 + AOP 机制，这一层是理解项目的关键。

### 5.1 `@CountControl`

相关代码：

- `src/main/java/ikun/yc/ycpage/common/anno/CountControl.java`
- `src/main/java/ikun/yc/ycpage/common/aop/CountControlAspect.java`

用途：

- 用 Redis 做接口频控
- 按“用户 + 操作类型 + 频率配置”维度计数
- 超限后短时间封禁

常用于新增、修改、删除接口，防止短时间高频调用。

### 5.2 `@RedisCache`

相关代码：

- `src/main/java/ikun/yc/ycpage/common/anno/RedisCache.java`
- `src/main/java/ikun/yc/ycpage/common/aop/RedisCacheAspect.java`

用途：

- 给查询接口做 Redis 缓存
- key 规则大致是：`userId:区域名:方法名:参数...`
- 默认缓存 1 天

当前主要用于书签、搜索引擎等读取接口。

### 5.3 `@DelCache`

相关代码：

- `src/main/java/ikun/yc/ycpage/common/anno/DelCache.java`
- `src/main/java/ikun/yc/ycpage/common/aop/DelCacheAspect.java`

用途：

- 在更新成功后清理对应用户的缓存
- 与 `@RedisCache` 配套使用

### 5.4 `@UserId`

相关代码：

- `src/main/java/ikun/yc/ycpage/common/anno/UserId.java`
- `src/main/java/ikun/yc/ycpage/common/aop/UserIdAspect.java`

用途：

- 在方法执行前，把 `BaseContext` 里的当前用户 ID 自动塞进参数对象指定字段
- 避免前端传用户 ID

这个设计在 `BookmarksController`、`MiniController`、`OtherController` 等地方都有使用。

### 5.5 `@Log`

相关代码：

- `src/main/java/ikun/yc/ycpage/common/anno/Log.java`
- `src/main/java/ikun/yc/ycpage/common/aop/LogAspect.java`

用途：

- 记录操作日志到 `operate_log`
- 记录类名、方法名、参数、返回值、耗时

## 6. 核心业务模块

## 6.1 用户与登录

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/UserController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/UserServiceImpl.java`

主要接口：

- `POST /users/login`
- `GET /users/getNameAndAvatar`
- `PUT /users`

理解：

- 登录不是传统账号密码登录
- 登录入口接受一个 `key`，这个 `key` 实际上是 Redis 里的验证码
- 验证码由微信公众号消息交互生成（见后面的 WeChat 模块）
- 登录成功后返回 JWT

新用户首次登录时，系统会自动初始化：

- `users`
- `bookmarks` 根节点
- `user_config`
- 默认搜索引擎数据

这说明项目的用户初始化是“懒创建”策略：首次登录才建档。

另外，`UserServiceImpl` 对登录接口做了基于 IP 的 Redis 防爆破限制。

## 6.2 书签模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/BookmarksController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/BookmarksServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/entity/Bookmarks.java`

主要接口：

- `POST /bookmarks`
- `GET /bookmarks`
- `PUT /bookmarks/dragSort`
- `PUT /bookmarks`
- `DELETE /bookmarks`

数据模型重点：

- `type = 0`：根节点
- `type = 1`：书签分组
- `type = 2`：普通书签
- `type = 3`：大图标书签

这个模块最重要的实现特点：

1. 书签排序不是单独表，而是用字符串字段 `sort`
2. 分组的 `sort` 存本组下书签 ID 串，例如 `1/3/8`
3. 根节点的 `sort` 存所有分组 ID 串

也就是说，书签树结构和排序信息都被压进了字符串字段里。  
好处是简单直接；代价是排序、删除、拖拽时需要频繁做字符串拼接和校验。

## 6.3 备忘录 / 待办模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/MemoController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/MemoServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/entity/Memo.java`
- `src/main/java/ikun/yc/ycpage/entity/enumeration/MemoType.java`
- `src/main/java/ikun/yc/ycpage/mapper/MemoMapper.java`

主要接口：

- `POST /memo`
- `GET /memo/{type}`
- `PUT /memo`
- `DELETE /memo/{id}`

理解：

- 这是项目里最核心的业务之一
- `itemType` 表示待办类型，包含普通、循环、长期、紧急、英语、日记、工作、其他等
- 查询支持分页、完成状态过滤、关键字、首字母、日期范围、排序方式

几个实现细节值得记住：

- 删除不是物理删除，而是把 `completed` 加 10 作为逻辑删除
- 更新时如果标记完成但没传完成时间，会自动补当前时间
- `Memo.toReviseInfo()` 会强制把 `userId` 改成当前登录用户，并限制 `okTime` 的可接受范围
- `MemoMapper.selectGroupMemoCount()` 会统计其它类别未完成数量，用于前端标签计数

## 6.4 循环备忘录明细

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/LoopMemoItemController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/LoopMemoItemServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/entity/LoopMemoItem.java`

主要接口：

- `GET /loopMemoItem/{itemId}`
- `POST /loopMemoItem`
- `PUT /loopMemoItem`
- `DELETE /loopMemoItem/{memoId}/{loopId}`

理解：

- 这是 `Memo` 的子模块
- 用 `loop_memo_item` 存循环事项的每次记录
- 新增/删除明细时，会同步回写 `memo.number_of_recurrences`
- 同时更新 `memo.update_time`

说明作者把“循环待办的统计结果”冗余保存在主表里，便于前端直接展示。

## 6.5 搜索引擎模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/SearchEnginesController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/SearchEnginesServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/entity/SearchEngines.java`
- `src/main/java/ikun/yc/ycpage/entity/enumeration/LinkType.java`
- `src/main/java/ikun/yc/ycpage/service/impl/UserConfigServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/common/SearchEngineDataInitializer.java`
- `src/main/resources/initial-engines.json`

主要接口：

- `GET /searchEngines`
- `POST /searchEngines`
- `PUT /searchEngines`
- `DELETE /searchEngines/{id}`
- `POST /searchEngines/sort`

理解：

- 每个用户都有自己的搜索引擎列表
- 初始数据来自 `initial-engines.json`
- 排序信息不放在 `search_engines` 表里，而是放在 `user_config` 里

`LinkType` 目前有三种：

- `SEARCH`：常用搜索
- `LOW_SEARCH`：低频搜索
- `HOME_LINK`：首页链接/大图标排序

这里的设计和书签模块很像：  
“数据本体”在一张表里，“排序关系”在另一张表里以 `id/id/id` 字符串保存。

## 6.6 用户配置模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/UserConfigController.java`
- `src/main/java/ikun/yc/ycpage/entity/UserConfig.java`
- `src/main/java/ikun/yc/ycpage/service/impl/UserConfigServiceImpl.java`

主要接口：

- `PUT /userConfig`
- `GET /userConfig/getBg`

主要存储内容：

- 背景图 URL
- 搜索引擎排序
- 低频搜索排序
- 首页书签排序

它本质上是前端展示层的个性化配置中心。

## 6.7 微信小程序模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/MiniController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/MiniUserServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/common/WechatMiniAuthService.java`
- `src/main/java/ikun/yc/ycpage/entity/MiniUser.java`
- `src/main/java/ikun/yc/ycpage/entity/MiniCheckinRecords.java`

主要接口：

- `POST /mini/login`
- `POST /mini/checkin`
- `POST /mini/checkinList/{page}`
- `POST /mini/deleteCheckin/{id}`
- `POST /mini/updateCheckin`
- `POST /mini/getUserInfo`
- `POST /mini/updateUserInfo`

理解：

- `/mini/login` 通过微信 `code` 调 `jscode2session` 换取 `openid`
- 小程序用户表是 `mini_user`
- 登录成功后返回 JWT，JWT 里用 `openid` 作为 `userId`
- 打卡记录保存在 `mini_checkin_records`

打卡逻辑有几个约束：

- 同一用户、同一经纬度、同一天不能重复打卡
- 一天打卡次数超过 100 次直接拒绝
- 删除是软删除，用 `is_deleted`

说明这是一个偏“个人足迹/到此打卡”功能，而不是考勤系统。

## 6.8 微信公众号模块

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/WechatController.java`
- `src/main/java/ikun/yc/ycpage/service/impl/WechatServiceImpl.java`

接口：

- `GET /接收微信接口`
- `POST /接收微信接口`

理解：

- `GET` 用于微信服务器接入校验
- `POST` 解析微信 XML 消息，再根据内容回复文本

当前支持的能力不只登录，还包括：

- 生成网页登录验证码
- 快速新增备忘录
- 翻译
- 一言
- 舔狗日记
- KFC 文案
- 短剧搜索
- 一些个人定制接口

其中最重要的是：

- 用户在公众号发“登录/登陆”
- 后端生成验证码写入 Redis
- 用户把验证码拿到网页端 `/users/login`
- 网页端换取 JWT

这相当于把公众号当作“登录验证码分发渠道”。

## 6.9 其它统计/工具接口

相关文件：

- `src/main/java/ikun/yc/ycpage/controller/OtherController.java`
- `src/main/java/ikun/yc/ycpage/entity/DySeeTime.java`
- `src/main/java/ikun/yc/ycpage/entity/DeviceUsageLog.java`

主要接口：

- `GET /other/startTime`
- `GET /other/run-script`
- `POST /other/dySeeTime`
- `POST /other/getSeeTime`
- `POST /other/deviceUsageLog`

理解：

- `startTime`：返回本次服务启动时间
- `run-script`：在 Linux 服务器执行 `/var/script/` 下脚本
- `dySeeTime`：记录某类观看时长
- `getSeeTime`：按日/周/月/年聚合观看时长
- `deviceUsageLog`：记录设备使用日志

这部分明显是作者自己的个人数据接口。

## 6.10 账号备忘录模块（开发中）

相关文件：

- `src/main/java/ikun/yc/ycpage/entity/MiniAccountMemo.java`
- `src/main/java/ikun/yc/ycpage/service/MiniAccountMemoService.java`
- `src/main/java/ikun/yc/ycpage/service/impl/MiniAccountMemoServiceImpl.java`
- `src/main/java/ikun/yc/ycpage/controller/MiniAccountMemoController.java`

理解：

- 数据表和 service 已生成
- controller 目前还是空的
- `@RequestMapping("/memo")` 与现有 `MemoController` 同路径，后续如果继续开发，需要重新设计路由

这说明该功能处于“刚建骨架、尚未完成”的状态。

## 7. 数据层理解

从实体类可以看出当前核心表包括：

- `users`
- `user_config`
- `bookmarks`
- `memo`
- `loop_memo_item`
- `search_engines`
- `operate_log`
- `mini_user`
- `mini_checkin_records`
- `mini_account_memo`
- `dy_see_time`
- `device_usage_log`

一个明显特征是：  
很多实体继承了 MyBatis-Plus 的 `Model<T>`，项目大量使用 ActiveRecord 风格写法，例如：

- `insert()`
- `updateById()`
- `selectOne()`
- `selectCount()`

因此这个项目并不是“严格分层到 service/mapper 才能访问数据库”的风格，而是 service、controller、entity 三层里都有一定的 AR 调用痕迹。

## 8. 配置层理解

### 8.1 MVC 配置

`src/main/java/ikun/yc/ycpage/config/WebMvcConfig.java`

主要做了三件事：

- 注册登录拦截器
- 自定义 Jackson 时间格式
- 注册 `String -> LinkType` 转换器

### 8.2 MyBatis-Plus 配置

`src/main/java/ikun/yc/ycpage/config/MybatisPlusConfig.java`

- 启用了 MySQL 分页插件

### 8.3 Redis 配置

`src/main/java/ikun/yc/ycpage/config/RedisConfig.java`

- 自定义了 `RedisTemplate<String, Object>`
- 用 Jackson 处理对象序列化
- 注册了 Java 8 时间模块

### 8.4 RestTemplate 配置

`src/main/java/ikun/yc/ycpage/config/Config.java`

- 注册 `RestTemplate`
- 给所有外部请求默认加浏览器 `User-Agent`

## 9. 自动填充与异常处理

### 9.1 自动填充

`src/main/java/ikun/yc/ycpage/common/MyMetaObjecthandler.java`

会自动填充：

- `createTime`
- `updateTime`

### 9.2 统一返回

`src/main/java/ikun/yc/ycpage/common/R.java`

项目统一返回格式：

- `code`
- `success`
- `msg`
- `data`
- `map`

成功通常是 `code = 1`，失败是 `code = 0`。

### 9.3 全局异常

`src/main/java/ikun/yc/ycpage/common/exception/GlobalExceptionHandler.java`

处理了：

- SQL 唯一约束异常
- 参数校验异常
- 空指针异常
- 运行时异常

## 10. 我对这个项目的整体判断

### 10.1 优点

- 模块划分基本清晰，业务边界能看出来
- 自定义注解 + AOP 做缓存、日志、限流，复用性不错
- 用户上下文统一通过 `BaseContext` 传递，接入成本低
- 小程序、公众号、网页端三类入口之间有联动
- 用户首次登录自动初始化，使用体验比较顺

### 10.2 现阶段特点

- 偏个人项目/个人工具后端
- 业务需求比较贴近作者自己的使用场景
- 既有“正式功能”，也有明显“个人定制接口”
- 代码风格混合了 Service 风格和 MyBatis-Plus ActiveRecord 风格

### 10.3 当前可见的维护点

- `SwaggerConfig` 默认未启用
- 测试非常少，`YcPageApplicationTests` 里的 `@SpringBootTest` 还是注释状态
- `MiniAccountMemoController` 为空，说明账号备忘录功能未完成
- `OtherController` 的 `/other/run-script` 是一个带明显运维性质的接口，需要谨慎使用
- 书签和搜索引擎排序都依赖字符串拼接，后续复杂度继续增长时可维护性会下降

## 11. 建议的继续阅读顺序

如果后续要继续深入，我建议按下面顺序看：

1. 先看 `UserController` + `UserServiceImpl`，弄清登录与初始化
2. 再看 `LoginInterceptor`、`BaseContext`、几个 AOP，理解项目底层机制
3. 然后看 `BookmarksController` / `SearchEnginesController`，理解排序字符串方案
4. 再看 `MemoController` + `LoopMemoItemController`，理解主业务
5. 最后看 `MiniController`、`WechatController`、`OtherController`，理解扩展能力

## 12. 本次阅读结论

我已经能够确认：

- 这是一个“个人主页 + 个人工具后端”的 Spring Boot 项目
- 登录体系基于微信公众号验证码 + JWT
- 小程序体系基于微信 `openid` + JWT
- 书签、搜索引擎、备忘录是最主要的三块业务
- Redis 主要承担频控、验证码和接口缓存
- 项目目前仍在继续演进，至少“账号备忘录”还在开发中

说明：  
本次是静态阅读源码得出的理解，没有实际启动项目、连接数据库或联调接口。
