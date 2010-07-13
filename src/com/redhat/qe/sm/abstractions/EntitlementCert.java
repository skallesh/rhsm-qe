package com.redhat.qe.sm.abstractions;

import java.util.HashMap;

public class EntitlementCert extends CandlepinAbstraction {
	
	// Note: these public fields must match the fields in ModuleTasks.parseCerts(...)
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
}
