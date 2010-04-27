package com.redhat.qe.sm.tasks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Pool {
	public Date startDate;
	public Date endDate;
	public Boolean activeSubscription;
	public Integer consumed;
	public Integer quantity;
	public String poolId;
	public String productId;
	
	private Date parseDateString(String dateString) throws ParseException{
		//DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
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
		return ((Pool)obj).productId.contains(this.productId);
	}
	
	public Pool(String subscriptionLine) throws ParseException{
		String[] components = subscriptionLine.split("\\t");
		
		productId = components[0].trim();
		endDate = this.parseDateString(components[1].trim());
		poolId = components[2].trim();
		quantity = Integer.parseInt(components[3].trim());
	}
	
	public Pool(Date startDate,
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
		this.poolId = id;
		this.productId = productId;
	}
}
