package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LibraryApi {

	static String getLibExistingInfo = "SELECT l.id, l.coordinates, p.id as packageid, p.package, m.class, m.member, m.id as memberid " +
			"FROM library as l JOIN package as p ON p.libraryid=l.id JOIN api_member as m ON m.packageid=p.id WHERE l.id=?";
	//insertType
	//insertMethod
	//insertField



	//Package -> Class -> member
	public Map<String,Map<String,Set<Map.Entry<String,Boolean>>>> apiMembers;
	public Map<String, Integer> packageIds;

	int libraryId;

	public LibraryApi(int libraryId) {
		apiMembers = new HashMap<>();
		packageIds = new HashMap<>();
		this.libraryId = libraryId;
	}

	static String separator = ".";

	public void insert(String owner, String targetMember, boolean isPublic) {
		int separatorIndex = owner.lastIndexOf('/');
		String targetPackageName = owner.substring(0, separatorIndex);
		String className = owner.substring(separatorIndex+1);
		String member = targetMember.equals("") ? "NULL" : targetMember;
		insert(targetPackageName, className, member, isPublic);
	}

	public void insert(String packageName, String className, String member, boolean isPublic) {
		Map<String,Set<Map.Entry<String,Boolean>>> packageMembers = apiMembers.computeIfAbsent(packageName, p -> new HashMap<>());
		Set<Map.Entry<String,Boolean>> classMembers = packageMembers.computeIfAbsent(className, c -> new HashSet<>());
		classMembers.add(new HashMap.SimpleEntry<>(member,isPublic));
		packageMembers.put(className,classMembers);
		apiMembers.put(packageName,packageMembers);
	}

	static String getPackageIds = "SELECT package, id FROM package WHERE libraryid=?";
	public void getPackageIds(Connection db) throws SQLException {
		PreparedStatement getPackageIdsStmt = db.prepareStatement(getPackageIds);
		getPackageIdsStmt.setInt(1, libraryId);
		ResultSet result = getPackageIdsStmt.executeQuery();
		while(result.next()) {
			String packageName = result.getString("package");
			int packageId = result.getInt("id");
			packageIds.put(packageName,packageId);
		}
	}
	static String insertUsage = "INSERT INTO api_member_full (id, package, class, member, isPublic, libraryid)\n" +
			"VALUES\n";

	public boolean pushToDB(Connection db) throws SQLException {
		getPackageIds(db);
		String query = insertUsage;
		for(String packageName : apiMembers.keySet()) {
			if(!packageIds.containsKey(packageName)) System.err.println("Package: (lib " + libraryId + "): " + packageName + " is not in DB");
			int packageId = packageIds.get(packageName);
			Map<String,Set<Map.Entry<String,Boolean>>> packagesMembers = apiMembers.get(packageName);
			for(String className : packagesMembers.keySet()) {
				Set<Map.Entry<String,Boolean>> classMembers = packagesMembers.get(className);
				for(Map.Entry<String,Boolean> member: classMembers) {
					//get memberID and insert if necessary
					query += "(NULL, '" + packageId + "', '" + className + "', '" + member.getKey() + "', " + (member.getValue() ? 1 : 0) + ", " + libraryId + "),";
				}
			}
		}
		if(query.length() > insertUsage.length()) {
			query = query.substring(0, query.length() - 1);
			db.prepareStatement(query).execute();
			return true;
		} else {
			System.out.println("Nothing to push for.");
			return false;
		}
	}
}