package fr.inria.diverse.maven.common;

import org.neo4j.graphdb.RelationshipType;

public enum DependencyRelation implements RelationshipType {
	DEPENDS_ON,
	NEXT,
	MONTH,
	YEAR,
	DAY,
	RAISES;
}
