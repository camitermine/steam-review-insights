# Etapa 1: compilar. Usa el JDK completo y el Maven Wrapper del proyecto.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primero solo el pom: así Docker cachea las dependencias y no las vuelve a bajar
# cada vez que cambia el código.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# Etapa 2: ejecutar. Solo el JRE y el .jar: la imagen final es mucho más chica.
FROM eclipse-temurin:21-jre
WORKDIR /app

# No correr como root dentro del contenedor.
RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
