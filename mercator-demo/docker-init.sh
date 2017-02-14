#!/bin/bash


/start-neo4j.sh

cd /var/lib/projector

JAR_FILE=$(find ./lib -name 'projector-*-all.jar' | head -1)

java -jar ${JAR_FILE} &


tail -f /dev/null
