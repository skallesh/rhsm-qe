package com.redhat.qe.sm.abstractions;

import java.util.Date;
import java.util.HashMap;

public class ProductCert extends CandlepinAbstraction {
	
	// Note: these public fields must match the fields in ModuleTasks.parseInstalledProductCerts(...)
	public String productName;
	public String status;
	public Date expires;
	public Integer subscription;
	
	public ProductCert(HashMap<String, String> productData) {
		super(productData);
	}

	@Override
	public String toString() {
		
//		public String productName;
//		public String status;
//		public Date expires;
//		public Integer subscription;
		
		String string = "";
		if (productName != null)		string += String.format(" %s='%s'", "productName",productName);
		if (subscription != null)		string += String.format(" %s='%s'", "subscription",subscription);
		if (status != null)				string += String.format(" %s='%s'", "status",status);
		if (expires != null)			string += String.format(" %s='%s'", "expires",expires);

		return string.trim();
	}
}
