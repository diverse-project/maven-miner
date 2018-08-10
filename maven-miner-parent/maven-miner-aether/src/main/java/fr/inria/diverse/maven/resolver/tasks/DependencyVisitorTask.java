package fr.inria.diverse.maven.resolver.tasks;

import org.sonatype.aether.graph.DependencyNode;

public interface DependencyVisitorTask {
	/**
	 * Is triggered when the visitor enters a {@link DependencyNode} 
	 * @param node {@link DependencyNode}
	 */
	public void enter (DependencyNode node);
	/**
	 * Is triggered when the visitor leaves a {@link DependencyNode} 
	 * @param node {@link DependencyNode}
	 */
	public void leave (DependencyNode node);
	/**
	 * Is triggered manually in case an initialization phase is needed before running the visitor
	 * 
	 */
	public void init();
	/**
	 * Is triggered manually in case a shutdown phase is needed after running the visitor
	 */
	public void shutdown(); 
		
}
