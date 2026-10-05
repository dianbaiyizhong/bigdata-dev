# docker-hadoop3.4.1（JDK 21 / Hadoop 3.4.1 / Spark 4.2.0 / Hive 4.2.0）

基于 `docker-hadoop3.4` 工程结构升级的三节点大数据集群，安装包需自行下载。

## 版本

| 组件 | 版本 | 镜像内路径 |
|---|---|---|
| JDK | 21 LTS（任意发行版） | /usr/bigdata/jdk21（软链） |
| Hadoop | 3.4.1 | /usr/bigdata/hadoop-3.4.1 |
| Spark | 4.2.0（Scala 2.13） | /usr/bigdata/spark-4.2.0-bin-hadoop3 |
| Hive | 4.2.0 | /usr/bigdata/apache-hive-4.2.0-bin |

## 一、下载安装包

把以下文件放入 `bigdata-ubuntu/` 目录（**文件名必须与下面完全一致**，Dockerfile 按这些名字 ADD）：

| 文件名 | 下载地址 |
|---|---|
| `openjdk-21.0.2_linux-aarch64_bin.tar.gz` | Temurin：https://adoptium.net/temurin/releases/?version=21&os=linux&arch=aarch64 （下载 `OpenJDK21U-jdk_aarch64_linux_hotspot_x.x.x_x.tar.gz` 后重命名）；Bellsoft/Zulu 也可以，下载后同样重命名 |
| `hadoop-3.4.1.tar.gz` | https://archive.apache.org/dist/hadoop/common/hadoop-3.4.1/hadoop-3.4.1.tar.gz |
| `spark-4.2.0-bin-hadoop3.gz` | https://archive.apache.org/dist/spark/spark-4.2.0/spark-4.2.0-bin-hadoop3.gz |
| `apache-hive-4.2.0-bin.tar.gz` | https://archive.apache.org/dist/hive/hive-4.2.0/apache-hive-4.2.0-bin.tar.gz |
| `mysql-connector-j-8.0.31.jar` | 已从旧工程复制，无需再下载 |

> 注意：
> - 机器是 Apple Silicon / ARM 服务器时 JDK 下载 `aarch64` 版；x86 服务器请下载 `x64` 版后同样重命名为 `openjdk-21.0.2_linux-aarch64_bin.tar.gz`。
> - JDK 解压目录名因发行版而异（如 `jdk-21.0.9+10`、`zulu21...-ca-jdk21...`），Dockerfile 会自动识别并软链为 `/usr/bigdata/jdk21`，无需手动处理。
> - Spark 4.x 基于 Scala 2.13，无需单独安装 Scala（旧工程里的 scala-2.12.11 已不需要）。
> - Hive 4.2 自带较新的 commons-collections / commons-text，旧工程手动添加的那两个 jar 已不需要。

## 二、构建

```bash
cd docker-hadoop3.4.1
docker compose build
```

如只构建大数据镜像：`docker compose build linux001`（构建出 `bigdata-ubuntu:jdk21-hadoop3.4.1`，linux002/003 复用同一镜像）。

## 三、启动

```bash
# 1. 启动容器（linux001-003、mysql、backend、nginx）
docker compose up -d

# 2. 首次启动：进 linux001 执行集群初始化（格式化 NN、免密、启动全套服务、初始化 Hive 元数据）
docker exec -it linux001 bash
/start-bigdata.sh
```

启动内容：HDFS（NameNode/DataNode/SecondaryNN）→ YARN（RM/NM/TimelineServer/JobHistory）→ Spark Standalone + HistoryServer → Hive 元数据初始化（schematool，MySQL 库 `hive`）→ HiveServer2（MR 引擎）。

## 四、常用端口（容器内）

- HDFS NameNode WebUI: `linux001:9870`
- YARN WebUI: `linux001:8088`
- Spark Master/HistoryServer: `linux001:8080 / 18080`
- HiveServer2: `linux001:10000`（beeline 连接）
- MySQL 宿主机映射：`3307 -> 3306`（root/root）
- 前端：`2408 -> 80`

## 五、与旧工程（docker-hadoop3.4）的差异

1. JDK 8 → JDK 21：在 `hadoop-env.sh` / `hive-env.sh` 中加入了 JDK 17+ 必需的 `--add-opens`；Hive 4.2.0 对 JDK 21 并非官方支持组合，若 schematool/HiveServer2 报模块访问错误，优先检查这里的参数是否生效。
2. 不再整份替换 `start-dfs.sh`/`stop-dfs.sh`/`hdfs`/`yarn` 脚本：改为在 `hadoop-env.sh` 里声明 `HDFS_NAMENODE_USER=root` 等变量 + 构建时 sed 修改 `bin/hdfs`/`bin/yarn` 的 `HADOOP_SHELL_EXECNAME`，与官方 3.4.1 脚本保持一致。
3. Spark 3 → Spark 4：`spark-yarn_2.12-3.5.9.jar` 换成 `spark-yarn_2.13-4.2.0.jar`（见 start-bigdata.sh）。
4. 移除 Scala 独立安装、移除 Hive 3 时代的 commons-collections/commons-text 补丁 jar；新增 Hive↔Hadoop guava 版本统一步骤。
5. apt 源改用 Ubuntu 24.04 的 deb822 格式文件 `ubuntu.sources`。
6. backend 镜像基础镜像从 temurin 17 升级为 21，并同步内嵌新版组件。
