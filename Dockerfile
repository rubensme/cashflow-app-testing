FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY cashflow-app/pom.xml cashflow-app/pom.xml
COPY cashflow-app/src cashflow-app/src

RUN mvn -B -f cashflow-app/pom.xml clean install -DskipTests

COPY cashflow-web/pom.xml cashflow-web/pom.xml
COPY cashflow-web/src cashflow-web/src

RUN mvn -B -f cashflow-web/pom.xml clean package -DskipTests


FROM eclipse-temurin:21-jre-noble AS runtime

WORKDIR /app

COPY --from=build \
    /workspace/cashflow-web/target/cashflow-web-0.0.1-SNAPSHOT.jar \
    app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]