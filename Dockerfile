FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY . ./investment-advisor

WORKDIR /app


FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/investment-advisor/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
