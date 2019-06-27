package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;


import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import com.rabbitmq.client.Channel;
import org.apache.commons.io.FileUtils;

public class LibrarySelfUsage extends LibrariesUsage {
	public LibrarySelfUsage(Map<Integer, Map<Integer, String>> librariesPackages) {
		super(librariesPackages);
	}

	static String separator = ".";

	@Override
	public void insertIfPartOfPackages(String owner, String targetMember, String fromClass) {
		int separatorIndex = owner.lastIndexOf('/');
		if(separatorIndex < 0) return;
		String targetPackageName = owner.substring(0, separatorIndex);

		String fromPackage = fromClass.substring(0,fromClass.lastIndexOf('/'));
		if(fromPackage.equals(targetPackageName)) return;



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

	@Override
	public boolean pushToDB(Connection db, String clientGAV) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean pushToQueue(Connection db, Channel queue, String clientGAV) throws SQLException, IOException {
		throw new UnsupportedOperationException();
	}


	static String updateMember = "UPDATE api_member_full SET isSelfUsed=1 WHERE ";

	public boolean pushToFile(File out, String clientGAV) throws IOException {
		int clientID = 0;
		boolean empty = true;
		for(Integer libraryId: librariesPackagesMembersUsage.keySet()) {
			Map<String,Map<String, Usage>> packagesMembersUsage = librariesPackagesMembersUsage.get(libraryId);
			Map<String, Integer> packageIds = libraryPackagesId.get(libraryId);
			for(String packageName: packagesMembersUsage.keySet()) {
				Map<String, Usage> membersUsage = packagesMembersUsage.get(packageName);
				int packageId = packageIds.get(packageName);
				for(String member: membersUsage.keySet()) {
					Usage usage = membersUsage.get(member);
					if(usage.nb != 0) {
						nbElements++;
						String clazz = member;
						String memberName = "NULL";
						if (member.contains(".")) {
							clazz = member.split("\\.")[0];
							memberName = member.split("\\.")[1];
						}
						empty = false;

						//get memberID and insert if necessary
						String message = updateMember + "package=" + packageId + " AND class='" + clazz + "' AND member='" + memberName + "';\n";
						FileUtils.write(out, message, true);
					}
				}
			}
		}
		if(!empty) {
			return true;
		} else {
			System.out.println("Nothing to push for client " + clientGAV + " (" + clientID + ").");
			return false;
		}
	}
}
