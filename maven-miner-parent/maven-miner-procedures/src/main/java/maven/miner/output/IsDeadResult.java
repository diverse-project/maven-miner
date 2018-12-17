package maven.miner.output;

import scala.Tuple2;

public class IsDeadResult {

	public String coordinates;
	public boolean isDead;
	
	public IsDeadResult (String coordinates, boolean isDead) {
		this.coordinates = coordinates;
		this.isDead = isDead;
	}
	
	public IsDeadResult (Tuple2<String, Boolean> tuple) {
		this(tuple._1(),tuple._2);
	}
	
}
