package fr.inria.diverse.maven.resolver.tasks;

import org.sonatype.aether.graph.DependencyNode;

public interface DependencyVisitorTask {

	public void enter (DependencyNode node);
	public void leave (DependencyNode node);
	public void init();
	public void shutdown(); 
		
}
