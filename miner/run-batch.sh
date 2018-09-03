#!/bin/bash
SCRIPT_NAME=$0
function print_usage_and_exit {
  echo "Usage: $SCRIPT_NAME"
  echo "--file <arg>: path to artifacts info version.  Optional!"
  echo "--database <arg>: path to database. Optional! The value |maven-index.db/| is used by default."
  echo "--resolve-jars <arg>: Actionning jars resolution and classes count. Optional"
  exit 1
}

echo "Setting up default variables"

DB_PATH=/results/maven-index.db/
ARTIFACT_PATH=/results/allArtifacts
RESOLVE_JARS=" "

while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --file)
    ARTIFACT_PATH="$1"
    shift
    ;;
    --resolve-jars)
    RESOLVE_JARS="-r"
    shift
    ;;
    --db)
    DB_PATH="$1"
    shift
    ;;
    *)
    print_usage_and_exit
    ;;
esac
done

WORKING_DIR=`pwd`

INDEXER_JAR=/maven-miner/maven-indexer/miner-indexer.jar
AETHER_JAR=/maven-miner/maven-aether/miner-aether.jar

SORTED_ARTIFACTS=$ARTIFACT_PATH-sorted
UNIQUE_ARTIFACTS=$ARTIFACT_PATH-unique

mkdir /results/logs

if [ -f $ARTIFACT_PATH ]; then
    echo "Artifacts file already exists. Index update phase is skipped"
else
    echo "Creating artifacts index file with name $ARTIFACT_PATH"
    java -Xms256m -Xmx8g -jar $INDEXER_JAR -t $ARTIFACT_PATH 2>&1 | tee -a /results/logs/indexer.log
fi

echo "Sorting artifacts"
sort -u $ARTIFACT_PATH > $SORTED_ARTIFACTS
echo "Removing duplicated artifacts"
awk '!a[$0]++' $SORTED_ARTIFACTS > $UNIQUE_ARTIFACTS
echo "Collecting maven index info from $UNIQUE_ARTIFACTS and dumping it into $DB_PATH"
java -Xms256m -Xmx8g -jar $AETHER_JAR -f $UNIQUE_ARTIFACTS -db $DB_PATH $RESOLVE_JARS 2>&1 | tee -a /results/logs/resolver.log
