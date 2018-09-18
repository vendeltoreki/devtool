package com.liferay.devtool.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.liferay.devtool.bundles.BundleEntry;

public class DbUtil {
	private BundleEntry bundleEntry;

	public void cleanDb() {
		String schemaName = StringUtils.extractSchemaNameFromMySqlUrl(bundleEntry.getDbUrl());
		try {
			// Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection con = DriverManager.getConnection(
					bundleEntry.getDbUrl() + "&serverTimezone=UTC",
					bundleEntry.getDbUsername(),
					bundleEntry.getDbPassword());
			
			dropAndCreateSchema(schemaName, con);
			
			con.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void dropAndCreateSchema(String schemaName, Connection con) {
		executeQuery("drop schema " + schemaName, con);
		executeQuery("create schema " + schemaName + " CHARACTER SET utf8 COLLATE utf8_general_ci", con);
	}

	private void executeQuery(String query, Connection con) {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			stmt.execute(query);
		} catch (Exception e) {
			// ignore
			e.printStackTrace();
		} finally {
			tryClose(null, stmt);
		}
	}	
	
	private void tryClose(ResultSet rs, Statement stmt) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception e) {
				// ignore
			}
		}
		
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}
	
	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	
}
