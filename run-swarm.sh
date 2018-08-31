#!/bin/bash

docker build -t miner/rabbitmq rabbitmq/
docker build -t miner/dockerize dockerize/

docker stack deploy -c docker-compose-deploy.yml mavenMinerExp
