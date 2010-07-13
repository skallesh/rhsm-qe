package com.redhat.qe.sm.abstractions;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

public class ProductSubscription extends CandlepinAbstraction {
	
	// Note: these public fields must match the fields in ModuleTasks.parseConsumedProducts(...)
	public String productName;
	public Integer serialNumber;
	public Integer contractNumber;
	public Boolean isActive;
	public Date startDate;
	public Date endDate;
	
	public SubscriptionPool fromPool;
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	@Override
	public boolean equals(Object obj){
		//return ((ProductSubscription)obj).productName.contains(this.productName);	// I think we need to compare more attributes than using contains on the productName jsefler 7/13/2010

		// assume the combination of productName and serialNumber is unique across all ProductSubscriptions
		return ((ProductSubscription)obj).productName.equals(this.productName) && ((ProductSubscription)obj).serialNumber.equals(this.serialNumber);
	}
	
	public ProductSubscription(HashMap<String, String> productData){
		super(productData);
		
		if (this.productName == null)
			this.productName = "";
	}
	
	public ProductSubscription(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		productName = components[0].trim();
		isActive = components[1].toLowerCase().contains("true");
		endDate = this.parseDateString(components[2].trim());
	}
	
	public ProductSubscription(String productName, SubscriptionPool fromPool){
		super(null);
		
		this.productName = productName;
		this.fromPool = fromPool;
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
		if (startDate != null)		string += String.format(" %s='%s'", "startDate",startDate);
		if (endDate != null)		string += String.format(" %s='%s'", "endDate",endDate);
		if (fromPool != null)		string += String.format(" %s='%s'", "fromPool",fromPool);

		return string.trim();
	}
}
