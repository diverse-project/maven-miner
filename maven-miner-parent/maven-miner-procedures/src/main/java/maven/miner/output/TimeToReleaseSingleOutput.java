package maven.miner.output;

import java.util.Map;


public class TimeToReleaseSingleOutput {

	public String groupId;
	public String artifactId;
	public Long duration;
	
	
	public TimeToReleaseSingleOutput(Long duration, String groupId, String artifactId) {
		this.artifactId = artifactId;
		this.groupId = groupId;
		this.duration  = duration;
	}
	
	public TimeToReleaseSingleOutput(Map<String, Object> entries) {
		this((Long) entries.get("timeToRelease"),
				(String) entries.get("groupId"),
				(String) entries.get("artifactId"));
	}
}
