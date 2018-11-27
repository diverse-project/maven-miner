package maven.miner.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class TimeToReleaseOutput {

	public String groupId;
	public String artifactId;
	public List<Long> durations;
	
	
	public TimeToReleaseOutput(List<Long> durations, String groupId, String artifactId) {
		this.artifactId = artifactId;
		this.groupId = groupId;
		
		Collections.sort((List<Long>) durations);
		
		int size = durations.size();
		List<Long> result =new ArrayList<Long>(size);
		result.add(0,durations.get(0));
		for (int i = 1; i < size ; i++) {
			result.add(i, durations.get(i) - durations.get(i-1));
		}
		this.durations  = result;
	}
	
	@SuppressWarnings("unchecked")
	public TimeToReleaseOutput(Map<String, Object> entries) {
		this((List<Long>) entries.get("timeToRelease"),
				(String) entries.get("groupId"),
				(String) entries.get("artifactId"));
	}
}
