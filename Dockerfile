FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src ./src
RUN mkdir -p out && javac -d out $(find src -name '*.java')

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "out", "tw.winlab.reportlab.Main"]
