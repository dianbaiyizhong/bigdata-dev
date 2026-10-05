#
# 定制的 hadoop-env.sh：JDK 21 + 以 root 运行守护进程
#

export JAVA_HOME=/usr/bigdata/jdk21
export HADOOP_HOME=/usr/bigdata/hadoop-3.5.0
export HADOOP_CONF_DIR=${HADOOP_HOME}/etc/hadoop
export HADOOP_OS_TYPE=${HADOOP_OS_TYPE:-$(uname -s)}

# 守护进程运行用户（整个集群以 root 运行，替代旧工程中改 start-dfs.sh 等脚本的做法）
export HDFS_NAMENODE_USER=root
export HDFS_DATANODE_USER=root
export HDFS_SECONDARYNAMENODE_USER=root
export HDFS_DATANODE_SECURE_USER=root
export YARN_RESOURCEMANAGER_USER=root
export YARN_NODEMANAGER_USER=root
export YARN_TIMELINESERVER_USER=root
export MAPRED_HISTORYSERVER_USER=root

# JDK 17+/21 必需的模块开放（Hadoop 守护进程）
export HADOOP_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.x509=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED -Djava.net.preferIPv4Stack=true $HADOOP_OPTS"
