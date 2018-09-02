#!/bin/bash
SCRIPT_NAME=./run-swarm.sh
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--n-consumer <NUM>        Number of consumers. Default=2"
  echo "--neo4j-dump <path>       Local path where to dump neo4j data and logs. Default=$HOME/neo4j-server"
  echo "--from-file <path>        Local path of the file containing the desired set of artifacts to be resolved. Required"
  exit 1
}
NEO4J_DUMP=$HOME/neo4j-server
CONSUMERS=2

while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --from-file)
    FILE_PATH="--file $1"
    shift
    ;;
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

if [ -z "$FILE_PATH" ]; then
  echo "Missing the path to the artifacts file."
  print_usage_and_exit
fi

# Adding if a file ecists conditional

mkdir $NEO4J_DUMP
mkdir $NEO4J_DUMP/data
mkdir $NEO4J_DUMP/logs

#docker build -t miner/rabbitmq rabbitmq/
#docker build -t miner/dockerize dockerize/
export NEO4J_VAR=$NEO4J_DUMP
export MINER=`pwd`
export FILE_COMMAND=$FILE_PATH
#export REPLICAS=$CONSUMERS
docker-compose up --scale consumer=$CONSUMERS
