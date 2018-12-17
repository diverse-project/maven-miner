package maven.miner.procedures;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import maven.miner.output.IsDeadResult;
import scala.Tuple2;

public class TransitiveProc extends AbstractProcedureEnv {
	/**
	 * 
	 */
	public static Map<String, Integer> hopeScore = new ConcurrentHashMap<String, Integer>();
	/**
	 * 
	 */
	public static Map<String, Boolean> deathStatus =  new ConcurrentHashMap<String, Boolean>();
	
	@Procedure(value="maven.miner.artifacts.group.dead")
	@Description("maven.miner.artifacts.group.dead")
	public Stream<IsDeadResult> tagDeadProjectsByGroup (@Name(value="label")String label) {

		log.info("Start tagging artifacts with label "+label);
		try {
			return graphDB.findNodes(Label.label(label))
					  .stream()
					  .map((e) ->  { 
						   Traverser traversal= graphDB.traversalDescription()
						   						       .breadthFirst()
						   						       .uniqueness(Uniqueness.RELATIONSHIP_PATH)
													   .relationships(DependencyRelation.DEPENDS_ON, 
															 		Direction.INCOMING)
													   //.expand(new IsDeadExpander())
													   .evaluator(Evaluators.toDepth(10))
													   .evaluator((path)-> {
													       if (path.length() == 0) 
													    	   return Evaluation.EXCLUDE_AND_CONTINUE;
													       // if not empty path do:
													       Node endNode = path.endNode();
													      System.out.println(endNode.getProperty(Properties.COORDINATES));
													       if (endNode.hasRelationship(Direction.OUTGOING, 
													    		 					 DependencyRelation.NEXT))		
													         	return Evaluation.EXCLUDE_AND_CONTINUE;
													       else if (!endNode.hasRelationship(Direction.OUTGOING, 
													    		 						   DependencyRelation.NEXT))
													        	return Evaluation.INCLUDE_AND_PRUNE;
													       return Evaluation.EXCLUDE_AND_CONTINUE; })
													   .traverse(e);
						   
						   Iterator<Path> iter = traversal.iterator();
						   
						   if (iter.hasNext()) {
							   		return new IsDeadResult( new Tuple2<String, Boolean>((String) 
															e.getProperty(Properties.COORDINATES), false)); }
						
						   return new IsDeadResult( new Tuple2<String, Boolean>((String) 
															e.getProperty(Properties.COORDINATES), true));
					});
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}
	}

	/**
	 * 
	 * @return
	 */
	@Procedure(value="maven.miner.artifacts.dead", mode = Mode.READ)
	@Description("maven.miner.artifacts.dead")
	public Stream<IsDeadResult> tagDeadProjects() {
		
		log.info("Start tagging artifacts");
		
		try {
			return allLabelsInUse().flatMap((label) -> tagDeadProjectsByGroup(label));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException(e);	
		}

	}
	@SuppressWarnings("rawtypes")
	public class IsDeadExpander implements PathExpander {

		@Override
	    public Iterable<Relationship> expand(Path path, BranchState branchState) {

			Node endNode = path.endNode();
		    System.out.println(endNode.getProperty(Properties.COORDINATES));
		       if (endNode.hasRelationship(Direction.OUTGOING, 
		    		 					 DependencyRelation.NEXT))		
		         	return Collections.emptyList();
		       return endNode.getRelationships(DependencyRelation.DEPENDS_ON, Direction.INCOMING);
		    		  
	        } 

		
		@Override
		public PathExpander reverse() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
