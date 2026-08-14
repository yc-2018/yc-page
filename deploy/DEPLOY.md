# yc-page 部署与更新

本文说明基础环境安装完成后，如何首次部署 `yc-page`，以及以后如何更新版本。

如果服务器还没有 JDK、Maven、MySQL 或 Redis，先按照 [`INSTALL.md`](INSTALL.md) 安装基础环境。

## 一、部署前确认

- 本地改动已经提交并推送到服务器所使用的 Git 仓库。
- 服务器源码目录是 `/var/javaCode/yc-page`。
- 使用 root 用户执行部署命令。
- JDK 版本是 21 或更高版本。
- MySQL 和 Redis 已启动，现有服务器继续使用原来的账号和密码。

检查基础环境：

```bash
/usr/local/java/jdk-21/bin/java -version
mvn -version
systemctl --no-pager --full status yc-mysql redis redis-server yc-redis 2>/dev/null || true
```

不同安装方式的 MySQL、Redis 服务名可能不同，只要实际使用的服务处于运行状态即可。

## 二、拉取部署文件

```bash
cd /var/javaCode/yc-page
git pull --ff-only
```

如果 `git pull --ff-only` 提示服务器存在未提交修改，应先人工确认，不要强制覆盖。

## 三、创建应用环境变量文件

创建只允许 root 访问的配置目录：

```bash
install -d -m 700 /etc/yc-page
```

仅在配置文件不存在时复制模板，避免覆盖服务器已有密码：

```bash
test -e /etc/yc-page/yc-page.env || \
  install -m 600 deploy/yc-page.env.example /etc/yc-page/yc-page.env
```

编辑配置：

```bash
vi /etc/yc-page/yc-page.env
```

需要确认的配置如下：

```bash
# 安装脚本创建的固定入口；实际 JDK 可以是 21 或更高版本。
JAVA_HOME='/usr/local/java/jdk-21'

# 可按服务器内存调整。
JAVA_OPTS='-Xms256m -Xmx512m'

# 微信公众号和小程序配置。
YC_WECHAT_TOKEN='填写真实值'
YC_WECHAT_ENCODINGAESKEY='填写真实值'
YC_WECHAT_MINI_APPID='填写真实值'
YC_WECHAT_MINI_APPSECRET='填写真实值'

# Redis 配置；全新本机安装且未设置密码时，密码保持为空。
YC_REDIS_HOST='127.0.0.1'
YC_REDIS_PASSWORD=''
YC_REDIS_DATABASE='0'

# MySQL 配置。
YC_MYSQL_HOST='127.0.0.1'
YC_MYSQL_USERNAME='填写真实值'
YC_MYSQL_PASSWORD='填写真实值'

# JWT 签名密钥，至少 32 个字符。
YC_JWT_SECRET='填写随机密钥'
```

如果 MySQL 是基础环境脚本全新安装的，生成的应用账号位于：

```bash
cat /root/.yc-stack/credentials.env
```

将其中的 `YC_MYSQL_HOST`、`YC_MYSQL_USERNAME`、`YC_MYSQL_PASSWORD` 填入应用环境变量文件。如果服务器原来就有 MySQL，继续使用原来的数据库账号和密码。

## 四、生成 JWT 密钥

首次部署执行：

```bash
openssl rand -hex 32
```

把输出的 64 个十六进制字符填入 `YC_JWT_SECRET`。不要把真实密钥写进 Git 仓库。

`YC_JWT_SECRET` 一旦更换，旧登录 Token 将全部失效，用户需要重新登录。因此后续更新应保留原密钥，不要每次重新生成。

检查配置文件权限：

```bash
chmod 600 /etc/yc-page/yc-page.env
ls -l /etc/yc-page/yc-page.env
```

## 五、安装并执行部署脚本

将仓库里的部署脚本安装到 root 目录：

```bash
cd /var/javaCode/yc-page
install -m 700 deploy/runJava.sh /root/runJava.sh
install -m 700 deploy/catJavaLog.sh /root/catJavaLog.sh
```

执行首次部署：

```bash
/root/runJava.sh
```

脚本会依次执行：

1. 校验环境变量、JDK、Maven、Git 和端口检查工具。
2. 在旧服务继续运行时拉取代码并完成 Maven 打包。
3. 保存新 jar 和可回滚的上一版 jar。
4. 正常停止旧进程并启动新版本。
5. 等待 8080 端口监听；启动失败时自动切回上一版。

## 六、验证部署结果

检查 Java 进程和端口：

```bash
ps -ef | grep '[y]c-page'
ss -ltnp | grep ':8080'
```

查看最近日志：

```bash
tail -n 100 /var/java/yc-page/logs/yc-page.log
```

持续观察日志：

```bash
/root/catJavaLog.sh
```

日志脚本默认先显示最近 200 行再持续跟踪。临时调整首次显示的行数：

```bash
LINES=500 /root/catJavaLog.sh
```

运行文件位置：

```text
/var/java/yc-page/current.jar
/var/java/yc-page/releases/
/var/java/yc-page/logs/yc-page.log
```

## 七、以后更新版本

普通业务代码更新，在本地提交并推送后，服务器只需要执行：

```bash
/root/runJava.sh
```

如果仓库里的 `deploy/runJava.sh` 本身也有更新，先手动更新并重新安装脚本：

```bash
cd /var/javaCode/yc-page
git pull --ff-only
install -m 700 deploy/runJava.sh /root/runJava.sh
install -m 700 deploy/catJavaLog.sh /root/catJavaLog.sh
/root/runJava.sh
```

修改 `/etc/yc-page/yc-page.env` 后，也需要重新执行 `/root/runJava.sh` 才会让新环境变量生效。

## 八、常见问题

### 拉取或打包失败

脚本在停止旧服务之前拉代码和打包，因此这个阶段失败时，线上旧服务仍会继续运行。根据终端里的 Git 或 Maven 错误处理后再次执行即可。

### 新版本启动失败

脚本会打印最近 80 行日志并尝试自动回滚。继续检查：

```bash
tail -n 200 /var/java/yc-page/logs/yc-page.log
```

### 提示 JDK 版本过低

确认 `/etc/yc-page/yc-page.env` 中的 `JAVA_HOME` 指向 JDK 21 或更高版本，不要指向原来的 JDK 8。

### 提示端口 8080 未监听

检查 MySQL、Redis 是否可连接，以及微信、JWT 环境变量是否完整。应用错误会记录在 `/var/java/yc-page/logs/yc-page.log`。

### 提示端口 8080 已被占用

先查看监听进程，不要直接按端口强制结束未知服务：

```bash
ss -ltnp | grep ':8080'
```

从输出取得 PID 后确认完整命令：

```bash
ps -fp PID
```

确认它确实是旧版 `yc-page` 后，先执行 `kill -15 PID` 正常停止，再重新运行 `/root/runJava.sh`。

## 九、安全说明

- `/etc/yc-page/yc-page.env` 和 `/root/.yc-stack/credentials.env` 不得提交到 Git。
- MySQL、Redis 默认只供本机访问，不需要向公网开放 3306 和 6379 端口。
- 只根据实际访问需求开放应用端口 8080，生产环境建议通过 Nginx 和 HTTPS 对外提供服务。
- 项目已经移除 Swagger/OpenAPI，不需要配置相关环境变量或开放文档端点。
