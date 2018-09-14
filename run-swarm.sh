#!/bin/bash
SCRIPT_NAME=./run-swarm.sh
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--n-consumer <NUM>        Number of consumers. Default=$HOME/neo4j-server"
  echo "--neo4j-dump <path>       Local path where to dump neo4j data and logs. Default=2"
  echo "--rabbit-mnesia <path>    Local path to the nmesia folder of rabbitMQ"
  echo "--tag <NUM>               The tag number of the "
  exit 1
}
NEO4J_DUMP=$HOME/neo4j-server
RABBIT_DUMP=$HOME/rabbitMQ
CONSUMERS=2
VERSION=0.3.0
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --n-consumer)
    CONSUMERS=$1
    shift
    ;;
    --tag)
    VERSION=$1
    shift
    ;;
    --neo4j-dump)
    NEO4J_DUMP=$1
    shift
    ;;
    --rabbit-dump)
    RABBIT_DUMP=$1
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

mkdir $RABBIT_DUMP
mkdir $RABBIT_DUMP/data

export RABBIT_VAR=$RABBIT_DUMP/data
export NEO4J_VAR=$NEO4J_DUMP
export REPLICAS=$CONSUMERS
export MINER=`pwd`
export TAG=$VERSION

docker stack deploy -c docker-stack-deploy.yml maven-miner-exp
