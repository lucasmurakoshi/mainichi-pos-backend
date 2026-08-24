# ==========================================
# Etapa 1: Construcción (Build Stage)
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos del wrapper de Maven y pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x ./mvnw

# Descargar dependencias en caché
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY src/ src/

# Compilar y generar el paquete JAR (omitiendo tests durante el build)
RUN ./mvnw clean package -DskipTests

# ==========================================
# Etapa 2: Imagen de Ejecución (Runtime Stage)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario y grupo no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el artefacto JAR generado desde la etapa builder
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Variables de entorno configurables
ENV PORT=8080 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
