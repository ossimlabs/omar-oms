# OMAR OMS

## Dockerfile
```
FROM omar-ossim-base
ENV SPRING_CLOUD_CONFIG_ENABLED true
ENV SPRING_CLOUD_DISCOVERY_ENABLED true
ENV HOME /home/omar
COPY run.sh $HOME/run.sh
COPY omar-oms-app-1.1.0-SNAPSHOT.jar $HOME
USER root
RUN yum -y install
RUN yum -y install nss_wrapper
RUN yum -y install gfuse
RUN yum -y install fuse-libs
RUN yum -y install libevent
RUN yum -y install curl
RUN yum clean all
RUN curl -k -s "https://s3.amazonaws.com/ossimlabs/dependencies/riofs/riofs" -o /usr/bin/riofs
RUN curl -k -s "https://s3.amazonaws.com/ossimlabs/dependencies/goofys/goofys" -o /usr/bin/goofys
RUN chmod +x /usr/bin/goofys
RUN mkdir -p /s3
RUN chown -R 1001:0 /s3
RUN chmod 777 /s3
RUN chmod ugo+x /usr/bin/riofs
RUN echo "user_allow_other" > /etc/fuse.conf
RUN chown -R 1001:0 $HOME
RUN chmod +x $HOME/run.sh
RUN find $HOME -type d -exec chmod g+x {} +
USER 1001
WORKDIR $HOME
EXPOSE 8080
CMD $HOME/run.sh
```
Ref: [omar-ossim-base](../../../omar-ossim-base/docs/install-guide/omar-ossim-base/)

## JAR
[http://artifacts.radiantbluecloud.com/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-oms-app](http://artifacts.radiantbluecloud.com/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-oms-app)
