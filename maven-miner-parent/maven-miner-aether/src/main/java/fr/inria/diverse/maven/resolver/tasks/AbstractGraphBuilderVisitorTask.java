package fr.inria.diverse.maven.resolver.tasks;

import java.util.Stack;
import org.sonatype.aether.artifact.Artifact;

public abstract class AbstractGraphBuilderVisitorTask implements DependencyVisitorTask {

	/**
	 * A stack to resolve the current parent node 
	 */
	protected Stack<Artifact> stack = new Stack<Artifact>();
	/**
	 * A pointer to the parent node
	 */
	protected Artifact root;
	/**
	 * the depth of the graph traversal w.r.t. the root node
	 */
	protected int depth = 0;
	

   }
