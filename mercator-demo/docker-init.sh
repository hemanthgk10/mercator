#!/bin/bash


/start-neo4j.sh

cd /var/lib/mercator

JAR_FILE=$(find ./lib -name 'mercator-*-all.jar' | head -1)

java -jar ${JAR_FILE} &


tail -f /dev/null
