#!/bin/bash
SCRIPT_NAME=$0
function print_usage_and_exit {
  echo "Usage: ./run-prducer.sh"
  echo "--file <arg>: path to artifacts info version.  Optional!"
  echo "--queue <arg>: hostname and prot number of rabbitMQ server"
  exit 1
}
sleep 30s
echo "Setting up default variables"

ARTIFACT_PATH=""

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
esac
done

INDEXER_JAR=/dist/miner-indexer.jar

mkdir logs

if [ -z "$QUEUE" ]; then
    echo "hostname and port number of rabbitMQ server is not provided 'host:number'"
    print_usage_and_exit
fi

HOSTNAME=`cat /etc/hostname`
java -jar $INDEXER_JAR -q $QUEUE $ARTIFACT_PATH  2>&1 | tee -a logs/publisher-$HOSTNAME.log
