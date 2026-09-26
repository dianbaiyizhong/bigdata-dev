FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY backend/bigdata-dev-backend-1.0.jar app.jar


ADD bigdata-ubuntu/spark-3.5.9-bin-hadoop3.tgz /usr/bigdata/
ADD bigdata-ubuntu/apache-hive-3.1.3-bin.tar.gz /usr/bigdata/
ADD bigdata-ubuntu/hadoop-3.4.3-aarch64.tar.gz /usr/bigdata/

ADD bigdata-ubuntu/bellsoft-jdk8u492+9-linux-aarch64.tar.gz /usr/bigdata/

# 设置环境变量与系统时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

## 各种配置文件
ADD bigdata-ubuntu/hadoop-env.sh /usr/bigdata/hadoop-3.4.3/etc/hadoop/
ADD bigdata-ubuntu/core-site.xml /usr/bigdata/hadoop-3.4.3/etc/hadoop/
ADD bigdata-ubuntu/hdfs-site.xml /usr/bigdata/hadoop-3.4.3/etc/hadoop/
ADD bigdata-ubuntu/workers /usr/bigdata/hadoop-3.4.3/etc/hadoop/
ADD bigdata-ubuntu/start-dfs.sh /usr/bigdata/hadoop-3.4.3/sbin/
ADD bigdata-ubuntu/stop-dfs.sh /usr/bigdata/hadoop-3.4.3/sbin/
ADD bigdata-ubuntu/hdfs /usr/bigdata/hadoop-3.4.3/bin/
ADD bigdata-ubuntu/yarn-site.xml /usr/bigdata/hadoop-3.4.3/etc/hadoop/
ADD bigdata-ubuntu/start-yarn.sh /usr/bigdata/hadoop-3.4.3/sbin/
ADD bigdata-ubuntu/stop-yarn.sh /usr/bigdata/hadoop-3.4.3/sbin/
ADD bigdata-ubuntu/yarn /usr/bigdata/hadoop-3.4.3/bin/
ADD bigdata-ubuntu/spark_workers /usr/bigdata/spark-3.5.9-bin-hadoop3/conf/workers
ADD bigdata-ubuntu/spark-env.sh /usr/bigdata/spark-3.5.9-bin-hadoop3/conf/
ADD bigdata-ubuntu/spark-config.sh /usr/bigdata/spark-3.5.9-bin-hadoop3/sbin
ADD bigdata-ubuntu/spark-defaults.conf /usr/bigdata/spark-3.5.9-bin-hadoop3/conf/
ADD bigdata-ubuntu/hive-site.xml /usr/bigdata/apache-hive-3.1.3-bin/conf/
ADD bigdata-ubuntu/hive-log4j2.properties /usr/bigdata/apache-hive-3.1.3-bin/conf/
ADD bigdata-ubuntu/hive-env.sh /usr/bigdata/apache-hive-3.1.3-bin/conf/

## 设置环境变量

ENV HADOOP_HOME=/usr/bigdata/hadoop-3.4.3
ENV PATH=$PATH:$HADOOP_HOME/bin


ENV HIVE_HOME=/usr/bigdata/apache-hive-3.1.3-bin
ENV PATH=$PATH:$HIVE_HOME/bin
ENV HADOOP_CONF_DIR="/usr/bigdata/hadoop-3.4.3/etc/hadoop/"
ENV YARN_CONF_DIR="/usr/bigdata/hadoop-3.4.3/etc/hadoop/"

RUN chmod -R +x /usr/bigdata/hadoop-3.4.3/

EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Shanghai", "-jar", "app.jar"]

