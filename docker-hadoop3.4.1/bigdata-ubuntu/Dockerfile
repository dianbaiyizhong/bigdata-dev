FROM ubuntu:24.04

# ============ 1. apt 换阿里云源（Ubuntu 24.04 使用 deb822 格式） ============
RUN sed -i \
    -e 's|http://archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
    -e 's|http://security.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
    -e 's|http://cn.archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
    /etc/apt/sources.list.d/ubuntu.sources

# ============ 2. 基础工具 + 时区 ============
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    tzdata openssh-server net-tools sudo curl git vim zsh zip unzip sshpass \
 && rm -rf /var/lib/apt/lists/*

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# ============ 3. 安装包（请先下载并放入本目录，文件名必须一致） ============
#   openjdk-21.0.2_linux-aarch64_bin.tar.gz                 -> JDK 21 LTS（任意发行版压缩包，重命名即可）
#   hadoop-3.4.1.tar.gz          -> Hadoop 3.4.1
#   spark-4.2.0-bin-hadoop3.gz  -> Spark 4.2.0
#   apache-hive-4.2.0-bin.tar.gz -> Hive 4.2.0
ADD openjdk-21.0.2_linux-aarch64_bin.tar.gz /usr/bigdata/
ADD hadoop-3.4.1.tar.gz /usr/bigdata/
ADD spark-4.2.0-bin-hadoop3.gz /usr/bigdata/
ADD apache-hive-4.2.0-bin.tar.gz /usr/bigdata/

# JDK 解压目录名因发行版而异（如 jdk-21.0.9+10 / zulu21...-ca-jdk21...），统一软链为 jdk21
RUN JDK_DIR=$(find /usr/bigdata -maxdepth 1 -type d -name '*jdk*21*' | head -n 1) && \
    test -n "$JDK_DIR" && ln -snf "$JDK_DIR" /usr/bigdata/jdk21

# ============ 4. Hadoop / Spark / Hive 配置文件 ============
ADD hadoop-env.sh /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD core-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD hdfs-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD workers /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD yarn-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/

ADD spark_workers /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/workers
ADD spark-env.sh /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/
ADD spark-defaults.conf /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/

ADD hive-site.xml /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD hive-env.sh /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD hive-log4j2.properties /usr/bigdata/apache-hive-4.2.0-bin/conf/

# ============ 5. MySQL 驱动（Hive 元数据库 / Spark 读取 MySQL 用） ============
ADD mysql-connector-j-8.0.31.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/mysql-connector-java.jar
ADD mysql-connector-j-8.0.31.jar /usr/bigdata/spark-4.2.0-bin-hadoop3/jars/mysql-connector-java.jar

# Guava 版本统一：用 Hadoop 3.4.1 的 guava 替换 Hive 自带的旧版，避免 NoSuchMethodError
RUN rm -f /usr/bigdata/apache-hive-4.2.0-bin/lib/guava-*.jar && \
    cp /usr/bigdata/hadoop-3.4.1/share/hadoop/common/lib/guava-*.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/

# 允许 root 运行 Hadoop 守护进程（与旧工程修改 bin/hdfs、bin/yarn 的做法等价）
RUN sed -i 's/^HADOOP_SHELL_EXECNAME="hdfs"/HADOOP_SHELL_EXECNAME="root"/' /usr/bigdata/hadoop-3.4.1/bin/hdfs && \
    sed -i 's/^HADOOP_SHELL_EXECNAME="yarn"/HADOOP_SHELL_EXECNAME="root"/' /usr/bigdata/hadoop-3.4.1/bin/yarn

# ============ 6. 环境变量 ============
ENV JAVA_HOME=/usr/bigdata/jdk21
ENV HADOOP_HOME=/usr/bigdata/hadoop-3.4.1
ENV HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin
ENV SPARK_HOME=/usr/bigdata/spark-4.2.0-bin-hadoop3
ENV PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HIVE_HOME/bin:$SPARK_HOME/bin:$PATH

RUN ln -s /usr/bigdata/jdk21/bin/java /bin/java

# Hive 目录
RUN mkdir -p /usr/bigdata/apache-hive-4.2.0-bin/logs /usr/bigdata/apache-hive-4.2.0-bin/tmp

# ============ 7. 启动脚本 ============
COPY ./start-bigdata.sh /
RUN chmod +x /start-bigdata.sh
RUN chmod -R 777 /usr/bigdata

# ============ 8. SSH（root/root，供集群节点免密互通） ============
RUN mkdir /var/run/sshd
RUN echo 'root:root' | chpasswd
RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config
# SSH login fix. Otherwise user is kicked off after login
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

# ============ 9. 环境变量写入 /etc/profile 与 zshrc ============
RUN echo 'export JAVA_HOME=/usr/bigdata/jdk21' >> /etc/profile && \
    echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.4.1' >> /etc/profile && \
    echo 'export HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin' >> /etc/profile && \
    echo 'export SPARK_HOME=/usr/bigdata/spark-4.2.0-bin-hadoop3' >> /etc/profile && \
    echo 'export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HIVE_HOME/bin:$SPARK_HOME/bin:$PATH' >> /etc/profile

# 安装 oh-my-zsh 与自动补全插件
RUN yes | sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
ENV ZSH_CUSTOM=/root/.oh-my-zsh/custom
RUN mkdir -p ${ZSH_CUSTOM}/plugins
RUN git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM}/plugins/zsh-autosuggestions
RUN sed -i 's/plugins=(git)/plugins=(git zsh-autosuggestions)/' ~/.zshrc

RUN echo 'export JAVA_HOME=/usr/bigdata/jdk21' >> /root/.zshrc && \
    echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.4.1' >> /root/.zshrc && \
    echo 'export HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin' >> /root/.zshrc && \
    echo 'export SPARK_HOME=/usr/bigdata/spark-4.2.0-bin-hadoop3' >> /root/.zshrc && \
    echo 'export PATH=$JAVA_HOME/bin:$HADOOP_HOME/bin:$HIVE_HOME/bin:$SPARK_HOME/bin:$PATH' >> /root/.zshrc

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
