#!/bin/bash

WORKING_DIR=`pwd`

MAVEN_INDEXER=$WORKING_DIR/maven-miner-parent/maven-miner-indexer
MAVEN_MINER=$WORKING_DIR/maven-miner-parent/maven-miner-aether
FILES=$WORKING_DIR/files
MAVEN_INDEXER_DIR=$FILES/maven-indexer
MAVEN_MINER_DIR=$FILES/maven-aether

mkdir $MAVEN_MINER_DIR
mkdir $MAVEN_INDEXER_DIR

echo "Removing existing jars"
INDEXER_JAR=target/maven-miner-indexer-*-jar-with-dependencies.jar
AETHER_JAR=target/maven-miner-aether-*-jar-with-dependencies.jar
rm -f $INDEXER_JAR
rm -f $AETHER_JAR
echo "Building maven-miner Indexer"
cd $MAVEN_INDEXER
mvn clean package
mv -f $INDEXER_JAR $MAVEN_INDEXER_DIR/miner-indexer.jar

echo "Building maven-miner Indexer"
cd $MAVEN_MINER
mvn clean package
mv -f $AETHER_JAR $MAVEN_MINER_DIR/miner-aether.jar
