# 服务器基础环境安装

这套脚本用于安装 `yc-page` 后端的运行和构建环境：

- Eclipse Temurin JDK 21 最新补丁版
- Maven 3.9.11（已有 Maven 3.6.3 或更高版本时跳过）
- MySQL 5.7.25
- Redis（已有任意版本时跳过；未安装时优先使用系统软件包）
- Git、curl、OpenSSL、Python、进程和网络检查工具、MySQL 运行库

## 支持范围

- CentOS 7 x86_64
- Ubuntu 20.04、22.04、24.04 x86_64
- 使用 systemd 的普通云服务器或物理服务器

MySQL 5.7.25 的官方通用二进制包按 x86_64 编写，因此 ARM 服务器不会自动安装。CentOS 7 和 MySQL 5.7 都已停止官方维护，脚本保留这些版本只是为了匹配现有服务器要求；新服务器优先考虑仍受支持的操作系统和 MySQL 8.4 LTS。

## 安装规则

- 已有 JDK 21 或更高版本：复用并跳过下载。
- 只有 JDK 8/17：并行安装 JDK 21，不删除旧 JDK。
- 已有 Maven 3.6.3 或更高版本：跳过。
- 已有任何 MySQL 或 MariaDB：只显示版本并跳过，不修改服务、配置和数据。
- 已有任何 Redis：只显示版本并跳过，不修改配置和数据。
- 新安装 MySQL：只监听 `127.0.0.1`，创建 `yc_page` 数据库和同名应用账号。
- 新安装 Redis：只供本机访问，不开放公网端口。

## 执行

代码提交并推送到 Git 仓库后，在服务器拉取最新代码。

CentOS 7：

```bash
cd /var/javaCode/yc-page
bash deploy/install-centos7.sh
```

Ubuntu：

```bash
cd /var/javaCode/yc-page
bash deploy/install-ubuntu.sh
```

直接用 `bash` 执行即可，不要对仓库里的脚本运行 `chmod`，否则 Git 会把执行权限变化识别为本地修改。脚本必须由 root 执行。安装过程需要连接系统软件源、Adoptium、Apache、MySQL 官方归档站；CentOS 7 默认镜像失效时，会先备份原 Yum repo 文件，再切换到 CentOS 7.9.2009 归档镜像。

JDK 默认优先从清华 TUNA 的 Adoptium 镜像下载，镜像未同步时自动回退到 Adoptium/GitHub 官方地址。JDK、Maven、MySQL 和源码版 Redis 的大文件缓存在 `/var/cache/yc-stack`，下载中断后重新执行脚本会尝试断点续传；安装完成后可以保留缓存供排障或重装使用。

## 数据库凭据

全新安装 MySQL 时会自动生成 root 密码和应用账号密码，保存在：

```text
/root/.yc-stack/credentials.env
```

文件权限为 `600`。将其中的 `YC_MYSQL_HOST`、`YC_MYSQL_USERNAME`、`YC_MYSQL_PASSWORD` 填入：

```text
/etc/yc-page/yc-page.env
```

如果服务器已有 MySQL/MariaDB，脚本不会读取或生成数据库密码，需要继续使用原有配置。

## 服务管理

全新安装的 MySQL 服务名：

```bash
systemctl status yc-mysql
journalctl -u yc-mysql -n 100 --no-pager
```

Redis 优先使用系统包提供的服务，通常为 `redis` 或 `redis-server`。仅当系统包不可用并回退到源码安装时，服务名是 `yc-redis`。

基础环境安装完成后，再配置 `/etc/yc-page/yc-page.env` 并执行：

```bash
/root/runJava.sh
```

首次部署、环境变量配置、JWT 密钥和后续更新的完整步骤见 [`DEPLOY.md`](DEPLOY.md)。
