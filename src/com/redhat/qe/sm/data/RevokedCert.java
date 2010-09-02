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

/**
 * @author jsefler
 *
 */
public class RevokedCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT

	// abstraction fields
	public Integer serialNumber;
	public Calendar revocationDate;
	public String reasonCode;


	public RevokedCert(Map<String, String> crlData) {
		super(crlData);
		// TODO Auto-generated constructor stub
		
		// Overridden fields
	}
	
	@Override
	public String toString() {
		
		String string = "";
		if (serialNumber != null)		string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (revocationDate != null)		string += String.format(" %s='%s'", "revocationDate",formatDateString(revocationDate));
		if (reasonCode != null)			string += String.format(" %s='%s'", "reasonCode",reasonCode);

		
		return string.trim();
	}
	
	// Note: Override needed since the raw CRL renders the serialNumber as a hexadecimal.
	@Override
	protected Integer parseInt(String intString){
		return Integer.parseInt(intString,16);	
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
	 * @param crls - stdout from "openssl crl -noout -text -in /var/lib/candlepin/candlepin-crl.crl"
	 * @return
	 */
	static public List<RevokedCert> parse(String stdoutFromOpensslCrl) {
		
		/* [root@jsefler-f12-candlepin candlepin]# openssl crl -noout -text -in /var/lib/candlepin/candlepin-crl.crl 
Certificate Revocation List (CRL):
        Version 2 (0x1)
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: /CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
        Last Update: Aug 23 13:45:00 2010 GMT
        Next Update: Aug 24 13:45:00 2010 GMT
        CRL extensions:
            X509v3 Authority Key Identifier: 
                keyid:A7:D6:0C:DC:E5:78:A6:06:F4:18:4C:00:01:F3:A7:A3:77:73:BF:F6
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:D5:18:14:4D:5A:E8:3A:DC

            X509v3 CRL Number: 
                2392
Revoked Certificates:
    Serial Number: 02
        Revocation Date: Aug 13 16:00:00 2010 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Privilege Withdrawn
    Serial Number: 6C
        Revocation Date: Aug 23 08:42:00 2010 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Privilege Withdrawn
    Serial Number: 28
        Revocation Date: Aug 23 08:15:00 2010 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Privilege Withdrawn
    Serial Number: 80
        Revocation Date: Aug 23 08:45:00 2010 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Privilege Withdrawn
    Serial Number: 4C
        Revocation Date: Aug 23 08:27:00 2010 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Privilege Withdrawn
    Signature Algorithm: sha1WithRSAEncryption
        84:cd:98:14:60:a6:48:b3:7b:1e:c5:e6:5d:6e:7f:97:19:78:
        9b:77:b5:86:6a:4d:3b:59:2f:77:10:b1:93:f6:76:66:6f:15:
        2a:97:3b:7b:6c:89:57:f6:49:dc:f8:1b:48:1e:7d:44:be:11:
        53:b0:a3:af:38:ff:16:16:e3:83:1d:51:a1:21:41:97:d5:81:
        6e:59:35:2d:fd:f4:c3:58:10:88:60:1f:14:94:af:a2:92:35:
        46:a5:90:51:17:a3:a3:20:6a:c5:70:b7:4f:5b:e7:dc:1e:89:
        d8:63:9c:bf:2f:ff:0d:0a:e4:10:15:ea:24:69:fe:da:64:ac:
        ae:5c
		*/
		
		// EXAMPLE OF NO REVOKED CERTIFICATES:
		/* [root@jsefler-f12-candlepin candlepin]# openssl crl -noout -text -in /var/lib/candlepin/candlepin-crl.crl 
Certificate Revocation List (CRL):
        Version 2 (0x1)
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: /CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
        Last Update: Aug 23 13:51:00 2010 GMT
        Next Update: Aug 24 13:51:00 2010 GMT
        CRL extensions:
            X509v3 Authority Key Identifier: 
                keyid:95:90:D1:C1:55:2E:78:05:3E:7E:B6:83:29:6D:96:E9:73:D9:EC:7B
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:DA:DC:6A:42:17:50:C6:42

            X509v3 CRL Number: 
                2
No Revoked Certificates.
    Signature Algorithm: sha1WithRSAEncryption
        5e:48:88:a6:21:25:b6:f4:d7:8b:30:82:8b:63:73:40:26:73:
        fd:60:ef:7c:ac:b6:e4:66:12:ab:c1:7f:a4:50:f5:e5:8b:66:
        de:0c:f7:f3:8d:7c:df:a4:c9:a9:18:d5:eb:a5:08:74:ab:22:
        9e:62:f2:13:38:56:40:ec:05:e2:6a:61:b8:87:c3:1d:e6:5e:
        5c:8d:39:d8:f5:23:48:8f:ca:36:23:33:e1:e9:de:88:e3:d7:
        ed:a1:33:69:ef:45:0f:7c:d1:6a:0a:fa:d8:1b:e4:97:29:43:
        b1:c9:73:cb:f8:c2:a8:e0:2b:b3:ce:85:ac:b0:27:13:0c:6d:
        3c:91

		 */
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)ass)
		regexes.put("serialNumber",			"Serial Number:\\s*(.*)");
		regexes.put("revocationDate",		"Revocation Date:\\s*(.*)");
		regexes.put("reasonCode",			"X509v3 CRL Reason Code:[\\s\\cM]*(.*)");

		List<Map<String,String>> listRevokedCertMaps = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, stdoutFromOpensslCrl, listRevokedCertMaps, field);
		}
		
		List<RevokedCert> revokedCerts = new ArrayList<RevokedCert>();
		for(Map<String,String> revokedCertMap : listRevokedCertMaps)
			revokedCerts.add(new RevokedCert(revokedCertMap));
		
		return revokedCerts;
	}
}
