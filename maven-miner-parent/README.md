## Resolve library packages

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.LibResolverApp libraries.list
```

**Input**: a simple list of coordinates separated with new lines

**Result**: output dir containing a file per library conatining the list of packages

## Upload libraries and their packages in db

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.populate.LibraryPackages
```

**Input**: output dir

**Result**: Db tables `library` and `package` are populated

## Upload clients and their dependencies

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.populate.ClientDependencies library-clients.list
```

**Input**: file containing `Library coordinates,[client,...]`

**Result**: Db tables `client` and `dependency` are populated

## Populate Queue
Order lib by popularity?

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.ClientResolverApp -f -d mariadb.properties -q qhost:port -u quser:password
```

**Result**: Queue is filled with clients coordinates to be processed

## Run

Beware of writting queue

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.ClientResolverApp -d mariadb.properties -q qhost:port -u quser:password
```

**Result**: Queue is filled api usages

## Write Usages

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.UsageResolverApp  -d mariadb.properties -q qhost:port -u quser:password -s batch-size
```

**Result**: Db tables `api_member` and `api_usage` are populated

## Full api resolution

```bash
	java -cp maven-miner.jar fr.inria.diverse.maven.resolver.launcher.LibApiResolverApp -f libraries.list
```
**Result**: Db table `api_member_full` is populated






