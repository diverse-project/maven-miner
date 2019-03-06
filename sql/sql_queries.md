# Questions

How diverse are the usage of library? Are all client of one library usiing the same part? Is that part typically big or small?
How dispersed are library usage? How numerous?


# Queries on maven_dep_usage

## List the most popular version of each library

```sql
	SELECT t.id, t.groupid, t.artifactid, t.version, t.clients FROM (SELECT l.id, l.groupid, l.artifactid, l.version, COUNT(d.clientid) as clients FROM library as l JOIN dependency as d ON l.id=d.libraryid GROUP BY l.id ORDER BY clients DESC) as t GROUP BY t.groupid, t.artifactid
```
Only ids
```sql
	SELECT t.id FROM (SELECT l.id, l.groupid, l.artifactid, l.version, COUNT(d.clientid) as clients FROM library as l JOIN dependency as d ON l.id=d.libraryid GROUP BY l.id ORDER BY clients DESC) as t GROUP BY t.groupid, t.artifactid
```

# Per libraries

## get number of clients (as declared in pom)

```sql
	SELECT libraryid, COUNT(clientid) as clients FROM dependency WHERE libraryid=? GROUP BY libraryid
```

## get number of client (as observed in usages)

```sql
	SELECT m.libraryid, COUNT(DISTINCT(u.clientid)) as clients FROM api_usage as u JOIN api_member as m ON u.apimemberid=m.libraryid WHERE m.libraryid=? GROUP BY m.libraryid 
```


org.apache.camel:camel-core:2.22.0,com.h2database:h2:1.3.146,com.sun.xml.bind:jaxb-impl:2.2.7-b53,
