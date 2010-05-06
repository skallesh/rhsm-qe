package com.redhat.qe.sm.tasks;

import java.lang.reflect.Field;
import java.util.HashMap;

public class EntitlementCert {
	public String key;
	public String label;
	public String name;
	public String phys_ent;
	public String flex_ent;
	public String vendor_id;
	public String download_url;
	public String enabled;
	
	@Override
	public boolean equals(Object obj){
		EntitlementCert certObj = (EntitlementCert)obj;
		boolean matched = true;
		for(Field certField:certObj.getClass().getDeclaredFields()){
			try {
				Field correspondingField = this.getClass().getField(certField.getName());
				matched = correspondingField.get(this).equals(certField.get(certObj));
			} catch (Exception e) {
				return false;
			}
		}
		return matched;
	}
	
	public EntitlementCert(String key, HashMap<String, String> certData){
		this.key = key;
		for (String certElem: certData.keySet()){
			try {
				Field correspondingField = this.getClass().getField(certElem);
				correspondingField.set(this, certData.get(certElem));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
