#!/bin/bash




/usr/bigdata/hadoop-3.4.3/sbin/start-dfs.sh
# 要解除安全模式，否则会报错https://cloud.tencent.com/developer/article/2039410
hadoop dfsadmin -safemode leave
/usr/bigdata/hadoop-3.4.3/sbin/start-yarn.sh
/usr/bigdata/hadoop-3.4.3/sbin/yarn-daemon.sh start timelineserver
/usr/bigdata/hadoop-3.4.3/sbin/mr-jobhistory-daemon.sh start historyserver
/usr/bigdata/spark-3.5.9-bin-hadoop3/sbin/start-all.sh
/usr/bigdata/spark-3.5.9-bin-hadoop3/sbin/start-history-server.sh


# 启动hive
# nohup hive --service metastore 2>&1 &
nohup hive --service hiveserver2 -hiveconf hive.execution.engine=mr 2>&1 &
# nohup java -jar /usr/local/bigdata-dev-backend.jar 2>&1 &