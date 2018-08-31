# Maven-Miner

Maven miner is a set of java tools aiming at, programmatically, resolving all Maven dependencies hosted in the Maven central repository, then, storing them into a graph database. First, the maven central index is resolved and transformed into a flat file, containing all the hosted dependencies using the maven-indexer tool. Note, this tool is inspired by the [aether-examples](https://github.com/eclipse/aether-demo) project. Later, this file can be passed to the maven-miner tool in order to collect dependency requests for each artifact available in the file. Each artifact is then visited and persisted in a graph database. We rely on Neo4j, a well-known graph database, to persist the maven dependency graph.
![alt text](https://github.com/diverse-project/maven-miner/blob/master/images/screenshot.png)
## Schema
We rely on a simple schema to represent the maven dependency graph. Artifacts are represented using neo4j nodes. Every node holds common information about Maven artifacts, namely, the *groupId*, *artifactId*, *version*, *packaging*, and *last_modified*, which refers to the artifact deployment date. In addition, to be able to identify nodes uniquely, we use the property *coordinates* (groupId:artifact:version). Finally, *exception* nodes capture all the raised exceptions while resolving a given artifact. The relationship type "**RAISES**" had a property *count*, to indicate how many times an exception was raised. The "**NEXT**" relationship id used to refer to next version. Note this relationship is resolved only on the standalone version.

![alt text](https://github.com/diverse-project/maven-miner/blob/master/images/schema.png)

## User guide
### General Prerequisites
- Docker (1.13.0+)
  - Docker-compose to run the maven-miner in docker-compose mode (Optional)
  - Docker swarm to run the maven-miner in docker-compose mode (Optional)
- Maven
- bash

### Maven indexer
```
usage: java -jar maven-miner-indexer.jar
 -f,--to-file <path>         File path to retrieve artifacts coordinates list.
                             If not specified, the maven central index is used
                             instead. Note, artifacts are per line and come in
                             the form groupId:artifactId:version.
-q,--queue <host:ip>         Hostname and port of the RabbitMQ broker. Note, URI
                             comes in the form hostname:port
-t,--to-file                 Dumping the index into a file with name
                             allArtifacsInfo. Note the args \'t\' and \'q\' are
                             mutually exclusive, only one should be provided"
 -h,--help                   Show help
```
### Maven miner on standalone mode
```
usage: java -cp maven-miner-aether.jar
                fr.inria.diverse.maven.resolver.launcher.BatchResolverApp
  -db,--database <arg>        Path to store the neo4j database. REQUIRED!
  -f,--file <arg>             Path to artifacts coordinates list file. Note,
                              artifacts are per line
  -p,--pretty-printer <arg>   Path to the output file stream. Optional
  -r,--resolve-jars           Actionning jars resolution and classes count.
                              Not activated by default!
  -h,--help                   Show help
```
### Maven miner on message passing mode (only when the indexer is used in producer mode with the argument -q)

```
usage: java -cp maven-miner-aether.jar
                fr.inria.diverse.maven.resolver.launcher.ConsumerResolverApp
  -db,--database <host:ip>    Hostname and port of the neo4j server. REQUIRED!
  -q,--queue <path>           Hostname and port of the RabbitMQ broker. Note, URI
                              comes in the form hostname:port
  -p,--pretty-printer <path>  Path to the output file stream. Optional
  -r,--resolve-jars           Actionning jars resolution and classes count.
                              Not activated by default!
  -h,--help                   Show help
```
### Using Maven-miner with Docker
This repository comes along with a set of scripts in order to prepare a ready-to-use docker machine, to create and launch a container, and to mine the maven central.
Regardless of the docker execution mode you are opting for, it is recommended to package the tool using the scrpit below.
#### Packaging the Maven project
After cloning the repository on your local machine or remote server, you will simply need to execute the *prepare.sh* script.
It is responsible of packaging the maven project and moving it to the files folder.

```
usage: prepare.sh
user@ubuntu/path/to/repository$ ./prepare.sh
```

#### Running maven-miner inside a container
Once the packages are built and moved to the files folder, you may build and run the maven miner using the *buildAndRun.sh* script as shown below:
```
Usage: buildAndRun-batch.sh
user@ubuntu$ ~/path/to/repository/buildAndRun.sh
  --file <arg>             Path to artifacts info version. Optional!
  --database <arg>         Path to database. Optional!
                           |maven-index.db/| is used by default.
  --results <arg>          Path to the host results folder. Required
  --resolve-jars           Actionning jars resolution and classes count. Optional
  --rebuild                Activates the build of the docker image build
```
#### Running maven-miner inside a docker node using docker-compose
The following script assumes that you didn't change the default port numbers of Neo4j and RabbitMQ:
```
user@ubuntu/path/to/repository$./run-swarm.sh
  --n-consumer            Number of consumers to be deployed
  --neo4j-dump            Local path to dump neo4j data and logs
```
#### Running maven-miner inside a docker swarm node using docker stack
The following script assumes that you didn't change the default port numbers of Neo4j and RabbitMQ:
```
user@ubuntu/path/to/repository$./run-swarm.sh
  --n-consumer            Number of consumers to be deployed
  --neo4j-dump            Local path to dump neo4j data and logs
```
## Developer guide
TBD
