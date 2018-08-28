#!/bin/bash

function print_usage_and_exit {
  echo "Usage: ./chooseRandom.sh"
  echo "--src <arg>: path to the source file. Mandatory!"
  echo "--trg <arg>: path to the target file. Optional! a file with name '\$source-random' is sused by default"
  echo "--max-lines <arg>: the max number of lines to be chosen. 1000 is chosed by default"
  exit 1
}


while [[ $# > 1 ]]
do
key="$1"
shift
case $key in
    --max-lines)
    maxLines=$1
    shift
    ;;
    --src)
    src=$1
    shift
    ;;
    --trg)
    trg=$1
    shift
    ;;
    *)
#    print_usage_and_exit
    ;;
esac
done

if [ -z "$src" ]
then
  echo "source path not specified"
  print_usage_and_exit
fi

if [ -z "$trg" ]
then
  trg=$src-random
  echo "No target path argument was provided. $trg was given by default"
fi

if [ -z "$maxLines" ]
then
  maxLines=1000
  echo "No max lines was provided. $trg was given by default"
fi

shuf -n $maxLines $src > $trg
