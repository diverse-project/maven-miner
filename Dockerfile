FROM openjdk:8u171-jdk-alpine3.8
MAINTAINER Amine BENELALLAM

RUN apk add --no-cache bash
EXPOSE 80 8081

sudo docker run -d \
    --hostname neo4j-apoc \--name neo4j-apoc \
    --publish=7474:7474 --publish=7687:7687 \
    --volume=$HOME/neo4j-server/data:/data \
    --env=NEO4J_AUTH=none \
    amineben/neo4j:miner-0.3.0
