package maven.miner.functions;

import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.Version;

import fr.inria.diverse.maven.common.Properties;

public class VersionFunc {
	 
	 public static final GenericVersionScheme versionScheme = new GenericVersionScheme();
	
	 @UserFunction(name = "maven.miner.version.isGreater")
	 @Description("maven.miner.version.isGreater(n, 'v') - returns true if the the version of a node 'n' is Greater than teh provided one")
	 public Boolean isVersionGreater(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) == 1;
	 }
	 
	 @UserFunction(name = "maven.miner.version.isGreaterOrEqual")
	 @Description("maven.miner.version.isGreater(n, 'v') - returns true if the the version of a node 'n' is Greater or equat to the provided one")
	 public Boolean isVersionGreaterOrEqual(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) >= 0 ;
	 }
	 
	 @UserFunction(name = "maven.miner.version.isLower")
	 @Description("maven.miner.version.isGreater(n, 'v') - returns true if the the version of a node 'n' is Greater than teh provided one")
	 public Boolean isVersionLower(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) == -1;
	 }
	 
	 @UserFunction(name = "maven.miner.version.isLowerOrEqual")
	 @Description("maven.miner.version.isGreater(n, 'v') - returns true if the the version of a node 'n' is Greater than teh provided one")
	 public Boolean isVersionLowerOrEqual(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) <= 0;
		 
	 }
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 private int compareVersion (Node node , String version) {
		 Version v1 = null;
		 Version v2 = null;
		 try {
				v1 = versionScheme.parseVersion((String)node.getProperty(Properties.VERSION));
				v2 = versionScheme.parseVersion(version);
			} catch (Throwable e) {
				throw new RuntimeException("Unable to compare node version", e);
			}
		 return v1.compareTo(v2);
	 }
	 
	
}

