#!/bin/bash
SCRIPT_NAME=./run-swarm.sh
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--n-consumer <NUM>        Number of consumers. Default=$HOME/neo4j-server"
  echo "--neo4j-dump <path>       Local path where to dump neo4j data and logs. Default=2"
  exit 1
}
NEO4J_DUMP=$HOME/neo4j-server
CONSUMERS=2

while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --n-consumer)
    CONSUMERS=$1
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

mkdir $NEO4J_DUMP
mkdir $NEO4J_DUMP/data
mkdir $NEO4J_DUMP/logs

docker build -t miner/rabbitmq rabbitmq/
docker build -t miner/dockerize dockerize/
export NEO4J_VAR=$NEO4J_DUMP
export MINER=`pwd`
#export REPLICAS=$CONSUMERS
docker-compose up -d --scale consumer=$CONSUMERS
