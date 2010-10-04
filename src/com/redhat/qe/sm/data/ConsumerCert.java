package com.redhat.qe.sm.data;

import java.text.DateFormat;
import java.text.ParseException;
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
public class ConsumerCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT

	// abstraction fields
	public String consumerid;
	public String username;
	public String issuer;
	public Integer serialNumber;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;



	public ConsumerCert(Map<String, String> crlData) {
		super(crlData);
		// TODO Auto-generated constructor stub
		
		// Overridden fields
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (consumerid != null)			string += String.format(" %s='%s'", "consumerid",consumerid);
		if (username != null)			string += String.format(" %s='%s'", "username",username);
		if (issuer != null)				string += String.format(" %s='%s'", "issuer",issuer);
		if (serialNumber != null)		string += String.format(" %s='%s'", "serialNumber",serialNumber);
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

		return	((ConsumerCert)obj).consumerid.equals(this.consumerid) &&
				((ConsumerCert)obj).username.equals(this.username) &&
				((ConsumerCert)obj).issuer.equals(this.issuer) &&
				((ConsumerCert)obj).serialNumber.equals(this.serialNumber) &&
				((ConsumerCert)obj).validityNotBefore.equals(this.validityNotBefore) &&
				((ConsumerCert)obj).validityNotAfter.equals(this.validityNotAfter);
	}
	
	/**
	 * @param crls - stdout from "openssl x509 -noout -text -in /etc/pki/consumer/cert.pem"
	 * @return
	 * @throws ParseException 
	 */
	static public ConsumerCert parse(String stdoutFromOpensslx509) {
		
		/* [root@jsefler-rhel6-client01 pki]# openssl x509 -noout -text -in /etc/pki/consumer/cert.pem 
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number: 11 (0xb)
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
        Validity
            Not Before: Aug 24 14:38:10 2010 GMT
            Not After : Aug 24 14:38:10 2011 GMT
        Subject: CN=094a4cab-5347-4761-8d09-a7e556301768
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:d2:61:3b:26:db:b5:f4:2f:fa:33:c2:e1:5d:fe:
                    07:61:5e:a1:a2:95:aa:46:54:f3:86:b9:38:51:12:
                    6c:74:0d:e2:44:3f:ba:96:20:6b:11:55:6d:ff:07:
                    c0:7c:af:0b:ae:81:59:23:27:a1:51:af:87:c4:67:
                    2c:95:51:32:6a:d6:07:c1:85:ed:21:bd:8d:ae:26:
                    7a:3b:7a:51:f4:f0:6e:7b:25:5d:eb:d4:71:7a:84:
                    73:62:a4:1e:3e:8b:b8:4c:ae:87:8a:77:a6:25:79:
                    16:19:24:93:4f:0c:51:0e:fe:83:73:c8:8e:d2:88:
                    24:4e:cf:66:52:fb:69:df:e4:29:a3:80:43:8e:dc:
                    06:99:36:8f:f1:41:a0:c8:c0:c4:1f:ea:6f:76:61:
                    5b:1f:d0:78:c5:3d:a2:3e:50:cc:b5:70:af:d4:aa:
                    58:b7:ff:77:fa:7f:5d:2f:22:d9:82:74:07:16:42:
                    49:a5:70:7b:bc:9a:78:0d:80:4b:64:7a:7c:df:14:
                    1c:14:17:59:29:e4:36:f0:3c:54:b7:be:f0:54:7b:
                    c1:23:1c:bc:45:c0:ec:9c:8a:26:2b:72:fa:c7:78:
                    20:0c:2e:ff:79:3c:43:13:e5:fe:cb:db:00:1b:1d:
                    11:b5:31:21:ac:45:7c:16:1e:30:50:96:06:94:8c:
                    da:bb
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            Netscape Cert Type: 
                SSL Client, S/MIME
            X509v3 Key Usage: 
                Digital Signature, Key Encipherment, Data Encipherment
            X509v3 Authority Key Identifier: 
                keyid:17:85:AA:04:72:A2:49:CB:CB:26:A5:D7:00:BE:4D:45:56:61:A2:0B
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:98:B4:35:F2:6C:9A:91:D7

            X509v3 Subject Key Identifier: 
                34:44:42:CC:CA:B6:B7:41:5B:61:A8:58:A9:B4:EE:9A:C6:73:79:40
            X509v3 Extended Key Usage: 
                TLS Web Client Authentication
            X509v3 Subject Alternative Name: 
                DirName:/CN=testuser1
    Signature Algorithm: sha1WithRSAEncryption
        68:c3:3d:07:64:88:92:94:c0:93:3f:79:1f:e4:eb:ac:84:44:
        64:c6:66:00:c3:fc:8b:c5:dd:0f:cf:35:7b:b1:ab:6c:c6:db:
        2b:2c:69:45:0c:0e:19:4f:5d:e1:3b:86:0b:31:9d:e3:d5:8a:
        32:4c:d9:d4:53:69:65:e0:99:cf:a8:db:a3:f2:19:2e:03:c4:
        7a:fa:f8:2d:81:e4:49:fe:6b:7a:fd:a9:6a:da:a4:d3:b8:c9:
        fe:b1:22:3a:dc:3d:cb:52:f8:10:f8:75:32:8f:cf:5e:48:f6:
        da:21:e7:3d:83:82:26:9d:5d:31:59:17:81:64:00:57:7a:8e:
        3a:e2

		 */
		
		/* [root@jsefler-rhel6-client01 pki]# openssl x509 -noout -text -in /etc/pki/consumer/cert.pem 
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            28:18:c4:bc:b0:34:68
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
        Validity
            Not Before: Oct  4 22:45:26 2010 GMT
            Not After : Oct  4 23:59:59 2011 GMT
        Subject: CN=036c775b-c63e-4d34-9dbc-f62f098f8ba6
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:d4:1d:dd:34:5b:e8:dc:3d:3e:a8:b8:66:15:8a:
                    f8:a3:2e:74:6a:9c:89:3a:06:5c:c8:51:3f:32:fc:
                    f8:99:0b:cb:b9:12:b7:4b:79:a8:45:23:74:33:75:
                    ef:10:aa:2b:9b:01:71:88:7e:c5:ef:09:b0:aa:3e:
                    a5:69:34:83:55:79:63:2c:7d:0b:25:ac:2b:22:2e:
                    a3:bb:f3:16:2b:c6:bd:62:83:88:79:12:cf:d8:d3:
                    f9:de:ed:a9:ef:4b:e7:99:98:b6:74:2d:ee:b1:4b:
                    c3:8b:66:57:eb:8f:b1:dc:a1:c9:ad:b9:0c:14:eb:
                    27:99:f9:db:6b:a3:82:48:c2:1d:1f:5f:e5:fe:d2:
                    ad:24:ee:5f:54:d9:62:cd:84:48:7f:0e:a3:01:c9:
                    00:9a:6d:a5:19:ce:e9:db:a6:a8:fa:db:70:f6:5f:
                    13:98:9b:c7:f1:e6:84:36:52:4a:b8:a0:c1:5a:bf:
                    44:6f:e5:a4:fc:11:fb:65:ab:e7:9e:0b:98:b3:81:
                    93:b7:ab:fb:80:06:f5:2d:be:51:32:d7:46:3e:eb:
                    cc:2e:f5:8a:ca:24:e7:11:b5:47:f8:8d:55:43:45:
                    1c:d8:2f:ec:a9:4c:f7:c8:92:1a:a0:ff:f5:ec:3d:
                    38:57:1a:f4:25:5d:e0:e1:b1:ab:b0:2e:fd:c2:91:
                    9a:e1
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            Netscape Cert Type: 
                SSL Client, S/MIME
            X509v3 Key Usage: 
                Digital Signature, Key Encipherment, Data Encipherment
            X509v3 Authority Key Identifier: 
                keyid:4F:08:F7:36:8D:EB:B1:3A:0D:3D:70:48:9F:1B:E7:A8:81:D5:09:A6
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:C6:A9:65:BE:53:A5:69:46

            X509v3 Subject Key Identifier: 
                5B:04:CB:D6:0D:16:31:79:9A:F9:F1:F7:E5:B7:6E:0B:EF:3D:01:E6
            X509v3 Extended Key Usage: 
                TLS Web Client Authentication
            X509v3 Subject Alternative Name: 
                DirName:/CN=testuser1
    Signature Algorithm: sha1WithRSAEncryption
        42:e4:b4:02:4d:b5:80:4b:62:67:3f:c1:56:5e:3c:d2:8d:77:
        c7:1c:34:24:52:6f:1b:d6:a5:ed:ce:14:fa:6a:0f:4c:df:ff:
        0a:41:15:ee:f6:5d:76:07:8d:b8:70:b2:09:50:bd:d9:73:51:
        41:27:b7:62:3e:c9:9d:d3:03:35:64:75:83:75:09:50:15:3d:
        cf:e9:30:50:d0:6e:08:55:fb:eb:b0:4d:54:72:56:17:14:10:
        2a:03:5a:cb:26:8e:f3:04:1b:ba:6f:47:5f:9a:9f:7f:0a:04:
        df:63:22:85:ad:47:1f:48:de:b7:5b:7f:52:df:3c:bc:47:0c:
        b3:67

		 */
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("consumerid",			"Subject: CN=\\s*(.*)");
		regexes.put("username",				"X509v3 Subject Alternative Name:[\\s\\cM]*DirName:/CN=\\s*(.*)");
		regexes.put("issuer",				"Issuer: CN=([^,\\n]*)");
		regexes.put("serialNumber",			"Serial Number:\\s*(\\d+)");
		regexes.put("validityNotBefore",	"Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");

		List<Map<String,String>> listMaps = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutFromOpensslx509, listMaps, field);
		}
		
		// listMaps should only be one in size
		if (listMaps.size()!=1) throw new RuntimeException("Expected to parse only one group of consumer certificate data.");
	
		return new ConsumerCert(listMaps.get(0));
	}
}
