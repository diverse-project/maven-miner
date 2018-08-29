FROM openjdk:8u171-jdk-alpine3.8
RUN apk add --no-cache bash
##RUN apt-get update && apt-get install tee
##RUN apk update && apk add coreutils
#RUN apk add --no-cache bash
#ENV DIRNAME /maven-miner
#ENV ARETIFACTS_PATH artifactsInfo
#ENV DB_PATH maven-index.db/
#WORKDIR $DIRNAME
#COPY files/ .
EXPOSE 80 8081
