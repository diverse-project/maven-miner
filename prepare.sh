#!/bin/bash

WORKING_DIR=`pwd`

MAVEN_INDEXER=$WORKING_DIR/maven-miner-parent/maven-miner-indexer
MAVEN_MINER=$WORKING_DIR/maven-miner-parent/maven-miner-aether
FILES=$WORKING_DIR/files
echo "Removing existing jars"
INDEXER_JAR=target/maven-miner-indexer-*-jar-with-dependencies.jar
AETHER_JAR=target/maven-miner-aether-*-jar-with-dependencies.jar
rm -f $INDEXER_JAR
rm -f $AETHER_JAR
echo "Building maven-miner Indexer"
cd $MAVEN_INDEXER
mvn clean package
mv -f $INDEXER_JAR $FILES/maven-miner-indexer.jar

echo "Building maven-miner Indexer"
cd $MAVEN_MINER
mvn clean package
mv -f $AETHER_JAR $FILES/maven-miner-aether.jar
