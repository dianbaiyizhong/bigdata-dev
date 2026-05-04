#!/bin/bash
echo "config zookeeper zk myid:$1..."
echo $1 > /usr/bigdata/apache-zookeeper-3.6.3-bin/data/zk/myid

/usr/bigdata/apache-zookeeper-3.6.3-bin/bin/zkServer.sh start
