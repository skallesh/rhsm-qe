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
	public String serialNumber;
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
		regexes.put("serialNumber",			"Serial Number:\\s*([\\d\\w:]+)");
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
