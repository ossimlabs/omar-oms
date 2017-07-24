#!/bin/bash
if [ ! -z $AWS_ACCESS_KEY ] ; then
  export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY
  export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
fi
mkdir -p /mnt/s3/$BUCKET
echo "BUCKET = ${BUCKET}"
riofs -o allow_other -c /usr/share/omar/riofs.conf.xml  $BUCKET /mnt/s3/$BUCKET

java -server -Xms256m -Xmx1024m -Djava.awt.headless=true -XX:+CMSClassUnloadingEnabled -XX:+UseGCOverheadLimit -Djava.security.egd=file:/dev/./urandom -jar /usr/share/omar/omar-oms-app.jar
