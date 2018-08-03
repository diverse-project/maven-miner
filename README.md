# Maven-Miner

Maven miner is a set of java tools aiming at, programmatically, resolving all Maven dependencies hosted in the Maven central repository, then, storing them into a graph database. First, the maven central index is resolved and transformed into a flat file, containing all the hosted dependencies using the maven-indexer tool. Note, this tool is inspired by the [aether-examples](https://github.com/eclipse/aether-demo) project. Later, this file can be passed to the maven-miner tool, in order to collect dependency requests for each artifact available in the file. Each artifact dependency, then, is visited and persisted in a graph database. We rely on Neo4j, a well-known graph database, to persist the maven dependency graph.

## Schema

## User guide

## Developer guide
