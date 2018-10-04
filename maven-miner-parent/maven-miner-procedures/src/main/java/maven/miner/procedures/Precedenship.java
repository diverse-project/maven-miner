package maven.miner.procedures;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import maven.miner.output.BooleanOutput;


public class Precedenship extends AbstractProcedureEnv {
	
	/**
	 * This procedure is responsible for creating  
	 */
	@Procedure(value = "maven.miner.precedenceship", mode = Mode.WRITE)
	@Description("Creating per-version precedence relationship of all artifacts node ")
	public Stream<BooleanOutput> createPrecedenceship() {
		log.info("Creating plugins version's evolution ");
		Stream<String> allLabels = allLabelsInUse();
		allLabels.forEach(labelname -> createPerGroupPercedenceship(labelname));//end foreach	
		BooleanOutput result = new BooleanOutput();
		result.output = true;
		return Stream.of(result);
	}
	/**
	 * 
	 * @param groupName
	 */
	@Procedure(value="maven.miner.group.precedenceship", mode = Mode.WRITE)
	@Description("Creating per-version precedence relationship for all group nodes ")
	public Stream<BooleanOutput> createPerGroupPercedenceship(@Name("The group name") String groupName) {
		log.info(String.format("Creating precedenceship for %s group's artifacts",groupName));
		Label label = Label.label(groupName);	
		
		try (Transaction tx = graphDB.beginTx()) {
			
		List<Node> sortedNodes = graphDB.findNodes(label).stream().sorted(new Comparator<Node>() {
			@Override
			public int compare(Node n1, Node n2) {
				//String p1 = n1.getProperty
				String p1 = (String) n1.getProperty(Properties.ARTIFACT);
				String p2 = (String) n2.getProperty(Properties.ARTIFACT);
				Version v1 = null;
				Version v2 = null;
				if (p1.compareTo(p2) != 0) return p1.compareTo(p2);
				final GenericVersionScheme versionScheme = new GenericVersionScheme();
				try {
					v1 = versionScheme.parseVersion((String)n1.getProperty(Properties.VERSION));
					v2 = versionScheme.parseVersion((String)n2.getProperty(Properties.VERSION));
				} catch (InvalidVersionSpecificationException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			    return v1.compareTo(v2); 		
			}
		}).collect(Collectors.toList());//end find nodes
		
		for (int i =0; i< sortedNodes.size() - 2; i++) {
			Node firstNode = sortedNodes.get(i);
			Node secondNode = sortedNodes.get(i+1);
			
			if (isNextRelease(firstNode, secondNode)) {
				firstNode.createRelationshipTo(secondNode, DependencyRelation.NEXT);
			}
		}
		tx.success();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			throw e;	
		}
		BooleanOutput result = new BooleanOutput();
		result.output = true;
		return Stream.of(result);
	}
	
	protected boolean isNextRelease(Node firstNode, Node secondNode) {
		String artifact1 = (String) firstNode.getProperty(Properties.ARTIFACT);
		String artifact2 = (String) secondNode.getProperty(Properties.ARTIFACT);
		return artifact1.equals(artifact2);	
	}
	

	
}
