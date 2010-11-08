package com.redhat.qe.sm.data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

public class InstalledProduct extends AbstractCommandLineData {
	
	// abstraction fields
	public String productName;
	public String status;
	public Calendar expires;
	public BigInteger subscription;
	public Long contractNumber;
	
	public InstalledProduct(Map<String, String> productData) {
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
		if (status != null)				string += String.format(" %s='%s'", "status",status);
		if (expires != null)			string += String.format(" %s='%s'", "expires",formatDateString(expires));
		if (subscription != null)		string += String.format(" %s='%s'", "subscription",subscription);
		if (contractNumber != null)		string += String.format(" %s='%s'", "contractNumber",contractNumber);

		return string.trim();
	}
	
	
	/**
	 * @param stdoutListingOfProductCerts - stdout from "subscription-manager-cli list"
	 * @return
	 */
	static public List<InstalledProduct> parse(String stdoutListingOfProductCerts) {
		/*
		# subscription-manager-cli list
		+-------------------------------------------+
		    Installed Product Status
		+-------------------------------------------+

		ProductName:        	Shared Storage (GFS)     
		Status:             	Not Installed            
		Expires:            	2011-07-01               
		Subscription:       	17                       
		ContractNumber:        	0   
		*/
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("productName",			"ProductName:\\s*(.*)");
		regexes.put("status",				"Status:\\s*(.*)");
		regexes.put("expires",				"Expires:\\s*(.*)");
		regexes.put("subscription",			"Subscription:\\s*(.*)");
		regexes.put("contractNumber",		"ContractNumber:\\s*(.*)");
		
		List<Map<String,String>> productCertList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutListingOfProductCerts, productCertList, field);
		}
		
		List<InstalledProduct> productCerts = new ArrayList<InstalledProduct>();
		for(Map<String,String> prodCertMap : productCertList)
			productCerts.add(new InstalledProduct(prodCertMap));
		return productCerts;
	}
}
