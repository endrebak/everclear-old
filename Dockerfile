FROM openjdk:8-alpine

COPY target/uberjar/everclear.jar /everclear/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/everclear/app.jar"]
