FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/layer-line-1.0.jar app.jar
ENV PORT=8080
EXPOSE 8080
USER 10001
CMD ["java", "-cp", "app.jar", "layerline.LayerLineWebApp"]
