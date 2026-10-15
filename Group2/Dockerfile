# ============ 第 1 阶段：把 Angular 前端编译成静态文件 ============
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ============ 第 2 阶段：把前端产物放进 Spring Boot 并打成 jar ============
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY . .
COPY --from=frontend /build/frontend/dist/browser/ src/main/resources/static/products/
RUN mvn -q -DskipTests package

# ============ 第 3 阶段：只留 JRE + jar 的最终运行镜像 ============
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/target/shopping-cart-*.jar app.jar
EXPOSE 18080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
