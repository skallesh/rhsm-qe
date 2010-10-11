package com.redhat.qe.sm.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

public class EntitlementCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT

	// abstraction fields
	public Long serialNumber;	// this is the key
	public String rawCertificate;
	public String id;			// entitlement uuid on the candlepin server
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	public String productId;	// comes from the Order Namespace
	public List<ContentNamespace> contentNamespaces;


	public EntitlementCert(Long serialNumber, Map<String, String> certData){
		super(certData);
		this.serialNumber = serialNumber;
		contentNamespaces = ContentNamespace.parse(this.rawCertificate);
	}

	
	
	@Override
	public String toString() {
		
		String string = "";
		if (serialNumber != null)		string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)					string += String.format(" %s='%s'", "id",id);
		if (issuer != null)				string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)	string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)	string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
		if (productId != null)			string += String.format(" %s='%s'", "productId",productId);
	
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
				((EntitlementCert)obj).productId.equals(this.productId);
	}
	
	/**
	 * @param certificates - stdout from "find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @return
	 */
	static public List<EntitlementCert> parse(String certificates) {
		/*
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
		                    df:b2:30:2e:92:3f:09:18:d3:3a:6d:10:95:1e:bc:
		                    57:55:15:04:74:19:23:cb:ac:1e:18:91:5c:88:04:
		                    2a:b8:56:9d:9a:55:fa:f3:dd:f8:13:51:ae:61:21:
		                    b8:a1:be:13:57:6f:3d:29:d1:c5:da:e2:8b:8c:58:
		                    fb:7c:20:37:e8:fc:e4:7e:1f:a2:07:09:dd:7e:20:
		                    aa:35:4b:17:60:ea:c0:f2:a5:6b:e8:15:ef:38:65:
		                    70:a1:b2:15:33:f7:19:27:fe:44:3b:26:90:6e:e2:
		                    73:85:ed:c4:80:5b:58:38:82:a1:9f:a7:76:97:99:
		                    82:9a:c6:28:9b:a4:f5:dd:b0:8d:87:fd:37:c4:c4:
		                    d3:c4:50:dc:8b:57:69:7b:92:0f:a5:dd:0c:a3:e0:
		                    2b:6f:33:18:d6:92:ca:c0:16:6d:b1:4a:98:ce:95:
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
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("id",					"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Subject: CN=(.+)");
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Issuer: CN=(.*?),");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");
		regexes.put("productId",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.[.\\s\\n](.+)");
		regexes.put("rawCertificate",		"Serial Number:\\s*([\\d\\w:]+).*((?:\\n.*?)*).1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.5\\.1:");	// FIXME THIS IS ONLY PART OF THE CERT

		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, certificates, productMap, field);
		}
		
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		for(String key : productMap.keySet()) {
			
			// convert the key inside the raw entitlement file (04:02:7b:dc:b7:fb:33) to a Long serialNumber (11286372344531148)
			Long serialNumber = Long.parseLong(key.replaceAll(":", ""), 16);
		
			entitlementCerts.add(new EntitlementCert(serialNumber, productMap.get(key)));
		}
		return entitlementCerts;
	}
}
