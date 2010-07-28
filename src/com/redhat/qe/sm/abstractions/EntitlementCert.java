package com.redhat.qe.sm.abstractions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EntitlementCert extends CandlepinAbstraction {
	
	public String name;
	public String label;
	public String phys_ent;
	public String flex_ent;
	public String vendor_id;
	public String download_url;
	public String enabled;

	public String key;
	
	public EntitlementCert(String key, Map<String, String> certData){
		super(certData);
		this.key = key;
	}
	
	
	@Override
	public String toString() {
		
//		public String key;
//		public String label;
//		public String name;
//		public String phys_ent;
//		public String flex_ent;
//		public String vendor_id;
//		public String download_url;
//		public String enabled;
		
		String string = "";
		if (key != null)			string += String.format(" %s='%s'", "key",key);
		if (label != null)			string += String.format(" %s='%s'", "label",label);
		if (name != null)			string += String.format(" %s='%s'", "name",name);
		if (phys_ent != null)		string += String.format(" %s='%s'", "phys_ent",phys_ent);
		if (flex_ent != null)		string += String.format(" %s='%s'", "flex_ent",flex_ent);
		if (vendor_id != null)		string += String.format(" %s='%s'", "vendor_id",vendor_id);
		if (download_url != null)	string += String.format(" %s='%s'", "download_url",download_url);
		if (enabled != null)		string += String.format(" %s='%s'", "enabled",enabled);
		
		return string.trim();
	}
	
	
	
	/**
	 * @param certificates - stdout from "find /etc/pki/entitlement/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @return
	 */
	static public List<EntitlementCert> parse(String certificates) {
		/* # openssl x509 -in /etc/pki/entitlement/product/314.pem -noout -text
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number: 314 (0x13a)
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
		        Validity
		            Not Before: Jul  2 00:00:00 2010 GMT
		            Not After : Jul  2 00:00:00 2011 GMT
		        Subject: CN=admin/UID=28b1c0d1-f621-4a05-b94e-6b9f53230a3a
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (2048 bit)
		                Modulus:
		                    00:b8:4f:1f:23:98:08:a0:3f:82:c5:fe:af:13:38:
		                    61:ad:42:1a:ee:64:89:e9:84:f6:b0:65:14:8c:8d:
		                    48:fb:f6:9b:6c:d1:96:b8:e3:e4:85:39:00:59:b1:
		                    26:6e:7d:ff:c5:ac:96:70:ab:7e:8f:7d:05:b7:b6:
		                    f0:49:76:b3:65:56:29:9c:9c:5f:8c:d5:4b:32:73:
		                    d4:05:bf:c5:55:4b:68:d8:1a:8a:0c:61:0e:3a:1c:
		                    a2:fd:d4:c8:db:62:b5:a4:58:47:7b:13:e5:fd:27:
		                    50:76:58:f1:0c:1b:58:57:73:33:fd:c9:d1:f9:32:
		                    a0:0f:c7:f8:23:77:6c:85:5e:c1:92:f6:3a:81:65:
		                    f8:8d:ab:ff:ed:d8:77:d7:a3:d2:ea:56:c6:0c:bb:
		                    2a:3b:68:f3:be:18:9f:70:ed:97:01:36:4d:d6:8e:
		                    e7:0f:cf:d8:f9:d0:70:60:07:d0:52:c8:a5:3b:7d:
		                    d8:ca:54:1b:df:07:53:49:12:31:3f:0c:1b:57:c0:
		                    ec:f0:28:eb:78:d2:23:f3:02:a0:35:51:c7:17:f8:
		                    e8:66:6a:76:95:22:4b:b7:bb:49:df:dc:f0:82:5e:
		                    8d:39:c7:8b:68:51:1a:bf:4e:5c:d5:75:3a:f4:12:
		                    d7:57:01:be:08:af:fb:88:24:0c:b6:b4:0b:61:58:
		                    6e:2d
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            Netscape Cert Type: 
		                SSL Client, S/MIME
		            X509v3 Key Usage: 
		                Digital Signature, Key Encipherment, Data Encipherment
		            X509v3 Authority Key Identifier: 
		                keyid:10:8A:47:5F:A1:97:B7:2E:F1:D0:0F:8B:D7:D8:39:83:24:D5:38:6C
		                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
		                serial:96:06:43:DE:F4:D5:C7:F8

		            X509v3 Subject Key Identifier: 
		                65:8D:5D:F0:0B:80:F3:66:5E:F4:27:24:46:F3:59:C6:D8:4F:99:87
		            X509v3 Extended Key Usage: 
		                TLS Web Client Authentication
		            1.3.6.1.4.1.2312.9.1.37065.1: 
		                .!High availability (cluster suite)
		            1.3.6.1.4.1.2312.9.1.37065.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37065.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37065.4: 
		                ..1.0
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
		                ..0
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
		                ..1
		            1.3.6.1.4.1.2312.9.1.37060.1: 
		                ..RHEL for Physical Servers SVC
		            1.3.6.1.4.1.2312.9.1.37060.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37060.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37060.4: 
		                ..6.1
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
		                ..0
		            1.3.6.1.4.1.2312.9.1.37070.1: 
		                ..Load Balancing
		            1.3.6.1.4.1.2312.9.1.37070.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37070.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37070.4: 
		                ..1.0
		            1.3.6.1.4.1.2312.9.1.37068.1: 
		                ..Large File Support (XFS)
		            1.3.6.1.4.1.2312.9.1.37068.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37068.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37068.4: 
		                ..1.0
		            1.3.6.1.4.1.2312.9.1.37067.1: 
		                ..Shared Storage (GFS)
		            1.3.6.1.4.1.2312.9.1.37067.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37067.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37067.4: 
		                ..1.0
		            1.3.6.1.4.1.2312.9.1.37069.1: 
		                .<Smart Management (RHN Management & Provisioing & Monitoring)
		            1.3.6.1.4.1.2312.9.1.37069.2: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37069.3: 
		                ..ALL
		            1.3.6.1.4.1.2312.9.1.37069.4: 
		                ..1.0
		            1.3.6.1.4.1.2312.9.4.1: 
		                ..MKT-rhel-physical-2-socket
		            1.3.6.1.4.1.2312.9.4.2: 
		                ..4
		            1.3.6.1.4.1.2312.9.4.5: 
		                ..10
		            1.3.6.1.4.1.2312.9.4.6: 
		                ..2010-07-01 20:00:00.0
		            1.3.6.1.4.1.2312.9.4.7: 
		                ..2011-07-01 20:00:00.0
		            1.3.6.1.4.1.2312.9.4.12: 
		                ..3
		            1.3.6.1.4.1.2312.9.4.13: 
		                ..1
		            1.3.6.1.4.1.2312.9.5.1: 
		                .$28b1c0d1-f621-4a05-b94e-6b9f53230a3a
		    Signature Algorithm: sha1WithRSAEncryption
		        5f:24:18:39:f6:02:d6:35:c3:bd:5b:79:c0:6a:7a:d2:c6:5d:
		        be:20:02:c2:e1:da:2f:21:fd:bd:e5:1e:44:15:c9:5a:dd:44:
		        76:4f:02:51:3c:25:ad:d7:93:ed:65:6f:f9:46:19:ae:71:b6:
		        63:39:b4:52:f1:d8:a1:ca:5f:a1:d7:36:4b:69:52:7e:55:77:
		        0f:87:9f:68:53:81:77:32:49:b7:ac:e2:9c:b2:ed:6b:31:f6:
		        52:60:65:11:0e:ac:ef:f8:10:fc:a8:a6:75:20:31:57:06:9c:
		        06:c4:a2:65:05:81:8c:d7:5b:e3:9f:4f:1f:6c:9b:3c:85:e5:
		        30:28
		*/
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// EntitlementCert abstractionField		pattern		(Note: the abstractionField must be defined in the EntitlementCert class)
		/* https://docspace.corp.redhat.com/docs/DOC-30244
		  1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (Supplementary))
		  1.3.6.1.4.1.2312.9.2.<content_hash>.1 (Yum repo type))
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.1 (Name) : Red Hat Enterprise Linux (Supplementary)
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.2 (Label) : rhel-server-6-supplementary
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.3 (Physical Entitlements): 1
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.4 (Flex Guest Entitlements): 0
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.6 (Download URL): content/rhel-server-6-supplementary/$releasever/$basearch
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.7 (GPG Key URL): file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		    1.3.6.1.4.1.2312.9.2.<content_hash>.1.8 (Enabled): 1
		*/
//		regexes.put("name",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.1:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("label",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.2:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("phys_ent",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.3:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("flex_ent",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.4:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("vendor_id",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.5:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("download_url",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.6:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
//		regexes.put("enabled",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.8:[\\s\\.\\cM]*([a-zA-Z_0-9-]+)");
		regexes.put("name",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.1:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("label",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.2:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("phys_ent",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.3:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("flex_ent",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.4:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("vendor_id",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.5:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("download_url",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.6:[\\s\\cM]*\\.[.\\n](.+)");
		regexes.put("enabled",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\.1\\.8:[\\s\\cM]*\\.[.\\n](.+)");
		
		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, certificates, productMap, field);
		}
		
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		for(String certKey : productMap.keySet())
			entitlementCerts.add(new EntitlementCert(certKey, productMap.get(certKey)));
		return entitlementCerts;
	}
}
