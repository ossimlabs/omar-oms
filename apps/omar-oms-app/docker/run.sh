#!/bin/bash
if [ ! -z $AWS_ACCESS_KEY ] ; then
  export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY
  export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
fi
mkdir -p /s3/$BUCKET
#echo "BUCKET = ${BUCKET}"
#riofs -o allow_other -c /usr/share/omar/riofs.conf.xml  $BUCKET /s3/$BUCKET
export JAR_FILE=`find /usr/share/omar -name "*.jar"`
java -server -Xms256m -Xmx1024m -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom -jar $JAR_FILE
