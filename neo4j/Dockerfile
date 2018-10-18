FROM neo4j:3.4.7
MAINTAINER Amine BENELALLAM

ENV NEO4J_AUTH=none
ENV NEO4J_dbms_memory_pagecache_size=4g
ENV NEO4J_dbms.memory.heap.max_size=4g
ENV NEO4J_dbms_security_procedures_unrestricted=apoc.\\\*,maven.\\\*,algo.\\\*


COPY miner-proc.jar plugins/

RUN wget -P plugins/ https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/3.4.0.1/apoc-3.4.0.1-all.jar
RUN wget -P plugins/ https://github.com/neo4j-contrib/neo4j-graph-algorithms/releases/download/3.4.0.0/graph-algorithms-algo-3.4.0.0.jar

ENTRYPOINT ["/sbin/tini", "-g", "--", "/docker-entrypoint.sh"]
CMD ["neo4j"]
