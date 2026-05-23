FROM adoptopenjdk/openjdk8
WORKDIR /app
COPY bigdata-dev-backend-1.0.jar app.jar
ADD hive-site.xml /usr/bigdata/apache-hive-3.1.2-bin/conf/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]