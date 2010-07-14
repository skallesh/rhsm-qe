package com.redhat.qe.sm.abstractions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

public class SubscriptionPool extends CandlepinAbstraction {
	
	public String subscriptionName;
	public String productId;	//public String productSku;		// productSku is replaced by productId in subscription-manager-0.68-1.el6.i686  jsefler 7/13/2010
	public String poolId;
	public Integer quantity;
	public Date endDate;
	
	public Date startDate;
	public Boolean activeSubscription;
	public Integer consumed;
	public ArrayList<ProductSubscription> associatedProductIDs;
	
	public Boolean isConsumed(){
		return (consumed > 0);
	}
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	public void addProductID(String productID){
		associatedProductIDs.add(new ProductSubscription(productID, this));
	}
	
	@Override
	public boolean equals(Object obj){
		// return ((SubscriptionPool)obj).subscriptionName.contains(this.subscriptionName);		// this is not correct jsefler 7/13/2010 

		// assumes productId is unique across all SubscriptionPools
		return ((SubscriptionPool)obj).productId.equals(this.productId);
	}
	
	public SubscriptionPool(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		subscriptionName = components[0].trim();
		endDate = this.parseDateString(components[1].trim());
		poolId = components[2].trim();
		quantity = Integer.parseInt(components[3].trim());
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}
	
	public SubscriptionPool(Date startDate,
			Date endDate,
			Boolean activeSubscription,
			Integer consumed,
			Integer quantity,
			String id,
			String productId){
		super(null);
		
		this.startDate = startDate;
		this.endDate = endDate;
		this.activeSubscription = activeSubscription;
		this.consumed = consumed;
		this.quantity = quantity;
		this.poolId = id;
		this.subscriptionName = productId;
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}
	
	public SubscriptionPool(String productId, String poolId){
		super(null);
		
		this.productId = productId;
		this.poolId = poolId;
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}

	public SubscriptionPool(HashMap<String, String> poolMap) {
		super(poolMap);
	}
	
	@Override
	public String toString() {
		
//		public Date startDate;
//		public Date endDate;
//		public Boolean activeSubscription;
//		public Integer consumed;
//		public Integer quantity;
//		public String poolId;
//		public String subscriptionName;
//		public String productId;
		
		String string = "";
		if (subscriptionName != null)	string += String.format(" %s='%s'", "subscriptionName",subscriptionName);
		if (productId != null)			string += String.format(" %s='%s'", "productId",productId);
		if (poolId != null)				string += String.format(" %s='%s'", "poolId",poolId);
		if (quantity != null)			string += String.format(" %s='%s'", "quantity",quantity);
		if (consumed != null)			string += String.format(" %s='%s'", "consumed",consumed);
		if (activeSubscription != null)	string += String.format(" %s='%s'", "activeSubscription",activeSubscription);
		if (startDate != null)			string += String.format(" %s='%s'", "startDate",startDate);
		if (endDate != null)			string += String.format(" %s='%s'", "endDate",endDate);


		return string.trim();
	}
	
	
	
	/**
	 * @param entitlements - stdout from "subscription-manager-cli list --available"
	 * @return
	 */
	static public ArrayList<HashMap<String,String>> parseAvailableSubscriptions(String entitlements) {
		/*
		# subscription-manager-cli list --available
		+-------------------------------------------+
    		Available Subscriptions
		+-------------------------------------------+
		
		Name:              	Basic RHEL Server        
		ProductId:         	MKT-simple-rhel-server-mkt
		PoolId:            	2                        
		quantity:          	10                       
		Expires:           	2011-07-01     * 		/*
		*/

		
		ArrayList<HashMap<String,String>> entitlementList = new ArrayList<HashMap<String,String>>();
		HashMap<String,String> regexes = new HashMap<String,String>();
		
//		regexes.put("poolName",		"Name:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("productSku",	"Product SKU:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("poolId",		"PoolId:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("quantity",		"quantity:\\s*([a-zA-Z0-9 ,:()]*)");
//		regexes.put("endDate",		"Expires:\\s*([a-zA-Z0-9 ,:()]*)");
		
		// SubscriptionPool abstractionField		pattern		(Note: the abstractionField must be defined in the SubscriptionPool class)
		regexes.put("subscriptionName",		"Name:\\s*(.*)");
		regexes.put("productId",			"ProductId:\\s*(.*)");
		regexes.put("poolId",				"PoolId:\\s*(.*)");
		regexes.put("quantity",				"[Qq]uantity:\\s*(.*)");	// https://bugzilla.redhat.com/show_bug.cgi?id=612730
		regexes.put("endDate",				"Expires:\\s*(.*)");
		
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, entitlements, entitlementList, field);
		}
		
		return entitlementList;
	}
}
