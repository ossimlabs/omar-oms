# OMAR OMS
https://github.com/ossimlabs/omar-oms

## Purpose
The OSSIM Mapping Service (OMS) application serves as a core communication channel to essential OSSIM functionality. Other servies such as the Web Map Service (WMS) communicate to OMS for data.

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
[https://artifactory.ossim.io/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-oms-app](https://artifactory.ossim.io/artifactory/webapp/#/artifacts/browse/tree/General/omar-local/io/ossim/omar/apps/omar-oms-app)

## Installation in Openshift

**Assumption:** The omar-oms-app docker image is pushed into the OpenShift server's internal docker registry and available to the project.

### Persistent Volumes

OMS requires shared access to OSSIM imagery data. This data is assumed to be accessible from the local filesystem of the pod. Therefore, a volume mount must be mapped into the container. A PersistentVolumeClaim should be mounted to a configured location (see environment variables) in the service, but is typically */data*

### Environment variables

|Variable|Value|
|------|------|
|SPRING_PROFILES_ACTIVE|Comma separated profile tags (*e.g. production, dev*)|
|SPRING_CLOUD_CONFIG_LABEL|The Git branch from which to pull config files (*e.g. master*)|
|OSSIM_PREFS_FILE|The location of the preferences file (*e.g. /usr/share/ossim/ossim-site-preferences)|
|OSSIM_INSTALL_PREFIX|The directory in which OSSIM is installed (*e.g. /usr*)|
|OSSIM_DATA|The location of shared imagery data (*e.g. /data*)|

### An Example DeploymentConfig
```yaml
apiVersion: v1
kind: DeploymentConfig
metadata:
  annotations:
    openshift.io/generated-by: OpenShiftNewApp
  creationTimestamp: null
  generation: 1
  labels:
    app: omar-openshift
  name: omar-oms-app
spec:
  replicas: 1
  selector:
    app: omar-openshift
    deploymentconfig: omar-oms-app
  strategy:
    activeDeadlineSeconds: 21600
    resources: {}
    rollingParams:
      intervalSeconds: 1
      maxSurge: 25%
      maxUnavailable: 25%
      timeoutSeconds: 600
      updatePeriodSeconds: 1
    type: Rolling
  template:
    metadata:
      annotations:
        openshift.io/generated-by: OpenShiftNewApp
      creationTimestamp: null
      labels:
        app: omar-openshift
        deploymentconfig: omar-oms-app
    spec:
      containers:
      - env:
        - name: SPRING_PROFILES_ACTIVE
          value: dev
        - name: SPRING_CLOUD_CONFIG_LABEL
          value: master
        - name: OSSIM_PREFS_FILE
          value: /usr/share/ossim/ossim-site-preferences
        - name: OSSIM_INSTALL_PREFIX
          value: /usr
        - name: OSSIM_DATA
          value: /data
        image: 172.30.181.173:5000/o2/omar-oms-app@sha256:cedfb03614eb0ad2386a073793bff517b0e2c47864ed7698e3f7c46884753b7b
        imagePullPolicy: Always
        livenessProbe:
          failureThreshold: 3
          initialDelaySeconds: 60
          periodSeconds: 10
          successThreshold: 1
          tcpSocket:
            port: 8080
          timeoutSeconds: 1
        name: omar-oms-app
        ports:
        - containerPort: 8080
          protocol: TCP
        readinessProbe:
          failureThreshold: 3
          initialDelaySeconds: 30
          periodSeconds: 10
          successThreshold: 1
          tcpSocket:
            port: 8080
          timeoutSeconds: 1
        resources: {}
        securityContext:
          capabilities:
            add:
            - SYS_ADMIN
          privileged: true
          runAsUser: 1001
        terminationMessagePath: /dev/termination-log
        volumeMounts:
        - mountPath: /data
          name: volume-oms
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
      - name: volume-oms
        persistentVolumeClaim:
          claimName: ossim-data-dev-pvc
  test: false
  triggers:
  - imageChangeParams:
      automatic: true
      containerNames:
      - omar-oms-app
      from:
        kind: ImageStreamTag
        name: omar-oms-app:latest
        namespace: o2
    type: ImageChange
  - type: ConfigChange
status:
  availableReplicas: 0
  latestVersion: 0
  observedGeneration: 0
  replicas: 0
  unavailableReplicas: 0
  updatedReplicas: 0
```
