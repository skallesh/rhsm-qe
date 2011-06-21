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
public class ProductCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT

	// abstraction fields
	public BigInteger serialNumber;	// this is the key
	public String rawCertificate;
	public String cn;
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
		
	public List<ProductNamespace> productNamespaces;

	public String productName;	// comes from the ProductNamespace
	public String productId;	// comes from the ProductNamespace


	public ProductCert(BigInteger serialNumber, Map<String, String> certData){
		super(certData);
		this.serialNumber = serialNumber;
		productNamespaces = ProductNamespace.parse(this.rawCertificate);
		// TODO we should assert that there was only one ProductNamespace parsed!
		productName = productNamespaces.get(0).name;	// extract the product name
		productId = productNamespaces.get(0).id;			// extract the hash
	}

	
	
	@Override
	public String toString() {
		
		String string = "";
		if (productName != null)		string += String.format(" %s='%s'", "productName",productName);
		if (productId != null)			string += String.format(" %s='%s'", "productId",productId);
		if (serialNumber != null)		string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (cn != null)					string += String.format(" %s='%s'", "cn",cn);
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
				((ProductCert)obj).cn.equals(this.cn) &&
				((ProductCert)obj).issuer.equals(this.issuer) &&
				((ProductCert)obj).validityNotBefore.equals(this.validityNotBefore) &&
				((ProductCert)obj).validityNotAfter.equals(this.validityNotAfter) &&
				((ProductCert)obj).productName.equals(this.productName);
	}
	
	/**
	 * @param certificates - stdout from "find /etc/pki/product/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @return
	 */
	static public List<ProductCert> parse(String certificates) {
		
		/* [root@jsefler-itclient01 ~]# openssl x509 -noout -text -in /etc/pki/product/2156.pem 
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
		        3e:34:ab:6e:b6:87:6b:34:9c:77:ae:67:96:60:5f:0e:d9:11:
		        78:a6:91:22:b4:72:61:cc:13:f5:ec:4d:6e:61:21:42:73:85:
		        15:81:8b:a6:51:67:08:2e
		 */
		
		/* [root@jsefler-stage-6server product]# openssl x509 -text -in /etc/pki/product/69.pem 
		Certificate:
		    Data:
		        Version: 3 (0x2)
		        Serial Number:
		            b0:f1:44:bb:7f:b5:49:ec
		        Signature Algorithm: sha1WithRSAEncryption
		        Issuer: C=US, ST=North Carolina, O=Red Hat, Inc., OU=Red Hat Network, CN=Red Hat Entitlement Product Authority/emailAddress=ca-support@redhat.com
		        Validity
		            Not Before: Apr 27 19:37:13 2011 GMT
		            Not After : Apr 22 19:37:13 2031 GMT
		        Subject: CN=Red Hat Product ID [a81db0cc-f72c-4386-a60d-dd6d1e037378]
		        Subject Public Key Info:
		            Public Key Algorithm: rsaEncryption
		                Public-Key: (4096 bit)
		                Modulus:
		                    00:c6:3f:49:d3:8c:fe:13:37:72:c7:55:37:de:41:
		                    5f:b4:bb:f4:9e:d3:52:d4:14:9e:ba:1a:19:2c:38:
		                    65:f6:45:ea:7b:5c:0e:74:5d:02:c1:a8:78:37:af:
		                    f7:2f:32:17:10:42:b2:10:9c:4d:a9:0c:2d:35:cd:
		                    88:54:4f
		                Exponent: 65537 (0x10001)
		        X509v3 extensions:
		            X509v3 Basic Constraints: 
		                CA:FALSE
		            1.3.6.1.4.1.2312.9.1.69.1: 
		                .!Red Hat Enterprise Linux 6 Server
		            1.3.6.1.4.1.2312.9.1.69.2: 
		                ..6.1
		            1.3.6.1.4.1.2312.9.1.69.3: 
		                ..x86_64
		            1.3.6.1.4.1.2312.9.1.69.4: 
		                ..rhel-6,rhel-6-server
		    Signature Algorithm: sha1WithRSAEncryption
		        50:8c:61:f7:6c:37:66:52:b1:87:82:ac:fb:2d:24:62:40:d0:
		        34:e8:e5:ba:6b:65:6a:76:fc:77:62:9e:d2:9d:7c:be:b9:38:
		        ed:3f:2f:89:d4:f6:a7:0c:60:7c:a6:17:63:cb:39:13:28:fb:
		        a8:bd:3d:64:9e:c3:13:af:d9:9c:9c:d9:3a:ac:6a:a5:7d:f8:
		        1b:79:8b:59:12:09:72:e1
		-----BEGIN CERTIFICATE-----
		MIIGDzCCA/egAwIBAgIJALDxRLt/tUnsMA0GCSqGSIb3DQEBBQUAMIGuMQswCQYD
		VQQGEwJVUzEXMBUGA1UECAwOTm9ydGggQ2Fyb2xpbmExFjAUBgNVBAoMDVJlZCBI
		XYJsRbON5l4Umk6tcRpBrAT7BX04h6fbMErU0r3j9aiFqcCaXBUWMlfK1YGg8OHS
		ommdlVFsLtPx/cny5ICiMxEWDS8eX5aVpKqdT2LyUD9RrkovEYlER9kWG4QnKOor
		R9PGcixNdxYHpP4/LYmhhNfstBGBZIHtPy+J1PanDGB8phdjyzkTKPuovT1knsMT
		r9mcnNk6rGqlffgbeYtZEgly4Q==
		-----END CERTIFICATE-----
		*/
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("cn",					"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Subject: CN=(.+)");	// FIXME not quite right
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Issuer:\\s*(.*),");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");
		regexes.put("rawCertificate",		"Serial Number:\\s*([\\d\\w:]+).*((?:\\n.*?)*)Signature Algorithm:.*\\s+(?:([a-f]|[\\d]){2}:){10}");	// FIXME THIS IS ONLY PART OF THE CERT

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
