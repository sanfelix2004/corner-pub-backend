# 1. Usa immagine base con Java 17
FROM eclipse-temurin:17-jdk
  
  # 2. Imposta cartella di lavoro
WORKDIR /app
  
  # 3. Copia il JAR buildato
COPY target/*.jar app.jar
  
  # 4. Espone la porta 8080 (quella usata da Spring Boot)
EXPOSE 8080
  
  # 5. Comando di avvio
ENTRYPOINT ["java", "-jar", "app.jar"]
