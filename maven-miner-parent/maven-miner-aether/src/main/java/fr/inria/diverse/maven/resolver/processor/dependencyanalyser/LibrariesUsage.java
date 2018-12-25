package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LibrariesUsage {
	//Lib id -> package -> Member -> <Usages, From class>
	public Map<Integer, Map<String, Map<String, Usage>>> librariesPackagesMembersUsage = new HashMap<>();
	public Map<Integer, Map<String, Integer>> libraryPackagesId = new HashMap<>();


	//public Map<Integer,Map<String,Map<String, Integer>>> librariesPackagesMembersUsage = new HashMap<>();

	public LibrariesUsage(Map<Integer, Map<Integer, String>> librariesPackages) {
		for(Map.Entry<Integer, Map<Integer, String>> entry: librariesPackages.entrySet()) {
			Map<String,Map<String, Usage>> packagesMembersUsage = new HashMap<>();
			Map<String, Integer> packageIds = new HashMap<>();
			for(Integer packageId : entry.getValue().keySet()) {
				String packageName = entry.getValue().get(packageId);
				packagesMembersUsage.put(packageName, new HashMap<>());
				packageIds.put(packageName,packageId);
			}
			librariesPackagesMembersUsage.put(entry.getKey(), packagesMembersUsage);
			libraryPackagesId.put(entry.getKey(), packageIds);
		}
	}

	static String separator = ".";
	public void insertIfPartOfPackages(String owner, String targetMember, String fromClass) {
		int separatorIndex = owner.lastIndexOf('/');
		if(separatorIndex < 0) return;
		String targetPackageName = owner.substring(0, separatorIndex);
		String apiMember = owner.substring(separatorIndex+1) + (targetMember.equals("") ? "" : (separator + targetMember));
		for(Integer libraryId: librariesPackagesMembersUsage.keySet()) {
			Map<String,Map<String, Usage>> packagesMembersUsage = librariesPackagesMembersUsage.get(libraryId);
			if(!packagesMembersUsage.containsKey(targetPackageName)) {
				continue;
			} else {
				Map<String, Usage> membersUsage = packagesMembersUsage.get(targetPackageName);
				Usage usage = membersUsage.computeIfAbsent(apiMember, m -> new Usage());
				usage.nb++;
				usage.from.add(fromClass);
				membersUsage.put(apiMember,usage);
			}
		}
	}

	static String getClientID = "SELECT id FROM client WHERE coordinates=?";

	static String insertUsage = "INSERT INTO api_usage (clientid, apimemberid, nb, diversity)\n" +
			"VALUES\n";

	public boolean pushToDB(Connection db, String clientGAV) throws SQLException {
		int clientID = 0;
		PreparedStatement getLibIdQueryStmt = db.prepareStatement(getClientID);
		getLibIdQueryStmt.setString(1, clientGAV);
		ResultSet result = getLibIdQueryStmt.executeQuery();
		if (result.next()) {
			clientID = result.getInt("id");
			result.close();
		} else {
			result.close();
			throw new SQLException("Client not found");
		}

		String query = insertUsage;
		for(Integer libraryId: librariesPackagesMembersUsage.keySet()) {
			Map<String,Map<String, Usage>> packagesMembersUsage = librariesPackagesMembersUsage.get(libraryId);
			Map<String, Integer> packageIds = libraryPackagesId.get(libraryId);
			for(String packageName: packagesMembersUsage.keySet()) {
				Map<String, Usage> membersUsage = packagesMembersUsage.get(packageName);
				int packageId = packageIds.get(packageName);
				for(String member: membersUsage.keySet()) {
					Usage usage = membersUsage.get(member);
					if(usage.nb != 0) {
						String clazz = member;
						String memberName = "NULL";
						if (member.contains(".")) {
							clazz = member.split("\\.")[0];
							memberName = member.split("\\.")[1];
						}

						//get memberID and insert if necessary
						query += "(" + clientID +
								", funcApiMemberID('" + packageId + "', '" + clazz + "', '" + memberName + "', " + libraryId + "), " +
								usage.nb + ", " + usage.from.size() + "),";
					}
				}
			}
		}
		if(query.length() > insertUsage.length()) {
			query = query.substring(0, query.length() - 1);
			db.prepareStatement(query).execute();
			return true;
		} else {
			System.out.println("Nothing to push for client " + clientGAV + " (" + clientID + ").");
			return false;
		}
	}

	public class Usage {
		public int nb = 0;
		public Set<String> from = new HashSet<>();

		@Override
		public String toString() {
			return "(" + nb + ", " + from.size() +")";
		}
	}
}
