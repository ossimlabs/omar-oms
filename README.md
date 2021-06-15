# omar-oms

## Description

The OMAR OSSIM Mapping Service contains all of OSSIM's functionality, binding it via JNI to be web accessible.

[![Build Status](https://jenkins.ossim.io/buildStatus/icon?job=omar-oms-dev)]()

### Required environment variable
- OMAR_COMMON_PROPERTIES

### Optional environment variables
Only required for Jenkins pipelines or if you are running Nexus and/or Openshift locally

- OPENSHIFT_USERNAME
- OPENSHIFT_PASSWORD
- REPOSITORY_MANAGER_USER
- REPOSITORY_MANAGER_PASSWORD

## How to Install omar-oms-plugin locally

0. Ensure joms is installed. See [ossim-oms/README.md](http://github.com/ossimlabs/ossim-oms/blob/master/README.md).

1. Git clone the following repos or git pull the latest versions if you already have them.

``` sh
  git clone https://github.com/ossimlabs/omar-common.git
  git clone https://github.com/ossimlabs/omar-core.git
  git clone https://github.com/ossimlabs/omar-openlayers.git
  git clone https://github.com/ossimlabs/omar-oms.git
```

2. Set O2_INLINE_BUILD environment variable to true:

``` sh
  export O2_INLINE_BUILD=1
```

3. Install omar-core-plugin (it is part of the omar-core repo).

``` sh
 cd omar-core/plugins/omar-core-plugin
 gradle clean install
```

4. Install omar-openlayers-plugin

``` sh
 cd omar-openlayers/plugins/omar-openlayers-plugin
 gradle clean install
```

5. Install omar-oms-plugin

``` sh
 cd omar-oms
 ./gradlew -p apps/omar-oms-app assemble
```
 test
