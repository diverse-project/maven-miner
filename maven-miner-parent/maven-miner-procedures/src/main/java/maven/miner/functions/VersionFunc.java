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
	
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isGreater")
	 @Description("maven.miner.version.isGreater(Node n, String 'v') - Returns true if the the version of a node 'n' is Greater than the provided version value")
	 public Boolean isVersionGreater(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) == 1;
	 }
	 
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isGreaterOrEqual")
	 @Description("maven.miner.version.isGreaterOrEqual(Node n, 'v') - returns true if the the version of a node 'n' is Greater or equal the provided version value")
	 public Boolean isVersionGreaterOrEqual(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) >= 0 ;
	 }
	 
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isLower")
	 @Description("maven.miner.version.isLower(Node n, 'v') - returns true if the the version of a node 'n' is lower than the provided version value")
	 public Boolean isVersionLower(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) == -1;
	 }
	 
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isLowerOrEqual")
	 @Description("maven.miner.version.isLowerOrEqual(Node n, 'v') - returns true if the the version of a node 'n' is lower of equal than the provided version value")
	 public Boolean isVersionLowerOrEqual(@Name("node") Node node, 
			 									@Name("version") String version) {
		
		 return compareVersion(node, version) <= 0;
		 
	 }
	 
	 /**
	  * 
	  * @param version1
	  * @param version2
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isSameMinor")
	 @Description("maven.miner.version.isSameMinor('version'1, 'version2') - returns true if the the two versions share the same major and minor version numbers")
	 public Boolean isVersionSameMinor(@Name("version1") String version1, 
			 									@Name("version2") String version2) {

		 String [] version1_items = new String[5];
		 String [] version2_items = new String[5];
		 if (version1.contains(".")) {
			version1_items = version1.split("\\."); 
		 } else {
			 version1_items [0] = version1;
			 version1_items [1] = "0";
		 }
		 
		 if (version2.contains(".")) {
				version2_items = version2.split("\\."); 
		 } else {
				 version2_items [0] = version1;
				 version2_items [1] = "0";
		 }
		 try {
			 if (! version1_items[0].equals(version2_items[0])) {
				 return false;
			 } else  {
				 //trimming qualifiers if 
				 String item1 = version1_items[1];
				 if (item1.contains("-")) item1 = item1.substring(0, item1.indexOf('-'));
				 
				 String item2 = version2_items[1];//.substring(0, version2_items[1].indexOf('-'));
				 if (item2.contains("-")) item2 = item2.substring(0, item1.indexOf('-'));
				 if (item1.equals(item2))
				 return true;
			 }
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		return false;
	 }
	 
	 /**
	  * 
	  * @param version1
	  * @param version2
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.isSameMajor")
	 @Description("maven.miner.version.isSameMajor('version'1, 'version2') - returns true if the the two versions share the same major version number")
	 public Boolean isVersionSameMajor(@Name("version1") String version1, 
			 									@Name("version2") String version2) {
		
		 String [] version1_items = new String[5];
		 String [] version2_items = new String[5];
		 if (version1.contains(".")) {
			version1_items = version1.split("\\."); 
		 } else {
			 version1_items [0] = version1;
			 version1_items [1] = "0";
		 }
		 
		 if (version2.contains(".")) {
				version2_items = version2.split("\\."); 
		 } else {
				 version2_items [0] = version1;
				 version2_items [1] = "0";
		 }
		 try {
			 if ( version1_items[0].equals(version2_items[0])) {
				 return true;
			 } 
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		return false;
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

