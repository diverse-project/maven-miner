#!/bin/bash

function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--file <arg>: path to artifacts info version. Optional! The maven-miner-aether.jar is shipped with a toy file to for demonstration purpose"
  echo "--database <arg>: path to database. Optional!the value |maven-index.db/| is used by default."
  echo "--results <arg>: absolute path to the host results folder. Required"
  echo "--resolve-jars: Actionning jars resolution and classes count. Optional"
  echo "--rebuild: Activates the build of the docker image build"
  exit 1
}

NAME=miner
MACHINE_NAME=maven-miner
DB_PATH=" "
ARTIFACT_PATH=" "
RESOLVE_JARS=" "
RESULTS_FOLDER=""
RE_BUILD=false;
while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --rebuild)
    RE_BUILD=true
    shift
    ;;
    --results)
    echo "enters results"
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

echo "$RESULTS_FOLDER"
if [[ $RESULTS_FOLDER = "" ]]; then
  echo "results folder not specified. See usage below!"
  print_usage_and_exit
fi

if [ "$RE_BUILD"=true ]; then
  docker build -t $MACHINE_NAME .
fi

docker rm -f $NAME
WORKING_DIR=`pwd`
FILES_DIR=$WORKING_DIR/miner
echo $FILES_DIR
docker run -it \
--volume $RESULTS_FOLDER:/results \
--volume $FILES_DIR:/maven-miner \
--name $NAME $MACHINE_NAME \
 /maven-miner/run-batch.sh $ARTIFACT_PATH $DB_PATH $RESOLVE_JARS
