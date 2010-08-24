package com.redhat.qe.sm.abstractions;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jsefler
 *
 */
public class ConsumerCert extends CandlepinAbstraction {
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
