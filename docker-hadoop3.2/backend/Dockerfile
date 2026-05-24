FROM adoptopenjdk/openjdk8
WORKDIR /app
COPY bigdata-dev-backend-1.0.jar app.jar
ADD hive-site.xml /usr/bigdata/apache-hive-3.1.2-bin/conf/


ADD spark-3.2.1-bin-hadoop3.2.gz /usr/bigdata/
ADD spark-defaults.conf /usr/bigdata/spark-3.2.1-bin-hadoop3.2/conf/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]