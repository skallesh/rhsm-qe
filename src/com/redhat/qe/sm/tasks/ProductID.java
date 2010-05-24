package com.redhat.qe.sm.tasks;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ProductID extends CandlepinAbstraction {
	public String productId;
	public Integer serialNumber;
	public Pool fromPool;
	public Boolean isActive;
	public Date startDate;
	public Date endDate;
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	@Override
	public boolean equals(Object obj){
		return ((ProductID)obj).productId.contains(this.productId);
	}
	
	public ProductID(HashMap<String, String> productData){
		super(productData);
		
		if (this.productId == null)
			this.productId = "";
	}
	
	public ProductID(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		productId = components[0].trim();
		isActive = components[1].toLowerCase().contains("true");
		endDate = this.parseDateString(components[2].trim());
	}
	
	public ProductID(String productID, Pool fromPool){
		super(null);
		
		this.productId = productID;
		this.fromPool = fromPool;
	}
}
