#!/bin/bash
SCRIPT_NAME=./publish-images.sh
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--all           Bulding and publishing all images <default>"
  echo "--producer      Bulding and publishing the producer"
  echo "--consumer      Bulding and publishing the consumer"
  echo "--rabbit        Bulding and publishing the rabbitMQ"
  echo "--neo4j         Bulding and publishing neo4j"
  echo "--tag           The tag of the built images"
  exit 1
}

function build_and_publish () {
  docker build -t amineben/$2:miner-$3 $1
  docker push amineben/$2:miner-$3
}

ALL=true
PRODUCER=false
CONSUMER=false
RABBIT=false
NEO4J=false
TAG=0.3.1
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --all)
    ALL=true
    shift
    ;;
    --consumer)
    CONSUMER=true
    ALL=false
    shift
    ;;
    --neo4j)
    NEO4J=true
    ALL=false
    shift
    ;;
    --rabbit)
    RABBIT=true
    ALL=false
    shift
    ;;
    --producer)
    PRODUCER=true
    ALL=false
    shift
    ;;
    --tag)
    TAG=$1
    shift
    ;;
    *)
esac
done

if [ "$ALL" == true ]
then
  build_and_publish neo4j neo4j $TAG
  build_and_publish rabbitmq rabbitmq $TAG
  build_and_publish miner/maven-aether consumer $TAG
  build_and_publish miner/maven-indexer producer $TAG
else
  if [ "$CONSUMER" == true ]
  then
    build_and_publish miner/maven-aether consumer $TAG
  fi
  if [ "$PRODUCER" == true ]
  then
    build_and_publish  miner/maven-indexer producer $TAG
  fi
  if [ "$NEO4J" == true ]
  then
    build_and_publish  neo4j neo4j $TAG
  fi
  if [ "$RABBIT" == true ]
  then
    build_and_publish  rabbitmq rabbitmq $TAG
  fi
fi
