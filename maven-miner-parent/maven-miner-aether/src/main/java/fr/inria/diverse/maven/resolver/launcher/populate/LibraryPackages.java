package fr.inria.diverse.maven.resolver.launcher.populate;

import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class LibraryPackages {

	static String insertLibraryQuery = "INSERT INTO library (id, coordinates, groupid, artifactid, version, api_size)\n" +
			"VALUES (NULL, ?, ?, ?, ?, NULL); ";
	static String insertPackageQuery = "INSERT INTO package (id, libraryid, package)\n" +
			"VALUES (NULL, ?, ?); ";

	public static void main(String[] args) throws SQLException {
		MariaDBWrapper db = new MariaDBWrapper();
		File packageInfoDir = new File("./output");
		for(File packageInfo: packageInfoDir.listFiles()) {
			Set<String> packages = new HashSet<>();
			String library = packageInfo.getName();
			PreparedStatement insertLibraryStmt = db.getConnection().prepareStatement(insertLibraryQuery);
			insertLibraryStmt.setString(1, library);
			insertLibraryStmt.setString(2, library.split(":")[0]);
			insertLibraryStmt.setString(3, library.split(":")[1]);
			insertLibraryStmt.setString(4, library.split(":")[2]);

			int affectedRows = insertLibraryStmt.executeUpdate();
			if (affectedRows != 1) {
				throw new SQLException("Creating library failed, no rows affected.");
			}

			int libID = 0;

			try (ResultSet generatedKeys = insertLibraryStmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					libID = generatedKeys.getInt(1);
					try(BufferedReader br = new BufferedReader(new FileReader(packageInfo))) {
						for(String line; (line = br.readLine()) != null; ) {
							PreparedStatement insertPackageStmt = db.getConnection().prepareStatement(insertPackageQuery);
							insertPackageStmt.setInt(1, libID);
							insertPackageStmt.setString(2, line);

							affectedRows = insertPackageStmt.executeUpdate();
							if (affectedRows != 1) {
								throw new SQLException("Creating package failed, no rows affected.");
							}
							packages.add(line);

						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					throw new SQLException("Creating library failed, no ID obtained.");
				}
			}


			System.out.println("Done");
		}
	}
}
