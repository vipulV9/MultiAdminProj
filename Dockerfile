# Use OpenJDK 17 base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy jar from target to container
COPY target/*.jar app.jar

# Expose port (change if your app runs on a different port)
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
