package fr.inria.diverse.maven.resolver.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.DependencyNode;

import fr.inria.diverse.maven.model.DependencyGraph;
import fr.inria.diverse.maven.model.Edge.Scope;
import fr.inria.diverse.maven.model.Vertex;
import fr.inria.diverse.maven.resolver.processor.MultiTaskDependencyVisitor;
import fr.inria.diverse.maven.util.MavenMinerUtil;

public class GraphBuilderVisitorTask implements DependencyVisitorTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTaskDependencyVisitor.class);

    private DependencyGraph dependencyGraph;

    private Vertex root;
    
    private  int resolutionLevel = 0;
    
    public GraphBuilderVisitorTask () {
    	super();
    	dependencyGraph = new DependencyGraph();
    }
    public GraphBuilderVisitorTask (DependencyGraph dependencyGraph) {
    	this.dependencyGraph = dependencyGraph;
    }
	public void setDependencyGraph(DependencyGraph dependencyGraph) {
		this.dependencyGraph = dependencyGraph;
	}
	@Override
	public void enter(DependencyNode node) {
		 // registering the root dependency node
		resolutionLevel += 1;
        if (resolutionLevel == 1) {      	
            root = MavenMinerUtil.getVertexFromArtifactCoordinate(node.getDependency());
        }
        // get the nodes on the second level (the direct dependencies), and add these with the first node to the graph
        else if (resolutionLevel == 2) {
            Vertex secondLevelVerteX = MavenMinerUtil.getVertexFromArtifactCoordinate(node.getDependency());

            LOGGER.info("   ----> Source artifact vertex and destination artifact vertex being added...");
            LOGGER.info("   ----> Adding dependency #" + (dependencyGraph.getEdges().size() + 1));

            Scope scope = MavenMinerUtil.deriveScope(node.getDependency());

            dependencyGraph.addDependency(root, secondLevelVerteX, scope);
        } else {
            LOGGER.info("   ----> Artifact vertex NOT being added...");
        }
	}

	@Override
	public void leave(DependencyNode node) {
		resolutionLevel -= 1;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
			
	}
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
			
	}

}
