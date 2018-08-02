#return n random paths
match (n)-[r:DEPENDS_ON]->(m)
return n limit 10

#return a list of dependencies size per node
match (n)
return n.coordinates as artifact, size((n)-[:DEPENDS_ON]->()) as deps_size

#return the list of artifacts with no dependencies
match (n) where not (n)-[:DEPENDS_ON]->()
return n.artifact as artifact

#return the number of elements with no deps
match (n)
where not (n)-[:DEPENDS_ON]->()
return count(distinct n) as size

#top 20 most used artifacts
match (n)
with size(()-[:DEPENDS_ON]->(n)) as sizes,
     n.coordinates as coor
return coor, sizes order by sizes desc limit 20

#avg per node inverse density
match (n)
 with size(()-[:DEPENDS_ON]->(n)) as sizes
 return max(sizes), min(sizes), avg(sizes)
#node id of top 20 most used dependencies
 match (n)
  with size(()-[:DEPENDS_ON]->(n)) as sizes,
       id(n) as id
  return id order by sizes desc limit 20

  
