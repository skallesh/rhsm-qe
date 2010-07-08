package com.redhat.qe.sm.abstractions;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import com.redhat.qe.auto.testng.TestScript;


public abstract class CandlepinAbstraction {
	//public final String dateFormat = "EEE MMM d HH:mm:ss yyyy";
	public final String dateFormat = "yyyy-MM-dd";
	protected static Logger log = Logger.getLogger(CandlepinAbstraction.class.getName());
	
	@Override
	public boolean equals(Object obj){
		CandlepinAbstraction certObj = (CandlepinAbstraction)obj;
		boolean matched = true;
		for(Field certField:certObj.getClass().getDeclaredFields()){
			try {
				Field correspondingField = this.getClass().getField(certField.getName());
				matched = correspondingField.get(this).equals(certField.get(certObj));
			} catch (Exception e) {
				return false;
			}
		}
		return matched;
	}
	
	protected Date parseDateString(String dateString){
		try{
			DateFormat df = new SimpleDateFormat(this.dateFormat);
			return df.parse(dateString);
		}
		catch (Exception e){
			log.warning("Unparseable date string: "+dateString+"\nException: "+e.getMessage());
			return null;
		}
	}
	
	public CandlepinAbstraction(HashMap<String, String> productData){
		if (productData == null)
			return;
		
		for (String productElem: productData.keySet()){
			Field correspondingField = null;
			try {
				correspondingField = this.getClass().getField(productElem);
				if (correspondingField.getType().equals(Date.class))
					correspondingField.set(this, this.parseDateString(productData.get(productElem)));
				else if (correspondingField.getType().equals(Integer.class))
					correspondingField.set(this, Integer.parseInt(productData.get(productElem)));
				else if (correspondingField.getType().equals(Boolean.class))
					correspondingField.set(this, productData.get(productElem).toLowerCase().contains("true"));
				else
					correspondingField.set(this, productData.get(productElem));
			} catch (Exception e){
				log.warning("Exception caught while creating Candlepin abstraction: " + e.getMessage());
				if (correspondingField != null)
					try {
						correspondingField.set(this, null);
					} catch (Exception x){
						log.warning("and we can't even set it to null.  Whaaaaaa?");
					}
				for (StackTraceElement ste:e.getStackTrace()){
					log.warning(ste.toString());
				}
			}
		}
	}
}
