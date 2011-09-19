package com.redhat.qe.sm.data;

import java.io.File;
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

	// abstraction fields
	public File file;
	public BigInteger serialNumber;	// this is the key
	public String id;			// entitlement uuid on the candlepin server
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	
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
		if (file != null)					string += String.format(" %s='%s'", "file",file);
		if (serialNumber != null)			string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)						string += String.format(" %s='%s'", "id",id);
		if (issuer != null)					string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)		string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)		string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
	
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
	 * @param certificates - OLD WAY: stdout from "find /etc/pki/entitlement/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @param certificates - stdout from: find /etc/pki/entitlement/ -name '*.pem' -exec openssl x509 -in '{}' -noout -text \; -exec echo "    File: {}" \;
	 * @return
	 */
	static public List<EntitlementCert> parse(String rawCertificates) {
		
		/* [root@jsefler-onprem-62server ~]# find /etc/pki/entitlement/ -name '*.pem' -exec openssl x509 -in '{}' -noout -text \; -exec echo "    File: {}" \;
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number:
		            66:23:33:7e:79:13:71:32
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: CN=jsefler-onprem-62candlepin.usersys.redhat.com, C=US, L=Raleigh
		        Validity
		            Not Before: Sep 19 00:00:00 2011 GMT
		            Not After : Sep 18 00:00:00 2012 GMT
		        Subject: CN=8a90f8c63282c09a013283bbb40609ea
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (2048 bit)
		                Modulus:
		                    00:92:e9:4a:dd:a7:94:c7:1f:d3:46:42:10:90:1d:
		                    a6:4f:44:82:14:1b:ba:7a:5a:5e:b4:c5:cb:de:96:
		                    c8:d3:7a:e1:a5:43:f7:52:88:1d:ac:50:6a:bb:2c:
		                    aa:20:42:4b:97:8c:48:56:8d:6e:87:51:ac:22:60:
		                    02:1b:86:77:a7:a6:f9:20:6e:4e:4e:6b:f7:12:54:
		                    04:c0:17:eb:e4:44:05:84:32:88:02:92:39:4b:08:
		                    83:f7:1b:b3:11:48:76:f2:fb:99:36:ad:fc:98:48:
		                    f9:e0:ce:69:80:4d:50:a0:a9:b8:99:70:61:5a:49:
		                    70:56:82:d8:1e:ef:a8:ca:22:e2:9d:79:e5:5c:64:
		                    aa:70:57:06:30:d1:cd:a7:dd:a9:e6:b1:f8:b0:27:
		                    81:0b:72:00:d0:79:a5:13:1d:ac:46:68:bf:94:34:
		                    1f:40:53:e9:46:7a:da:16:82:93:5a:b8:5e:81:76:
		                    46:92:f0:00:e1:d2:bc:80:a5:e4:aa:63:03:4e:75:
		                    07:11:24:2d:65:79:bb:75:b0:50:79:55:c9:a2:93:
		                    06:a7:71:0e:90:ce:71:cf:9b:70:c1:5e:84:f1:23:
		                    ed:1b:48:09:7d:e7:50:5f:0c:4f:c5:91:b6:1d:31:
		                    a7:d8:dd:f1:0c:d5:b1:84:e9:f7:7f:34:7f:31:aa:
		                    8b:79
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            Netscape Cert Type: 
		                SSL Client, S/MIME
		            X509v3 Key Usage: 
		                Digital Signature, Key Encipherment, Data Encipherment
		            X509v3 Authority Key Identifier: 
		                keyid:82:7C:66:96:9E:DB:01:A9:01:A4:32:EE:97:80:26:6D:1F:AC:03:66
		                DirName:/CN=jsefler-onprem-62candlepin.usersys.redhat.com/C=US/L=Raleigh
		                serial:BC:92:AE:6A:DE:86:B6:D6

		            X509v3 Subject Key Identifier: 
		                6C:77:A9:5B:18:8A:7B:D1:DE:77:89:44:A9:60:AF:F0:5E:32:E6:F0
		            X509v3 Extended Key Usage: 
		                TLS Web Client Authentication
		            1.3.6.1.4.1.2312.9.1.100000000000002.1: 
		                ..Awesome OS for x86_64 Bits
		            1.3.6.1.4.1.2312.9.1.100000000000002.3: 
		                ..x86_64
		            1.3.6.1.4.1.2312.9.1.100000000000002.2: 
		                ..3.11
		            1.3.6.1.4.1.2312.9.2.11113.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.11113.1.1: 
		                ..awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.2: 
		                ..awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.5: 
		                ..Red Hat
		            1.3.6.1.4.1.2312.9.2.11113.1.6: 
		                ../path/to/awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.7: 
		                ../path/to/awesomeos/gpg/
		            1.3.6.1.4.1.2312.9.2.11113.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.2.11113.1.9: 
		                ..3600
		            1.3.6.1.4.1.2312.9.2.11124.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.11124.1.1: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.2: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.5: 
		                ..Red Hat
		            1.3.6.1.4.1.2312.9.2.11124.1.6: 
		                ../path/to/awesomeos/x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.7: 
		                ../path/to/awesomeos/gpg/
		            1.3.6.1.4.1.2312.9.2.11124.1.8: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.11124.1.9: 
		                ..3600
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
		            1.3.6.1.4.1.2312.9.2.0.1.8: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.0.1.9: 
		                ..600
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
		            1.3.6.1.4.1.2312.9.2.1.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.2.1.1.9: 
		                ..200
		            1.3.6.1.4.1.2312.9.4.1: 
		                ..Awesome OS for x86_64
		            1.3.6.1.4.1.2312.9.4.2: 
		                . 8a90f8c63282c09a013282c1be32014f
		            1.3.6.1.4.1.2312.9.4.3: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.4.9: 
		                ..1
		            1.3.6.1.4.1.2312.9.4.6: 
		                ..2011-09-19T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.7: 
		                ..2012-09-18T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.12: 
		                ..30
		            1.3.6.1.4.1.2312.9.4.10: 
		                ..30
		            1.3.6.1.4.1.2312.9.4.13: 
		                ..12331131231
		            1.3.6.1.4.1.2312.9.4.14: 
		                ..0
		            1.3.6.1.4.1.2312.9.4.17: 
		                ..1
		            1.3.6.1.4.1.2312.9.4.11: 
		                ..1
		            1.3.6.1.4.1.2312.9.5.1: 
		                .$55bde607-e90d-41bd-b794-7abfe6e34ba7
		    Signature Algorithm: sha1WithRSAEncryption
		        10:4c:f5:71:05:30:8d:cf:7a:a8:c7:16:76:09:ea:5f:55:6c:
		        84:4e:1a:27:87:81:08:68:77:22:34:35:76:81:60:fe:65:4e:
		        a0:87:a8:64:ff:46:ad:c8:6f:0f:13:de:30:92:f2:4f:23:ca:
		        7b:ea:a8:71:f4:98:42:85:e5:56:b7:5e:2a:f0:87:8d:a3:81:
		        63:e6:e5:e8:06:8d:92:9e:07:74:79:ab:41:bc:d8:fc:27:a4:
		        0f:c9:0f:21:f5:0d:b5:fd:5a:eb:e2:4c:45:af:16:32:13:82:
		        ee:be:57:a3:6e:57:d5:26:cd:e0:58:1e:32:4f:6f:5b:9f:c6:
		        45:10
		    File: /etc/pki/entitlement/7359782834343735602.pem
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number:
		            46:4e:35:0d:65:78:23:a6
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: CN=jsefler-onprem-62candlepin.usersys.redhat.com, C=US, L=Raleigh
		        Validity
		            Not Before: Sep 19 00:00:00 2011 GMT
		            Not After : Sep 18 00:00:00 2012 GMT
		        Subject: CN=8a90f8c63282c09a013283ba6a1909e6
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (2048 bit)
		                Modulus:
		                    00:92:e9:4a:dd:a7:94:c7:1f:d3:46:42:10:90:1d:
		                    a6:4f:44:82:14:1b:ba:7a:5a:5e:b4:c5:cb:de:96:
		                    c8:d3:7a:e1:a5:43:f7:52:88:1d:ac:50:6a:bb:2c:
		                    aa:20:42:4b:97:8c:48:56:8d:6e:87:51:ac:22:60:
		                    02:1b:86:77:a7:a6:f9:20:6e:4e:4e:6b:f7:12:54:
		                    04:c0:17:eb:e4:44:05:84:32:88:02:92:39:4b:08:
		                    83:f7:1b:b3:11:48:76:f2:fb:99:36:ad:fc:98:48:
		                    f9:e0:ce:69:80:4d:50:a0:a9:b8:99:70:61:5a:49:
		                    70:56:82:d8:1e:ef:a8:ca:22:e2:9d:79:e5:5c:64:
		                    aa:70:57:06:30:d1:cd:a7:dd:a9:e6:b1:f8:b0:27:
		                    81:0b:72:00:d0:79:a5:13:1d:ac:46:68:bf:94:34:
		                    1f:40:53:e9:46:7a:da:16:82:93:5a:b8:5e:81:76:
		                    46:92:f0:00:e1:d2:bc:80:a5:e4:aa:63:03:4e:75:
		                    07:11:24:2d:65:79:bb:75:b0:50:79:55:c9:a2:93:
		                    06:a7:71:0e:90:ce:71:cf:9b:70:c1:5e:84:f1:23:
		                    ed:1b:48:09:7d:e7:50:5f:0c:4f:c5:91:b6:1d:31:
		                    a7:d8:dd:f1:0c:d5:b1:84:e9:f7:7f:34:7f:31:aa:
		                    8b:79
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            Netscape Cert Type: 
		                SSL Client, S/MIME
		            X509v3 Key Usage: 
		                Digital Signature, Key Encipherment, Data Encipherment
		            X509v3 Authority Key Identifier: 
		                keyid:82:7C:66:96:9E:DB:01:A9:01:A4:32:EE:97:80:26:6D:1F:AC:03:66
		                DirName:/CN=jsefler-onprem-62candlepin.usersys.redhat.com/C=US/L=Raleigh
		                serial:BC:92:AE:6A:DE:86:B6:D6

		            X509v3 Subject Key Identifier: 
		                6C:77:A9:5B:18:8A:7B:D1:DE:77:89:44:A9:60:AF:F0:5E:32:E6:F0
		            X509v3 Extended Key Usage: 
		                TLS Web Client Authentication
		            1.3.6.1.4.1.2312.9.1.100000000000002.1: 
		                ..Awesome OS for x86_64 Bits
		            1.3.6.1.4.1.2312.9.1.100000000000002.3: 
		                ..x86_64
		            1.3.6.1.4.1.2312.9.1.100000000000002.2: 
		                ..3.11
		            1.3.6.1.4.1.2312.9.2.11113.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.11113.1.1: 
		                ..awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.2: 
		                ..awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.5: 
		                ..Red Hat
		            1.3.6.1.4.1.2312.9.2.11113.1.6: 
		                ../path/to/awesomeos
		            1.3.6.1.4.1.2312.9.2.11113.1.7: 
		                ../path/to/awesomeos/gpg/
		            1.3.6.1.4.1.2312.9.2.11113.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.2.11113.1.9: 
		                ..3600
		            1.3.6.1.4.1.2312.9.2.11124.1: 
		                ..yum
		            1.3.6.1.4.1.2312.9.2.11124.1.1: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.2: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.5: 
		                ..Red Hat
		            1.3.6.1.4.1.2312.9.2.11124.1.6: 
		                ../path/to/awesomeos/x86_64
		            1.3.6.1.4.1.2312.9.2.11124.1.7: 
		                ../path/to/awesomeos/gpg/
		            1.3.6.1.4.1.2312.9.2.11124.1.8: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.11124.1.9: 
		                ..3600
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
		            1.3.6.1.4.1.2312.9.2.0.1.8: 
		                ..0
		            1.3.6.1.4.1.2312.9.2.0.1.9: 
		                ..600
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
		            1.3.6.1.4.1.2312.9.2.1.1.8: 
		                ..1
		            1.3.6.1.4.1.2312.9.2.1.1.9: 
		                ..200
		            1.3.6.1.4.1.2312.9.4.1: 
		                ..Awesome OS for x86_64
		            1.3.6.1.4.1.2312.9.4.2: 
		                . 8a90f8c63282c09a013282c1bea20150
		            1.3.6.1.4.1.2312.9.4.3: 
		                ..awesomeos-x86_64
		            1.3.6.1.4.1.2312.9.4.9: 
		                ..1
		            1.3.6.1.4.1.2312.9.4.6: 
		                ..2011-09-19T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.7: 
		                ..2012-09-18T00:00:00Z
		            1.3.6.1.4.1.2312.9.4.12: 
		                ..30
		            1.3.6.1.4.1.2312.9.4.10: 
		                ..31
		            1.3.6.1.4.1.2312.9.4.13: 
		                ..12331131231
		            1.3.6.1.4.1.2312.9.4.14: 
		                ..0
		            1.3.6.1.4.1.2312.9.4.17: 
		                ..1
		            1.3.6.1.4.1.2312.9.4.11: 
		                ..1
		            1.3.6.1.4.1.2312.9.5.1: 
		                .$55bde607-e90d-41bd-b794-7abfe6e34ba7
		    Signature Algorithm: sha1WithRSAEncryption
		        94:86:e3:8d:a7:e8:76:fd:1a:af:7e:77:28:28:a8:03:16:0a:
		        74:0a:fd:3b:95:0e:ca:ea:03:48:ee:b1:65:06:78:a3:eb:40:
		        b2:74:db:91:4c:37:c7:e7:c9:96:9c:20:37:e3:32:74:46:8c:
		        0d:c1:7e:f7:6f:b5:27:37:43:04:56:cb:95:08:a8:7c:ab:f7:
		        9b:f6:5d:a0:54:e3:c2:76:2b:64:ab:2d:50:24:9f:67:1b:39:
		        fd:be:ff:4a:33:ee:c6:80:c4:28:0b:e6:f9:24:3c:07:21:b7:
		        9b:4d:86:95:a2:42:6a:df:32:d5:ad:cd:56:4a:25:3a:7a:32:
		        3e:c0
		    File: /etc/pki/entitlement/5066044962491605926.pem
		[root@jsefler-onprem-62server ~]# 
		*/
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("file",					"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+File: (.+)");
		regexes.put("id",					"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Subject: CN=(.+)");
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Issuer:\\s*(.*)");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+)(?:\\n.*?)+Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");

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

/* FIXME USING TIME ZONE NOTES FROM AJAY
 * SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd");
        iso8601DateFormat.setTimeZone(TimeZone.getDefault());
        System.out.println(iso8601DateFormat.format(new Date()));
        */
