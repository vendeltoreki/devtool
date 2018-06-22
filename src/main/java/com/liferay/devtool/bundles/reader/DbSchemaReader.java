package com.liferay.devtool.bundles.reader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.DbSchemaEntry;
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
		if (bundleEntry.getDbUrl() != null &&
				bundleEntry.getDbDriverClass() != null &&
				bundleEntry.getDbDriverClass().equals("com.mysql.jdbc.Driver") &&
				bundleEntry.getDbUrl().startsWith("jdbc:mysql://localhost/")) {
			readMySqlSchemaData();
		}
	}

	private void readMySqlSchemaData() {
		String schemaName = StringUtils.extractSchemaNameFromMySqlUrl(bundleEntry.getDbUrl());
		DbSchemaEntry dbSchemaEntry = getDbSchemaEntry(schemaName);
		try {
			// Class.forName("com.mysql.jdbc.Driver");
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection con = DriverManager.getConnection(
					bundleEntry.getDbUrl() + "&serverTimezone=UTC",
					bundleEntry.getDbUsername(),
					bundleEntry.getDbPassword());
			
			int tableCount = queryTableCount(schemaName, con);
			if (tableCount > 0) {
				dbSchemaEntry.setTableCount(tableCount);
				String schemaVersion = querySchemaVersion(schemaName, con);
				if (schemaVersion != null) {
					dbSchemaEntry.setSchemaVersion(schemaVersion);
				}
				
				boolean atDeployed = queryAtDeployed(schemaName, con);
				dbSchemaEntry.setAtDeployed(atDeployed);
			}
			
			con.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	private DbSchemaEntry getDbSchemaEntry(String schemaName) {
		if (bundleEntry != null) {
			DbSchemaEntry dbSchemaEntry = bundleEntry.getDbSchemaEntry();
			if (dbSchemaEntry == null) {
				dbSchemaEntry = new DbSchemaEntry();
				dbSchemaEntry.setSchemaName(schemaName);
				bundleEntry.setDbSchemaEntry(dbSchemaEntry);
			}
			return dbSchemaEntry;
		} else {
			return null;
		}
	}

	private int queryTableCount(String schemaName, Connection con) {
		return queryForInt("SELECT count(*) FROM information_schema.tables where table_schema='"+schemaName+"'", con);
	}

	private String querySchemaVersion(String schemaName, Connection con) {
		return queryForString("SELECT buildNumber FROM "+schemaName+".release_ where releaseId = 1", con);
	}

	private boolean queryAtDeployed(String schemaName, Connection con) {
		return queryForInt("SELECT count(*) FROM information_schema.tables where table_schema='"+schemaName+"' and table_name like 'ct_%'", con) > 0;
	}

	private int queryForInt(String query, Connection con) {
		int res = 0;
		
		ResultSet rs = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				res = rs.getInt(1);
			}
		} catch (Exception e) {
			// ignore
		} finally {
			tryClose(rs, stmt);
		}
		
		return res;
	}

	private String queryForString(String query, Connection con) {
		String res = null;
		
		ResultSet rs = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				res = rs.getString(1);
			}
		} catch (Exception e) {
			// ignore
		} finally {
			tryClose(rs, stmt);
		}
		
		return res;
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
}
