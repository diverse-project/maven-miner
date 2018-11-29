package fr.inria.diverse.maven.resolver.launcher.populate;

import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDependencies {

	static String getLibIdQuery = "SELECT id FROM library WHERE coordinates=?;";
	static String getClientIdQuery = "SELECT id FROM client WHERE coordinates=?;";

	static String insertClientQuery = "INSERT INTO client (id, coordinates, groupid, artifactid, version)\n" +
			"VALUES (NULL, ?, ?, ?, ?); ";
	static String insertDependencyQuery = "INSERT INTO dependency (clientid, libraryid, intensity, diversity)\n" +
			"VALUES (?, ?, NULL, NULL); ";

	public static void main(String[] args) throws SQLException {
		//File clientDependencies = new File("/home/nharrand/Documents/depusageminer/data/top100_used_artifacts.csv");
		File clientDependencies = new File(args[0]);
		MariaDBWrapper db = new MariaDBWrapper();
		try(BufferedReader br = new BufferedReader(new FileReader(clientDependencies))) {
			br.readLine();
			//Skip jusqu a javax.inject:javax.inject:1
			for(String line; (line = br.readLine()) != null; ) {
				try {
					String raw[] = line.split("\\[");
					String library = raw[0].split(",")[0];
					System.out.println("Processing lib: " + library);
					String[] dependencies = raw[1].split("]")[0].split(",");

					//Is library there?

					PreparedStatement getLibIdQueryStmt = db.getConnection().prepareStatement(getLibIdQuery);
					getLibIdQueryStmt.setString(1, library);
					ResultSet result = getLibIdQueryStmt.executeQuery();
					if (result.next()) {
						int libID = result.getInt("id");
						result.close();
						for (String dep : dependencies) {
							try {
								//Is client there?
								int clientID;
								PreparedStatement getClientIdQueryStmt = db.getConnection().prepareStatement(getClientIdQuery);
								getClientIdQueryStmt.setString(1, dep);
								ResultSet resultCli = getClientIdQueryStmt.executeQuery();
								if (resultCli.next()) {
									clientID = resultCli.getInt("id");
									resultCli.close();
								} else {
									resultCli.close();
									PreparedStatement insertClientStmt = db.getConnection().prepareStatement(insertClientQuery);
									insertClientStmt.setString(1, dep);
									insertClientStmt.setString(2, dep.split(":")[0]);
									insertClientStmt.setString(3, dep.split(":")[1]);
									insertClientStmt.setString(4, dep.split(":")[2]);

									int affectedRows = insertClientStmt.executeUpdate();
									if (affectedRows != 1) {
										throw new SQLException("Creating client failed, no rows affected.");
									} else {
										ResultSet r = insertClientStmt.getGeneratedKeys();
										r.next();
										clientID = r.getInt(1);
										r.close();
									}

								}

								//insert dep relationship
								PreparedStatement insertDependencyStmt = db.getConnection().prepareStatement(insertDependencyQuery);
								insertDependencyStmt.setInt(1, clientID);
								insertDependencyStmt.setInt(2, libID);
								int affectedRows = insertDependencyStmt.executeUpdate();
								if (affectedRows != 1) {
									throw new SQLException("Creating dependency failed, no rows affected.");
								}
							} catch (Exception e) {
								System.err.println("Failed to process dep: " + dep);
							}
						}
					} else {
						result.close();
						throw new SQLException("Library not found.");
					}
					System.out.println("Done");
				} catch (Exception e) {
					System.err.println("Failed to read or process line.");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
