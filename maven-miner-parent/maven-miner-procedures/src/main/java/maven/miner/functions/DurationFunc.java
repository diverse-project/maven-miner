package maven.miner.functions;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import fr.inria.diverse.maven.util.MavenMinerUtil;

public class DurationFunc {
	 
	 @UserFunction(name = "maven.miner.duration.between")
	 @Description("maven.miner.duration.between(String startTime, String EndTime)"
	 				+ "- Computes the the average duration  matrix.") 
	 
	 public Number getAverageDuration(@Name(value ="the start time") String start, 
			 								@Name(value ="the start time") String end ) {
		 
		 ZonedDateTime startTime = null; 
		 ZonedDateTime endTime =null;
		 try {
			 
			 startTime = MavenMinerUtil.fromZonedTime(start);
			 endTime =MavenMinerUtil.fromZonedTime(end);
			 
			 return Math.abs(Duration.between(startTime, endTime).toDays());
		 } catch (Throwable th) {
			 throw new RuntimeException(th);
		 } 
	 }
}
