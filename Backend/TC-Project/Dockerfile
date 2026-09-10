# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run with JRE (Jammy au lieu de Alpine pour la compatibilité ONNX / IA)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/RAG_pdf
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
