#!/bin/bash


rm -rf lib
mkdir lib
../gradlew -b ./build.gradle shadowJar

if [ ! -f ./build/neo4j-community-3.1.1-unix.tar.gz ]; then
  
  curl -o ./build/neo4j-community-3.1.1-unix.tar.gz http://dist.neo4j.org/neo4j-community-3.1.1-unix.tar.gz
fi


docker build . -t mercator-demo