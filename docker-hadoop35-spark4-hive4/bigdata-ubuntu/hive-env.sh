#
# 定制的 hive-env.sh：JDK 21 + Hadoop 3.4.1
#

export HADOOP_HEAPSIZE=1024
HADOOP_HOME=/usr/bigdata/hadoop-3.5.0
export HIVE_CONF_DIR=/usr/bigdata/apache-hive-4.2.1-bin/conf

# JDK 17+/21 必需的模块开放（schematool / hiveserver2 / beeline 等客户端）
export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
