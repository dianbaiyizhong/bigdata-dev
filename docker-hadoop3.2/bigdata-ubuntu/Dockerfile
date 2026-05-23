FROM ubuntu:24.04



RUN echo "deb https://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb https://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb https://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb https://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse" >> /etc/apt/sources.list

# 更新软件源
RUN apt-get update


ADD hadoop-3.2.0.tar.gz /usr/bigdata/
ADD bellsoft-jdk8u492+9-linux-aarch64.tar.gz /usr/bigdata/
ADD scala-2.12.11.tgz /usr/bigdata/
ADD spark-3.2.1-bin-hadoop3.2.gz /usr/bigdata/
ADD apache-hive-3.1.2-bin.tar.gz /usr/bigdata/

## 各种配置文件
ADD hadoop-env.sh /usr/bigdata/hadoop-3.2.0/etc/hadoop/
ADD core-site.xml /usr/bigdata/hadoop-3.2.0/etc/hadoop/
ADD hdfs-site.xml /usr/bigdata/hadoop-3.2.0/etc/hadoop/
ADD workers /usr/bigdata/hadoop-3.2.0/etc/hadoop/
ADD start-dfs.sh /usr/bigdata/hadoop-3.2.0/sbin/
ADD stop-dfs.sh /usr/bigdata/hadoop-3.2.0/sbin/
ADD hdfs /usr/bigdata/hadoop-3.2.0/bin/
ADD yarn-site.xml /usr/bigdata/hadoop-3.2.0/etc/hadoop/
ADD start-yarn.sh /usr/bigdata/hadoop-3.2.0/sbin/
ADD stop-yarn.sh /usr/bigdata/hadoop-3.2.0/sbin/
ADD yarn /usr/bigdata/hadoop-3.2.0/bin/
ADD spark_workers /usr/bigdata/spark-3.2.1-bin-hadoop3.2/conf/workers
ADD spark-env.sh /usr/bigdata/spark-3.2.1-bin-hadoop3.2/conf/
ADD spark-config.sh /usr/bigdata/spark-3.2.1-bin-hadoop3.2/sbin
ADD spark-defaults.conf /usr/bigdata/spark-3.2.1-bin-hadoop3.2/conf/
ADD hive-site.xml /usr/bigdata/apache-hive-3.1.2-bin/conf/
ADD hive-log4j2.properties /usr/bigdata/apache-hive-3.1.2-bin/conf/
ADD hive-env.sh /usr/bigdata/apache-hive-3.1.2-bin/conf/



#MYSQL驱动
ADD mysql-connector-j-8.0.31.jar /usr/bigdata/apache-hive-3.1.2-bin/lib/mysql-connector-java.jar

ADD mysql-connector-j-8.0.31.jar /usr/bigdata/spark-3.2.1-bin-hadoop3.2/jars/mysql-connector-java.jar

# ADD bigdata-dev-backend-1.0.jar /usr/local/bigdata-dev-backend.jar





## 设置环境变量

ENV SCALA_HOME=/usr/bigdata/scala-2.12.11
ENV PATH=$PATH:$SCALA_HOME/bin

ENV JAVA_HOME=/usr/bigdata/jdk8u492
ENV PATH=$JAVA_HOME/bin:$PATH

ENV HADOOP_HOME=/usr/bigdata/hadoop-3.2.0
ENV PATH=$PATH:$HADOOP_HOME/bin


ENV HIVE_HOME=/usr/bigdata/apache-hive-3.1.2-bin
ENV PATH=$PATH:$HIVE_HOME/bin





# 设置北京时间
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
RUN echo "Asia/Shanghai" > /etc/timezone


## 创建hive目录
RUN mkdir /usr/bigdata/apache-hive-3.1.2-bin/logs


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
RUN echo 'export JAVA_HOME=/usr/bigdata/jdk8u492' >> /etc/profile
RUN echo 'export PATH=$PATH:$JAVA_HOME/bin' >> /etc/profile
RUN echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.2.0' >> /etc/profile
RUN echo 'export PATH=$PATH:$HADOOP_HOME/bin' >> /etc/profile
RUN echo 'export HIVE_HOME=/usr/bigdata/apache-hive-3.1.2-bin' >> /etc/profile
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
RUN echo 'export JAVA_HOME=/usr/bigdata/jdk8u492' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$JAVA_HOME/bin' >> /root/.zshrc
RUN echo 'export HADOOP_HOME=/usr/bigdata/hadoop-3.2.0' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$HADOOP_HOME/bin' >> /root/.zshrc
RUN echo 'export HIVE_HOME=/usr/bigdata/apache-hive-3.1.2-bin' >> /root/.zshrc
RUN echo 'export PATH=$PATH:$HIVE_HOME/bin' >> /root/.zshrc

# 自动设置zsh为默认终端（不能设置，会报错bash v3.2+ is required. Sorry.）
# RUN chsh -s /bin/zsh


RUN ln -s /usr/bigdata/jdk8u492/bin/java /bin/java

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
