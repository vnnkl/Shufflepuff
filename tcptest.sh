#!/bin/bash

if [ -z "$1" ]
  then
    echo "No argument supplied"
    exit 1
fi

N=$1

[[ $N =~ ^[0-9]{1,2}$ ]] && echo "correct format."

echo "Running shuffle tcp test for $N players."

minPort=1808
player=1

while [ $player -le $N ] ;
do
  echo "running player $player with port $((minPort + player))"
  player=$((player+1))
done
