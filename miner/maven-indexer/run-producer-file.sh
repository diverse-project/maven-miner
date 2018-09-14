#!/bin/bash
SCRIPT_NAME=$0
function print_usage_and_exit {
  echo "Usage: ./run-prducer.sh"
  echo "--file <arg>: path to artifacts info version.  Optional!"
  echo "--queue <arg>: hostname and prot number of rabbitMQ server"
  echo "--index"
  exit 1
}
sleep 30s
echo "Setting up default variables"

ARTIFACT_PATH="/dist/artifacts/artifact-0"
INDEX=0
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --file)
    ARTIFACT_PATH="$1"
    shift
    ;;
    --queue)
    QUEUE="$1"
    shift
    ;;
    *)
    print_usage_and_exit
    ;;
    --index)
    INDEX="$1"
    shift
    ;;
esac
done

INDEXER_JAR=/dist/miner-indexer.jar

mkdir logs

if [ -z "$QUEUE" ]; then
    echo "hostname and port number of rabbitMQ server is not provided 'host:number'"
    print_usage_and_exit
fi
./run-producer --queue $QUEUE --file $ARTIFACT_PATH$INDEX 
