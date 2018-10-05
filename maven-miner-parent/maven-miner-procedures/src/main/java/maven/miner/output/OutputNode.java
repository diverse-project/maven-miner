package maven.miner.output;

import org.neo4j.graphdb.Node;

public class OutputNode {
	public Node outputNode;
	
	public OutputNode (Node n) {
		this.outputNode = n;
	}
}
