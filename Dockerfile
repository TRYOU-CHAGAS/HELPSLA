FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:10.1-jdk17-temurin
ENV PORT=10000
COPY --from=build /app/target/helpsla.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/tomcat/bin/helpsla-entrypoint.sh
RUN chmod +x /usr/local/tomcat/bin/helpsla-entrypoint.sh
EXPOSE 10000
CMD ["/usr/local/tomcat/bin/helpsla-entrypoint.sh"]
