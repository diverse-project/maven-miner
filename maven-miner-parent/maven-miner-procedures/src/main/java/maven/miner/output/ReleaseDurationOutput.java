package maven.miner.output;

import java.util.Map;

public class ReleaseDurationOutput {
	public Number duration;
	public String sourceCoordinates;
	public String targetCoordinates;
	
	public ReleaseDurationOutput (Number number, String source, String target) {
		this.sourceCoordinates = source;
		this.targetCoordinates = target;
		this.duration = number;
	}
	
	public ReleaseDurationOutput (Map<String, Object> entries) {
		this(((Number)entries.get("duration")), 
				((String)entries.get("sourceCoordinates")),
				((String)entries.get("targetCoordinates")));
	}
}
