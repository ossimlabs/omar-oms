#!/bin/bash
if [ ! -z $AWS_ACCESS_KEY ] ; then
  export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY
  export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
fi

#export USER_ID=$(id -u)
#export GROUP_ID=$(id -g)
#sed '/^omar/d' /etc/passwd > /tmp/passwd
#echo omar:x:$USER_ID:$GROUP_ID:Default Application User:$HOME:/sbin/nologin >> /tmp/passwd

#export LD_PRELOAD=/usr/lib64/libnss_wrapper.so
#export NSS_WRAPPER_PASSWD=/tmp/passwd
#export NSS_WRAPPER_GROUP=/etc/group
if [ -z $MOUNT_POINT ] ; then
  export MOUNT_POINT=/s3
fi

if [ ! -z $BUCKETS ] ; then
   #!/bin/bash
   SPLIT_BUCKET=${BUCKETS//\,/ }

   for BUCKET in $SPLIT_BUCKET ; do
      mkdir -p /s3/$BUCKET
      goofys -f -o allow_other $BUCKET $MOUNT_POINT/$BUCKET &
   done
fi

export JAR_FILE=`find $HOME -name "*.jar"`
java -server -Xms256m -Xmx1024m -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom -jar $JAR_FILE
