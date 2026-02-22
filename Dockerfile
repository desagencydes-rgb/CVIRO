# Stage 1: Build the Java application using Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src
# Package the application (builds the WAR)
RUN mvn clean package -DskipTests

# Stage 2: Run the application on Tomcat
FROM tomcat:10.1-jdk21
WORKDIR /usr/local/tomcat

# Remove default Tomcat applications
RUN rm -rf webapps/*

# Copy our generated WAR file to the ROOT of the Tomcat server
COPY --from=build /app/target/cv-generator.war webapps/ROOT.war

# Expose Tomcat's default port
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
