# ─── Stage 1: build the jar with Maven (no local Maven/JDK needed) ──────────
# Dependencies are resolved in their own layer so that source-only changes
# don't re-download the world on every rebuild.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ─── Stage 2: slim runtime ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8000
# Capped heap so the JVM doesn't reserve ~25% of host RAM by default.
# SerialGC = no background GC threads (lower idle CPU + footprint on a small box).
# TieredStopAtLevel=1 trims JIT compiler work at startup so the CPU doesn't spike.
ENV JAVA_OPTS="-Xmx128m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
