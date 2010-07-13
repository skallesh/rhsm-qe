package com.redhat.qe.sm.abstractions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class SubscriptionPool extends CandlepinAbstraction {
	
	// Note: these public fields must match the fields in ModuleTasks.parseAvailableSubscriptions(...)
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
}
