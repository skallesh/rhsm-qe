package com.redhat.qe.sm.abstractions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Pool extends CandlepinAbstraction {
	public Date startDate;
	public Date endDate;
	public Boolean activeSubscription;
	public Integer consumed;
	public Integer quantity;
	public String poolId;
	public String poolName;
	public String productSku;
	
	public ArrayList<ProductID> associatedProductIDs;
	
	public Boolean isConsumed(){
		return (consumed > 0);
	}
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	public void addProductID(String productID){
		associatedProductIDs.add(new ProductID(productID, this));
	}
	
	@Override
	public boolean equals(Object obj){
		return ((Pool)obj).poolName.contains(this.poolName);
	}
	
	public Pool(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		poolName = components[0].trim();
		endDate = this.parseDateString(components[1].trim());
		poolId = components[2].trim();
		quantity = Integer.parseInt(components[3].trim());
		associatedProductIDs = new ArrayList<ProductID>();
	}
	
	public Pool(Date startDate,
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
		this.poolName = productId;
		associatedProductIDs = new ArrayList<ProductID>();
	}
	
	public Pool(String productSku, String poolId){
		super(null);
		
		this.productSku = productSku;
		this.poolId = poolId;
		associatedProductIDs = new ArrayList<ProductID>();
	}

	public Pool(HashMap<String, String> poolMap) {
		super(poolMap);
	}
}
