package com.redhat.qe.sm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class ProductNamespace extends AbstractCommandLineData {
	
	// abstraction fields
	public String name;
	public String version;
	public String arch;
	public String providedTags;	// comma separated list of tags: String1, String2, String3

	public String id;
	
	public ProductNamespace(String id, Map<String, String> certData){
		super(certData);
		this.id = id;
	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (id != null)				string += String.format(" %s='%s'", "id",id);
		if (name != null)			string += String.format(" %s='%s'", "name",name);
		if (version != null)		string += String.format(" %s='%s'", "version",version);
		if (arch != null)			string += String.format(" %s='%s'", "arch",arch);
		if (providedTags != null)	string += String.format(" %s='%s'", "providedTags",providedTags);
		
		return string.trim();
	}
	
	@Override
	public boolean equals(Object obj){

		return	((ProductNamespace)obj).id.equals(this.id) &&
				((ProductNamespace)obj).name.equals(this.name);
	}
	
	/**
	 * @param rawCertificate - stdout from  openssl x509 -noout -text -in /etc/pki/entitlement/1129238407379723.pem
	 * @return
	 */
	static public List<ProductNamespace> parse(String rawCertificate) {
		
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
		        59:08:13:b0:5a:97:67:8a:3b:80:3c:08:9e:c0:46:ee:12:d1:
		        97:db:31:4a:34:6a:fb:3f:d1:f6:09:3d:03:17:5c:5a:c5:c0:
		        e8:44
		-----BEGIN CERTIFICATE-----
		MIIIZDCCB82gAwIBAgIHBAMJTiN7CzANBgkqhkiG9w0BAQUFADBSMTEwLwYDVQQD
		DChqc2VmbGVyLWYxMi1jYW5kbGVwaW4udXNlcnN5cy5yZWRoYXQuY29tMQswCQYD
		hGU0+veB7V3xGeCEy3b87AcsePChNESXEDyqjtpvqd7Dyv3f4IBlBLrwpczGSP91
		WJrxDi83TfdBqqjCSA09vSM1UAKX2zFKNGr7P9H2CT0DF1xaxcDoRA==
		-----END CERTIFICATE-----
		*/
		
		// https://docspace.corp.redhat.com/docs/DOC-30244
		//                         <hash> is a bad name - call it <id>
		//    1.3.6.1.4.1.2312.9.1.<product_hash>  (Red Hat Enterprise Linux for Physical Servers)
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.1 (Name) : Red Hat Enterprise Linux
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.2 (Version) : 6.0
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.3 (Architecture) : x86_64
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.4 (Provides) : String1, String2, String3
		//    1.3.6.1.4.1.2312.9.1.<product_hash>  (High Availability)
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.1 (Name) : High Availability
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.2 (Version) : 6.0
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.3 (Architecture) : x86_64
		//    1.3.6.1.4.1.2312.9.1.<product_hash>.4 (Provides) : String1, String2, String3
		//... (#may contain many products, with distinct product_hash id's)
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("name",					"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(\\d+)\\.1:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		regexes.put("version",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(\\d+)\\.2:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		regexes.put("arch",					"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(\\d+)\\.3:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		regexes.put("providedTags",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(\\d+)\\.4:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		
		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, rawCertificate, productMap, field);
		}
		
		List<ProductNamespace> productNamespaces = new ArrayList<ProductNamespace>();
		for(String key : productMap.keySet())
			productNamespaces.add(new ProductNamespace(key, productMap.get(key)));
		return productNamespaces;
	}
}
