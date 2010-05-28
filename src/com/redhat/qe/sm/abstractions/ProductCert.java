package com.redhat.qe.sm.abstractions;

import java.util.Date;
import java.util.HashMap;

public class ProductCert extends CandlepinAbstraction{
	public String productName;
	public String status;
	public Date expires;
	public Integer subscription;
	
	public ProductCert(HashMap<String, String> productData) {
		super(productData);
	}

}
