package com.redhat.qe.sm.data;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

public class ProductSubscription extends AbstractCommandLineData {
	
	// abstraction fields
	public String productName;
	public Long serialNumber;	// Integer serialNumber; // serialNumber=290624661330496 is out of range for an Integer
	public Integer contractNumber;
	public Boolean isActive;
	public Calendar startDate;
	public Calendar endDate;
	
	public SubscriptionPool fromSubscriptionPool;
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	@Override
	public boolean equals(Object obj){
		//return ((ProductSubscription)obj).productName.contains(this.productName);	// I think we need to compare more attributes than using .contains(...) on the productName jsefler 7/13/2010

		// assume the combination of productName and serialNumber is unique across all ProductSubscriptions
		return ((ProductSubscription)obj).productName.equals(this.productName) && ((ProductSubscription)obj).serialNumber.equals(this.serialNumber);
	}
	
	public ProductSubscription(Map<String, String> productData){
		super(productData);
		
		if (this.productName == null)
			this.productName = "";
	}
	
	public ProductSubscription(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		productName = components[0].trim();
		isActive = components[1].toLowerCase().contains("true");
		endDate = /*this.*/parseDateString(components[2].trim());
	}
	
	public ProductSubscription(String productName, SubscriptionPool fromPool){
		super(null);
		
		this.productName = productName;
		this.fromSubscriptionPool = fromPool;
	}
	
	@Override
	public String toString() {
		
//		public String productName;
//		public Integer serialNumber;
//		public Integer contractNumber;
//		public Pool fromPool;
//		public Boolean isActive;
//		public Date startDate;
//		public Date endDate;
		
		String string = "";
		if (productName != null)	string += String.format(" %s='%s'", "productName",productName);
		if (serialNumber != null)	string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (contractNumber != null)	string += String.format(" %s='%s'", "contractNumber",contractNumber);
		if (isActive != null)		string += String.format(" %s='%s'", "isActive",isActive);
		if (startDate != null)		string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
		if (endDate != null)		string += String.format(" %s='%s'", "endDate",formatDateString(endDate));
		if (fromSubscriptionPool != null)		string += String.format(" %s='%s'", "fromPool",fromSubscriptionPool);

		return string.trim();
	}
	
	
	/**
	 * @param stdoutListOfConsumedProducts - stdout from "subscription-manager-cli list --consumed"
	 * @return
	 */
	static public List<ProductSubscription> parse(String stdoutListOfConsumedProducts) {
		/*
		# subscription-manager-cli list --consumed
		+-------------------------------------------+
    		Consumed Product Subscriptions
		+-------------------------------------------+
		
		Name:               	High availability (cluster suite)
		ContractNumber:       	0                        
		SerialNumber:       	17                       
		Active:             	True                     
		Begins:             	2010-07-01               
		Expires:            	2011-07-01   
		*/

		Map<String,String> regexes = new HashMap<String,String>();
		
//		regexes.put("productId",	"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("serialNumber",	"SerialNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("orderNumber",	"OrderNumber:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("isActive",		"Active:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("startDate",	"Begins:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");

		// abstraction field				regex pattern (with a capturing group)
		regexes.put("productName",			"Name:\\s*(.*)");
		regexes.put("serialNumber",			"SerialNumber:\\s*(.*)");
		regexes.put("contractNumber",		"ContractNumber:\\s*(.*)");
		regexes.put("isActive",				"Active:\\s*(.*)");
		regexes.put("startDate",			"Begins:\\s*(.*)");
		regexes.put("endDate",				"Expires:\\s*(.*)");
		
		List<Map<String,String>> productList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutListOfConsumedProducts, productList, field);
		}
		
		List<ProductSubscription> productSubscriptions = new ArrayList<ProductSubscription>();
		for(Map<String,String> productMap : productList)
			productSubscriptions.add(new ProductSubscription(productMap));
		return productSubscriptions;
	}
}
