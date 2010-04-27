package com.redhat.qe.sm.tasks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProductID {
	public String productId;
	public Pool fromPool;
	public boolean isActive;
	public Date endDate;
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	private Date parseDateString(String dateString) throws ParseException{
		DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
		return df.parse(dateString);
	}
	
	public ProductID(String subscriptionLine) throws ParseException{
		String[] components = subscriptionLine.split("\\t");
		
		productId = components[0].trim();
		isActive = components[1].toLowerCase().contains("true");
		endDate = this.parseDateString(components[2].trim());
	}
	
	public ProductID(String productID, Pool fromPool){
		this.productId = productID;
		this.fromPool = fromPool;
	}
}
