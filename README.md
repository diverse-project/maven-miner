# Maven-Miner

Maven miner is a set of java tools aiming at, programmatically, resolving all Maven dependencies hosted in the Maven central repository, then, storing them into a graph database. First, the maven central index is resolved and transformed into a flat file, containing all the hosted dependencies using the maven-indexer tool. Note, this tool is inspired by the [aether-examples](https://github.com/eclipse/aether-demo) project. Later, this file can be passed to the maven-miner tool in order to collect dependency requests for each artifact available in the file. Each artifact is then visited and persisted in a graph database. We rely on Neo4j, a well-known graph database, to persist the maven dependency graph.
![alt text](https://github.com/diverse-project/maven-miner/blob/master/images/screenshot.png)
## Schema
We rely on a simple schema to represent the maven dependency graph. Artifacts are represented using neo4j nodes. Every node holds common information about Maven artifacts, namely, the *groupId*, *artifactId*, *version*, *packaging*, and *last_modified*, which refers to the artifact deployment date. In addition, to be able to identify nodes uniquely, we use the property *coordinates* (groupId:artifact:version). Finally, *exception* nodes capture all the raised exceptions while resolving a given artifact. The relationship type "**RAISES**" had a property *count*, to indicate how many times an exception was raised. The "**NEXT**" relationship id used to refer to next version. 

![alt text](https://github.com/diverse-project/maven-miner/blob/master/images/schema.png)

## User guide
### Prerequisites
- Docker
- Maven
- bash

### Maven indexer
```
usage: java -jar maven-miner-indexer.jar
 -f,--file <arg>             Path to artifacts coordinates list file
 -h,--help                   Show help
```
### Maven miner
```
usage: java -jar maven-miner-aether.jar
  -db,--database <arg>        Path to store the neo4j database. REQUIRED!
  -f,--file <arg>             Path to artifacts coordinates list file. Note,
                              artifacts are per line
  -p,--pretty-printer <arg>   Path to the output file stream. Optional
  -r,--resolve-jars           Actionning jars resolution and classes count.
                              Not activated by default!
  -h,--help                   Show help
```
### Using Docker image
This repository comes along with a set of scripts in order to prepare a ready-to-use docker machine, to create and launch a container, and to mine the maven central.
#### Packaging the Maven project
After cloning the repository on your local machine or remote server, you will simply need to execute the *prepare.sh* script.
It is responsible of packaging the maven project and moving it to the files folder.

```
usage: prepare.sh
user@ubuntu$ ~/path/to/repository/prepare.sh
```

#### Building the Docker Image and running the miner
Once the packages are built and moved to the files folder, you may build and run the maven miner using the *buildAndRun.sh* script as shown below
```
Usage: buildAndRun.sh
user@ubuntu$ ~/path/to/repository/buildAndRun.sh
  --file <arg>: path to artifacts info version. Optional! The maven-miner-aether.jar is shipped with a toy file to for demonstration purpose
  --database <arg>: path to database. Optional!the value |maven-index.db/| is used by default.
  --results <arg>: path to the host results folder. Required
  --resolve-jars: Actionning jars resolution and classes count. Optional
  --rebuild: Activates the build of the docker image build
```
## Developer guide
TBD
