FROM nexus-docker-public-hosted.ossim.io/openjdk8:alpine-slim

ENV HOME /home/omar
RUN adduser -D -h $HOME -s /sbin/nologin -u 1001 omar
COPY run.sh $HOME
RUN chown 1001:0 -R $HOME && \
    chmod 777 $HOME && \
    chmod 777 $HOME/*.sh
USER 1001

ENV SPRING_CLOUD_CONFIG_ENABLED=true \
    SPRING_CLOUD_DISCOVERY_ENABLED=true
WORKDIR $HOME
EXPOSE 8080
COPY *.jar $HOME/
CMD ["/bin/sh", "/home/omar/run.sh"]