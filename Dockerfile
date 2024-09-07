FROM eclipse-temurin:17-jdk-alpine

ENV APP_HOME /usr/src/app
WORKDIR $APP_HOME

COPY target/*.jar $APP_HOME/app.jar
   
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

