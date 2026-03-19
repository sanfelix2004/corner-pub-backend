# 1. Stage di Build: usa Maven per compilare il JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia il file pom.xml e scarica le dipendenze
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia il codice sorgente e compila
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Stage di Runtime: usa JRE leggera per l'esecuzione
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia solo il JAR dallo stage di build
COPY --from=build /app/target/*.jar app.jar

# Espone la porta e comando di avvio
EXPOSE 8080
ENTRYPOINT ["java", "-Dlogging.level.root=DEBUG", "-jar", "app.jar"]
