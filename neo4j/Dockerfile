FROM neo4j:3.2
MAINTAINER Amine BENELALLAM

ENV NEO4J_AUTH=none
ENV NEO4J_dbms_memory_pagecache_size=4g
ENV NEO4J_dbms_security_procedures_unrestricted=apoc.\\\*,maven.\\\*,algo.\\\

ADD miner-proc.jar plugins/
