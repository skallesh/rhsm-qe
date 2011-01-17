package com.redhat.qe.sm.data;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class EntitlementCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";			// Aug 23 08:42:00 2010 GMT   validityNotBefore
//MOVED to OrderNamespace
//	protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// "2010-09-01T15:45:12.068+0000"   startDate
//	protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";		// "2011-12-02T00:00:00Z"   startDate

	// abstraction fields
	public BigInteger serialNumber;	// this is the key
	public String id;			// entitlement uuid on the candlepin server
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	
//MOVED to OrderNamespace	
//	public String productName;
//	public String orderNumber;
//	public String productId;	// SKU
//	public String subscriptionNumber;	// REGTOKEN
//	public String quantity;
//	public Calendar startDate;
//	public Calendar endDate;
//	public String virtualizationLimit;
//	public String socketLimit;
//	public String contractNumber;
//	public String quantityUsed;
//	public String warningPeriod;
//	public String accountNumber;
//	public Boolean providesManagement;
	
	public OrderNamespace orderNamespace;
	public List<ProductNamespace> productNamespaces;
	public List<ContentNamespace> contentNamespaces;
	public String rawCertificate;

	public EntitlementCert(BigInteger serialNumber, Map<String, String> certData){
		super(certData);
		this.serialNumber = serialNumber;
		orderNamespace = OrderNamespace.parse(this.rawCertificate);
		productNamespaces = ProductNamespace.parse(this.rawCertificate);
		contentNamespaces = ContentNamespace.parse(this.rawCertificate);
	}

	
	
	@Override
	public String toString() {
		
		String string = "";
		if (serialNumber != null)			string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)						string += String.format(" %s='%s'", "id",id);
		if (issuer != null)					string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)		string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)		string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
//MOVED to OrderNamespace		
//		if (productName != null)			string += String.format(" %s='%s'", "productName",productName);
//		if (orderNumber != null)			string += String.format(" %s='%s'", "orderNumber",orderNumber);
//		if (subscriptionNumber != null)		string += String.format(" %s='%s'", "subscriptionNumber",subscriptionNumber);
//		if (startDate != null)				string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
//		if (endDate != null)				string += String.format(" %s='%s'", "endDate",formatDateString(endDate));
//		if (virtualizationLimit != null)	string += String.format(" %s='%s'", "virtualizationLimit",virtualizationLimit);
//		if (socketLimit != null)			string += String.format(" %s='%s'", "socketLimit",socketLimit);
//		if (contractNumber != null)			string += String.format(" %s='%s'", "contractNumber",contractNumber);
//		if (quantityUsed != null)			string += String.format(" %s='%s'", "quantityUsed",quantityUsed);
//		if (warningPeriod != null)			string += String.format(" %s='%s'", "warningPeriod",warningPeriod);
//		if (accountNumber != null)			string += String.format(" %s='%s'", "accountNumber",accountNumber);
	
		return string.trim();
	}
	
	@Override
	protected Calendar parseDateString(String dateString){
		return parseDateString(dateString, simpleDateFormat);
	}
	
	//@Override
	public static String formatDateString(Calendar date){
		DateFormat dateFormat = new SimpleDateFormat(simpleDateFormat);
		return dateFormat.format(date.getTime());
	}
	
	@Override
	public boolean equals(Object obj){

		return	((EntitlementCert)obj).serialNumber.equals(this.serialNumber) &&
				((EntitlementCert)obj).id.equals(this.id) &&
				((EntitlementCert)obj).issuer.equals(this.issuer) &&
				((EntitlementCert)obj).validityNotBefore.equals(this.validityNotBefore) &&
				((EntitlementCert)obj).validityNotAfter.equals(this.validityNotAfter) &&
				((EntitlementCert)obj).orderNamespace.productId.equals(this.orderNamespace.productId);
	}
	
	/**
	 * @param rawCertificates - stdout from "find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @return
	 */
	static public List<EntitlementCert> parse(String rawCertificates) {
		/* # find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number:
		            28:18:e5:56:66:08:cc
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
		        Validity
		            Not Before: Oct  6 13:39:04 2010 GMT
		            Not After : Oct  6 23:59:59 2011 GMT
		        Subject: CN=ff8080812b80901e012b81c442a8036c
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (2048 bit)
		                Modulus:
		                    00:b5:bc:ea:a7:4c:0d:76:fe:0a:6d:13:71:db:41:
		                    e1:a8:ad:7c:30:81:4a:e2:e3:c3:c0:23:25:f6:3d:
		                    78:d3:55:0b:4d:69:af:58:25:41:79:82:ea:f4:c2:
		                    49:37:76:fb:54:e2:df:a1:5a:42:d4:e5:7d:eb:b7:
		                    b6:92:8b:ec:08:61:af:40:eb:0f:ae:ef:fb:3c:19:
		                    ce:1c:29:3b:d8:a6:0d:99:67:ca:9c:e0:5c:e7:bd:
		                    9c:1d
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            Netscape Cert Type: 
		                SSL Client, S/MIME
		            X509v3 Key Usage: 
		                Digital Signature, Key Encipherment, Data Encipherment
		            X509v3 Authority Key Identifier: 
		                keyid:12:E4:53:46:28:B2:C1:08:AE:B1:61:66:D0:CB:E6:33:D7:F8:1E:BA
		                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
		                serial:F3:57:07:B0:21:C8:FF:D6

		            X509v3 Subject Key Identifier: 
		                44:2E:4D:51:22:CE:4D:6A:61:86:87:30:08:02:BB:52:D7:88:7A:3E
		            X509v3 Extended Key Usage: 
		                TLS Web Client Authentication
		            1.3.6.1.4.1.2312.9.1.37065.1: 
		                .!High availability (cluster suite)
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
		            1.3.6.1.4.1.2312.9.1.37060.1: 
		                ..RHEL for Physical Servers SVC
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
		            1.3.6.1.4.1.2312.9.1.37070.1: 
		                ..Load Balancing
		            1.3.6.1.4.1.2312.9.1.37068.1: 
		                ..Large File Support (XFS)
		            1.3.6.1.4.1.2312.9.1.37067.1: 
		                ..Shared Storage (GFS)
		            1.3.6.1.4.1.2312.9.1.37069.1: 
		                .<Smart Management (RHN Management & Provisioing & Monitoring)
		            1.3.6.1.4.1.2312.9.4.1: 
		                ..MKT-rhel-physical-servers-only
		            1.3.6.1.4.1.2312.9.4.2: 
		                . ff8080812b80901e012b8090f4e10081
		            1.3.6.1.4.1.2312.9.4.5: 
		                ..10
		            1.3.6.1.4.1.2312.9.4.6: 
		                ..2010-10-06T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.7: 
		                ..2011-10-06T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.14: 
		                ..0
		            1.3.6.1.4.1.2312.9.4.12: 
		                ..5
		            1.3.6.1.4.1.2312.9.4.13: 
		                ..1
		            1.3.6.1.4.1.2312.9.5.1: 
		                .$3733b061-9172-429c-a276-f810cb0dcb18
		    Signature Algorithm: sha1WithRSAEncryption
		        46:75:04:83:09:f2:14:d8:47:9b:04:a2:a7:b8:55:43:ab:6c:
		        a9:92:a1:40:55:15:4a:0c:f1:da:49:38:8f:4d:88:1a:ac:39:
		        c7:7d:59:64:6a:ce:6a:e4:27:38:62:9a:9e:08:49:0f:17:86:
		        93:7e:8a:a4:46:b9:5f:57:d0:22:58:f0:b2:bb:8d:4b:bc:18:
		        12:84:d0:35:24:93:95:c2:14:72:c2:5f:3f:4e:85:8e:07:c7:
		        10:b5:68:df:a4:80:fb:74:5a:7b:d6:77:42:e9:ab:18:c0:04:
		        bd:df:b1:f9:fe:3f:fa:9a:fc:0c:c7:91:99:5b:7b:2d:af:14:
		        cf:1c
		*/
		/*
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            28:1b:42:f7:9d:bf:2d
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
        Validity
            Not Before: Nov  5 16:11:44 2010 GMT
            Not After : Nov  5 23:59:59 2011 GMT
        Subject: CN=ff8080812c1b0fa2012c1ccecff303ca
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:ce:97:4d:65:c0:2d:fa:20:98:18:dd:ed:97:76:
                    d8:a1:4a:5b:ba:ea:f7:ea:3e:5c:c4:b6:7c:3f:de:
                    b3:40:5f:64:da:bf:b0:be:d1:81:3d:11:76:50:b3:
                    d2:96:84:ac:71:4e:c7:66:c2:a1:dc:c5:73:2e:f1:
                    6f:c5
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            Netscape Cert Type: 
                SSL Client, S/MIME
            X509v3 Key Usage: 
                Digital Signature, Key Encipherment, Data Encipherment
            X509v3 Authority Key Identifier: 
                keyid:B6:70:95:46:37:47:01:C8:2B:43:29:4F:82:EF:11:5C:5D:E4:70:6F
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:C6:E0:7B:6A:01:03:25:F7

            X509v3 Subject Key Identifier: 
                27:E0:57:92:B1:53:28:A0:EC:50:51:BC:29:DC:95:6A:D0:D3:7D:33
            X509v3 Extended Key Usage: 
                TLS Web Client Authentication
            1.3.6.1.4.1.2312.9.1.37065.1: 
                .!High availability (cluster suite)
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
            1.3.6.1.4.1.2312.9.1.37060.1: 
                ..RHEL for Physical Servers SVC
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
            1.3.6.1.4.1.2312.9.1.37070.1: 
                ..Load Balancing
            1.3.6.1.4.1.2312.9.1.37068.1: 
                ..Large File Support (XFS)
            1.3.6.1.4.1.2312.9.1.37067.1: 
                ..Shared Storage (GFS)
            1.3.6.1.4.1.2312.9.1.37069.1: 
                .<Smart Management (RHN Management & Provisioing & Monitoring)
            1.3.6.1.4.1.2312.9.4.1: 
                ...RHEL for Physical Servers ,2 Sockets, Standard Support with High
Availability,Load Balancing,Shared Storage,Large File Support,Smart
Management, Flexible Hypervisor(Unlimited)
            1.3.6.1.4.1.2312.9.4.2: 
                . ff8080812c1b0fa2012c1b108b660086
            1.3.6.1.4.1.2312.9.4.3: 
                ..MKT-rhel-physical-2-socket
            1.3.6.1.4.1.2312.9.4.5: 
                ..10
            1.3.6.1.4.1.2312.9.4.6: 
                ..2010-11-05T00:00:00Z
            1.3.6.1.4.1.2312.9.4.7: 
                ..2011-11-05T00:00:00Z
            1.3.6.1.4.1.2312.9.4.12: 
                ..30
            1.3.6.1.4.1.2312.9.4.10: 
                ..4
            1.3.6.1.4.1.2312.9.4.11: 
                ..1
            1.3.6.1.4.1.2312.9.5.1: 
                .$0cd89d72-d7a9-4e3d-9eb0-599c4a0e2f8d
    Signature Algorithm: sha1WithRSAEncryption
        bd:94:86:5d:82:b8:99:81:35:89:b4:78:12:25:5c:ae:24:09:
        46:ac:3d:54:8d:4a:56:05:4d:35:bc:66:01:b0:de:b7:56:1b:
        29:ed:30:e3:aa:51:c9:c4:ef:3f:b9:2e:2f:ca:34:de:11:47:
        58:7a:78:9a:8f:d0:31:9c:1f:ab:03:69:8f:dd:bf:82:26:0e:
        fc:c7:12:a0:61:46:f3:cd:8c:1f:6e:0c:dd:88:b2:05:bc:e2:
        5c:cd:44:d3:13:18:58:0c:02:60:18:50:d4:91:08:0f:f7:0f:
        2b:1c:bf:a6:fb:30:d4:13:6b:e3:7e:13:71:49:9d:44:8a:8b:
        18:cc
-----BEGIN CERTIFICATE-----
MIIJnDCCCQWgAwIBAgIHKBtC952/LTANBgkqhkiG9w0BAQUFADBSMTEwLwYDVQQD
DChqc2VmbGVyLWYxMi1jYW5kbGVwaW4udXNlcnN5cy5yZWRoYXQuY29tMQswCQYD
A2mP3b+CJg78xxKgYUbzzYwfbgzdiLIFvOJczUTTExhYDAJgGFDUkQgP9w8rHL+m
+zDUE2vjfhNxSZ1EiosYzA==
-----END CERTIFICATE-----

		 */
		
		/*
		        Issuer: C=US, ST=North Carolina, O=Red Hat, Inc., OU=Red Hat Network, CN=Red Hat Entitlement Product Authority/emailAddress=ca-support@redhat.com
		 */
//MOVED to OrderNamespace		
//		https://docspace.corp.redhat.com/docs/DOC-30244
//			  1.3.6.1.4.1.2312.9.4.1 (Name): Red Hat Enterprise Linux Server
//			  1.3.6.1.4.1.2312.9.4.2 (Order Number) : ff8080812c3a2ba8012c3a2cbe63005b  
//			  1.3.6.1.4.1.2312.9.4.3 (SKU) : MCT0982
//			  1.3.6.1.4.1.2312.9.4.4 (Subscription Number) : abcd-ef12-1234-5678   <- SHOULD ONLY EXIST IF ORIGINATED FROM A REGTOKEN
//			  1.3.6.1.4.1.2312.9.4.5 (Quantity) : 100
//			  1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) : 2010-10-25T04:00:00Z
//			  1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) : 2011-11-05T00:00:00Z
//			  1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) : 4
//			  1.3.6.1.4.1.2312.9.4.9 (Socket Limit) : None
//			  1.3.6.1.4.1.2312.9.4.10 (Contract Number): 152341643
//			  1.3.6.1.4.1.2312.9.4.11 (Quantity Used): 4
//			  1.3.6.1.4.1.2312.9.4.12 (Warning Period): 30
//			  1.3.6.1.4.1.2312.9.4.13 (Account Number): 9876543210
//			  1.3.6.1.4.1.2312.9.4.14 (Provides Management): 0 (boolean, 1 for true)
// reference bugzilla: https://bugzilla.redhat.com/show_bug.cgi?id=640463
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("id",					"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Subject: CN=(.+)");
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Issuer:\\s*(.*)");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");

//MOVED to OrderNamespace
//		// Order Namespace  // FIXME I believe this should be busted out into a single Order Namespace data object - WORK IN PROGRESS 12/14/2010
//		regexes.put("productName",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("orderNumber",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("productId",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.3:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("subscriptionNumber",	"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.4:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("quantity",				"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.5:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("startDate",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.6:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("endDate",				"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.7:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("virtualizationLimit",	"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.8:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("socketLimit",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.9:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("contractNumber",		"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.10:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("quantityUsed",			"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.11:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("warningPeriod",		"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.12:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("accountNumber",		"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.13:[\\s\\cM]*\\.(?:.|\\s)(.+)");
//		regexes.put("providesManagement",	"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.14:[\\s\\cM]*\\.(?:.|\\s)(\\d)");

		// FIXME THIS IS ONLY PART OF THE rawCertificate (another way to list the cert is: python /usr/share/rhsm/certificate.py /etc/pki/entitlement/11290530959739201.pem
		regexes.put("rawCertificate",		"Serial Number:\\s*([\\d\\w:]+)((?:\\n.*?)+)1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.5\\.1:");
		

		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, rawCertificates, productMap, field);
		}
		
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		for(String key : productMap.keySet()) {
			
			// convert the key inside the raw cert file (04:02:7b:dc:b7:fb:33) to a numeric serialNumber (11286372344531148)
			//Long serialNumber = Long.parseLong(key.replaceAll(":", ""), 16);
			BigInteger serialNumber = new BigInteger(key.replaceAll(":", ""),16);
			
			entitlementCerts.add(new EntitlementCert(serialNumber, productMap.get(key)));
		}
		return entitlementCerts;
	}
}

/* FIXME USING TIME ZOME NOTES FROM AJAY
 * SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd");
        iso8601DateFormat.setTimeZone(TimeZone.getDefault());
        System.out.println(iso8601DateFormat.format(new Date()));
        */
