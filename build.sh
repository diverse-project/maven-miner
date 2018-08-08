#!/bin/bash

function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--file <arg>: path to artifacts info version. Optional"
  echo "--database <arg>: path to database. Optional!the value |maven-index.db/| is used by default."
  echo "--results <arg>: path to the host results folder. Required"
  echo "--resolve-jars: A ctionning jars resolution and classes count. Optional"
  echo "--no-build: Activates the biuld o fthe docker image build"
  exit 1
}

DB_PATH=" "
ARTIFACT_PATH=" "
RESOLVE_JARS=" "
RESULTS_FOLDER=""
NO_BUILD=false;
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --no-build)
    NO_BUILD=true
    shift
    ;;
    --results)
    RESULTS_FOLDER="$1"
    shift
    ;;
    --file)
    ARTIFACT_PATH="--file $1"
    shift
    ;;
    --resolve-jars)
    RESOLVE_JARS="--resolve-jars"
    shift
    ;;
    --db)
    DB_PATH="--db $1"
    shift
    ;;
    *)
    print_usage_and_exit
    ;;
esac
done

echo "value of RESULTS_FOLDER $RESULTS_FOLDER"
if [ $RESULTS_FOLDER="" ]; then
  print_usage_and_exit
fi

if [ $NO_BUILD = true ]; then
  docker build -t maven-miner .
fi

docker run -it  --volume $RESULTS_FOLDER:/maven-miner/results --name miner maven-miner -c ./run.sh $ARTIFACT_PATH $ARTIFACT_PATH $RESOLVE_JARS
