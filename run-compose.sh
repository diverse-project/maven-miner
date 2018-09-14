#!/bin/bash
SCRIPT_NAME=./run-swarm.sh
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--n-consumer <NUM>        Number of consumers. Default=$HOME/neo4j-server"
  echo "--neo4j-dump <path>       Local path where to dump neo4j data and logs. Default=2"
  echo "--rabbit-dump <path>      Local path to the nmesia folder of rabbitMQ"
  echo "--detach                  "
  exit 1
}

NEO4J_DUMP=$HOME/neo4j-server
RABBIT_DUMP=$HOME/rabbitMQ/
CONSUMERS=2
DETACH=" "
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --n-consumer)
    CONSUMERS=$1
    shift
    ;;
    --detach)
    DETACH=" -d "
    shift
    ;;
    --neo4j-dump)
    NEO4J_DUMP=$1
    shift
    ;;
    *)
    print_usage_and_exit
    ;;
esac
done

mkdir $RABBIT_DUMP
mkdir $RABBIT_DUMP/data

mkdir $NEO4J_DUMP
mkdir $NEO4J_DUMP/data
mkdir $NEO4J_DUMP/logs

#docker build -t miner/rabbitmq rabbitmq/
#docker build -t miner/dockerize dockerize/
export NEO4J_VAR=$NEO4J_DUMP
export RABBIT_VAR=$RABBIT_DUMP/data
export MINER=`pwd`
#export REPLICAS=$CONSUMERS
docker-compose up $DETACH --scale consumer=$CONSUMERS
