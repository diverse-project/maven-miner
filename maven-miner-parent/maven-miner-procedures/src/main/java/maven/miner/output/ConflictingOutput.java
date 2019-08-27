package maven.miner.output;

import java.util.Map;

public class ConflictingOutput {

	public String client;
	public String lib;
	public String scope;
	public boolean conflicts;
	
	/**
	 * 
	 * @param client
	 * @param lib
	 * @param conflicts
	 */
	public ConflictingOutput (String client, String lib, String scope, boolean conflicts) {
		this.client = client;
		this.lib = lib;
		this.conflicts = conflicts;
	}
	/**
	 * 
	 * @param row
	 */
	@SuppressWarnings("unchecked")
	public ConflictingOutput (Map<String, Object> row) {
		this ((String)row.get("client"), 
				(String)row.get("lib"), 
				(String)row.get("scope"), 
				(boolean)row.get("conflicts"));
	}
	
}
