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
		if (dbParamsSpecified() && dbConnectionAllowed()) {
			readMySqlSchemaData();
		}
	}

	private boolean dbParamsSpecified() {
		return 
				bundleEntry.getDbUrl() != null &&
				bundleEntry.getDbDriverClass() != null &&
				bundleEntry.getDbUsername() != null &&
				bundleEntry.getDbPassword() != null;
	}

	private boolean dbConnectionAllowed() {
		return bundleEntry.getDbDriverClass().equals("com.mysql.jdbc.Driver") &&
				bundleEntry.getDbUrl().startsWith("jdbc:mysql://localhost");
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
				if (atDeployed) {
					dbSchemaEntry.addDeployedApp("AudienceTargeting");
				}
				
				boolean growDeployed = queryGrowDeployed(schemaName, con);
				if (growDeployed) {
					dbSchemaEntry.addDeployedApp("GROW");
				}
				
				Double sizeInMb = querySizeInMb(schemaName, con);
				if (sizeInMb != null) {
					dbSchemaEntry.setSizeInMb(sizeInMb);
				}
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
	
	private boolean queryGrowDeployed(String schemaName, Connection con) {
		return queryForInt("SELECT count(*) FROM information_schema.tables where table_schema='"+schemaName+"' and table_name in (\r\n" + 
				"'analysis_analysisentry',\r\n" + 
				"'analysis_analysisuser',\r\n" + 
				"'candidate_candidateentry',\r\n" + 
				"'decision_decisionentry',\r\n" + 
				"'task_candidatemaintenance',\r\n" + 
				"'task_taskentry'\r\n" + 
				")", con) >= 3;
	}

	private Double querySizeInMb(String schemaName, Connection con) {
		Double res = null;

		try {
			String sizeString = queryForString("SELECT\r\n" +
					"ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) as DB_Size_in_MB\r\n" + 
					"FROM information_schema.tables\r\n" + 
					"WHERE table_schema = '"+schemaName+"'\r\n" + 
					"GROUP BY table_schema", con);
			res = Double.parseDouble(sizeString);
		} catch (Exception e) {
			// ignore
		}
		
		return res;
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
