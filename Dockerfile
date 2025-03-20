FROM openjdk:17-slim AS build

RUN apt-get update && \
    apt-get install -y curl && \
    curl -sL "https://github.com/sbt/sbt/releases/download/v1.9.8/sbt-1.9.8.tgz" | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

ENV TZ=Asia/Bangkok

WORKDIR /app

COPY build.sbt .
COPY project project

RUN sbt update

COPY src src

RUN sbt -debug assembly

# ----------------------------------------

FROM gcr.io/distroless/java17-debian12 AS runtime

WORKDIR /app

COPY --from=build /app/target/scala-3.6.3/shogi8017-backend-assembly-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]