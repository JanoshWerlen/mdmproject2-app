# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jre-slim

# Set the working directory
WORKDIR /app

# Copy the current directory contents into the container at /app
COPY target/*.jar app.jar

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
