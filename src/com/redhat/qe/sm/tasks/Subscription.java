package com.redhat.qe.sm.tasks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Subscription {
	public Date startDate;
	public Date endDate;
	public Boolean activeSubscription;
	public Integer consumed;
	public Integer quantity;
	public String id;
	public String productId;
	
	private Date parseDateString(String dateString) throws ParseException{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return df.parse(dateString);
	}
	
	public Boolean isConsumed(){
		return (consumed > 0);
	}
	
	public Boolean isExpired(){
		return endDate.after(new Date());
	}
	
	@Override
	public boolean equals(Object obj){
		return ((Subscription)obj).productId == this.productId;
	}
	
	public Subscription(String subscriptionLine) throws ParseException{
		String[] components = subscriptionLine.split("\\t");
		
		productId = components[0].trim();
		endDate = this.parseDateString(components[1].trim());
		id = components[2].trim();
		quantity = Integer.parseInt(components[3].trim());
	}
	
	public Subscription(Date startDate,
			Date endDate,
			Boolean activeSubscription,
			Integer consumed,
			Integer quantity,
			String id,
			String productId){
		this.startDate = startDate;
		this.endDate = endDate;
		this.activeSubscription = activeSubscription;
		this.consumed = consumed;
		this.quantity = quantity;
		this.id = id;
		this.productId = productId;
	}
}
