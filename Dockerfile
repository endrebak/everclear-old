FROM openjdk:8-alpine

COPY target/uberjar/everflow.jar /everflow/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/everflow/app.jar"]
