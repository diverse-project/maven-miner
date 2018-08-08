FROM maven

##RUN apt-get update && apt-get install tee
##RUN apk update && apk add coreutils
ENV DIRNAME /maven-miner
ENV ARETIFACTS_PATH artifactsInfo
ENV DB_PATH maven-index.db/

WORKDIR $DIRNAME

COPY files/ .
EXPOSE 80 8081
