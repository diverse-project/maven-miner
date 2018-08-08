#!/bin/bash
SCRIPT_NAME=$0
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME --version <maven-miner-indexer version>"
  exit 1
}

DEFAULT_VERSION=0.2-SNAPSHOT
VERSION=$DEFAULT_VERSION
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --version)
    VERSION="$1"
    shift
    ;;
    *)
    print_usage_and_exit
    ;;
esac
done

WORKING_DIR=`pwd`

MAVEN_INDEXER=$WORKING_DIR/maven-miner-parent/maven-miner-indexer
MAVEN_MINER=$WORKING_DIR/maven-miner-parent/maven-miner-aether
FILES=$WORKING_DIR/files
INDEXER_JAR=target/maven-miner-indexer-$VERSION-jar-with-dependencies.jar
AETHER_JAR=target/maven-miner-aether-$VERSION-jar-with-dependencies.jar
echo "Building maven-miner Indexer"
cd $MAVEN_INDEXER
mvn clean package
mv -f $INDEXER_JAR $FILES/maven-miner-indexer.jar

echo "Building maven-miner Indexer"
cd $MAVEN_MINER
mvn clean package
mv -f $AETHER_JAR $FILES/maven-miner-aether.jar
