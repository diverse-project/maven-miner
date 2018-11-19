package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LibrariesUsage {
	public Map<Integer,Map<String,Map<String, Integer>>> librariesPackagesMembersUsage = new HashMap<>();

	public LibrariesUsage(Map<Integer,Set<String>> librariesPackages) {
		for(Map.Entry<Integer,Set<String>> entry: librariesPackages.entrySet()) {
			Map<String,Map<String, Integer>> packagesMembersUsage = new HashMap<>();
			for(String peckageName : entry.getValue()) {
				packagesMembersUsage.put(peckageName, new HashMap<>());
			}
			librariesPackagesMembersUsage.put(entry.getKey(), packagesMembersUsage);
		}
	}

	static String separator = ".";
	public void insertIfPartOfPackages(String owner, String targetMember) {
		int separatorIndex = owner.lastIndexOf('/');
		String targetPackageName = owner.substring(0, separatorIndex);
		String apiMember = owner.substring(separatorIndex+1) + (targetMember.equals("") ? "" : (separator + targetMember));
		for(Integer libraryId: librariesPackagesMembersUsage.keySet()) {
			Map<String,Map<String, Integer>> packagesMembersUsage = librariesPackagesMembersUsage.get(libraryId);
			if(!packagesMembersUsage.containsKey(targetPackageName)) {
				continue;
			} else {
				Map<String,Integer> membersUsage = packagesMembersUsage.get(targetPackageName);
				Integer usage = membersUsage.computeIfAbsent(apiMember, m -> 0);
				usage++;
				membersUsage.put(apiMember,usage);
			}
		}

	}

	static String getClientID = "SELECT id FROM client WHERE coordinates=?";

	static String insertUsage = "INSERT INTO api_usage (clientid, apimemberid, nb)\n" +
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
			Map<String,Map<String, Integer>> packagesMembersUsage = librariesPackagesMembersUsage.get(libraryId);
			for(String packageName: packagesMembersUsage.keySet()) {
				Map<String,Integer> membersUsage = packagesMembersUsage.get(packageName);
				for(String member: membersUsage.keySet()) {
					Integer usage = membersUsage.get(member);
					if(usage != 0) {
						String clazz = member;
						String memberName = "NULL";
						if (member.contains(".")) {
							clazz = member.split("\\.")[0];
							memberName = member.split("\\.")[1];
						}
						//get memberID and insert if necessary
						query += "(" + clientID +
								", funcApiMemberID('" + packageName + "', '" + clazz + "', '" + memberName + "', " + libraryId + "), " +
								usage + "),";
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
}
