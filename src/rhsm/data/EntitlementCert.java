package rhsm.data;

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

import com.redhat.qe.Assert;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class EntitlementCert extends AbstractCommandLineData {
//	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";		// Aug 23 08:42:00 2010 GMT   validityNotBefore
//	protected static String simpleDateFormat = "yyyy-MM-dd HH:mm:ssZZZ";	// 2012-09-11 00:00:00+00:00 validityNotBefore from rct cat-cert

	// abstraction fields
	public BigInteger serialNumber;	// this is the key
	public String id;				// entitlement uuid on the candlepin server
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	public File file;
	public String version;
	
	public String serialString;
	public OrderNamespace orderNamespace;
	public List<ProductNamespace> productNamespaces;
	public List<ContentNamespace> contentNamespaces;
	public String rawCertificate;


	public EntitlementCert(String rawCertificate, Map<String, String> certData){
		super(certData);		
		this.rawCertificate = rawCertificate;
		if (this.serialString.contains(":")) {	// 28:18:c4:bc:b0:34:68
			this.serialNumber = new BigInteger(serialString.replaceAll(":", ""),16);	// strip out the colons and convert to a number
		} else {
			this.serialNumber = new BigInteger(serialString);
		}
		this.orderNamespace = OrderNamespace.parse(rawCertificate);
		this.productNamespaces = ProductNamespace.parse(rawCertificate);
		this.contentNamespaces = ContentNamespace.parse(rawCertificate);
	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (serialNumber != null)			string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)						string += String.format(" %s='%s'", "id",id);
		if (issuer != null)					string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)		string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)		string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
		if (file != null)					string += String.format(" %s='%s'", "file",file);
		if (version != null)				string += String.format(" %s='%s'", "version",version);
	
		return string.trim();
	}
	
	@Override
	protected Calendar parseDateString(String dateString){
		
		// make an educational guess at what simpleDateFormat should be used to parse this dateString
		String simpleDateFormatOverride;
		if (dateString.matches("[A-Za-z]{3} +[0-9]{1,2} [0-9]{2}:[0-9]{2}:[0-9]{2} [0-9]{4} \\w+")) {
			simpleDateFormatOverride = "MMM d HH:mm:ss yyyy z";		// used in certv1	// Aug 23 08:42:00 2010 GMT		// Oct  6 17:56:06 2012 GMT
		}
		else if (dateString.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}[+-][0-9]{2}:?[0-9]{2}")) {
			simpleDateFormatOverride = "yyyy-MM-dd HH:mm:ssZZZ";	// used in certv2	// 2012-09-11 00:00:00+00:00		
			dateString = dateString.replaceFirst("([-\\+]\\d{2}):(\\d{2})$", "$1$2");	// strip the ":" from the time zone to avoid a parse exception (e.g "2012-09-11 00:00:00+00:00" => "2012-09-11 00:00:00+0000")
		}
		else {
			log.warning("Cannot determine a simpleDateFormat to use for parsing dateString '"+dateString+"'.  Using default format '"+simpleDateFormat+"'.");
			simpleDateFormatOverride = simpleDateFormat;
		}
			
		return super.parseDateString(dateString, simpleDateFormatOverride);
	}
	
	//@Override
	public static String formatDateString(Calendar date){
		String simpleDateFormatOverride = simpleDateFormat;
		simpleDateFormatOverride = "MMM d yyyy HH:mm:ss z"; // "yyyy-MM-dd HH:mm:ssZZZ";	// can really be any useful format
		DateFormat dateFormat = new SimpleDateFormat(simpleDateFormatOverride);
		return dateFormat.format(date.getTime());
	}
	
/* 9/14/2012 jsefler believes the super.equals() is now smart enough to handle this
	@Override
	public boolean equals(Object obj){
	
//		return	((EntitlementCert)obj).serialNumber.equals(this.serialNumber) &&
//				((EntitlementCert)obj).id.equals(this.id) &&
//				((EntitlementCert)obj).issuer.equals(this.issuer) &&
//				((EntitlementCert)obj).validityNotBefore.equals(this.validityNotBefore) &&
//				((EntitlementCert)obj).validityNotAfter.equals(this.validityNotAfter) &&
//				((EntitlementCert)obj).orderNamespace.productId.equals(this.orderNamespace.productId);
 
		// exclude orderNamespace when comparing entitlements
		OrderNamespace thisOrderNamespace = this.orderNamespace;
		OrderNamespace thatOrderNamespace = ((EntitlementCert)obj).orderNamespace;
		this.orderNamespace = null;
		((EntitlementCert)obj).orderNamespace = null;
		
		List<ProductNamespace> thisProductNamespaces = this.productNamespaces;
		List<ProductNamespace> thatProductNamespaces = ((EntitlementCert)obj).productNamespaces;
		this.productNamespaces = null;
		((EntitlementCert)obj).productNamespaces = null;
		
		List<ContentNamespace> thisCotentNamespaces = this.contentNamespaces;
		List<ContentNamespace> thatCotentNamespaces = ((EntitlementCert)obj).contentNamespaces;
		this.contentNamespaces = null;
		((EntitlementCert)obj).contentNamespaces = null;
		
		boolean equals = super.equals(obj);
		
		// restore orderNamespace before returning;
		this.orderNamespace = thisOrderNamespace;
		((EntitlementCert)obj).orderNamespace = thatOrderNamespace;
		
		this.productNamespaces = thisProductNamespaces;
		((EntitlementCert)obj).productNamespaces = thatProductNamespaces;
		
		this.contentNamespaces = thisCotentNamespaces;
		((EntitlementCert)obj).contentNamespaces = thatCotentNamespaces;
		
		return equals;
	}
*/
	
	/**
	 * @param certificates - OLD WAY: stdout from "find /etc/pki/entitlement/ -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text"
	 * @param certificates - stdout from: find /etc/pki/entitlement/ -name '*.pem' -exec openssl x509 -in '{}' -noout -text \; -exec echo "    File: {}" \;
	 * @return
	 */
	static public List<EntitlementCert> parseStdoutFromOpensslX509(String rawCertificates) {
		
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
		
	
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		
		// begin by splitting the rawCertificates and processing each certificate individually
		String certificateDelimiter = "Certificate:";
		for (String rawCertificate : rawCertificates.split(certificateDelimiter)) {
			if (rawCertificate.trim().length()==0) continue;
			rawCertificate = certificateDelimiter+rawCertificate;
	
			Map<String,String> regexes = new HashMap<String,String>();

			// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
			regexes.put("serialString",			"Serial Number:\\s*([\\d\\w:]+)");
			regexes.put("id",					"Subject: CN=(.+)");
			regexes.put("issuer",				"Issuer:\\s*(.*)");
			regexes.put("validityNotBefore",	"Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
			regexes.put("validityNotAfter",		"Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");
			regexes.put("file",					"File: (.+)");

			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawCertificate, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the map
			if (certDataList.size()!=1) Assert.fail("Error when parsing raw entitlement certificates.");
			Map<String,String> certData = certDataList.get(0);
			
			// create a new EntitlementCert
			entitlementCerts.add(new EntitlementCert(rawCertificate, certData));
		}
		
		return entitlementCerts;
		
	}
	
	
	
	/**
	 * @param rawCertificates - stdout from: # find /etc/pki/entitlement/ -regex "/.+/[0-9]+.pem" -exec rct cat-cert {} \;
	 * @return
	 */
	static public List<EntitlementCert> parse(String rawCertificates) {
		
		//	[root@jsefler-rhel59 ~]# find /etc/pki/entitlement/ -regex "/.+/[0-9]+.pem" -exec rct cat-cert {} \;
		//
		//	+-------------------------------------------+
		//		Entitlement Certificate
		//	+-------------------------------------------+
		//
		//	Certificate:
		//		Path: /etc/pki/entitlement/6828740431923393274.pem
		//		Version: 1.0
		//		Serial: 6828740431923393274
		//		Start Date: 2012-09-09 00:00:00+00:00
		//		End Date: 2013-09-09 00:00:00+00:00
		//
		//	Subject:
		//		CN: 8a90f81d39ad82140139b21e6cbc39f6
		//
		//	Product:
		//		ID: 27060
		//		Name: Awesome OS Workstation Bits
		//		Version: 6.1
		//		Arch: ALL
		//		Tags: 
		//
		//	Order:
		//		Name: Awesome OS Workstation Basic
		//		Number: 8a90f81d39ad82140139ad83132500bd
		//		SKU: awesomeos-workstation-basic
		//		Contract: 24
		//		Account: 12331131231
		//		Service Level: Standard
		//		Service Type: L1-L3
		//		Quantity: 5
		//		Quantity Used: 1
		//		Socket Limit: 2
		//		Virt Limit: 
		//		Virt Only: False
		//		Subscription: 
		//		Stacking ID: 
		//		Warning Period: 30
		//		Provides Management: 0
		//
		//	Content:
		//		Name: always-enabled-content
		//		Label: always-enabled-content
		//		Vendor: test-vendor
		//		URL: /foo/path/always/$releasever
		//		GPG: /foo/path/always/gpg
		//		Enabled: True
		//		Expires: 200
		//		Required Tags: 
		//
		//	Content:
		//		Name: content
		//		Label: content-label
		//		Vendor: test-vendor
		//		URL: /foo/path
		//		GPG: /foo/path/gpg/
		//		Enabled: True
		//		Expires: 0
		//		Required Tags: 
		//
		//	Content:
		//		Name: never-enabled-content
		//		Label: never-enabled-content
		//		Vendor: test-vendor
		//		URL: /foo/path/never
		//		GPG: /foo/path/never/gpg
		//		Enabled: False
		//		Expires: 600
		//		Required Tags: 
		//
		//	+-------------------------------------------+
		//		Entitlement Certificate
		//	+-------------------------------------------+
		//
		//	Certificate:
		//		Path: /etc/pki/entitlement/6246850629384790696.pem
		//		Version: 1.0
		//		Serial: 6246850629384790696
		//		Start Date: 2012-09-09 00:00:00+00:00
		//		End Date: 2013-09-09 00:00:00+00:00
		//
		//	Subject:
		//		CN: 8a90f81d39ad82140139b1b76ad939c5
		//
		//	Product:
		//		ID: 37069
		//		Name: Management Bits
		//		Version: 1.0
		//		Arch: ALL
		//		Tags: 
		//	Product:
		//		ID: 37070
		//		Name: Load Balancing Bits
		//		Version: 1.0
		//		Arch: ALL
		//		Tags: 
		//	Product:
		//		ID: 37067
		//		Name: Shared Storage Bits
		//		Version: 1.0
		//		Arch: ALL
		//		Tags: 
		//	Product:
		//		ID: 37060
		//		Name: Awesome OS Server Bits
		//		Version: 6.1
		//		Arch: ALL
		//		Tags: 
		//	Product:
		//		ID: 37065
		//		Name: Clustering Bits
		//		Version: 1.0
		//		Arch: ALL
		//		Tags: 
		//	Product:
		//		ID: 37068
		//		Name: Large File Support Bits
		//		Version: 1.0
		//		Arch: ALL
		//		Tags: 
		//
		//	Order:
		//		Name: Awesome OS Server Bundled
		//		Number: 8a90f81d39ad82140139ad82f8220085
		//		SKU: awesomeos-server
		//		Contract: 6
		//		Account: 12331131231
		//		Service Level: Premium
		//		Service Type: Level 3
		//		Quantity: 5
		//		Quantity Used: 1
		//		Socket Limit: 2
		//		Virt Limit: 
		//		Virt Only: False
		//		Subscription: 
		//		Stacking ID: 
		//		Warning Period: 30
		//		Provides Management: 1
		//
		//	Content:
		//		Name: always-enabled-content
		//		Label: always-enabled-content
		//		Vendor: test-vendor
		//		URL: /foo/path/always/$releasever
		//		GPG: /foo/path/always/gpg
		//		Enabled: True
		//		Expires: 200
		//		Required Tags: 
		//
		//	Content:
		//		Name: content
		//		Label: content-label
		//		Vendor: test-vendor
		//		URL: /foo/path
		//		GPG: /foo/path/gpg/
		//		Enabled: True
		//		Expires: 0
		//		Required Tags: 
		//
		//	Content:
		//		Name: content-emptygpg
		//		Label: content-label-empty-gpg
		//		Vendor: test-vendor
		//		URL: /foo/path
		//		GPG: 
		//		Enabled: True
		//		Expires: 0
		//		Required Tags: 
		//
		//	Content:
		//		Name: content-nogpg
		//		Label: content-label-no-gpg
		//		Vendor: test-vendor
		//		URL: /foo/path
		//		GPG: 
		//		Enabled: True
		//		Expires: 0
		//		Required Tags: 
		//
		//	Content:
		//		Name: never-enabled-content
		//		Label: never-enabled-content
		//		Vendor: test-vendor
		//		URL: /foo/path/never
		//		GPG: /foo/path/never/gpg
		//		Enabled: False
		//		Expires: 600
		//		Required Tags: 
		//
		//	Content:
		//		Name: tagged-content
		//		Label: tagged-content
		//		Vendor: test-vendor
		//		URL: /foo/path/always
		//		GPG: /foo/path/always/gpg
		//		Enabled: True
		//		Expires: 
		//		Required Tags: TAG1, TAG2

		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("id",					"Subject:(?:(?:\\n.+)+)CN: (.+)");
		//regexes.put("issuer",				"Issuer:\\s*(.*)");
		regexes.put("serialString",			"Certificate:(?:(?:\\n.+)+)Serial: (.+)");
		regexes.put("validityNotBefore",	"Certificate:(?:(?:\\n.+)+)Start Date: (.+)");
		regexes.put("validityNotAfter",		"Certificate:(?:(?:\\n.+)+)End Date: (.+)");
		regexes.put("file",					"Certificate:(?:(?:\\n.+)+)Path: (.+)");
		regexes.put("version",				"Certificate:(?:(?:\\n.+)+)Version: (.+)");
		
		// split the rawCertificates process each individual rawCertificate
		String rawCertificateRegex = "\\+-+\\+\\n\\s+Entitlement Certificate\\n\\+-+\\+";
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		for (String rawCertificate : rawCertificates.split(rawCertificateRegex)) {
			
			// strip leading and trailing blank lines and skip blank rawCertificates
			rawCertificate = rawCertificate.replaceAll("^\\n*","").replaceAll("\\n*$", "");
			if (rawCertificate.length()==0) continue;
			
			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawCertificate, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the list
			if (certDataList.size()!=1) Assert.fail("Error when parsing raw entitlement certificate.  Expected to parse only one group of certificate data.");
			Map<String,String> certData = certDataList.get(0);
			
			// create a new EntitlementCert
			entitlementCerts.add(new EntitlementCert(rawCertificate, certData));
		}
		return entitlementCerts;
	}
}


/* FIXME USING TIME ZONE NOTES FROM AJAY
 * SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd");
 * iso8601DateFormat.setTimeZone(TimeZone.getDefault());
 * System.out.println(iso8601DateFormat.format(new Date()));
 */

// CertV2 Notes from jmolet for parsing possibilities using python:
// version 1.0 certificate parsing using python
// python -c "from rhsm import certificate; cert=certificate.create_from_file('/etc/pki/entitlement/5814667605372527909.pem');print cert.x509.get_all_extensions();" 
// version 2.0 certificate parsing using python
// python -c "from rhsm import certificate; import zlib; cert=certificate.create_from_file('/etc/pki/entitlement/5814667605372527909.pem');print zlib.decompress(cert.x509.get_all_extensions()['1.3.6.1.4.1.2312.9.2.0.1.7']);"
// 
// To run these python commands interactively...
//	[root@jsefler-rhel59 ~]# yum install -y -q ipython --enablerepo=epel
//	This system is not registered with RHN.
//	RHN Satellite or RHN Classic support will be disabled.
//	
//	[root@jsefler-rhel59 ~]# ipython 
//	**********************************************************************
//	Welcome to IPython. I will try to create a personal configuration directory
//	where you can customize many aspects of IPython's functionality in:
//	
//	/root/.ipython
//	Initializing from configuration /usr/lib/python2.4/site-packages/IPython/UserConfig
//	
//	Successful installation!
//	
//	Please read the sections 'Initial Configuration' and 'Quick Tips' in the
//	IPython manual (there are both HTML and PDF versions supplied with the
//	distribution) to make sure that your system environment is properly configured
//	to take advantage of IPython's features.
//	
//	Important note: the configuration system has changed! The old system is
//	still in place, but its setting may be partly overridden by the settings in 
//	"~/.ipython/ipy_user_conf.py" config file. Please take a look at the file 
//	if some of the new settings bother you. 
//	
//	
//	Please press <RETURN> to start IPython.
//	**********************************************************************
//	Python 2.4.3 (#1, Jun 20 2012, 20:08:35) 
//	Type "copyright", "credits" or "license" for more information.
//	
//	IPython 0.8.4 -- An enhanced Interactive Python.
//	?         -> Introduction and overview of IPython's features.
//	%quickref -> Quick reference.
//	help      -> Python's own help system.
//	object?   -> Details about 'object'. ?object also works, ?? prints more.
//	
//	In [1]: from rhsm import certificate
//	
//	In [2]: cert=certificate.create_from_file('/etc/pki/entitlement/1562018864567338600.pem')
//	
//	In [3]: print cert.x509.get_all_extensions()
//	{'1.3.6.1.4.1.2312.9.4.11': '1', '1.3.6.1.4.1.2312.9.4.2': '8a90f81d3922796c0139227b9337022a', '1.3.6.1.4.1.2312.9.4.13': '12331131231', '1.3.6.1.4.1.2312.9.4.12': '0', '1.3.6.1.4.1.2312.9.4.15': 'Layered', '1.3.6.1.4.1.2312.9.1.88888.2': '1.0', '1.3.6.1.4.1.2312.9.4.10': '127', '2.16.840.1.113730.1.1': 'SSL Client, S/MIME', '1.3.6.1.4.1.2312.9.1.88888.3': 'ALL', '2.5.29.35': 'keyid:01:E2:98:5D:6B:A7:92:7E:FB:CD:5D:97:D6:33:5E:DB:71:DD:78:61\nDirName:/CN=jsefler-f14-candlepin.usersys.redhat.com/C=US/L=Raleigh\nserial:B6:FD:5D:9F:75:59:3A:18\n', '2.5.29.37': 'TLS Web Client Authentication', '1.3.6.1.4.1.2312.9.4.5': '10', '1.3.6.1.4.1.2312.9.5.1': '20803327-e171-4660-883e-6c0a5b9ecb7e', '1.3.6.1.4.1.2312.9.4.7': '2013-08-13T00:00:00Z', '1.3.6.1.4.1.2312.9.4.6': '2012-08-13T00:00:00Z', '1.3.6.1.4.1.2312.9.4.1': 'Shared File System', '1.3.6.1.4.1.2312.9.1.88888.1': 'Shared File System Bits', '2.5.29.14': '\xf5\x04 %\xcc{\x8a\x15S\x05#P#\x01\x0b>\xf5V\x98\xe6', '2.5.29.15': 'Digital Signature, Key Encipherment, Data Encipherment', '1.3.6.1.4.1.2312.9.4.3': 'sfs', '1.3.6.1.4.1.2312.9.4.14': '0'}
//	
//	In [4]: print cert.    <============= USE TAB COMPLETION TO SEE ALL THE POSSIBILITIES
//	cert.__class__         cert.__doc__           cert.__module__        cert.__repr__          cert.content           cert.is_valid          cert.serial            cert.version
//	cert.__cmp__           cert.__getattribute__  cert.__new__           cert.__setattr__       cert.delete            cert.order             cert.start             cert.write
//	cert.__delattr__       cert.__hash__          cert.__reduce__        cert.__str__           cert.end               cert.path              cert.subject           cert.x509
//	cert.__dict__          cert.__init__          cert.__reduce_ex__     cert.__weakref__       cert.is_expired        cert.products          cert.valid_range       
//	
//	In [4]: print cert.serial
//	1562018864567338600


