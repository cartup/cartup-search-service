FROM openjdk:8
ADD build/docker/entrypoint.sh /entrypoint.sh
ADD src/main/resources/application.properties /
COPY conf /conf/
RUN apt update && apt install vim -y
EXPOSE 8443 8080 8004
ENTRYPOINT ["bash", "/entrypoint.sh"]
ADD target/cartup-search-service-0.0.1-SNAPSHOT.war /cartup-search-service.war
