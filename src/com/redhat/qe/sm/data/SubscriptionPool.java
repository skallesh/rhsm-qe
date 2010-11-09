package com.redhat.qe.sm.data;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

public class SubscriptionPool extends AbstractCommandLineData {
	
	// abstraction fields
	public String subscriptionName;
	public String productId;	// public String productSku;	// productSku was replaced by productId in subscription-manager-0.68-1.el6.i686  jsefler 7/13/2010
	public String poolId;
	public String quantity;	// public Integer quantity;	// can be "unlimited"
	public Calendar endDate;
	
	public Calendar startDate;
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

//		// assumes productId is unique across all SubscriptionPools
//		return ((SubscriptionPool)obj).productId.equals(this.productId);
		
		// assumes poolId is unique across all SubscriptionPools
		return ((SubscriptionPool)obj).poolId.equals(this.poolId);
	}
	
	public SubscriptionPool(String subscriptionLine) throws ParseException{
		super(null);
		
		String[] components = subscriptionLine.split("\\t");
		
		subscriptionName = components[0].trim();
		endDate = /*this.*/parseDateString(components[1].trim());
		poolId = components[2].trim();
		quantity = components[3].trim();	// Integer.parseInt(components[3].trim());
		associatedProductIDs = new ArrayList<ProductSubscription>();
	}
	
	public SubscriptionPool(Calendar startDate,
			Calendar endDate,
			Boolean activeSubscription,
			Integer consumed,
			String quantity,	//Integer quantity,
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

	public SubscriptionPool(Map<String, String> poolMap) {
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
		if (startDate != null)			string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
		if (endDate != null)			string += String.format(" %s='%s'", "endDate",formatDateString(endDate));


		return string.trim();
	}
	
	
	
	/**
	 * @param stdoutListingOfAvailableSubscriptions - stdout from "subscription-manager-cli list --available"
	 * @return
	 */
	static public List<SubscriptionPool> parse(String stdoutListingOfAvailableSubscriptions) {
		/*
		# subscription-manager-cli list --available
		+-------------------------------------------+
    		Available Subscriptions
		+-------------------------------------------+
		
		Name:              	Basic RHEL Server        
		ProductId:         	MKT-simple-rhel-server-mkt
		PoolId:            	2                        
		quantity:          	10                       
		Expires:           	2011-07-01     
		
		ProductName:       	Scalable File System (1-2 sockets)
		ProductId:         	RH1414165                
		PoolId:            	8a8aa80d2be34ec0012be505f5a20600
		Quantity:          	1                        
		Expires:           	2011-10-24  
		
		ProductName:       	Red Hat Enterprise Linux Entitlement Alpha (1-2 Sockets)
                        	(Unlimited Virtualization)
		ProductId:         	RH1050830                
		PoolId:            	8a8aa80d2be34ec0012be505f34d05f9
		Quantity:          	1                        
		Expires:           	2011-01-24   
		*/

		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field					regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("subscriptionName",			"ProductName:(.*)");
		regexes.put("productId",				"ProductId:(.*)");
		regexes.put("poolId",					"PoolId:(.*)");
		regexes.put("quantity",					"Quantity:(.*)");	// https://bugzilla.redhat.com/show_bug.cgi?id=612730
		regexes.put("endDate",					"Expires:(.*)");
		
		List<Map<String,String>> entitlementList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutListingOfAvailableSubscriptions, entitlementList, field);
		}
		
		List<SubscriptionPool> subscriptionPools = new ArrayList<SubscriptionPool>();
		for(Map<String,String> poolMap : entitlementList)
			subscriptionPools.add(new SubscriptionPool(poolMap));
		return subscriptionPools;
	}

// FIXME DELETEME
//	/**
//	 * @param certificates - stdout from "find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
//	 * @return - a map of serialNumber to SubscriptionPool pairs.  The SubscriptionPool is the source from where the serialNumber came from.
//	 */
//	static public Map<Long, SubscriptionPool> parseCerts(String certificates) {
//		/* # openssl x509 -in /etc/pki/entitlement/product/314.pem -noout -text
//		Certificate:
//		    Data:
//		        Version: 3 (0x2)
//		        Serial Number: 314 (0x13a)
//		        Signature Algorithm: sha1WithRSAEncryption
//		        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
//		        Validity
//		            Not Before: Jul  2 00:00:00 2010 GMT
//		            Not After : Jul  2 00:00:00 2011 GMT
//		        Subject: CN=admin/UID=28b1c0d1-f621-4a05-b94e-6b9f53230a3a
//		        Subject Public Key Info:
//		            Public Key Algorithm: rsaEncryption
//		                Public-Key: (2048 bit)
//		                Modulus:
//		                    00:b8:4f:1f:23:98:08:a0:3f:82:c5:fe:af:13:38:
//		                    61:ad:42:1a:ee:64:89:e9:84:f6:b0:65:14:8c:8d:
//		                    48:fb:f6:9b:6c:d1:96:b8:e3:e4:85:39:00:59:b1:
//		                    26:6e:7d:ff:c5:ac:96:70:ab:7e:8f:7d:05:b7:b6:
//		                    f0:49:76:b3:65:56:29:9c:9c:5f:8c:d5:4b:32:73:
//		                    d4:05:bf:c5:55:4b:68:d8:1a:8a:0c:61:0e:3a:1c:
//		                    a2:fd:d4:c8:db:62:b5:a4:58:47:7b:13:e5:fd:27:
//		                    50:76:58:f1:0c:1b:58:57:73:33:fd:c9:d1:f9:32:
//		                    a0:0f:c7:f8:23:77:6c:85:5e:c1:92:f6:3a:81:65:
//		                    f8:8d:ab:ff:ed:d8:77:d7:a3:d2:ea:56:c6:0c:bb:
//		                    2a:3b:68:f3:be:18:9f:70:ed:97:01:36:4d:d6:8e:
//		                    e7:0f:cf:d8:f9:d0:70:60:07:d0:52:c8:a5:3b:7d:
//		                    d8:ca:54:1b:df:07:53:49:12:31:3f:0c:1b:57:c0:
//		                    ec:f0:28:eb:78:d2:23:f3:02:a0:35:51:c7:17:f8:
//		                    e8:66:6a:76:95:22:4b:b7:bb:49:df:dc:f0:82:5e:
//		                    8d:39:c7:8b:68:51:1a:bf:4e:5c:d5:75:3a:f4:12:
//		                    d7:57:01:be:08:af:fb:88:24:0c:b6:b4:0b:61:58:
//		                    6e:2d
//		                Exponent: 65537 (0x10001)
//		        X509v3 extensions:
//		            Netscape Cert Type: 
//		                SSL Client, S/MIME
//		            X509v3 Key Usage: 
//		                Digital Signature, Key Encipherment, Data Encipherment
//		            X509v3 Authority Key Identifier: 
//		                keyid:10:8A:47:5F:A1:97:B7:2E:F1:D0:0F:8B:D7:D8:39:83:24:D5:38:6C
//		                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
//		                serial:96:06:43:DE:F4:D5:C7:F8
//
//		            X509v3 Subject Key Identifier: 
//		                65:8D:5D:F0:0B:80:F3:66:5E:F4:27:24:46:F3:59:C6:D8:4F:99:87
//		            X509v3 Extended Key Usage: 
//		                TLS Web Client Authentication
//		            1.3.6.1.4.1.2312.9.1.37065.1: 
//		                .!High availability (cluster suite)
//		            1.3.6.1.4.1.2312.9.1.37065.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37065.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37065.4: 
//		                ..1.0
//		            1.3.6.1.4.1.2312.9.2.1.1: 
//		                ..yum
//		            1.3.6.1.4.1.2312.9.2.1.1.1: 
//		                ..always-enabled-content
//		            1.3.6.1.4.1.2312.9.2.1.1.2: 
//		                ..always-enabled-content
//		            1.3.6.1.4.1.2312.9.2.1.1.5: 
//		                ..test-vendor
//		            1.3.6.1.4.1.2312.9.2.1.1.6: 
//		                ../foo/path/always
//		            1.3.6.1.4.1.2312.9.2.1.1.7: 
//		                ../foo/path/always/gpg
//		            1.3.6.1.4.1.2312.9.2.1.1.4: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.1.1.3: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.1.1.8: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.0.1: 
//		                ..yum
//		            1.3.6.1.4.1.2312.9.2.0.1.1: 
//		                ..never-enabled-content
//		            1.3.6.1.4.1.2312.9.2.0.1.2: 
//		                ..never-enabled-content
//		            1.3.6.1.4.1.2312.9.2.0.1.5: 
//		                ..test-vendor
//		            1.3.6.1.4.1.2312.9.2.0.1.6: 
//		                ../foo/path/never
//		            1.3.6.1.4.1.2312.9.2.0.1.7: 
//		                ../foo/path/never/gpg
//		            1.3.6.1.4.1.2312.9.2.0.1.4: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.0.1.3: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.0.1.8: 
//		                ..1
//		            1.3.6.1.4.1.2312.9.1.37060.1: 
//		                ..RHEL for Physical Servers SVC
//		            1.3.6.1.4.1.2312.9.1.37060.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37060.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37060.4: 
//		                ..6.1
//		            1.3.6.1.4.1.2312.9.2.1111.1: 
//		                ..yum
//		            1.3.6.1.4.1.2312.9.2.1111.1.1: 
//		                ..content
//		            1.3.6.1.4.1.2312.9.2.1111.1.2: 
//		content-label   .
//		            1.3.6.1.4.1.2312.9.2.1111.1.5: 
//		                ..test-vendor
//		            1.3.6.1.4.1.2312.9.2.1111.1.6: 
//		                ../foo/path
//		            1.3.6.1.4.1.2312.9.2.1111.1.7: 
//		                ../foo/path/gpg/
//		            1.3.6.1.4.1.2312.9.2.1111.1.4: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.1111.1.3: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.2.1111.1.8: 
//		                ..0
//		            1.3.6.1.4.1.2312.9.1.37070.1: 
//		                ..Load Balancing
//		            1.3.6.1.4.1.2312.9.1.37070.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37070.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37070.4: 
//		                ..1.0
//		            1.3.6.1.4.1.2312.9.1.37068.1: 
//		                ..Large File Support (XFS)
//		            1.3.6.1.4.1.2312.9.1.37068.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37068.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37068.4: 
//		                ..1.0
//		            1.3.6.1.4.1.2312.9.1.37067.1: 
//		                ..Shared Storage (GFS)
//		            1.3.6.1.4.1.2312.9.1.37067.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37067.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37067.4: 
//		                ..1.0
//		            1.3.6.1.4.1.2312.9.1.37069.1: 
//		                .<Smart Management (RHN Management & Provisioing & Monitoring)
//		            1.3.6.1.4.1.2312.9.1.37069.2: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37069.3: 
//		                ..ALL
//		            1.3.6.1.4.1.2312.9.1.37069.4: 
//		                ..1.0
//		            1.3.6.1.4.1.2312.9.4.1: 
//		                ..MKT-rhel-physical-2-socket
//		            1.3.6.1.4.1.2312.9.4.2: 
//		                ..4
//		            1.3.6.1.4.1.2312.9.4.5: 
//		                ..10
//		            1.3.6.1.4.1.2312.9.4.6: 
//		                ..2010-07-01 20:00:00.0
//		            1.3.6.1.4.1.2312.9.4.7: 
//		                ..2011-07-01 20:00:00.0
//		            1.3.6.1.4.1.2312.9.4.12: 
//		                ..3
//		            1.3.6.1.4.1.2312.9.4.13: 
//		                ..1
//		            1.3.6.1.4.1.2312.9.5.1: 
//		                .$28b1c0d1-f621-4a05-b94e-6b9f53230a3a
//		    Signature Algorithm: sha1WithRSAEncryption
//		        5f:24:18:39:f6:02:d6:35:c3:bd:5b:79:c0:6a:7a:d2:c6:5d:
//		        be:20:02:c2:e1:da:2f:21:fd:bd:e5:1e:44:15:c9:5a:dd:44:
//		        76:4f:02:51:3c:25:ad:d7:93:ed:65:6f:f9:46:19:ae:71:b6:
//		        63:39:b4:52:f1:d8:a1:ca:5f:a1:d7:36:4b:69:52:7e:55:77:
//		        0f:87:9f:68:53:81:77:32:49:b7:ac:e2:9c:b2:ed:6b:31:f6:
//		        52:60:65:11:0e:ac:ef:f8:10:fc:a8:a6:75:20:31:57:06:9c:
//		        06:c4:a2:65:05:81:8c:d7:5b:e3:9f:4f:1f:6c:9b:3c:85:e5:
//		        30:28
//		*/
//		/* have also seen:
//        1.3.6.1.4.1.2312.9.4.1: 
//            .#MKT-rhel-physical-2-sockets-premium
//		*/
//		
//		/* the serial number has changed to:
//        Serial Number:
//            28:18:d4:64:0d:da:e0
//		 */
//		
//		/* poolIds have changed to:
//        1.3.6.1.4.1.2312.9.4.2: 
//            . ff8080812b7b6b08012b7b6bcb3a007a
//		*/
//		
//		Map<String,String> regexes = new HashMap<String,String>();
//		
//		// abstraction field				regex pattern (with a capturing group)
//		/* https://docspace.corp.redhat.com/docs/DOC-30244 - appears to be out of date - jsefler 7/15/2010
//		1.3.6.1.4.1.2312.9.4 (Order Namespace)
//		  1.3.6.1.4.1.2312.9.4.1 (Name): Red Hat Enterprise Linux Server
//		  1.3.6.1.4.1.2312.9.4.2 (Order Number) : 12345
//		  1.3.6.1.4.1.2312.9.4.3 (SKU) : MCT0982
//		  1.3.6.1.4.1.2312.9.4.4 (Subscription Number) : abcd-ef12-1234-5678
//		  1.3.6.1.4.1.2312.9.4.5 (Quantity) : 100
//		  1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) : 1/1/2010
//		  1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) : 12/31/2011
//		  1.3.6.1.4.1.2312.9.4.8 (Subtype) : Supplementary
//		  1.3.6.1.4.1.2312.9.4.9 (Virtualization Limit) : 4
//		  1.3.6.1.4.1.2312.9.4.10 (Socket Limit) : None
//		  1.3.6.1.4.1.2312.9.4.11 (Product Option Code) : 98
//		  1.3.6.1.4.1.2312.9.4.12 (Contract Number): 152341643
//		  1.3.6.1.4.1.2312.9.4.13 (Quantity Used): 4
//		  */
////		regexes.put("productId",			"Serial Number: (\\d+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.[\\.#](.+)");
////		regexes.put("poolId",				"Serial Number: (\\d+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.[\\.#](.+)");
////		regexes.put("productId",			"Serial Number: (\\d+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.[.\\n](.+)");
////		regexes.put("poolId",				"Serial Number: (\\d+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.[.\\n](.+)");
//		regexes.put("productId",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.[.\\s\\n](.+)");
//		regexes.put("poolId",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.[.\\s\\n](.+)");
//		
//		Map<String, Map<String,String>> serialMapOfProductAndPoolIds = new HashMap<String, Map<String,String>>();
//		for(String field : regexes.keySet()){
//			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
//			addRegexMatchesToMap(pat, certificates, serialMapOfProductAndPoolIds, field);
//		}
//		
//		Map<Long, SubscriptionPool> serialMapOfSubscriptionPools = new HashMap<Long, SubscriptionPool>();
//		for(String serialNumber : serialMapOfProductAndPoolIds.keySet()) {
//			//serialMapOfSubscriptionPools.put(serialNumber, new SubscriptionPool(serialMapOfProductAndPoolIds.get(serialNumber).get("productId"), serialMapOfProductAndPoolIds.get(serialNumber).get("poolId")));
//			// the serialNumber formats have changed - jsefler 10/5/2010
//			Long serialPem = Long.parseLong(serialNumber.replaceAll(":", ""), 16);
//			serialMapOfSubscriptionPools.put(serialPem, new SubscriptionPool(serialMapOfProductAndPoolIds.get(serialNumber).get("productId"), serialMapOfProductAndPoolIds.get(serialNumber).get("poolId")));
//		}
//		return serialMapOfSubscriptionPools;
//	}
}
