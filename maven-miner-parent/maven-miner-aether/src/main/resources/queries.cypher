//return n random paths
match (n)-[r:DEPENDS_ON]->(m)
return n limit 10

//return a list of dependencies size per node
match (n)
return n.coordinates as artifact, size((n)-[:DEPENDS_ON]->()) as deps_size

//return the list of artifacts with no dependencies
match (n) where not (n)-[:DEPENDS_ON]->()
return n.artifact as artifact

//return the number of elements with no deps
match (n)
where not (n)-[:DEPENDS_ON]->()
return count(distinct n) as size

//top 20 most used artifacts
match (n)
with size(()-[:DEPENDS_ON]->(n)) as sizes,
     n.coordinates as coor
return coor, sizes order by sizes desc limit 20

//avg per node inverse density
match (n)
 with size(()-[:DEPENDS_ON]->(n)) as sizes
 return max(sizes), min(sizes), avg(sizes)

//node id of top 20 most used dependencies
 match (n)
  with size(()-[:DEPENDS_ON]->(n)) as sizes,
       id(n) as id
  return id order by sizes desc limit 20


  //top 20 sharing most used artis
  //selecting top 20 projetcs...
  match (n) with size(()-[:DEPENDS_ON]->(n)) as sizes, n as top20 order by sizes desc limit 20
  // passing top2O to the next
  match (m)-[:DEPENDS_ON]->(top20)
  //with  m, top20, m {.coordinates, top20.coordinates} as map
  return m.coordinates, m {sharedDepsCount: size(collect(top20)), deps: collect(top20) } order by m.sharedDepsCount desc limit 20
