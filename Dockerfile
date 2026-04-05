# 루트에서: docker build --build-arg SVC=gateway-service -t msa-gateway:latest .
# SVC: gateway-service | order-service | inventory-service | user-service
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY benchmark-common/pom.xml benchmark-common/
COPY benchmark-common/src benchmark-common/src
COPY benchmark-feign-common/pom.xml benchmark-feign-common/
COPY benchmark-feign-common/src benchmark-feign-common/src
COPY gateway-service/pom.xml gateway-service/
COPY gateway-service/src gateway-service/src
COPY order-service/pom.xml order-service/
COPY order-service/src order-service/src
COPY inventory-service/pom.xml inventory-service/
COPY inventory-service/src inventory-service/src
COPY user-service/pom.xml user-service/
COPY user-service/src user-service/src
ARG SVC=gateway-service
RUN mvn -q -pl ${SVC} -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
ARG SVC=gateway-service
WORKDIR /app
COPY --from=build /app/${SVC}/target/${SVC}-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
