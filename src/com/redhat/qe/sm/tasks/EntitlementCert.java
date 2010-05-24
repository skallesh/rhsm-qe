package com.redhat.qe.sm.tasks;

import java.util.HashMap;

public class EntitlementCert extends CandlepinAbstraction {
	public String key;
	public String label;
	public String name;
	public String phys_ent;
	public String flex_ent;
	public String vendor_id;
	public String download_url;
	public String enabled;
	
	public EntitlementCert(String key, HashMap<String, String> certData){
		super(certData);
		this.key = key;
	}
}
