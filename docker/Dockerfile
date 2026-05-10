FROM ubuntu:24.04


ADD apache-zookeeper-3.8.6-bin.tar.gz /usr/bigdata/
ADD hadoop-3.5.0-aarch64.tar.gz /usr/bigdata/
ADD openjdk-21.0.2_linux-aarch64_bin.tar.gz /usr/bigdata/
ADD scala-2.12.11.tgz /usr/bigdata/
ADD spark-3.5.8-bin-hadoop3.gz /usr/bigdata/
ADD apache-hive-4.2.0-bin.tar.gz /usr/bigdata/

## 各种配置文件
ADD hadoop-env.sh /usr/bigdata/hadoop-3.5.0/etc/hadoop/
ADD core-site.xml /usr/bigdata/hadoop-3.5.0/etc/hadoop/
ADD hdfs-site.xml /usr/bigdata/hadoop-3.5.0/etc/hadoop/
ADD workers /usr/bigdata/hadoop-3.5.0/etc/hadoop/
ADD start-dfs.sh /usr/bigdata/hadoop-3.5.0/sbin/
ADD stop-dfs.sh /usr/bigdata/hadoop-3.5.0/sbin/
ADD hdfs /usr/bigdata/hadoop-3.5.0/bin/
ADD yarn-site.xml /usr/bigdata/hadoop-3.5.0/etc/hadoop/
ADD start-yarn.sh /usr/bigdata/hadoop-3.5.0/sbin/
ADD stop-yarn.sh /usr/bigdata/hadoop-3.5.0/sbin/
ADD yarn /usr/bigdata/hadoop-3.5.0/bin/
ADD spark_workers /usr/bigdata/spark-3.5.8-bin-hadoop3/conf/workers
ADD spark-env.sh /usr/bigdata/spark-3.5.8-bin-hadoop3/conf/
ADD spark-config.sh /usr/bigdata/spark-3.5.8-bin-hadoop3/sbin
ADD spark-defaults.conf /usr/bigdata/spark-3.5.8-bin-hadoop3/conf/
ADD hive-site.xml /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD hive-log4j2.properties /usr/bigdata/apache-hive-4.2.0-bin/conf/
ADD hive-env.sh /usr/bigdata/apache-hive-4.2.0-bin/conf/



#MYSQL驱动
ADD mysql-connector-j-8.0.31.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/mysql-connector-java.jar

ADD mysql-connector-j-8.0.31.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/mysql-connector-java.jar

ADD bigdata-dev-backend-1.0.jar /usr/local/bigdata-dev-backend.jar


# 添加s3相关
ADD jars/aws-java-sdk-core-1.12.797.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/aws-java-sdk-core-1.12.797.jar
ADD jars/aws-java-sdk-dynamodb-1.12.797.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/aws-java-sdk-dynamodb-1.12.797.jar
ADD jars/aws-java-sdk-s3-1.12.797.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/aws-java-sdk-s3-1.12.797.jar
ADD jars/bundle-2.44.3.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/bundle-2.44.3.jar

ADD jars/commons-configuration2-2.1.1.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/commons-configuration2-2.1.1.jar
ADD jars/hadoop-aws-3.3.4.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/hadoop-aws-3.3.4.jar
ADD jars/protobuf-java-3.21.9.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/protobuf-java-3.21.9.jar
ADD jars/stax2-api-4.2.1.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/stax2-api-4.2.1.jar
ADD jars/woodstox-core-5.3.0.jar /usr/bigdata/spark-3.5.8-bin-hadoop3/jars/woodstox-core-5.3.0.jar

ADD jars/hadoop-aws-3.5.0.jar /usr/bigdata/hadoop-3.5.0/share/hadoop/common/hadoop-aws-3.5.0.jar
ADD jars/bundle-2.44.3.jar /usr/bigdata/hadoop-3.5.0/share/hadoop/common/bundle-2.44.3.jar

ADD jars/bundle-2.44.3.jar /usr/bigdata/apache-hive-4.2.0-bin/lib/bundle-2.44.3.jar



## 设置环境变量

ENV SCALA_HOME=/usr/bigdata/scala-2.12.11
ENV PATH=$PATH:$SCALA_HOME/bin

ENV JAVA_HOME=/usr/bigdata/jdk-21.0.2
ENV PATH=$JAVA_HOME/bin:$PATH

ENV HADOOP_HOME=/usr/bigdata/hadoop-3.5.0
ENV PATH=$PATH:$HADOOP_HOME/bin


ENV HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin
ENV PATH=$PATH:$HIVE_HOME/bin





# 设置北京时间
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo "Asia/Shanghai" > /etc/timezone


## 创建hive目录
RUN mkdir /usr/bigdata/apache-hive-4.2.0-bin/logs


COPY ./start-as-master.sh /
RUN chmod +x /start-as-master.sh
COPY ./start-as-slave.sh /
RUN chmod +x /start-as-slave.sh
COPY ./restart.sh /
RUN chmod +x /restart.sh
RUN chmod -R 777 /usr/bigdata


RUN apt-get update && apt-get install -y openssh-server
RUN mkdir /var/run/sshd
RUN echo 'root:root' | chpasswd
RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config

# SSH login fix. Otherwise user is kicked off after login
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd



RUN echo 'export SCALA_HOME=/usr/bigdata/scala-2.12.11' >> /etc/profile
RUN echo 'export PATH=$PATH:$SCALA_HOME/bin' >> /etc/profile
RUN echo 'export JAVA_HOME=/usr/bigdata/jdk-21.0.2' >> /etc/profile
RUN echo 'export PATH=$PATH:$JAVA_HOME/bin' >> /etc/profile
RUN echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.5.0' >> /etc/profile
RUN echo 'export PATH=$PATH:$HADOOP_HOME/bin' >> /etc/profile
RUN echo 'export HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin' >> /etc/profile
RUN echo 'export PATH=$PATH:$HIVE_HOME/bin' >> /etc/profile


RUN apt-get install -y net-tools
RUN apt-get install -y sudo
RUN apt-get install -y curl
RUN apt-get install -y git
RUN apt-get install -y vim
RUN apt-get install -y zsh
# 安装zsh
RUN  yes | sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
# 安装自动补全插件
RUN git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh}/plugins/zsh-autosuggestions
RUN sed -i 's/plugins=(git)/plugins=(git zsh-autosuggestions)/' ~/.zshrc


RUN echo 'export SCALA_HOME=/usr/bigdata/scala-2.12.11' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$SCALA_HOME/bin' >> /root/.zshrc
RUN echo 'export JAVA_HOME=/usr/bigdata/jdk-21.0.2' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$JAVA_HOME/bin' >> /root/.zshrc
RUN echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.5.0' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$HADOOP_HOME/bin' >> /root/.zshrc
RUN echo 'export HIVE_HOME=/usr/bigdata/apache-hive-4.2.0-bin' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$HIVE_HOME/bin' >> /root/.zshrc

# 自动设置zsh为默认终端
RUN chsh -s /bin/zsh


RUN ln -s /usr/bigdata/jdk-21.0.2/bin/java /bin/java

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
