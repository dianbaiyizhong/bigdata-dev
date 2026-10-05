#!/usr/bin/env bash

# This file is sourced when running various Spark programs.

# Hadoop 配置目录（读取 HDFS/YARN 配置）
export HADOOP_CONF_DIR=/usr/bigdata/hadoop-3.5.0/etc/hadoop/

# JDK 21
export JAVA_HOME=/usr/bigdata/jdk21

export SPARK_HISTORY_OPTS="
-Dspark.history.ui.port=18080
-Dspark.history.fs.logDirectory=hdfs://linux001:9820/logs
-Dspark.history.retainedApplications=30"
