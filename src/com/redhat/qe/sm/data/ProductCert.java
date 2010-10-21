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

public class ProductCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT

	// abstraction fields
	public BigInteger serialNumber;	// this is the key
	public String rawCertificate;
	public String id;
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	public String name;	// comes from the Product Namespace



	public ProductCert(BigInteger serialNumber, Map<String, String> certData){
		super(certData);
		this.serialNumber = serialNumber;
	}

	
	
	@Override
	public String toString() {
		
		String string = "";
		if (name != null)				string += String.format(" %s='%s'", "name",name);
		if (serialNumber != null)		string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)					string += String.format(" %s='%s'", "id",id);
		if (issuer != null)				string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)	string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)	string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
	
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

		return	((ProductCert)obj).serialNumber.equals(this.serialNumber) &&
				((ProductCert)obj).id.equals(this.id) &&
				((ProductCert)obj).issuer.equals(this.issuer) &&
				((ProductCert)obj).validityNotBefore.equals(this.validityNotBefore) &&
				((ProductCert)obj).validityNotAfter.equals(this.validityNotAfter) &&
				((ProductCert)obj).name.equals(this.name);
	}
	
	/**
	 * @param certificates - stdout from "find /etc/pki/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @return
	 */
	static public List<ProductCert> parse(String certificates) {
		
		/*[root@jsefler-itclient01 ~]# openssl x509 -noout -text -in /etc/pki/product/2156.pem 
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            b0:f1:44:bb:7f:b5:48:e1
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: C=US, ST=North Carolina, O=Red Hat, Inc., OU=Red Hat Network, CN=Red Hat Entitlement Product Authority/emailAddress=ca-support@redhat.com
        Validity
            Not Before: Oct  5 14:10:38 2010 GMT
            Not After : Nov  4 14:10:38 2010 GMT
        Subject: CN=Red Hat Product ID [ed92ebfb-e599-4d15-8383-1fcaed979211]
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (4096 bit)
                Modulus:
                    00:c6:3f:49:d3:8c:fe:13:37:72:c7:55:37:de:41:
                    5f:b4:bb:f4:9e:d3:52:d4:14:9e:ba:1a:19:2c:38:
                    6c:d7:cc:a4:b1:ea:46:ee:15:d7:b4:78:76:08:c1:
                    5f:2d:99:93:8c:0c:8b:f6:2d:57:b3:1c:a4:42:95:
                    50:75:74:c6:a5:41:77:dc:2d:aa:05:01:c1:e6:09:
                    58:b3:df:9d:ef:cd:71:f2:9f:26:f3:31:71:6c:f7:
                    16:f3:64:c8:25:76:e0:5b:71:2b:56:1f:2f:93:9a:
                    82:6c:6d:5c:08:00:07:6f:e0:d6:32:ad:04:03:2c:
                    b5:6e:5f:c9:80:08:21:60:d1:81:46:f2:89:39:b4:
                    dd:0a:b7:69:62:1d:36:29:4a:81:2e:44:8f:64:7b:
                    e8:e8:35:09:14:dd:fb:31:70:e9:55:e4:2a:f5:5b:
                    6a:46:3a:4a:2c:bc:2e:11:f5:db:d1:8e:c8:e7:11:
                    0e:ad:1d:e4:61:b3:9a:04:05:95:b7:79:98:67:5b:
                    74:2f:f2:9f:63:68:d5:39:d5:39:58:5c:b2:07:d3:
                    e1:6c:c7:4b:8b:5c:44:f3:4d:63:f8:62:6b:c1:c2:
                    da:c6:6a:af:8f:85:1a:20:24:73:70:f0:13:3f:ce:
                    b3:54:cd:41:05:06:be:96:29:64:45:07:ac:e4:7c:
                    a3:67:36:43:88:66:2e:76:75:e1:6e:a9:8b:a3:f9:
                    f4:72:e5:e8:40:15:55:8e:1c:d1:4c:cc:7b:d4:48:
                    a2:6a:19:a2:c3:e5:35:bc:6a:a4:1e:14:0d:c5:bd:
                    77:1e:d3:75:95:c0:21:50:20:eb:c7:17:8c:be:b0:
                    23:61:d5:a9:62:5a:48:c9:6d:cd:b2:c3:d6:b7:56:
                    14:1e:27:4c:05:20:09:e0:a7:2d:21:ff:75:77:21:
                    3d:dd:a4:ad:97:1c:02:fd:09:f2:16:c6:4e:99:c1:
                    2c:07:35:42:9f:3f:46:99:63:25:d7:fe:97:cc:14:
                    b5:c8:35:2a:07:29:25:c7:44:cb:1f:ec:ff:b0:af:
                    40:f8:ed:ab:64:0c:b5:98:1c:82:63:05:71:bd:b3:
                    99:86:7a:86:c4:0b:93:a0:84:be:03:cd:6f:e6:10:
                    a3:b0:28:8e:49:c5:41:f9:c8:a5:df:46:01:bb:47:
                    07:f3:94:59:d0:82:cd:90:77:4a:76:b2:cb:59:6e:
                    30:8e:98:4a:da:70:67:da:0d:a2:df:ec:ed:7f:e9:
                    d0:10:3d:a9:42:a6:7f:ae:1b:96:ef:d8:dc:c8:26:
                    65:f6:45:ea:7b:5c:0e:74:5d:02:c1:a8:78:37:af:
                    f7:2f:32:17:10:42:b2:10:9c:4d:a9:0c:2d:35:cd:
                    88:54:4f
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            X509v3 Basic Constraints: 
                CA:FALSE
            1.3.6.1.4.1.2312.9.1.2156.1: 
                .*Red Hat Enterprise Linux Entitlement Alpha
            1.3.6.1.4.1.2312.9.1.2156.2: 
                ..
            1.3.6.1.4.1.2312.9.1.2156.3: 
                ..
            1.3.6.1.4.1.2312.9.1.2156.4: 
                ..
    Signature Algorithm: sha1WithRSAEncryption
        86:a2:c3:72:5a:5a:61:df:f0:d4:0d:ca:ac:14:8c:8c:46:8b:
        7a:b8:46:53:40:6b:3b:15:dd:05:ec:3f:d4:d5:ab:6c:3c:ba:
        80:62:b9:32:97:53:f6:f8:12:62:03:80:eb:97:e1:54:da:fe:
        06:7f:5f:f5:63:58:aa:5c:8c:0d:32:b0:c7:e6:87:3c:9a:8e:
        1c:47:1f:d0:60:7e:59:c3:02:a6:4e:64:97:ba:00:33:5e:a9:
        d8:84:43:fc:90:ff:98:b9:26:43:91:0c:ca:7f:2f:46:1f:51:
        be:59:c1:6d:47:a6:ca:fa:1c:00:c9:e0:4b:ce:01:29:22:35:
        56:84:06:32:f3:54:09:f3:2b:d0:10:c2:d1:81:52:1d:a3:50:
        7e:6a:b4:36:b0:b6:c0:73:7f:6b:c2:ae:f1:63:e2:c8:a8:09:
        5e:bd:16:d4:71:bc:99:4b:34:9b:2c:18:3d:01:bc:8b:a4:f4:
        15:ec:32:d9:45:26:bc:f0:46:67:f9:cd:02:2a:ae:ba:10:c0:
        00:3d:48:02:13:e5:29:58:0c:2c:e8:1f:a9:4d:2e:d5:ad:bb:
        ad:1a:cc:70:90:a3:6d:0c:15:91:82:0d:b5:c7:c6:cf:f8:83:
        1e:c4:cb:69:e1:48:94:d8:e5:34:39:c0:bc:e7:d3:d1:3a:2a:
        50:db:2e:d6:10:4c:65:03:d5:a6:ed:90:b2:69:5e:22:b2:85:
        df:e7:9c:5c:f5:97:be:eb:4d:be:9c:ad:ac:50:ae:55:e6:e7:
        e0:5b:d1:99:a4:b3:49:6a:d2:ad:15:fd:f5:d3:04:c6:8c:2e:
        ef:84:09:4e:f7:29:5b:49:11:ae:4a:95:63:c0:63:0e:6d:10:
        7d:38:a8:db:2a:9d:82:fb:ea:35:88:60:53:46:86:81:c2:8e:
        4a:ab:81:87:9f:4c:5e:56:dd:a2:0e:80:31:73:86:b4:05:d0:
        58:ab:93:97:c0:ff:c4:a9:6e:1e:1f:82:00:68:51:a3:a7:12:
        80:33:6a:be:75:92:e2:9e:3f:ce:4c:cc:af:55:73:c6:de:39:
        60:fd:ac:b9:da:aa:51:63:c4:98:96:0b:38:db:98:bd:a9:c1:
        1c:3d:14:3b:b1:4f:21:94:67:b9:88:7a:5c:5d:30:db:dc:70:
        ff:41:cb:71:7e:fe:b2:6b:5f:1d:84:ee:76:01:68:6f:3e:a5:
        f0:35:2e:35:81:07:ba:d9:be:b2:16:65:48:76:00:8f:5d:62:
        3e:34:ab:6e:b6:87:6b:34:9c:77:ae:67:96:60:5f:0e:d9:11:
        78:a6:91:22:b4:72:61:cc:13:f5:ec:4d:6e:61:21:42:73:85:
        15:81:8b:a6:51:67:08:2e

		 */
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("id",					"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Subject: CN=(.+)");	// FIXME not quite right
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Issuer:\\s*(.*),");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");
		regexes.put("name",					"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(?:\\d+)\\.1:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("rawCertificate",		"Serial Number:\\s*([\\d\\w:]+).*((?:\\n.*?)*).1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.1\\.(?:\\d+)\\.4:");	// FIXME THIS IS ONLY PART OF THE CERT

		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, certificates, productMap, field);
		}
		
		List<ProductCert> productCerts = new ArrayList<ProductCert>();
		for(String key : productMap.keySet()) {
			
			// convert the key inside the raw cert file (04:02:7b:dc:b7:fb:33) to a numeric serialNumber (11286372344531148)
			//Long serialNumber = Long.parseLong(key.replaceAll(":", ""), 16);
			BigInteger serialNumber = new BigInteger(key.replaceAll(":", ""),16);
		
			productCerts.add(new ProductCert(serialNumber, productMap.get(key)));
		}
		return productCerts;
	}
}
