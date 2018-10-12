package maven.miner.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.interfaces.StringSimilarity;

public class SimilarityFunc {
	 
	 @UserFunction(name = "maven.miner.dependencies.similarity.cosine")
	 @Description("maven.miner.dependencies.similarity.cosine(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> cosineSimilarity(@Name("oldDependencies") String [] removedDependencies, 
			 									@Name("newDependencies") String [] newDependencies) {
		 Cosine similarity = new Cosine();
		 return computeSimilarity(removedDependencies, newDependencies, similarity::similarity);
	 }
	 @UserFunction(name = "maven.miner.dependencies.similarity.cosine.single")
	 @Description("maven.miner.dependencies.similarity.cosine.single(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> cosineSimilarity(@Name("oldDependencies") String  removedDependency, 
			 									@Name("newDependencies") String  newDependency) {
		 Cosine similarity = new Cosine();
		 return computeSimilarity(new String[] {removedDependency},new String[] {newDependency}, similarity::similarity);
	 }
	 @UserFunction(name = "maven.miner.dependencies.similarity.cosine.custom")
	 @Description("maven.miner.dependencies.similarity.cosine.custom(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> cosineCustomSimilarity(@Name("oldDependencies")String [] removedDependencies, 
			 										@Name("newDependencies") String [] newDependencies) {
		 CustomSimilarity similarity = new CustomSimilarity( new Cosine());
		 return computeSimilarity(removedDependencies, newDependencies, similarity::similarity);
	 }
	 @UserFunction(name = "maven.miner.dependencies.similarity.cosine.custom.single")
	 @Description("maven.miner.dependencies.similarity.cosine.custom.single(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> cosineCustomSimilarity(@Name("oldDependencies")String removedDependencies, 
			 										@Name("newDependencies") String  newDependencies) {
		 CustomSimilarity similarity = new CustomSimilarity( new Cosine());
		 return computeSimilarity(new String[] {removedDependencies}, new String[] {newDependencies}, similarity::similarity);
	 }
	 
	 @UserFunction(name = "maven.miner.dependencies.similarity.jaroWinkler")
	 @Description("maven.miner.dependencies.similarity.jaroWinkler(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> jaroWinklerSimilarity(@Name("oldDependencies")String [] removedDependencies, 
			 										@Name("newDependencies")	String [] newDependencies) {
		 JaroWinkler similarity = new JaroWinkler();
		 return computeSimilarity(removedDependencies, newDependencies, similarity::similarity);
	 }
	 @UserFunction(name = "maven.miner.dependencies.similarity.jaroWinkler.custom")
	 @Description("maven.miner.dependencies.similarity.jaroWinkler.custom(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> jaroWinklerCustomSimilarity(@Name("oldDependencies")String [] removedDependencies, 
			 											@Name("newDependencies") String [] newDependencies) {
		 CustomSimilarity similarity = new CustomSimilarity( new JaroWinkler());
		 return computeSimilarity(removedDependencies, newDependencies, similarity::similarity);
	 }

	 @UserFunction(name = "maven.miner.dependencies.similarity.jaroWinkler.single")
	 @Description("maven.miner.dependencies.similarity.jaroWinkler.custom(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> jaroWinklerSimilarity(@Name("oldDependencies")String  removedDependencies, 
			 											@Name("newDependencies") String  newDependencies) {
		 JaroWinkler similarity = new JaroWinkler();
		 return computeSimilarity(new String[] {removedDependencies}, new String[] {newDependencies}, similarity::similarity);
	 }
	 @UserFunction(name = "maven.miner.dependencies.similarity.jaroWinkler.custom.single")
	 @Description("maven.miner.dependencies.similarity.jaroWinkler.custom(['d1','d2',...], ['d1','d2',...]) - Computes the similarity matrix.")
	 public Map<String, Map<String,Number>> jaroWinklerCustomSimilarity(@Name("oldDependencies")String removedDependencies, 
			 											@Name("newDependencies") String newDependencies) {
		 CustomSimilarity similarity = new CustomSimilarity( new JaroWinkler());
		 return computeSimilarity(new String[] {removedDependencies}, new String[] {newDependencies}, similarity::similarity);
	 }
	 /**
	  * 
	  * @param removedDependencies
	  * @param newDependencies
	  * @param similarityFunc
	  * @return
	  */
	 private Map<String, Map<String,Number>> computeSimilarity (String [] removedDependencies, 
			    String [] newDependencies, BiFunction<String, String, Number> similarityFunc) {	
		 
			Map<String, Map<String,Number>> result = createMap(removedDependencies, newDependencies);
			try {
				for (int i = 0; i < removedDependencies.length; i++) {
					for (int j = 0; j < newDependencies.length; j++) {
					String first = removedDependencies[i];
					String second = newDependencies [j];
					result.get(first).put(second, similarityFunc.apply(first, second));		
					}
				}
			} catch (Throwable th) {
				throw new RuntimeException(th);
			}
		return result;
	 }
	 /**
	  * 
	  * @param removedDependencies
	  * @param newDependencies
	  * @return
	  */
	 private static Map<String, Map<String, Number>> createMap(String[] removedDependencies, 
														String[] newDependencies) {
		Map<String, Map<String,Number>> result = new HashMap<String, Map<String,Number>>();
		for (int i = 0; i < removedDependencies.length; i++) {
			Map<String,Number> row = new HashMap<String, Number>();
			result.put(removedDependencies[i], row);
			for (int j = 0; j < newDependencies.length; j++ ) {
				row.put(newDependencies[j],0);
			}
		}
		return result;
	 }
	 
	/**
	 * 
	 * @author amine
	 *
	 */
	 private class CustomSimilarity {
		 
		 StringSimilarity similarity;
		 public CustomSimilarity (StringSimilarity similarity) {
			 this.similarity = similarity;
		 }
		 /**
		  * 
		  * @param first {@link String}
		  * @param second {@link String}
		  * @return {@link Double} the similarity score
		  */
		 public double similarity (String first, String second) {
			 String [] gav1 = first.split(":");
			 String [] gav2 = second.split(":");
			 if (!gav1[0].equals(gav2[0])) return similarity.similarity(gav1[1], gav2[1]);
			 //String.join
 			 return similarity.similarity(String.join(":", gav1[0],gav1[1]),
 					 						String.join(":", gav2[0],gav2[1]));
		 }
	 }
	
	
}

