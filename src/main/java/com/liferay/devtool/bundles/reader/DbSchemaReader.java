package com.liferay.devtool.bundles.reader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.utils.StringUtils;
import com.liferay.devtool.utils.SysEnv;

public class DbSchemaReader {
	private SysEnv sysEnv;
	private BundleEntry bundleEntry;
	
	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	public void readDetails() {
		if (bundleEntry.getDbUrl() != null && bundleEntry.getDbDriverClass() != null && bundleEntry.getDbDriverClass().equals("com.mysql.jdbc.Driver")) {
			readMySqlSchemaData();
		}
	}

	private void readMySqlSchemaData() {
		String schemaName = StringUtils.extractSchemaNameFromMySqlUrl(bundleEntry.getDbUrl());
		try {
			// Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection con = DriverManager.getConnection(
					bundleEntry.getDbUrl() + "&serverTimezone=UTC",
					bundleEntry.getDbUsername(),
					bundleEntry.getDbPassword());
			
			queryTableCount(schemaName, con);
			querySchemaVersion(schemaName, con);
			
			con.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	private void queryTableCount(String schemaName, Connection con) throws SQLException {
		Statement stmt = con.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT count(*) FROM information_schema.tables where table_schema='"+schemaName+"'");
		while (rs.next()) {
			System.out.println("count="+rs.getInt(1));
		}
		rs.close();
	}
	
	private void querySchemaVersion(String schemaName, Connection con) throws SQLException {
		Statement stmt = con.createStatement();
		
		ResultSet rs = stmt.executeQuery("SELECT buildNumber FROM "+schemaName+".release_ where releaseId = 1");
		while (rs.next()) {
			System.out.println("sch version="+rs.getString(1));
		}
		rs.close();
	}
}
