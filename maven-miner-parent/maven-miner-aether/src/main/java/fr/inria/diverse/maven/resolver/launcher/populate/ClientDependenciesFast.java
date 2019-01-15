package fr.inria.diverse.maven.resolver.launcher.populate;

import fr.inria.diverse.maven.resolver.db.sql.MariaDBWrapper;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientDependenciesFast {

	static File libProblem = new File("libProblems.error");
	static File cliProblem = new File("cliProblems.error");

	static boolean dryRun = false;

	static String getLibIdQuery = "SELECT id FROM library WHERE coordinates=?;";

	static String insertQuery = "INSERT INTO dependency(clientid, libraryid, intensity,diversity)\nVALUES\n";

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

					if(dependencies.length <= 0) {
						System.err.println("No dependencies for <" + library + ">");
					} else {

						if (dryRun) {
							System.out.println("[DryRun] Read: " + library + " (" + dependencies.length + ")");
						}
						//Is library there?

						PreparedStatement getLibIdQueryStmt = db.getConnection().prepareStatement(getLibIdQuery);
						getLibIdQueryStmt.setString(1, library);
						ResultSet result = getLibIdQueryStmt.executeQuery();
						if (result.next()) {
							int libID = result.getInt("id");
							result.close();

							if (dryRun) {
								System.out.println("[DryRun] Library id: " + libID);
								for (String dep : dependencies) {
									String[] clidCoordinates= dep.replace("'","\'").split(":");
									String g = clidCoordinates[0];
									String a = clidCoordinates[1];
									String v = clidCoordinates[2];

									if(clidCoordinates.length != 3
											|| g.contains("'")
											|| a.contains("'")
											|| v.contains("'")
											|| g.length() == 0
											|| a.length() == 0
											|| v.length() == 0) {
										FileUtils.write(cliProblem, library + " <- " + dep + "\n", true);
										//System.err.println("error cli: <" + dep +"> incorrect");
									}
								}
							} else {
								String q = insertQuery;
								for (String dep : dependencies) {
									String[] clidCoordinates= dep.replace("'","\'").split(":");
									String g = clidCoordinates[0];
									String a = clidCoordinates[1];
									String v = clidCoordinates[2];

									q += "(funcClientID('" + dep + "', '" + g + "', '" + a + "', '" + v + "')," + libID + ",NULL,NULL),";
								}
								q = q.substring(0, q.length() - 1);
								PreparedStatement insertStmt = db.getConnection().prepareStatement(q);
								insertStmt.execute();
							}
						} else {
							result.close();
							FileUtils.write(libProblem, library + " SQLException\n", true);
							throw new SQLException("Library \"" + library + "\"not found.");
						}
					}
					System.out.println("Done");
				} catch (Exception e) {
					//FileUtils.write(libProblem, e.getMessage() + " Exception\n", true);
					System.err.println("Failed to read or process line:<" + e.getMessage() + ">");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
