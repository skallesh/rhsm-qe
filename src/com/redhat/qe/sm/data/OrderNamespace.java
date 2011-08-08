package com.redhat.qe.sm.data;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class OrderNamespace extends AbstractCommandLineData {
//	protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// "2010-09-01T15:45:12.068+0000"   startDate
	protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";		// "2011-01-20T05:00:00Z" GMT startDate  // "2011-07-20T03:59:59Z" GMT endDate

	// abstraction fields
	public String productName;
	public String orderNumber;
	public String productId;	// SKU
	public String subscriptionNumber;	// REGTOKEN
	public String quantity;
	public Calendar startDate;
	public Calendar endDate;
	public String virtualizationLimit;
	public String socketLimit;
	public Integer contractNumber;
	public String quantityUsed;
	public String warningPeriod;
	public BigInteger accountNumber;
	public Boolean providesManagement;
	public String supportLevel;
	public String supportType;



	public OrderNamespace(Map<String, String> certData) {
		super(certData);
		// TODO Auto-generated constructor stub
		
		// Overridden fields
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (productName != null)			string += String.format(" %s='%s'", "productName",productName);
		if (orderNumber != null)			string += String.format(" %s='%s'", "orderNumber",orderNumber);
		if (productId != null)				string += String.format(" %s='%s'", "productId",productId);
		if (subscriptionNumber != null)		string += String.format(" %s='%s'", "subscriptionNumber",subscriptionNumber);
		if (startDate != null)				string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
		if (endDate != null)				string += String.format(" %s='%s'", "endDate",formatDateString(endDate));
		if (virtualizationLimit != null)	string += String.format(" %s='%s'", "virtualizationLimit",virtualizationLimit);
		if (socketLimit != null)			string += String.format(" %s='%s'", "socketLimit",socketLimit);
		if (contractNumber != null)			string += String.format(" %s='%s'", "contractNumber",contractNumber);
		if (quantity != null)				string += String.format(" %s='%s'", "quantity",quantity);
		if (quantityUsed != null)			string += String.format(" %s='%s'", "quantityUsed",quantityUsed);
		if (warningPeriod != null)			string += String.format(" %s='%s'", "warningPeriod",warningPeriod);
		if (accountNumber != null)			string += String.format(" %s='%s'", "accountNumber",accountNumber);
				
		return string.trim();
	}
	
	@Override
	protected Calendar parseDateString(String dateString){
		try{
			DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Calendar calendar = new GregorianCalendar();
			calendar.setTimeInMillis(dateFormat.parse(dateString).getTime());
			return calendar;
		}
		catch (ParseException e){
			log.warning("Failed to parse GMT date string '"+dateString+"' with format '"+simpleDateFormat+"':\n"+e.getMessage());
			return null;
		}
	}
	
	//@Override
	public static String formatDateString(Calendar date){
		DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat.format(date.getTime());
	}
	
	@Override
	public boolean equals(Object obj){
		// FIXME: Not sure if this is sufficient:
		return	//((OrderNamespace)obj).productName.equals(this.productName) &&
				((OrderNamespace)obj).orderNumber.equals(this.orderNumber) &&
				((OrderNamespace)obj).productId.equals(this.productId);
	}
	
	/**
	 * @param rawCertificate - stdout from  openssl x509 -noout -text -in /etc/pki/entitlement/1129238407379723.pem
	 * @return
	 * @throws ParseException 
	 */
	static public OrderNamespace parse(String rawCertificate) {
		
		/* [root@jsefler-onprem01 ~]# openssl x509 -text -in /etc/pki/entitlement/1129238407379723.pem 
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number:
		            04:03:09:4e:23:7b:0b
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
		        Validity
		            Not Before: Dec 15 03:34:33 2010 GMT
		            Not After : Dec 14 23:59:59 2011 GMT
		        Subject: CN=ff8080812ce6affa012ce817f8210281
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (2048 bit)
		                Modulus:
		                    00:92:65:52:8f:13:29:17:d4:06:93:67:75:36:a9:
		                    5d:cd:af:60:78:b1:24:fa:dc:18:75:99:01:7e:bb:
		                    2a:8e:c4:f4:cd:82:59:72:ff:ce:6b:d6:24:e0:ee:
		                    f5:69
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            Netscape Cert Type: 
		                SSL Client, S/MIME
		            X509v3 Key Usage: 
		                Digital Signature, Key Encipherment, Data Encipherment
		            X509v3 Authority Key Identifier: 
		                keyid:93:3E:CB:C8:B3:DB:DE:99:D7:C6:4D:ED:5F:CE:EA:F3:C8:AC:1E:1A
		                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
		                serial:9E:59:0C:BE:23:E9:E6:A8

		            X509v3 Subject Key Identifier: 
		                39:2A:DA:B9:CF:43:54:4E:4A:2A:A2:65:AE:57:15:2A:10:57:4A:39
		            X509v3 Extended Key Usage: 
		                TLS Web Client Authentication
		            1.3.6.1.4.1.2312.9.1.37060.1: 
		                ..RHEL for Physical Servers SVC
		            1.3.6.1.4.1.2312.9.2.1.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.1.1.1: 
		                ..always-enabled-content
		            1.3.6.1.4.1.2312.9.2.1.1.2: 
		                ..always-enabled-content
		            1.3.6.1.4.1.2312.9.2.1.1.5: 
		                ..test-vendor
		            1.3.6.1.4.1.2312.9.2.1.1.6: 
		                ../foo/path/always
		            1.3.6.1.4.1.2312.9.2.1.1.7: 
		                ../foo/path/always/gpg
		            1.3.6.1.4.1.2312.9.2.1.1.4: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.1.1.3: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.1.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.2.0.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.0.1.1: 
		                ..never-enabled-content
		            1.3.6.1.4.1.2312.9.2.0.1.2: 
		                ..never-enabled-content
		            1.3.6.1.4.1.2312.9.2.0.1.5: 
		                ..test-vendor
		            1.3.6.1.4.1.2312.9.2.0.1.6: 
		                ../foo/path/never
		            1.3.6.1.4.1.2312.9.2.0.1.7: 
		                ../foo/path/never/gpg
		            1.3.6.1.4.1.2312.9.2.0.1.4: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.0.1.3: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.0.1.8: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.1111.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.1111.1.1: 
		                ..content
		            1.3.6.1.4.1.2312.9.2.1111.1.2: 
		content-label   .
		            1.3.6.1.4.1.2312.9.2.1111.1.5: 
		                ..test-vendor
		            1.3.6.1.4.1.2312.9.2.1111.1.6: 
		                ../foo/path
		            1.3.6.1.4.1.2312.9.2.1111.1.7: 
		                ../foo/path/gpg/
		            1.3.6.1.4.1.2312.9.2.1111.1.4: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.1111.1.3: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.1111.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.1.37062.1: 
		                ."RHEL for Premium Architectures SVC
		            1.3.6.1.4.1.2312.9.4.1: 
		                ..RHEL for Premium Architectures
		            1.3.6.1.4.1.2312.9.4.2: 
		                . ff8080812ce6affa012ce6b0bd900109
		            1.3.6.1.4.1.2312.9.4.3: 
		                ..MKT-rhel-premium
		            1.3.6.1.4.1.2312.9.4.5: 
		                ..5
		            1.3.6.1.4.1.2312.9.4.6: 
		                ..2010-12-14T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.7: 
		                ..2011-12-14T23:59:59Z
		            1.3.6.1.4.1.2312.9.4.12: 
		                ..0
		            1.3.6.1.4.1.2312.9.4.10: 
		                ..69
		            1.3.6.1.4.1.2312.9.4.13: 
		                ..12331131231
		            1.3.6.1.4.1.2312.9.4.14: 
		                ..0
		            1.3.6.1.4.1.2312.9.4.11: 
		                ..1
		            1.3.6.1.4.1.2312.9.5.1: 
		                .$14d260fd-c3fb-47e5-8fb5-058250e11bda
		    Signature Algorithm: sha1WithRSAEncryption
		        41:5f:4d:04:39:bc:ee:50:9d:c6:fc:66:f6:76:c9:ce:6d:b6:
		        f1:0e:2f:37:4d:f7:41:aa:a8:c2:48:0d:3d:bd:23:35:50:02:
		        97:db:31:4a:34:6a:fb:3f:d1:f6:09:3d:03:17:5c:5a:c5:c0:
		        e8:44
		-----BEGIN CERTIFICATE-----
		MIIIZDCCB82gAwIBAgIHBAMJTiN7CzANBgkqhkiG9w0BAQUFADBSMTEwLwYDVQQD
		DChqc2VmbGVyLWYxMi1jYW5kbGVwaW4udXNlcnN5cy5yZWRoYXQuY29tMQswCQYD
		hGU0+veB7V3xGeCEy3b87AcsePChNESXEDyqjtpvqd7Dyv3f4IBlBLrwpczGSP91
		WJrxDi83TfdBqqjCSA09vSM1UAKX2zFKNGr7P9H2CT0DF1xaxcDoRA==
		-----END CERTIFICATE-----
		*/
		
		//		https://docspace.corp.redhat.com/docs/DOC-30244
		//		  1.3.6.1.4.1.2312.9.4.1 (Name): Red Hat Enterprise Linux Server
		//		  1.3.6.1.4.1.2312.9.4.2 (Order Number) : ff8080812c3a2ba8012c3a2cbe63005b  
		//		  1.3.6.1.4.1.2312.9.4.3 (SKU) : MCT0982
		//		  1.3.6.1.4.1.2312.9.4.4 (Subscription Number) : abcd-ef12-1234-5678   <- SHOULD ONLY EXIST IF ORIGINATED FROM A REGTOKEN
		//		  1.3.6.1.4.1.2312.9.4.5 (Quantity) : 100
		//		  1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) : 2010-10-25T04:00:00Z
		//		  1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) : 2011-11-05T00:00:00Z
		//		  1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) : 4
		//		  1.3.6.1.4.1.2312.9.4.9 (Socket Limit) : None
		//		  1.3.6.1.4.1.2312.9.4.10 (Contract Number): 152341643
		//		  1.3.6.1.4.1.2312.9.4.11 (Quantity Used): 4
		//		  1.3.6.1.4.1.2312.9.4.12 (Warning Period): 30
		//		  1.3.6.1.4.1.2312.9.4.13 (Account Number): 9876543210
		//		  1.3.6.1.4.1.2312.9.4.14 (Provides Management): 0 (boolean, 1 for true)
		//		  1.3.6.1.4.1.2312.9.4.15 (Support Level): Premium
		//		  1.3.6.1.4.1.2312.9.4.16 (Support Type): Level 3
		// reference bugzilla: https://bugzilla.redhat.com/show_bug.cgi?id=640463
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("productName",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("orderNumber",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("productId",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.3:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("subscriptionNumber",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.4:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("quantity",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.5:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("startDate",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.6:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("endDate",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.7:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("virtualizationLimit",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.8:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("socketLimit",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.9:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("contractNumber",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.10:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("quantityUsed",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.11:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("warningPeriod",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.12:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("accountNumber",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.13:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("providesManagement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.14:[\\s\\cM]*\\.(?:.|\\s)(\\d)");
		regexes.put("supportLevel",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.15:[\\s\\cM]*\\.(?:.|\\s)(\\d)");
		regexes.put("supportType",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.16:[\\s\\cM]*\\.(?:.|\\s)(\\d)");
		

		List<Map<String,String>> listMaps = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, rawCertificate, listMaps, field);
		}
		
		// listMaps should only be one in size
		if (listMaps.size()> 1) throw new RuntimeException("Expected to parse only one group of order namespace certificate data.");
		if (listMaps.size()==0) throw new RuntimeException("Failed to parse a group of order namespace certificate data.");
	
		return new OrderNamespace(listMaps.get(0));
	}
}
