#az group create --name mdmproject2 --location switzerlandnorth
#az appservice plan create --name mdmproject2 --resource-group mdmproject2 --sku B1 --is-linux

#docker build -t werleja1/mdmproject2_app:latest . 
#docker push werleja1/mdmproject2_app:latest   
#az webapp create --resource-group mdmproject2 --plan mdmproject2 --name mdm-project-2-app --deployment-container-image-name werleja1/mdmproject2_app:latest






# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim AS builder

# Install required libraries in the builder stage
RUN apt-get update && apt-get install -y libfreetype6 libfontconfig1 ffmpeg && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the Maven configuration and wrapper script
COPY pom.xml mvnw ./
COPY .mvn .mvn/

# Copy the source code
COPY src src/

# Ensure the Maven wrapper and other scripts are executable
RUN chmod +x mvnw
RUN find .mvn -type f -exec chmod +x {} \;

# Attempt to run Maven to download dependencies and build the application
RUN ./mvnw dependency:go-offline
RUN ./mvnw -Dmaven.test.skip=true package

# Use a smaller runtime image for the final stage
FROM openjdk:21-jdk-slim

# Install required libraries in the runtime stage
RUN apt-get update && apt-get install -y libfreetype6 libfontconfig1 ffmpeg && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the jar file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy the models directory to the container
COPY models /app/models

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
