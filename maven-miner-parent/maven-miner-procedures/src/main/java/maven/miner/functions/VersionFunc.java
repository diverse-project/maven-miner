package maven.miner.functions;

import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.Version;

import fr.inria.diverse.maven.common.Properties;
import fr.inria.diverse.maven.util.VersionInformation;

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

			try {
				VersionInformation v1Info = new VersionInformation(version1);
				VersionInformation v2Info = new VersionInformation(version2);
				if (v2Info.getMajor() != v1Info.getMajor() && 
						v2Info.getMinor() != v1Info.getMinor()) 
					return true;
				return false;
			} catch (Exception e) {
				
				throw new RuntimeException(String.format("Unable to parse Versions %s, %s. "
						+ "\n See full stack below %s", version1, version2, e.getLocalizedMessage()));
			}
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
		
		 if (compareVersion(version1, version2)> 0 ) 
			 throw new RuntimeException("Invalid versions order! the second version number in parameter "
		 		+ "should be strictly greater than the first one!");
		try {
			VersionInformation v1Info = new VersionInformation(version1);
			VersionInformation v2Info = new VersionInformation(version2);
			if (v2Info.getMajor() == v1Info.getMajor()) 
				return true;
			return false;
		} catch (Exception e) {
			
			throw new RuntimeException(String.format("Unable to parse Versions %s, %s. "
					+ "\n See full stack below %s", version1, version2, e.getLocalizedMessage()));
		}
	 }
	 
	 /**
	  * 
	  * @param version1
	  * @param version2
	  * @return
	  */
	 @UserFunction(name = "maven.miner.version.upgradeType")
	 @Description("maven.miner.version.upgradeType(version1'', 'version2') - returns upgrade information between two consecutive versions (MAJOR, MINOR, PATCH)")
	 public String information(@Name("version1") String version1, 
			 									@Name("version2") String version2) {
		
		if (compareVersion(version1, version2)> 0 ) 
			 throw new RuntimeException("Invalid versions order! the second version number in parameter "
		 		+ "should be strictly greater than the first one!");
		try {
			VersionInformation v1Info = new VersionInformation(version1);
			VersionInformation v2Info = new VersionInformation(version2);
			if (v2Info.getMajor()>v1Info.getMajor()) 
				return UpgradeType.MAJOR.name();
			else if (v2Info.getMinor() > v1Info.getMinor())
				return UpgradeType.MINOR.name();
			return UpgradeType.PATCH.name();
		} catch (Exception e) {
			return UpgradeType.UNKNOWN.name();
//			throw new RuntimeException(String.format("Unable to parse Versions %s, %s. "
//					+ "\n See full stack below %s", version1, version2, e.getLocalizedMessage()));
		}
		
	 }
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 private int compareVersion (Node node , String version) {
		return  compareVersion((String)node.getProperty(Properties.VERSION), version);
	 }
	 
	 /**
	  * 
	  * @param node
	  * @param version
	  * @return
	  */
	 private int compareVersion (String version1 , String version2) {
		 Version v1 = null;
		 Version v2 = null;
		 try {
				v1 = versionScheme.parseVersion(version1);
				v2 = versionScheme.parseVersion(version2);
				
			 } catch (Throwable e) {
				throw new RuntimeException("Unable to compare node version", e);
			 }
		 return v1.compareTo(v2);
	 } 
	 public enum UpgradeType {
		 MAJOR, MINOR, PATCH, UNKNOWN
	 }
	
}

