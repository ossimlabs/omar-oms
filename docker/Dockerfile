ARG BASE_IMAGE
FROM ${BASE_IMAGE}
ENV SPRING_CLOUD_CONFIG_ENABLED=true \
    SPRING_CLOUD_DISCOVERY_ENABLED=true \
    HOME=/home/omar
WORKDIR $HOME
EXPOSE 8080
COPY *.jar $HOME
CMD $HOME/run.sh
USER root
RUN apk add --no-cache fontconfig ttf-dejavu
USER nobody