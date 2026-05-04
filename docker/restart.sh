#!/bin/bash




/usr/bigdata/hadoop-3.5.0/sbin/start-dfs.sh
/usr/bigdata/hadoop-3.5.0/sbin/start-yarn.sh
/usr/bigdata/hadoop-3.5.0/sbin/yarn-daemon.sh start timelineserver
/usr/bigdata/hadoop-3.5.0/sbin/mr-jobhistory-daemon.sh start historyserver
/usr/bigdata/spark-3.5.8-bin-hadoop3/sbin/start-all.sh
/usr/bigdata/spark-3.5.8-bin-hadoop3/sbin/start-history-server.sh


# 启动hive
nohup hive --service metastore 2>&1 &
nohup hive --service hiveserver2 -hiveconf hive.execution.engine=mr 2>&1 &