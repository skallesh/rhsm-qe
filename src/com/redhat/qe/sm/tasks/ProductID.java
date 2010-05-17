package com.redhat.qe.sm.tasks;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ProductID {
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
	
	private Date parseDateString(String dateString) throws ParseException{
		DateFormat df = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy");
		return df.parse(dateString);
	}
	
	public ProductID(HashMap<String, String> productData){
		this.productId = "";
		for (String productElem: productData.keySet()){
			try {
				Field correspondingField = this.getClass().getField(productElem);
				if (correspondingField.getType().equals(Date.class))
					correspondingField.set(this, this.parseDateString(productData.get(productElem)));
				else if (correspondingField.getType().equals(Integer.class))
					correspondingField.set(this, Integer.parseInt(productData.get(productElem)));
				else if (correspondingField.getType().equals(Boolean.class))
					correspondingField.set(this, productData.get(productElem).toLowerCase().contains("true"));
				else
					correspondingField.set(this, productData.get(productElem));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
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
