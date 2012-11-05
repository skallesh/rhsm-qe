package rhsm.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.qe.Assert;
import com.redhat.qe.tools.abstraction.AbstractCommandLineData;

/**
 * @author jsefler
 *
 */
public class ContentNamespace extends AbstractCommandLineData {
	
	// abstraction fields
	public String name;
	public String label;
	public String physicalEntitlement;
	public String flexGuestEntitlement;
	public String vendorId;
	public String downloadUrl;
	public String gpgKeyUrl;
	public Boolean enabled;
	public Integer metadataExpire;	// seconds
	public String requiredTags;	// comma separated list of tags: TAG1,TAG2,TAG3

	public String hash;
	public String type;
	
	public ContentNamespace(String type, String hash, Map<String, String> certData){
		super(certData);
		this.hash = hash;
		this.type = type;
	}
	public ContentNamespace(Map<String, String> certData){
		super(certData);
	}
	
	@Override
	public String toString() {
		
		
		String string = "";
		if (type != null)					string += String.format(" %s='%s'", "type",type);
		if (hash != null)					string += String.format(" %s='%s'", "hash",hash);
		if (label != null)					string += String.format(" %s='%s'", "label",label);
		if (name != null)					string += String.format(" %s='%s'", "name",name);
		if (physicalEntitlement != null)	string += String.format(" %s='%s'", "physicalEntitlement",physicalEntitlement);
		if (flexGuestEntitlement != null)	string += String.format(" %s='%s'", "flexGuestEntitlement",flexGuestEntitlement);
		if (vendorId != null)				string += String.format(" %s='%s'", "vendorId",vendorId);
		if (downloadUrl != null)			string += String.format(" %s='%s'", "downloadUrl",downloadUrl);
		if (gpgKeyUrl != null)				string += String.format(" %s='%s'", "gpgKeyUrl",gpgKeyUrl);
		if (enabled != null)				string += String.format(" %s='%s'", "enabled",enabled);
		if (metadataExpire != null)			string += String.format(" %s='%s'", "metadataExpire",metadataExpire);
		if (requiredTags != null)			string += String.format(" %s='%s'", "requiredTags",requiredTags);
		
		return string.trim();
	}

	/**
	 * @param rawCertificate - stdout from  openssl x509 -noout -text -in /etc/pki/entitlement/1129238407379723.pem
	 * @return
	 */
	static public List<ContentNamespace> parseStdoutFromOpensslX509(String rawCertificate) {
		/* [root@jsefler-onprem01 ~]# openssl x509 -text -in /etc/pki/entitlement/1129238407379723.pem 
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
		        52:60:65:11:0e:ac:ef:f8:10:fc:a8:a6:75:20:31:57:06:9c:
		        06:c4:a2:65:05:81:8c:d7:5b:e3:9f:4f:1f:6c:9b:3c:85:e5:
		        30:28
		*/
		
		// https://docspace.corp.redhat.com/docs/DOC-30244
		//  1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (Supplementary))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1 (Yum repo type))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.1 (Name) : Red Hat Enterprise Linux (Supplementary)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.2 (Label) : rhel-server-6-supplementary
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.3 (Physical Entitlements): 1
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.4 (Flex Guest Entitlements): 0
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.6 (Download URL): content/rhel-server-6-supplementary/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.7 (GPG Key URL): file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.8 (Enabled): 1
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.9 (Metadata Expire Seconds): 604800
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.10 (Required Tags): TAG1,TAG2,TAG3
		
		//  1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (core server) - ISOs)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2 (File repo type))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.1 (Name) : Red Hat Enterprise Linux (core server)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.2 (Label) : rhel-server
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.6 (Download URL): content/rhel-server-isos/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.7 (GPG Key URL): gpg/rhel-server-isos/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.8 (Enabled):  0
		List<ContentNamespace> contentNamespaces = new ArrayList<ContentNamespace>();
		
		for (int type : new int[]{1,2}) {	// type=1 is yum	// type=2 is file

			Map<String,String> regexes = new HashMap<String,String>();
		
			// abstraction field				regex pattern (with a capturing group)
			regexes.put("name",					"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.1:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("label",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.2:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("physicalEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.3:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("flexGuestEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.4:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("vendorId",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.5:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("downloadUrl",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.6:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("gpgKeyUrl",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.7:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("enabled",				"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.8:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("metadataExpire",		"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.9:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			regexes.put("requiredTags",			"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.10:[\\s\\cM]*\\.(?:.|\\s)(.*)");
			
			Map<String, Map<String,String>> productMap = new HashMap<String, Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToMap(pat, rawCertificate, productMap, field);
			}
			
			String sType = "unknown";
			if (type==1) sType = "yum";
			if (type==2) sType = "file";		
			for(String key : productMap.keySet())
				contentNamespaces.add(new ContentNamespace(sType, key, productMap.get(key)));
		}
		return contentNamespaces;
	}
	
	
	/**
	 * @param rawCertificate - stdout from: # rct cat-cert /etc/pki/entitlement/7586477374370607864.pem
	 * @return
	 */
	static public List<ContentNamespace> parseStdoutFromRctCatCert(String rawCertificate) {
		
		// https://docspace.corp.redhat.com/docs/DOC-30244
		//  1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (Supplementary))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1 (Yum repo type))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.1 (Name) : Red Hat Enterprise Linux (Supplementary)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.2 (Label) : rhel-server-6-supplementary
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.3 (Physical Entitlements): 1
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.4 (Flex Guest Entitlements): 0
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.6 (Download URL): content/rhel-server-6-supplementary/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.7 (GPG Key URL): file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.8 (Enabled): 1
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.9 (Metadata Expire Seconds): 604800
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.1.10 (Required Tags): TAG1,TAG2,TAG3
		
		//  1.3.6.1.4.1.2312.9.2.<content_hash> (Red Hat Enterprise Linux (core server) - ISOs)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2 (File repo type))
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.1 (Name) : Red Hat Enterprise Linux (core server)
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.2 (Label) : rhel-server
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.5 (Vendor ID): %Red_Hat_Id% or %Red_Hat_Label%
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.6 (Download URL): content/rhel-server-isos/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.7 (GPG Key URL): gpg/rhel-server-isos/$releasever/$basearch
		//  1.3.6.1.4.1.2312.9.2.<content_hash>.2.8 (Enabled):  0
		
		//	[root@rhsm-accept-rhel5 ~]# rct cat-cert /etc/pki/entitlement/5432619326294037185.pem 
		//	 
		//	+-------------------------------------------+
		//		Entitlement Certificate
		//	+-------------------------------------------+
		//
		//	Certificate:
		//		Path: /etc/pki/entitlement/5432619326294037185.pem
		//		Version: 1.0
		//		Serial: 5432619326294037185
		//		Start Date: 2011-12-31 05:00:00+00:00
		//		End Date: 2012-12-31 04:59:59+00:00
		//
		//	Subject:
		//		CN: 8a99f98439b64b3c0139b6bf9b1e07c3
		//
		//	Product:
		//		ID: 180
		//		Name: Red Hat Beta
		//		Version: 
		//		Arch: ia64,ppc,ppc64,s390,s390x,x86,x86_64
		//		Tags: 
		//
		//	Product:
		//		ID: 69
		//		Name: Red Hat Enterprise Linux Server
		//		Version: 
		//		Arch: x86,ia64,x86_64
		//		Tags: 
		//
		//	Order:
		//		Name: Red Hat Enterprise Linux Server, Premium (8 sockets) (Up to 4 guests)
		//		Number: 2573419
		//		SKU: RH0103708
		//		Contract: 3124124
		//		Account: 1615583
		//		Service Level: Premium
		//		Service Type: L1-L3
		//		Quantity: 100
		//		Quantity Used: 1
		//		Socket Limit: 8
		//		Virt Limit: 
		//		Virt Only: False
		//		Subscription: 
		//		Stacking ID: 
		//		Warning Period: 0
		//		Provides Management: 0
		//
		//	Content:
		//		Name: Red Hat Enterprise Linux 5 Server Beta (Debug RPMs)
		//		Label: rhel-5-server-beta-debug-rpms
		//		Vendor: Red Hat
		//		URL: /content/beta/rhel/server/5/$releasever/$basearch/debug
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-beta,file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat Enterprise Linux 5 Server Beta (RPMs)
		//		Label: rhel-5-server-beta-rpms
		//		Vendor: Red Hat
		//		URL: /content/beta/rhel/server/5/$releasever/$basearch/os
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-beta,file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat Enterprise Linux 5 Server Beta (Source RPMs)
		//		Label: rhel-5-server-beta-source-rpms
		//		Vendor: Red Hat
		//		URL: /content/beta/rhel/server/5/$releasever/$basearch/source/SRPMS
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-beta,file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat CloudForms Tools for RHEL 5 Beta (RPMs)
		//		Label: rhel-5-server-cf-tools-1-beta-rpms
		//		Vendor: Red Hat
		//		URL: /content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/os
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release,file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-beta
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat CloudForms Tools for RHEL 5 Beta (Source RPMs)
		//		Label: rhel-5-server-cf-tools-1-beta-source-rpms
		//		Vendor: Red Hat
		//		URL: /content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/source/SRPMS
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release,file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-beta
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat CloudForms Tools for RHEL 5 (RPMs)
		//		Label: rhel-5-server-cf-tools-1-rpms
		//		Vendor: Red Hat
		//		URL: /content/dist/rhel/server/5/$releasever/$basearch/cf-tools/1/os
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//		Enabled: True
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		//
		//	Content:
		//		Name: Red Hat CloudForms Tools for RHEL 5 (Source RPMs)
		//		Label: rhel-5-server-cf-tools-1-source-rpms
		//		Vendor: Red Hat
		//		URL: /content/dist/rhel/server/5/$releasever/$basearch/cf-tools/1/source/SRPMS
		//		GPG: file:///etc/pki/rpm-gpg/RPM-GPG-KEY-redhat-release
		//		Enabled: False
		//		Expires: 86400
		//		Required Tags: rhel-5-server
		
		
		/* THIS IMPLEMENTATION WORKS, BUT I PREFER A DIFFERENT ONE THAT MATCHES THE CONTENT GROUPS AND THEN CREATES ONE CONTENT NAMESPACE PER GROUP
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("name",					"Content:(?:(?:\\n.+)+)Name: (.+)");
		regexes.put("label",				"Content:(?:(?:\\n.+)+)Label: (.+)");
		// TODO regexes.put("physicalEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.3:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		// TODO regexes.put("flexGuestEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.4:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		regexes.put("vendorId",				"Content:(?:(?:\\n.+)+)Vendor: (.+)");
		regexes.put("downloadUrl",			"Content:(?:(?:\\n.+)+)URL: (.+)");
		regexes.put("gpgKeyUrl",			"Content:(?:(?:\\n.+)+)GPG: (.+)");
		regexes.put("enabled",				"Content:(?:(?:\\n.+)+)Enabled: (.+)");
		regexes.put("metadataExpire",		"Content:(?:(?:\\n.+)+)Expires: (.+)");
		regexes.put("requiredTags",			"Content:(?:(?:\\n.+)+)Required Tags: (.+)");
		// TODO regexes.put("type",			"Content:(?:(?:\\n.+)+)Repo Type: (.+)");	// Bug 856349 - rct cat-cert tool should report Content for "file" repo types as well as "yum" repos types
		
		List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, rawCertificate, certDataList, field);
		}
		
		List<ContentNamespace> contentNamespaces = new ArrayList<ContentNamespace>();
		for (Map<String, String> certData : certDataList) {
			contentNamespaces.add(new ContentNamespace(certData));
		}
		return contentNamespaces;
		*/
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group)
		regexes.put("name",					"^\\s+Name: (.+)");
		regexes.put("label",				"^\\s+Label: (.+)");
		// TODO regexes.put("physicalEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.3:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		// TODO regexes.put("flexGuestEntitlement",	"1\\.3\\.6\\.1\\.4\\.1\\.2312\\.9\\.2\\.(\\d+)\\."+type+"\\.4:[\\s\\cM]*\\.(?:.|\\s)(.*)");
		regexes.put("vendorId",				"^\\s+Vendor: (.+)");
		regexes.put("downloadUrl",			"^\\s+URL: (.+)");
		regexes.put("gpgKeyUrl",			"^\\s+GPG: (.+)");
		regexes.put("enabled",				"^\\s+Enabled: (.+)");
		regexes.put("metadataExpire",		"^\\s+Expires: (.+)");
		regexes.put("requiredTags",			"^\\s+Required Tags: (.*)");
		regexes.put("type",					"^\\s+Type: (.+)");	// Bug 856349 - rct cat-cert tool should report Content for "file" repo types as well as "yum" repos types

		
		// find all the raw "Content:" groupings and then create one ContentNamespace per raw "Content:" grouping
		String rawContentRegex = "Content:((\\n.+)+)";
		List<ContentNamespace> contentNamespaces = new ArrayList<ContentNamespace>();
		Matcher m = Pattern.compile(rawContentRegex, Pattern.MULTILINE).matcher(rawCertificate);
		while (m.find()) {
			String rawContent = m.group(1);
			
			// find a list of all the certData matching the content fields in the map of regexes
			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawContent, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the list for this content grouping
			if (certDataList.size()!=1) Assert.fail("Error when parsing raw content group.  Expected to parse only one group of content data from:\n"+m.group(0));
			Map<String,String> certData = certDataList.get(0);
			
			// create a new ContentNamespace
			contentNamespaces.add(new ContentNamespace(certData));
		}
		return contentNamespaces;
	}
	
	
	
	static public List<ContentNamespace> parse(String rawCertificate) {
		// where did this rawCertificate come from?
		if (rawCertificate.contains("Signature Algorithm: sha1WithRSAEncryption"))
			return parseStdoutFromOpensslX509(rawCertificate);
		else
			return parseStdoutFromRctCatCert(rawCertificate);
	}
}
