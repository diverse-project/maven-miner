#!/bin/bash
SCRIPT_NAME=$0
function print_usage_and_exit {
  echo "Usage: ./run-consumer.sh"
  echo "--db <arg>: hostname and port number of neo4j"
  echo "--queue <arg>: hostname and port number of rabbitMQ server"
  echo "--resolve-jars: Actioning jars resolution and classes count. Not activated by default!"
  exit 1
}

echo "Setting up default variables"
MAIN_CLASS=fr.inria.diverse.maven.resolver.launcher.ConsumerResolverApp
RESOLVE_JARS=" "
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --db)
    DB="$1"
    shift
    ;;
    --resolve-jars)
    RESOLVE_JARS="-r"
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

AETHER_JAR=/dist/miner-aether.jar

mkdir logs

if [ -z "$QUEUE" ]; then
    echo "hostname and port number of rabbitMQ server is not provided"
    print_usage_and_exit
fi

if [ -z "$DB" ]; then
    echo "hostname and port number of Neo4j server is not provided"
    print_usage_and_exit
fi

HOSTNAME=`cat /etc/hostname`
java -cp $AETHER_JAR $MAIN_CLASS -q $QUEUE -db $DB $RESOLVE_JARS 2>&1 | tee -a logs/consumer-$HOSTNAME.log
