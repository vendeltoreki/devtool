package com.liferay.devtool.bundles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbSchemaEntryFactory {
	public DbSchemaEntry createEntry(String connectionString, String userName, String password) {
		try {
			/*
			 * Loading class `com.mysql.jdbc.Driver'. This is deprecated. The new driver
			 * class is `com.mysql.cj.jdbc.Driver'. The driver is automatically registered
			 * via the SPI and manual loading of the driver class is generally unnecessary.
			 * java.sql.SQLException: The server time zone value 'K?z?p-eur?pai ny?ri id?'
			 * is unrecognized or represents more than one time zone. You must configure
			 * either the server or JDBC driver (via the serverTimezone configuration
			 * property) to use a more specifc time zone value if you want to utilize time
			 * zone support.
			 * 
			 * jdbc:mysql://localhost/lportal_62?characterEncoding=UTF-8&
			 * dontTrackOpenResources=true&holdResultsOpenOverStatementClose=true&
			 * useFastDateParsing=false&useUnicode=true
			 * jdbc:mysql://localhost/db?useUnicode=true&useJDBCCompliantTimezoneShift=true&
			 * useLegacyDatetimeCode=false&serverTimezone=UTC
			 */

			// Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection con = DriverManager.getConnection(connectionString, userName, password);
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from group_");
			while (rs.next()) {
				System.out.println(rs.getInt(1));
			}
			con.close();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) throws Exception {
		String connString = "jdbc:mysql://localhost/lportal_62?characterEncoding=UTF-8&dontTrackOpenResources=true&holdResultsOpenOverStatementClose=true&useFastDateParsing=false&useUnicode=true";

		DbSchemaEntryFactory factory = new DbSchemaEntryFactory();
		factory.createEntry(connString + "&serverTimezone=UTC", "root", "liferay123");
	}
}
