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

public class EntitlementCert extends AbstractCommandLineData {
	protected static String simpleDateFormat = "MMM d HH:mm:ss yyyy z";	// Aug 23 08:42:00 2010 GMT   validityNotBefore
//	protected static String simpleDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// "2010-09-01T15:45:12.068+0000"   startDate

	// abstraction fields
	public BigInteger serialNumber;	// this is the key
	public String id;			// entitlement uuid on the candlepin server
	public String issuer;
	public Calendar validityNotBefore;
	public Calendar validityNotAfter;
	
	public String productName;
	public String orderNumber;
	public String productId;	// SKU
	public String subscriptionNumber;
	public String quantity;
	public String startDate;
	public String endDate;
	public String virtualizationLimit;
	public String socketLimit;
	public String contractNumber;
	public String quantityUsed;
	public String warningPeriod;
	public String accountNumber;
	
	public List<ContentNamespace> contentNamespaces;
	public String rawCertificate;

	public EntitlementCert(BigInteger serialNumber, Map<String, String> certData){
		super(certData);
		this.serialNumber = serialNumber;
		contentNamespaces = ContentNamespace.parse(this.rawCertificate);
	}

	
	
	@Override
	public String toString() {
		
		String string = "";
		if (serialNumber != null)			string += String.format(" %s='%s'", "serialNumber",serialNumber);
		if (id != null)						string += String.format(" %s='%s'", "id",id);
		if (issuer != null)					string += String.format(" %s='%s'", "issuer",issuer);
		if (validityNotBefore != null)		string += String.format(" %s='%s'", "validityNotBefore",formatDateString(validityNotBefore));
		if (validityNotAfter != null)		string += String.format(" %s='%s'", "validityNotAfter",formatDateString(validityNotAfter));
		
		if (productName != null)			string += String.format(" %s='%s'", "productName",productName);
		if (orderNumber != null)			string += String.format(" %s='%s'", "orderNumber",orderNumber);
		if (subscriptionNumber != null)		string += String.format(" %s='%s'", "subscriptionNumber",subscriptionNumber);
		if (startDate != null)				string += String.format(" %s='%s'", "startDate",startDate);
		if (endDate != null)				string += String.format(" %s='%s'", "endDate",endDate);
		if (virtualizationLimit != null)	string += String.format(" %s='%s'", "virtualizationLimit",virtualizationLimit);
		if (socketLimit != null)			string += String.format(" %s='%s'", "socketLimit",socketLimit);
		if (contractNumber != null)			string += String.format(" %s='%s'", "contractNumber",contractNumber);
		if (quantityUsed != null)			string += String.format(" %s='%s'", "quantityUsed",quantityUsed);
		if (warningPeriod != null)			string += String.format(" %s='%s'", "warningPeriod",warningPeriod);
		if (accountNumber != null)			string += String.format(" %s='%s'", "accountNumber",accountNumber);
	
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
		/*
Certificate:
    Data:
        Version: 3 (0x2)
        Serial Number:
            28:1b:42:f7:9d:bf:2d
        Signature Algorithm: sha1WithRSAEncryption
        Issuer: CN=jsefler-f12-candlepin.usersys.redhat.com, C=US, L=Raleigh
        Validity
            Not Before: Nov  5 16:11:44 2010 GMT
            Not After : Nov  5 23:59:59 2011 GMT
        Subject: CN=ff8080812c1b0fa2012c1ccecff303ca
        Subject Public Key Info:
            Public Key Algorithm: rsaEncryption
                Public-Key: (2048 bit)
                Modulus:
                    00:ce:97:4d:65:c0:2d:fa:20:98:18:dd:ed:97:76:
                    d8:a1:4a:5b:ba:ea:f7:ea:3e:5c:c4:b6:7c:3f:de:
                    1d:2f:06:fa:6c:8c:1e:2f:a0:78:57:4e:96:9f:32:
                    4f:34:e8:c2:34:69:8f:8b:03:d9:b6:3e:02:23:9c:
                    4b:5e:54:58:aa:bb:b7:04:c6:9f:d6:2f:2a:cb:61:
                    a2:ea:d8:c5:72:8c:2d:19:19:aa:1b:a8:73:96:1e:
                    23:21:a6:e9:74:3e:78:b3:46:4d:4f:e6:f1:e7:24:
                    c8:b8:87:35:b0:08:6c:47:87:3b:3b:a6:1e:36:78:
                    10:40:5e:be:96:64:e4:a8:9a:02:54:78:a6:b8:bc:
                    2d:df:64:74:c3:02:4f:4a:81:88:6d:64:72:39:cb:
                    7b:b0:77:5d:a6:30:78:18:40:81:42:94:3b:a1:dd:
                    17:6b:6b:7b:e0:be:d3:29:02:d5:48:c6:c9:08:4a:
                    bb:5c:19:e9:50:26:04:51:7f:fc:06:12:4f:5d:64:
                    e2:69:e8:1d:15:92:ac:3d:18:98:16:c8:64:ac:27:
                    cf:c7:6c:63:7a:42:b0:32:47:b6:31:8f:c9:38:70:
                    b3:40:5f:64:da:bf:b0:be:d1:81:3d:11:76:50:b3:
                    d2:96:84:ac:71:4e:c7:66:c2:a1:dc:c5:73:2e:f1:
                    6f:c5
                Exponent: 65537 (0x10001)
        X509v3 extensions:
            Netscape Cert Type: 
                SSL Client, S/MIME
            X509v3 Key Usage: 
                Digital Signature, Key Encipherment, Data Encipherment
            X509v3 Authority Key Identifier: 
                keyid:B6:70:95:46:37:47:01:C8:2B:43:29:4F:82:EF:11:5C:5D:E4:70:6F
                DirName:/CN=jsefler-f12-candlepin.usersys.redhat.com/C=US/L=Raleigh
                serial:C6:E0:7B:6A:01:03:25:F7

            X509v3 Subject Key Identifier: 
                27:E0:57:92:B1:53:28:A0:EC:50:51:BC:29:DC:95:6A:D0:D3:7D:33
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
                ...RHEL for Physical Servers ,2 Sockets, Standard Support with High
Availability,Load Balancing,Shared Storage,Large File Support,Smart
Management, Flexible Hypervisor(Unlimited)
            1.3.6.1.4.1.2312.9.4.2: 
                . ff8080812c1b0fa2012c1b108b660086
            1.3.6.1.4.1.2312.9.4.3: 
                ..MKT-rhel-physical-2-socket
            1.3.6.1.4.1.2312.9.4.5: 
                ..10
            1.3.6.1.4.1.2312.9.4.6: 
                ..2010-11-05T00:00:00Z
            1.3.6.1.4.1.2312.9.4.7: 
                ..2011-11-05T00:00:00Z
            1.3.6.1.4.1.2312.9.4.12: 
                ..30
            1.3.6.1.4.1.2312.9.4.10: 
                ..4
            1.3.6.1.4.1.2312.9.4.11: 
                ..1
            1.3.6.1.4.1.2312.9.5.1: 
                .$0cd89d72-d7a9-4e3d-9eb0-599c4a0e2f8d
    Signature Algorithm: sha1WithRSAEncryption
        bd:94:86:5d:82:b8:99:81:35:89:b4:78:12:25:5c:ae:24:09:
        46:ac:3d:54:8d:4a:56:05:4d:35:bc:66:01:b0:de:b7:56:1b:
        29:ed:30:e3:aa:51:c9:c4:ef:3f:b9:2e:2f:ca:34:de:11:47:
        58:7a:78:9a:8f:d0:31:9c:1f:ab:03:69:8f:dd:bf:82:26:0e:
        fc:c7:12:a0:61:46:f3:cd:8c:1f:6e:0c:dd:88:b2:05:bc:e2:
        5c:cd:44:d3:13:18:58:0c:02:60:18:50:d4:91:08:0f:f7:0f:
        2b:1c:bf:a6:fb:30:d4:13:6b:e3:7e:13:71:49:9d:44:8a:8b:
        18:cc
-----BEGIN CERTIFICATE-----
MIIJnDCCCQWgAwIBAgIHKBtC952/LTANBgkqhkiG9w0BAQUFADBSMTEwLwYDVQQD
DChqc2VmbGVyLWYxMi1jYW5kbGVwaW4udXNlcnN5cy5yZWRoYXQuY29tMQswCQYD
VQQGEwJVUzEQMA4GA1UEBwwHUmFsZWlnaDAeFw0xMDExMDUxNjExNDRaFw0xMTEx
MDUyMzU5NTlaMCsxKTAnBgNVBAMMIGZmODA4MDgxMmMxYjBmYTIwMTJjMWNjZWNm
ZjMwM2NhMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzpdNZcAt+iCY
GN3tl3bYoUpbuur36j5cxLZ8P94dLwb6bIweL6B4V06WnzJPNOjCNGmPiwPZtj4C
I5xLXlRYqru3BMaf1i8qy2Gi6tjFcowtGRmqG6hzlh4jIabpdD54s0ZNT+bx5yTI
uIc1sAhsR4c7O6YeNngQQF6+lmTkqJoCVHimuLwt32R0wwJPSoGIbWRyOct7sHdd
pjB4GECBQpQ7od0Xa2t74L7TKQLVSMbJCEq7XBnpUCYEUX/8BhJPXWTiaegdFZKs
PRiYFshkrCfPx2xjekKwMke2MY/JOHCzQF9k2r+wvtGBPRF2ULPSloSscU7HZsKh
3MVzLvFvxQIDAQABo4IHHTCCBxkwEQYJYIZIAYb4QgEBBAQDAgWgMAsGA1UdDwQE
AwIEsDCBggYDVR0jBHsweYAUtnCVRjdHAcgrQylPgu8RXF3kcG+hVqRUMFIxMTAv
BgNVBAMMKGpzZWZsZXItZjEyLWNhbmRsZXBpbi51c2Vyc3lzLnJlZGhhdC5jb20x
CzAJBgNVBAYTAlVTMRAwDgYDVQQHDAdSYWxlaWdoggkAxuB7agEDJfcwHQYDVR0O
BBYEFCfgV5KxUyig7FBRvCnclWrQ030zMBMGA1UdJQQMMAoGCCsGAQUFBwMCMDQG
DSsGAQQBkggJAYKhSQEEIwwhSGlnaCBhdmFpbGFiaWxpdHkgKGNsdXN0ZXIgc3Vp
dGUpMBQGCysGAQQBkggJAgEBBAUMA3l1bTAoBgwrBgEEAZIICQIBAQEEGAwWYWx3
YXlzLWVuYWJsZWQtY29udGVudDAoBgwrBgEEAZIICQIBAQIEGAwWYWx3YXlzLWVu
YWJsZWQtY29udGVudDAdBgwrBgEEAZIICQIBAQUEDQwLdGVzdC12ZW5kb3IwIgYM
KwYBBAGSCAkCAQEGBBIMEC9mb28vcGF0aC9hbHdheXMwJgYMKwYBBAGSCAkCAQEH
BBYMFC9mb28vcGF0aC9hbHdheXMvZ3BnMBMGDCsGAQQBkggJAgEBBAQDDAEwMBMG
DCsGAQQBkggJAgEBAwQDDAEwMBMGDCsGAQQBkggJAgEBCAQDDAExMBQGCysGAQQB
kggJAgABBAUMA3l1bTAnBgwrBgEEAZIICQIAAQEEFwwVbmV2ZXItZW5hYmxlZC1j
b250ZW50MCcGDCsGAQQBkggJAgABAgQXDBVuZXZlci1lbmFibGVkLWNvbnRlbnQw
HQYMKwYBBAGSCAkCAAEFBA0MC3Rlc3QtdmVuZG9yMCEGDCsGAQQBkggJAgABBgQR
DA8vZm9vL3BhdGgvbmV2ZXIwJQYMKwYBBAGSCAkCAAEHBBUMEy9mb28vcGF0aC9u
ZXZlci9ncGcwEwYMKwYBBAGSCAkCAAEEBAMMATAwEwYMKwYBBAGSCAkCAAEDBAMM
ATAwEwYMKwYBBAGSCAkCAAEIBAMMATAwMAYNKwYBBAGSCAkBgqFEAQQfDB1SSEVM
IGZvciBQaHlzaWNhbCBTZXJ2ZXJzIFNWQzAVBgwrBgEEAZIICQKIVwEEBQwDeXVt
MBoGDSsGAQQBkggJAohXAQEECQwHY29udGVudDAgBg0rBgEEAZIICQKIVwECBA8M
DWNvbnRlbnQtbGFiZWwwHgYNKwYBBAGSCAkCiFcBBQQNDAt0ZXN0LXZlbmRvcjAc
Bg0rBgEEAZIICQKIVwEGBAsMCS9mb28vcGF0aDAhBg0rBgEEAZIICQKIVwEHBBAM
Di9mb28vcGF0aC9ncGcvMBQGDSsGAQQBkggJAohXAQQEAwwBMDAUBg0rBgEEAZII
CQKIVwEDBAMMATAwFAYNKwYBBAGSCAkCiFcBCAQDDAExMCEGDSsGAQQBkggJAYKh
TgEEEAwOTG9hZCBCYWxhbmNpbmcwKwYNKwYBBAGSCAkBgqFMAQQaDBhMYXJnZSBG
aWxlIFN1cHBvcnQgKFhGUykwJwYNKwYBBAGSCAkBgqFLAQQWDBRTaGFyZWQgU3Rv
cmFnZSAoR0ZTKTBPBg0rBgEEAZIICQGCoU0BBD4MPFNtYXJ0IE1hbmFnZW1lbnQg
KFJITiBNYW5hZ2VtZW50ICYgUHJvdmlzaW9pbmcgJiBNb25pdG9yaW5nKTCBwQYK
KwYBBAGSCAkEAQSBsgyBr1JIRUwgZm9yIFBoeXNpY2FsIFNlcnZlcnMgLDIgU29j
a2V0cywgU3RhbmRhcmQgU3VwcG9ydCB3aXRoIEhpZ2gKQXZhaWxhYmlsaXR5LExv
YWQgQmFsYW5jaW5nLFNoYXJlZCBTdG9yYWdlLExhcmdlIEZpbGUgU3VwcG9ydCxT
bWFydApNYW5hZ2VtZW50LCBGbGV4aWJsZSBIeXBlcnZpc29yKFVubGltaXRlZCkw
MAYKKwYBBAGSCAkEAgQiDCBmZjgwODA4MTJjMWIwZmEyMDEyYzFiMTA4YjY2MDA4
NjAqBgorBgEEAZIICQQDBBwMGk1LVC1yaGVsLXBoeXNpY2FsLTItc29ja2V0MBIG
CisGAQQBkggJBAUEBAwCMTAwJAYKKwYBBAGSCAkEBgQWDBQyMDEwLTExLTA1VDAw
OjAwOjAwWjAkBgorBgEEAZIICQQHBBYMFDIwMTEtMTEtMDVUMDA6MDA6MDBaMBIG
CisGAQQBkggJBAwEBAwCMzAwEQYKKwYBBAGSCAkECgQDDAE0MBEGCisGAQQBkggJ
BAsEAwwBMTA0BgorBgEEAZIICQUBBCYMJDBjZDg5ZDcyLWQ3YTktNGUzZC05ZWIw
LTU5OWM0YTBlMmY4ZDANBgkqhkiG9w0BAQUFAAOBgQC9lIZdgriZgTWJtHgSJVyu
JAlGrD1UjUpWBU01vGYBsN63Vhsp7TDjqlHJxO8/uS4vyjTeEUdYeniaj9AxnB+r
A2mP3b+CJg78xxKgYUbzzYwfbgzdiLIFvOJczUTTExhYDAJgGFDUkQgP9w8rHL+m
+zDUE2vjfhNxSZ1EiosYzA==
-----END CERTIFICATE-----

		 */
		
		/*
		        Issuer: C=US, ST=North Carolina, O=Red Hat, Inc., OU=Red Hat Network, CN=Red Hat Entitlement Product Authority/emailAddress=ca-support@redhat.com
		 */
		
//		https://docspace.corp.redhat.com/docs/DOC-30244
//			  1.3.6.1.4.1.2312.9.4.1 (Name): Red Hat Enterprise Linux Server
//			  1.3.6.1.4.1.2312.9.4.2 (Order Number) : ff8080812c3a2ba8012c3a2cbe63005b  
//			  1.3.6.1.4.1.2312.9.4.3 (SKU) : MCT0982
//			  1.3.6.1.4.1.2312.9.4.4 (Subscription Number) : abcd-ef12-1234-5678
//			  1.3.6.1.4.1.2312.9.4.5 (Quantity) : 100
//			  1.3.6.1.4.1.2312.9.4.6 (Entitlement Start Date) : 2010-10-25T04:00:00Z
//			  1.3.6.1.4.1.2312.9.4.7 (Entitlement End Date) : 2011-11-05T00:00:00Z
//			  1.3.6.1.4.1.2312.9.4.8 (Virtualization Limit) : 4
//			  1.3.6.1.4.1.2312.9.4.9 (Socket Limit) : None
//			  1.3.6.1.4.1.2312.9.4.10 (Contract Number): 152341643
//			  1.3.6.1.4.1.2312.9.4.11 (Quantity Used): 4
//			  1.3.6.1.4.1.2312.9.4.12 (Warning Period): 30
//			  1.3.6.1.4.1.2312.9.4.13 (Account Number): 9876543210

		// FIXME: Not sure how to handle the certificate dates Validity versus Entitlement Start Date
		// I suspect that I am currently using the wrong dates.
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("id",					"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Subject: CN=(.+)");
		regexes.put("issuer",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Issuer:\\s*(.*)");
		regexes.put("validityNotBefore",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not Before\\s*:\\s*(.*)");
		regexes.put("validityNotAfter",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*Validity[\\n\\s\\w:]*Not After\\s*:\\s*(.*)");

		regexes.put("productName",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("orderNumber",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.2:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("productId",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.3:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("subscriptionNumber",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.4:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("quantity",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.5:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("startDate",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.6:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("endDate",				"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.7:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("virtualizationLimit",	"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.8:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("socketLimit",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.9:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("contractNumber",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.10:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("quantityUsed",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.11:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("warningPeriod",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.12:[\\s\\cM]*\\.(?:.|\\s)(.+)");
		regexes.put("accountNumber",		"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.13:[\\s\\cM]*\\.(?:.|\\s)(.+)");

		regexes.put("rawCertificate",		"Serial Number:\\s*([\\d\\w:]+).*((?:\\n.*?)*).1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.5\\.1:");	// FIXME THIS IS ONLY PART OF THE CERT

//FIXME WORKAROUND: DELETE THE FOLLOWING LINE AFTER RESOLUTION FROM https://bugzilla.redhat.com/show_bug.cgi?id=650278
		regexes.put("productId",			"Serial Number:\\s*([\\d\\w:]+).*(?:\\n.*?)*.1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.4\\.1:[\\s\\cM]*\\.(?:.|\\s)(.+)");

		

		Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToMap(pat, certificates, productMap, field);
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
