package maven.miner.procedures;

import java.time.Month;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.stream.Stream;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import fr.inria.diverse.maven.common.DependencyRelation;
import fr.inria.diverse.maven.common.Properties;
import fr.inria.diverse.maven.resolver.util.MavenResolverUtil;
import maven.miner.output.BooleanOutput;

public class TemporalIndex extends AbstractProcedureEnv {
	/**
	 * an index of the nodes representing years
	 */
	public static HashMap<String, Node> yearNodes = new HashMap<String,Node>();
	/**
	 * an index of nodes representing the months 
	 */
	public static HashMap<String, Node> monthNodes = new HashMap<String,Node>();
	/**
	 * an index of nodes representing the days of the week
	 */
	public static HashMap<String, Node> dayNodes = new HashMap<String,Node>();
	/**
	 * The calendar label node
	 */
	public static Label calendarLabel = Label.label(Properties.CALENDAR_LABEL);
	/**
	 * 
	 */
	@Procedure(value="maven.miner.temporalIndex", mode = Mode.WRITE)
	@Description("Creating a temporal index")
	public Stream<BooleanOutput> createTemporalIndex() {	
		log.info("Creating plugins version's evolution ");
		Stream<String> allLabels = allLabelsInUse();
		allLabels.forEach(labelname -> createPerGroupTemporalIndex(labelname));//end foreach	
		BooleanOutput result = new BooleanOutput();
		result.output = true;
		return Stream.of(result);
	}
	/**
	 * 
	 * @param groupName
	 */
	@Procedure(value="maven.miner.group.temporalIndex", mode = Mode.WRITE)
	@Description("Creating per-GAV temporal index")
	public Stream<BooleanOutput> createPerGroupTemporalIndex(@Name("The group name") String groupName) {
		log.info(String.format("Creating temporal Index for artifacts of the group %s ",groupName));
		Label label = Label.label(groupName);	
		
		try (Transaction tx = graphDB.beginTx()) {
			graphDB.findNodes(label).stream().forEach(node -> 
			{
				String date = (String) node.getProperty(Properties.LAST_MODIFIED);
				ZonedDateTime zonedTime = MavenResolverUtil.fromZonedTime(date);
				createTemporalNodes (node, zonedTime);				
			});
			tx.success();
		} catch (Exception e) {
			if (e.getMessage() != null)
				log.error(e.getMessage());
			throw e;
		}
		BooleanOutput result = new BooleanOutput();
		result.output = true;
		return Stream.of(result);
	}
	/**
	 * 
	 * @param node
	 * @param zonedTime
	 */
	private void createTemporalNodes(Node node, ZonedDateTime zonedTime) {
		log.debug(String.format("creating temporal nodes for artifact: %s", 
				  node.getProperty(Properties.COORDINATES)));
		Node yearNode = getOrCreateYearNode(zonedTime.getYear());
		Node monthNode = getOrCreateMonthNode(zonedTime.getMonth());
		Node dayNode = getOrCreateDayNode(zonedTime.getDayOfMonth());
		
		if(!node.hasRelationship(DependencyRelation.YEAR))
			node.createRelationshipTo(yearNode, DependencyRelation.YEAR);
		if(!node.hasRelationship(DependencyRelation.MONTH))
			node.createRelationshipTo(monthNode, DependencyRelation.MONTH);
		if(!node.hasRelationship(DependencyRelation.DAY))
			node.createRelationshipTo(dayNode, DependencyRelation.DAY);
	}
	
	/**
	 * 
	 * @param dayOfWeek
	 * @return
	 */
	private Node getOrCreateDayNode(int dayOfWeek) {
		
		Node day = null; 
		String dayString = ""+dayOfWeek;
		if (dayNodes.containsKey(dayString)) {
			day = dayNodes.get(dayString);
		} else {
			day = findCalendarNode(Properties.DAY, dayString);
			dayNodes.put(dayString, day);
		}
		return day;
	}
	
	/**
	 * 
	 * @param month
	 * @return
	 */
	private Node getOrCreateMonthNode(Month monthP) {
		Node month = null; 
		if (monthNodes.containsKey(monthP.toString())) {
			month = monthNodes.get(monthP.toString());
		} else {
			month = findCalendarNode(Properties.MONTH, monthP.toString());
			monthNodes.put(monthP.toString(), month);
		}
		return month;
	}
	
	/**
	 * 
	 * @param years
	 * @return
	 */
	private Node getOrCreateYearNode(int years) {
		Node year = null;
		String yearP = ""+years;
		if (yearNodes.containsKey(yearP)) {
			year = yearNodes.get(yearP);
		} else {
			year = findCalendarNode(Properties.YEAR, yearP);
			yearNodes.put(yearP, year);
		}
		return year;
	}
	
	/**
	 * 
	 * @param property
	 * @param value
	 * @return
	 */
	private Node findCalendarNode(String property, String value) {
		Node node = graphDB.findNode(calendarLabel, property, value);
		if (node == null) {
			node = graphDB.createNode(calendarLabel);
			node.setProperty(property, value);
		}
		return node;
	}
}
