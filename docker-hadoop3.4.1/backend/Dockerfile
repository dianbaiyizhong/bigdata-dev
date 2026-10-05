FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY backend/bigdata-dev-backend-1.0.jar app.jar


ADD bigdata-ubuntu/spark-4.2.0-bin-hadoop3.gz /usr/bigdata/
ADD bigdata-ubuntu/apache-hive-4.2.0-bin.tar.gz /usr/bigdata/
ADD bigdata-ubuntu/hadoop-3.4.1.tar.gz /usr/bigdata/

ADD bigdata-ubuntu/openjdk-21.0.2_linux-aarch64_bin.tar.gz /usr/bigdata/

# JDK 解压目录名因发行版而异，统一软链为 jdk21
RUN JDK_DIR=$(find /usr/bigdata -maxdepth 1 -type d -name '*jdk*21*' | head -n 1) && \
    test -n "$JDK_DIR" && ln -snf "$JDK_DIR" /usr/bigdata/jdk21

# 设置环境变量与系统时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

## 各种配置文件
ADD bigdata-ubuntu/hadoop-env.sh /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD bigdata-ubuntu/core-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD bigdata-ubuntu/hdfs-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD bigdata-ubuntu/workers /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD bigdata-ubuntu/yarn-site.xml /usr/bigdata/hadoop-3.4.1/etc/hadoop/
ADD bigdata-ubuntu/spark_workers /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/workers
ADD bigdata-ubuntu/spark-env.sh /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/
ADD bigdata-ubuntu/spark-defaults.conf /usr/bigdata/spark-4.2.0-bin-hadoop3/conf/
ADD bigdata-ubuntu/hive-site.xml /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD bigdata-ubuntu/hive-log4j2.properties /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD bigdata-ubuntu/hive-env.sh /usr/bigdata/apache-hive-4.2.0-bin/conf/

# MySQL 驱动
ADD bigdata-ubuntu/mysql-connector-j-8.0.31.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/mysql-connector-java.jar
ADD bigdata-ubuntu/mysql-connector-j-8.0.31.jar /usr/bigdata/spark-4.2.0-bin-hadoop3/jars/mysql-connector-java.jar

# Guava 版本统一
RUN rm -f /usr/bigdata/apache-hive-4.2.0-bin/lib/guava-*.jar && \
    cp /usr/bigdata/hadoop-3.4.1/share/hadoop/common/lib/guava-*.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/

## 设置环境变量
ENV JAVA_HOME=/usr/bigdata/jdk21
ENV HADOOP_HOME=/usr/bigdata/hadoop-3.4.1
ENV PATH=$PATH:$HADOOP_HOME/bin

ENV HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin
ENV PATH=$PATH:$HIVE_HOME/bin
ENV HADOOP_CONF_DIR="/usr/bigdata/hadoop-3.4.1/etc/hadoop/"
ENV YARN_CONF_DIR="/usr/bigdata/hadoop-3.4.1/etc/hadoop/"

RUN chmod -R +x /usr/bigdata/hadoop-3.4.1/

EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]
