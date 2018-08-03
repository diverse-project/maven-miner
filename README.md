# Maven-Miner

Maven miner is a set of java tools aiming at, programmatically, resolving all Maven dependencies hosted in the Maven central repository, then, storing them into a graph database. First, the maven central index is resolved and transformed into a flat file, containing all the hosted dependencies using the maven-indexer tool. Note, this tool is inspired by the [aether-examples](https://github.com/eclipse/aether-demo) project. Later, this file can be passed to the maven-miner tool in order to collect dependency requests for each artifact available in the file. Each artifact is then visited and persisted in a graph database. We rely on Neo4j, a well-known graph database, to persist the maven dependency graph.

## Schema
We rely on a simple schema to represent the maven dependecy graph. Artifacts are represented using neo4j nodes. Every node holds common information about Maven artifacts, namely, the groupId, artifactId, version, and packaging. In addition, to be able to identify nodes uniquely, we use the property coordinates (groupId:artifact:version). Finally, field exceptions captures all the faced while resolving the artifact. The 

![alt text](https://github.com/diverse-project/maven-miner/blob/master/schema.png)

## User guide

## Developer guide
