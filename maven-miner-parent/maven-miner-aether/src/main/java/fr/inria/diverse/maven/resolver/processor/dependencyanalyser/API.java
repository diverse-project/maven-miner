package fr.inria.diverse.maven.resolver.processor.dependencyanalyser;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class API {
	int libID;

	Map<String, APIPackage> packages = new HashMap<>();

	class APIPackage {
		int id;
		String name;
		Map<String, APIClass> classes = new HashMap<>();

		public APIPackage(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public void insertType(String name, boolean isPublic, boolean isInterface, boolean isAnnotation, boolean isAbstract) {
			classes.putIfAbsent(name, new APIClass(name));
			APIClass cl = classes.get(name);
			cl.isPublic = isPublic;
			cl.isInterface =isInterface;
			cl.isAnnotation = isAnnotation;
			cl.isAbstract = isAbstract;
			cl.seen = true;
			classes.put(name,cl);
		}

		public void insertElement(String clazz, String element, boolean isStatic, boolean isPublic, boolean isField) {
			APIClass c = classes.get(clazz);
			if(c == null) {
				System.err.println("unknown class " + clazz + " in package " + name + ", in lib " + libID);
			} else {
				c.insertElement(element,isStatic,isPublic,isField);
			}
		}

		public void insertRaw(String clazz, String member, int memberID) {
			APIClass cl;
			classes.putIfAbsent(clazz, new APIClass(clazz));
			cl = classes.get(clazz);
			if(member.equals("NULL")) {
				cl.id = memberID;
			} else {
				cl.insertRaw(member, memberID);
			}
		}

		public String toSQLString() {
			String sql = "";
			for(APIClass cl : classes.values()) {
				sql += cl.toSQLString(id);
			}
			return sql;
		}
	}

	class APIClass {
		Integer id;
		String name;
		boolean isPublic;
		boolean isInterface;
		boolean isAnnotation;
		boolean isAbstract;
		boolean seen;
		Map<String, APIElement> elements = new HashMap<>();

		public APIClass(int id, String name) {
			this.id = id;
			this.name = name;
			this.seen = false;
		}

		public APIClass(String name) {
			this.name = name;
		}

		public void insertRaw(String element, int memberID) {
			elements.put(element, new APIElement(memberID,element));
		}

		public void insertElement(String element, boolean isStatic, boolean isPublic, boolean isField) {
			elements.putIfAbsent(element, new APIElement(element));
			APIElement el = elements.get(element);
			el.isStatic = isStatic;
			el.isPublic = isPublic;
			el.isField = isField;
			el.seen = true;
		}

		public String toSQLString(int packID) {
			String sql = "(NULL," + packID + ",'" + name +"'," +
					"'NULL'," +
					(isPublic ? 1 : 0) + "," +
					(isInterface ? 1 : 0) + "," +
					(isAnnotation ? 1 : 0) + "," +
					(isAbstract ? 1 : 0) + "," +
					"0," +
					"0," +
					(seen ? 1 : 0) + "," +
					libID + "," +
					(id == null ? "NULL" : id) + "),\n";

			for(APIElement el : elements.values()) {
				sql += el.toSQLString(packID,name);
			}
			return sql;
		}
	}

	class APIElement {
		Integer id;
		String name;
		boolean isStatic;
		boolean isPublic;
		boolean isField;
		boolean seen;

		public APIElement(int id, String name) {
			this.id = id;
			this.name = name;
			seen = false;
		}

		public APIElement(String name) {
			this.name = name;
		}

		public String toSQLString(int packID, String clazz) {
			return "(NULL," + packID + ",'" + clazz + "'," +
					"'" + name + "'," +
					(isPublic ? 1 : 0) + "," +
					0 + "," +
					0 + "," +
					0 + "," +
					(isStatic ? 1 : 0) + "," +
					(isField ? 1 : 0) + "," +
					(seen ? 1 : 0) + "," +
					libID + "," +
					(id == null ? "NULL" : id) + "),\n";
		}
	}

	public void insertType(String pack, String name, boolean isPublic, boolean isInterface, boolean isAnnotation, boolean isAbstract) {
		APIPackage p = packages.get(pack);
		if(p == null)  {
			System.err.println("unknown package " + pack + " in lib " + libID);
		} else {
			p.insertType(name,isPublic,isInterface,isAnnotation,isAbstract);
		}
	}

	public void insertElement(String pack, String clazz, String element, boolean isStatic, boolean isPublic, boolean isField) {
		APIPackage p = packages.get(pack);
		if(p == null) {
			System.err.println("unknown package " + pack + " in lib " + libID);
		} else {
			p.insertElement(clazz,element,isStatic,isPublic,isField);
		}
	}


	static String getLibPackages = "SELECT l.id, l.coordinates, p.id as packageid, p.package " +
			"FROM library as l JOIN package as p ON p.libraryid=l.id WHERE l.id=?";
	static String getLibExistingInfo = "SELECT l.id, l.coordinates, p.id as packageid, p.package, m.class, m.member, m.id as memberid " +
			"FROM library as l JOIN package as p ON p.libraryid=l.id JOIN api_member as m ON m.packageid=p.id WHERE l.id=?";


	public API(int libraryId, Connection db) throws SQLException {
		this.libID = libraryId;
		PreparedStatement getPackageIdsStmt = db.prepareStatement(getLibPackages);
		getPackageIdsStmt.setInt(1, libraryId);
		ResultSet result = getPackageIdsStmt.executeQuery();
		while(result.next()) {
			String packageName = result.getString("package");
			int packageId = result.getInt("packageid");
			packages.putIfAbsent(packageName, new APIPackage(packageId,packageName));
		}

		PreparedStatement getINfoStmt = db.prepareStatement(getLibExistingInfo);
		getINfoStmt.setInt(1, libraryId);
		ResultSet iresult = getINfoStmt.executeQuery();
		while(iresult.next()) {
			String packageName = iresult.getString("package");
			int packageId = iresult.getInt("packageid");
			APIPackage p = packages.get(packageName);
			String clazz = iresult.getString("class");
			String member = iresult.getString("member");
			int apimemberid = iresult.getInt("memberid");

			p.insertRaw(clazz,member,apimemberid);
		}
	}

	static String separator = ".";

	/*public void insert(String owner, String targetMember, boolean isPublic) {
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
	}*/

	static String insertUsage = "INSERT INTO api_member_full " +
			"(id,package,class,member,isPublic,isInterface,isAnnotation,isAbstract,isStatic,isField,isSeen,libraryid,apimemberid)\n" +
			"VALUES\n";

	public boolean pushToDB(Connection db) throws SQLException {
		String query = insertUsage;
		for(APIPackage p : packages.values()) {
			query += p.toSQLString();
		}
		if(query.length() > insertUsage.length()) {
			query = query.substring(0, query.length() - 2);
			db.prepareStatement(query).execute();
			return true;
		} else {
			System.out.println("Nothing to push for.");
			return false;
		}
	}
}
