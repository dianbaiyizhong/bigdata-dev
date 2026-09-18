FROM adoptopenjdk/openjdk8
WORKDIR /app
COPY bigdata-dev-backend-1.0.jar app.jar
ADD hive-site.xml /usr/bigdata/apache-hive-4.1.0-bin/conf/


ADD spark-3.5.9-bin-hadoop3.tgz /usr/bigdata/
ADD spark-defaults.conf /usr/bigdata/spark-3.5.9-bin-hadoop3/conf/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]