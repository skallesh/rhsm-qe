package com.redhat.qe.sm.abstractions;

import java.util.HashMap;
import java.util.regex.Pattern;

public class EntitlementCert extends CandlepinAbstraction {
	
	public String name;
	public String label;
	public String phys_ent;
	public String flex_ent;
	public String vendor_id;
	public String download_url;
	public String enabled;
	
	public String key;
	
	public EntitlementCert(String key, HashMap<String, String> certData){
		super(certData);
		this.key = key;
	}
	
	
	@Override
	public String toString() {
		
//		public String key;
//		public String label;
//		public String name;
//		public String phys_ent;
//		public String flex_ent;
//		public String vendor_id;
//		public String download_url;
//		public String enabled;
		
		String string = "";
		if (key != null)			string += String.format(" %s='%s'", "key",key);
		if (label != null)			string += String.format(" %s='%s'", "label",label);
		if (name != null)			string += String.format(" %s='%s'", "name",name);
		if (phys_ent != null)		string += String.format(" %s='%s'", "phys_ent",phys_ent);
		if (flex_ent != null)		string += String.format(" %s='%s'", "flex_ent",flex_ent);
		if (vendor_id != null)		string += String.format(" %s='%s'", "vendor_id",vendor_id);
		if (download_url != null)	string += String.format(" %s='%s'", "download_url",download_url);
		if (enabled != null)		string += String.format(" %s='%s'", "enabled",enabled);
		
		return string.trim();
	}
	
	
	
	static public HashMap<String, HashMap<String,String>> parseCerts(String certificates) {
		HashMap<String, HashMap<String,String>> productMap = new HashMap<String, HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
		// EntitlementCert abstractionField	pattern		(Note: the abstractionField must be defined in the EntitlementCert class)
		regexes.put("name",					"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.1:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("label",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.2:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("phys_ent",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.3:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("flex_ent",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.4:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("vendor_id",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.5:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("download_url",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.6:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("enabled",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.8:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, certificates, productMap, field);
		}
		
		return productMap;
	}
}
