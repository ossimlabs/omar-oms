#!/bin/bash
#
# ENV Variables:
#
# GOOFY_OPTS - options for goofy
# MOUNT_POINT - mount point for s3 buckets.  Should have default of /s3
# 
# AWS_ACCESS_KEY - AWS key.  Optional
# AWS_SECRET_KEY - AWS Secret Key. Optional
# JAVA_OPTS - Arguments to pass to java to start the app.  Optional
# 
if [ ! -z $AWS_ACCESS_KEY ] ; then
  export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY}
fi

if [ ! -z $AWS_SECRET_KEY ] ; then
  export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_KEY}
fi

export USER_ID=$(id -u)
export GROUP_ID=$(id -g)
sed '/^omar/d' /etc/passwd > /tmp/passwd
echo omar:x:$USER_ID:$GROUP_ID:Default Application User:$HOME:/sbin/nologin >> /tmp/passwd

export LD_PRELOAD=/usr/lib64/libnss_wrapper.so
export NSS_WRAPPER_PASSWD=/tmp/passwd
export NSS_WRAPPER_GROUP=/etc/group

if [ -z JAVA_OPTS ] ; then
   JAVA_OPTS="-server -Xms256m -Xmx1024m -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom"
fi
if [ -z $MOUNT_POINT ] ; then
  export MOUNT_POINT=/s3
fi

if [ -z $GOOFY_OPTS ] ; then
   GOOFY_OPTS="-o allow_other"
fi
GOOFY_OPTS="-f ${GOOFY_OPTS}"
if [ ! -z $BUCKETS ] ; then
   #!/bin/bash
   SPLIT_BUCKET=${BUCKETS//\,/ }

   for BUCKET in $SPLIT_BUCKET ; do
      mkdir -p /${MOUNT_POINT}/$BUCKET
      goofys ${GOOFY_OPTS} ${BUCKET} ${MOUNT_POINT}/${BUCKET} &
   done
fi

export JAR_FILE=`find $HOME -name "*.jar"`
java $JAVA_OPTS -jar $JAR_FILE
