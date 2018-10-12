package maven.miner.procedures;

import java.util.HashMap;
import java.util.Map;


import info.debatty.java.stringsimilarity.Cosine;

public class Test {
	public static Cosine similarity = new Cosine(2);
	public static void main(String[] args) throws Exception {

//		LocalDate date = LocalDate.parse("9999-12-02");
//		System.out.println("");
////		LocalDate.now().toString();
////		System.out.println("");
		
		String [] oldDependencies = {
		                            "geronimo-spec:geronimo-spec-j2ee-management:1.0-rc4",
		                            "geronimo-spec:geronimo-spec-j2ee-jacc:1.0-rc4",
		                            "activemq:activemq-core:3.2.3",
		                            "geronimo-spec:geronimo-spec-j2ee-connector:1.5-rc4",
		                            "activemq:activemq-optional:3.2.3",
		                            "geronimo-spec:geronimo-spec-jta:1.0.1B-rc4",
		                            "incubator-derby:derby:10.0.2.1",
		                            "geronimo-spec:geronimo-spec-jms:1.1-rc4",
		                            "activemq:activemq-core-test:3.2.3"
									};
		
		String [] newDependencies = {
									"org.apache.derby:derby:10.1.1.0",
								    "org.apache.geronimo.specs:geronimo-j2ee-connector_1.5_spec:1.0",
								    "activemq:activemq-core:3.2.4",
								    "activemq:activemq-optional:3.2.4",
								    "org.apache.geronimo.specs:geronimo-j2ee-management_1.0_spec:1.0",
								    "org.apache.geronimo.specs:geronimo-jms_1.1_spec:1.0",
								    "org.apache.geronimo.specs:geronimo-jta_1.0.1B_spec:1.0",
								    "org.apache.geronimo.specs:geronimo-j2ee-jacc_1.0_spec:1.0",
								    "activemq:activemq-core-test:3.2.4"
								 };
		
		Map<String, Map<String,Number>> result = new HashMap<String, Map<String,Number>>();
		for (int i = 0; i < oldDependencies.length; i++) {
			Map<String,Number> row = new HashMap<String, Number>();
			result.put(oldDependencies[i], row);
			for (int j = 0; j < newDependencies.length; j++ ) {
				row.put(newDependencies[j],0);
			}
		}
		for (int i = 0; i < oldDependencies.length; i++) {
			for (int j = 0; j < oldDependencies.length; j++) {
				computeCosineSimilarity(result, oldDependencies[i], newDependencies[j]);
			}
		}

		System.out.println(result.toString());
	}
	
	private static void computeCosineSimilarity(Map<String, Map<String, Number>> matrix,
			  String first, 
			   String second) throws Exception {
			
			matrix.get(first).put(second, similarity.similarity(similarity.getProfile(first), similarity.getProfile(first)));
	}	
}



