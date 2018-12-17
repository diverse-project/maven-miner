package maven.miner.functions;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import fr.inria.diverse.maven.util.MavenMinerUtil;

public class DurationFunc {
	 
	 @UserFunction(name = "maven.miner.duration.between")
	 @Description("maven.miner.duration.between(String startTime, String endTime, String unit)"
	 				+ "- Returns the duration, in a given unit, between two string dates."
	 				+ " Suppprted units are H (for HOURS), D (for DAYS), and M(for MONTHS)") 
	 
	 public Number getAverageDuration(@Name(value ="the start time") String start, 
			 								@Name(value ="the start time") String end, 
			 								@Name(value ="Time Unit", defaultValue="D") String unit) {
		 
		 ZonedDateTime startTime = null; 
		 ZonedDateTime endTime =null;
		 try {
			 
			 startTime = MavenMinerUtil.fromZonedTime(start);
			 endTime =MavenMinerUtil.fromZonedTime(end);
			 unit = unit.trim();
			 switch (unit) {
				 case "D" : return ChronoUnit.DAYS.between(startTime, endTime);
				 case "H" : return ChronoUnit.HOURS.between(startTime, endTime);
				 case "M" : return ChronoUnit.MONTHS.between(startTime, endTime);
			 }

		 } catch (Throwable th) {
			 throw new RuntimeException(th);
		 } 
		 // Unreachable due to 
		 return null;
	 }
}
