package fr.inria.diverse.maven.resolver.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public abstract class AbstractGraphBuilderVisitorTask implements DependencyVisitorTask {

	/**
	 * A stack to resolve the current parent node 
	 */
	protected Stack<Node> stack = new Stack<Node>();
	/**
	 * A pointer to the parent node
	 */
	protected Node root;
	/**
	 * the depth of the graph traversal w.r.t. the root node
	 */
	protected int depth = 0;
	/**
	 * An index of already resolved artifacts in the form of Neo4j {@link Node} 
	 */
	protected Map<String,Node> nodesIndex = new HashMap<String,Node>();
	/**
	 * An index of already resolved dependency relationships
	 */
	protected ListMultimap<String, String> edgesIndex = ArrayListMultimap.create();
	/**
	 * A String to Label map to cache already created labels
	 */
	protected Map<String,Label> labelsIndex = new HashMap<String,Label>();

   }
