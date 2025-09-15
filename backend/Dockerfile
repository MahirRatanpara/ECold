FROM openjdk:17-jdk-alpine

# Install wget for health checks and Maven
RUN apk add --no-cache wget maven

WORKDIR /app

# Copy pom.xml first
COPY pom.xml ./

# Download dependencies using system Maven
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src src/
RUN mvn clean package -DskipTests -B

# Copy the built JAR
RUN cp target/ecold-backend-*.jar app.jar

# Create uploads directory
RUN mkdir -p uploads

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]