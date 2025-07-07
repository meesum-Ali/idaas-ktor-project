FROM openjdk:21-slim
COPY build/libs/idaas-ktor.jar /app/idaas-ktor.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/idaas-ktor.jar"]