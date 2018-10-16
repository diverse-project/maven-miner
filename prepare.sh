#!/bin/bash
WORKING_DIR=`pwd`
#variables instantiation
PARENT_DIR=$WORKING_DIR/maven-miner-parent
MAVEN_INDEXER=$PARENT_DIR/maven-miner-indexer
MAVEN_MINER=$PARENT_DIR/maven-miner-aether
MAVEN_PROC=$PARENT_DIR/maven-miner-procedures
FILES=$WORKING_DIR/miner
NEO4J=$WORKING_DIR/neo4j
MAVEN_INDEXER_DIR=$FILES/maven-indexer
MAVEN_MINER_DIR=$FILES/maven-aether
# Cleaning the repo
echo "Removing existing jars"
INDEXER_JAR=$MAVEN_INDEXER/target/maven-miner-indexer-*-jar-with-dependencies.jar
AETHER_JAR=$MAVEN_MINER/target/maven-miner-aether-*-jar-with-dependencies.jar
PROC_JAR=$MAVEN_PROC/target/maven-miner-procedures*.jar
rm -f $INDEXER_JAR
rm -f $AETHER_JAR
# Building and moving files
echo "Building maven-miner"
cd $PARENT_DIR
mvn clean package -Dmaven.test.skip=true
echo "Moving jars ..."

rm -f $MAVEN_MINER_DIR/miner-aether.jar
mv -f $AETHER_JAR $MAVEN_MINER_DIR/miner-aether.jar

rm -f $NEO4J/miner-proc.jar
mv -f $PROC_JAR $NEO4J/miner-proc.jar

echo "Building maven-miner"
cd $MAVEN_INDEXER
mvn clean package -Dmaven.test.skip=true
echo "Moving jars ..."
rm -f $MAVEN_INDEXER_DIR/miner-indexer.jar
mv -f $INDEXER_JAR $MAVEN_INDEXER_DIR/miner-indexer.jar
