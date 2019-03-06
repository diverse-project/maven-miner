package fr.inria.diverse.maven.resolver.db.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MariaDBWrapper {
	Connection connection;
	int port;
	String domain;
	String user;
	String password;
	String dbname;
	File propFile;


	public MariaDBWrapper() throws SQLException {
		this.propFile = new File("./maven-miner-aether/src/main/resources/mariadb.properties");
		init();
	}


	public MariaDBWrapper(File propFile) throws SQLException {
		this.propFile = propFile;
		init();
	}

	private void init() throws SQLException {

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propFile));
			port = Integer.parseInt(properties.getProperty("port"));
			domain = properties.getProperty("domain");
			user = properties.getProperty("user");
			password = properties.getProperty("password");
			dbname = properties.getProperty("dbname");
			connection = DriverManager.getConnection("jdbc:mariadb://" +
					domain + ":" +
					port + "/" +
					dbname + "?user=" +
					user + "&password=" +
					password
			);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reset() throws SQLException, InterruptedException {
		connection.close();
		Thread.sleep(10000);
		init();
	}

	public Connection getConnection() {
		return connection;
	}
}
