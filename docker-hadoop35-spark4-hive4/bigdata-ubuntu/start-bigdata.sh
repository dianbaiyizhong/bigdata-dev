#!/bin/bash
# 首次启动整个大数据集群（只在第一次 docker exec 进 linux001 手动执行一次）

# 1. 配置 SSH 免密
ssh-keygen -t rsa
sshpass -p 'root' ssh-copy-id -o StrictHostKeyChecking=no root@linux001
sshpass -p 'root' ssh-copy-id -o StrictHostKeyChecking=no root@linux002
sshpass -p 'root' ssh-copy-id -o StrictHostKeyChecking=no root@linux003

# 2. 格式化并启动 HDFS
hdfs namenode -format
/usr/bigdata/hadoop-3.5.0/sbin/start-dfs.sh
hadoop fs -mkdir /logs

# 3. 启动 YARN / TimelineServer / JobHistory
/usr/bigdata/hadoop-3.5.0/sbin/start-yarn.sh
/usr/bigdata/hadoop-3.5.0/sbin/yarn-daemon.sh start timelineserver
/usr/bigdata/hadoop-3.5.0/sbin/mr-jobhistory-daemon.sh start historyserver

# 4. 启动 Spark Standalone 与 HistoryServer
/usr/bigdata/spark-4.2.0-bin-hadoop3/sbin/start-all.sh
/usr/bigdata/spark-4.2.0-bin-hadoop3/sbin/start-history-server.sh

# 5. 初始化 Hive 元数据（MySQL）
schematool -initSchema -dbType mysql
hadoop fs -mkdir -p /user/hive/warehouse
hadoop fs -chmod g+w /tmp
hadoop fs -chmod g+w /user/hive/warehouse

# 6. 上传 Spark jars 到 HDFS（YARN 模式加速）
hadoop fs -mkdir -p /user/spark/jars

# 压缩上传
zip -jr spark-jars.zip /usr/bigdata/spark-4.2.0-bin-hadoop3/jars/*
hadoop fs -put -f spark-jars.zip /user/spark/spark-jars.zip
hadoop fs -put -f /usr/bigdata/spark-4.2.0-bin-hadoop3/jars/spark-yarn_2.13-4.2.0.jar /user/spark/jars/spark-yarn_2.13-4.2.0.jar

# 7. 启动 HiveServer2（Hive 4 默认引擎为 tez，这里显式使用 mr）
nohup hive --service hiveserver2 -hiveconf hive.execution.engine=mr 2>&1 &

# nohup java -jar /usr/local/bigdata-dev-backend.jar 2>&1 &
