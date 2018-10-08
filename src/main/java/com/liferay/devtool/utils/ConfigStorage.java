package com.liferay.devtool.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.liferay.devtool.DevToolContext;

public class ConfigStorage {
	private String name;
	private String comment;
	private Map<String, String> singleValues = new HashMap<>();
	private Map<String, List<String>> listValues = new HashMap<>();
	private boolean xml = false;
	private DevToolContext context;

	public ConfigStorage() {
		super();
	}

	public ConfigStorage(String name, String comment) {
		super();
		this.name = name;
		this.comment = comment;
	}

	public boolean isXml() {
		return xml;
	}

	public void setXml(boolean xml) {
		this.xml = xml;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void addValue(String key, String value) {
		singleValues.put(key, value);
	}

	public void setContext(DevToolContext context) {
		this.context = context;
	}

	public void addToList(String key, String value) {
		if (!listValues.containsKey(key)) {
			listValues.put(key, new ArrayList<String>());
		}

		listValues.get(key).add(value);
	}
	
	public void save() {
	    try {
	        Properties props = new Properties();
	        
	        saveProperties(props);
	        
	        File f = new File(getFileName());
        	context.getLogger().log("saving \""+f.getAbsolutePath()+"\"");
	        OutputStream out = new FileOutputStream(f);
	        if (xml) {
	        	props.storeToXML(out, comment);
	        } else {
	        	props.store(out, comment);
	        }
	    } catch (Exception e ) {
	    	context.getLogger().log(e);
	    }
	}

	private void saveProperties(Properties props) {
		for (String key : singleValues.keySet()) {
			props.setProperty(key, singleValues.get(key));
		}
		
		for (String key : listValues.keySet()) {
			List<String> values = listValues.get(key);
			
		    for (int i=0; i<values.size(); ++i) {
		    	props.setProperty(key+"."+i, values.get(i));
		    }
		}
	}

	public boolean load() {
		boolean exists = false;
	    Properties props = new Properties();
	    InputStream is = null;
	 
	    try {
	        File f = new File(getFileName());
	        context.getLogger().log("reading from \""+f.getAbsolutePath()+"\"");
	        is = new FileInputStream(f);
	    } catch ( Exception e ) {
	    	context.getLogger().log(e);
	    	is = null;
	    }
	 
	    try {
	        if (is == null) {
	            is = getClass().getResourceAsStream(getFileName());
	        }
	 
	        if (xml) {
	        	props.loadFromXML(is);
	        } else {
	        	props.load(is);
	        }
	        
	        loadFromProperties(props);
	        exists = true;
	    } catch ( Exception e ) {
	    	context.getLogger().log(e);
	    }
	    
	    return exists;
	}

	private void loadFromProperties(Properties props) {
		Map<String,Integer> maxKeys = new HashMap<>();
		
		for (Object key : props.keySet()) {
        	String keyStr = (String)key;
    		Integer keyId = getKeyId(keyStr);
    		
    		if (keyId != null) {
    			String keyPart = getKeyPart(keyStr);
        		//System.out.println("key: "+keyStr+", keyPart="+keyPart+", id="+keyId);
    			if (maxKeys.containsKey(keyPart)) {
    				if (keyId.intValue() > maxKeys.get(keyPart).intValue()) {
        				maxKeys.put(keyPart, keyId);
    				}
    			} else {
    				maxKeys.put(keyPart, keyId);
    			}
        	} else {
        		singleValues.put(keyStr, props.getProperty(keyStr));
        	}
        }
		
		for (String keyPart : maxKeys.keySet()) {
			int max = maxKeys.get(keyPart).intValue();
	        for (int i=0; i <= max; ++i) {
	        	String key = keyPart+"."+i;
	        	
	        	String value = props.getProperty(key);
	        	if (value != null) {
		        	//System.out.println("adding "+key+" = "+value);
		        	addToList(keyPart, value);
	        	}
	        }
		}
	}

	private String getFileName() {
		if (xml) {
			return name+"_properties.xml";
		} else {
			return name+".properties";
		}
	}

	private Integer getKeyId(String key) {
		Integer res = null;
		
		if (key == null || key.trim().length() == 0) {
			return res;
		}
		
		int lastPos = key.lastIndexOf(".");
		
		if (lastPos > -1) {
			String keyIdStr = key.substring(lastPos+1);
		
			try {
				res = Integer.valueOf(keyIdStr);
			} catch (Exception ex) {
				// do nothing
			}
		}
		
		return res;
	}

	private String getKeyPart(String key) {
		if (key == null || key.trim().length() == 0) {
			return null;
		}
		
		int lastPos = key.lastIndexOf(".");
		
		if (lastPos > -1) {
			return key.substring(0, lastPos);
		}
		
		return key;
	}

	public List<String> getList(String key) {
		return listValues.get(key);
	}

	public String getValue(String key) {
		return singleValues.get(key);
	}
}
