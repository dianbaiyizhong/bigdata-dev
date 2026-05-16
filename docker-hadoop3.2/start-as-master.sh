#!/bin/bash

#!/bin/bash

ssh-keygen -t rsa
ssh-copy-id linux001
ssh-copy-id linux002
ssh-copy-id linux003


hdfs namenode -format
/usr/bigdata/hadoop-3.2.0/sbin/start-dfs.sh
hadoop fs -mkdir /logs
/usr/bigdata/hadoop-3.2.0/sbin/start-yarn.sh
/usr/bigdata/hadoop-3.2.0/sbin/yarn-daemon.sh start timelineserver
/usr/bigdata/hadoop-3.2.0/sbin/mr-jobhistory-daemon.sh start historyserver
/usr/bigdata/spark-3.2.1-bin-hadoop3.2/sbin/start-all.sh
/usr/bigdata/spark-3.2.1-bin-hadoop3.2/sbin/start-history-server.sh

schematool -initSchema -dbType mysql
hadoop fs -mkdir -p /user/hive/warehouse
hadoop fs -chmod g+w /tmp
hadoop fs -chmod g+w /user/hive/warehouse

# 启动hive
# nohup hive --service metastore 2>&1 &
nohup hive --service hiveserver2 -hiveconf hive.execution.engine=mr 2>&1 &
nohup java -jar /usr/local/bigdata-dev-backend.jar 2>&1 &