package rhsm.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
public class ManifestSubscription extends AbstractCommandLineData {

	// abstraction fields
	public String name;
	public String quantity;
	public Calendar created;
	public Calendar startDate;
	public Calendar endDate;
	public String supportLevel;
	public String supportType;
	public String architectures;	// comma separated list: x86,x86_64,ia64,s390x,ppc,s390,ppc64
	public String productId;
	public Integer contract;
	public String subscriptionId;
	public String entitlementFile;
	public String certificateFile;
	public String certificateVersion;
	public List<String> providedProducts;
	public List<String> contentSets;


	public ManifestSubscription(Map<String, String> manifestData){
		super(manifestData);		
	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (name != null)				string += String.format(" %s='%s'", "name",name);
		if (quantity != null)			string += String.format(" %s='%s'", "quantity",quantity);
		if (created != null)			string += String.format(" %s='%s'", "created",formatDateString(created));
		if (startDate != null)			string += String.format(" %s='%s'", "startDate",formatDateString(startDate));
		if (endDate != null)			string += String.format(" %s='%s'", "endDate",formatDateString(endDate));
		if (supportLevel != null)		string += String.format(" %s='%s'", "supportLevel",supportLevel);
		if (supportType != null)		string += String.format(" %s='%s'", "supportType",supportType);
		if (architectures != null)		string += String.format(" %s='%s'", "architectures",architectures);
		if (productId != null)			string += String.format(" %s='%s'", "productId",productId);
		if (subscriptionId != null)		string += String.format(" %s='%s'", "subscriptionId",subscriptionId);
		if (entitlementFile != null)	string += String.format(" %s='%s'", "entitlementFile",entitlementFile);
		if (certificateFile != null)	string += String.format(" %s='%s'", "certificateFile",certificateFile);
		if (certificateVersion != null)	string += String.format(" %s='%s'", "certificateVersion",certificateVersion);
		if (providedProducts != null)	string += String.format(" %s='%s'", "providedProducts",providedProducts);
		if (contentSets != null)		string += String.format(" %s='%s'", "contentSets",contentSets);
		
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
		else if (dateString.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}[+-][0-9]{4}")) {
			simpleDateFormatOverride = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// used in manifest	// 2013-01-21T21:24:16.193+0000
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
		simpleDateFormatOverride = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";	// can really be any useful format
		DateFormat dateFormat = new SimpleDateFormat(simpleDateFormatOverride);
		return dateFormat.format(date.getTime());
	}
	
	
	/**
	 * @param rawManifest
	 * @return
	 */
	static public List<ManifestSubscription> parse(String rawManifest) {
		
		//[root@jsefler-7 ~]# rct cat-manifest manifest.zip
		//
		//+-------------------------------------------+
		//	Manifest
		//+-------------------------------------------+
		//
		//General:
		//	Server: access.stage.redhat.com/management/distributors/
		//	Server Version: 0.7.13.10-1
		//	Date Created: 2013-01-21T21:24:16.193+0000
		//	Creator: qa@redhat.com
		//
		//Consumer:
		//	Name: jsefler
		//	UUID: b2837b9a-d2d9-4b41-acd9-34bdcf72af66
		//	Type: sam
		//
		//Subscription:
		//	Name: Red Hat Enterprise Linux Server, Self-support (1-2 sockets) (Up to 1 guest)
		//	Quantity: 2
		//	Created: 2013-01-21T21:22:57.000+0000
		//	Start Date: 2012-12-31T05:00:00.000+0000
		//	End Date: 2013-12-31T04:59:59.000+0000
		//	Suport Level: Self-support
		//	Suport Type: L1-L3
		//	Architectures: x86,x86_64,ia64,s390x,ppc,s390,ppc64
		//	Product Id: RH0197181
		//	Contract: 
		//	Subscription Id: 2677511
		//	Entitlement File: export/entitlements/8a99f9843c401207013c5efe1e1931ce.json
		//	Certificate File: export/entitlement_certificates/4134818306731067736.pem
		//	Certificate Version: 1.0
		//	Provided Products:
		//		69: Red Hat Enterprise Linux Server
		//		180: Red Hat Beta
		//	Content Sets:
		//		/content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/os
		//		/content/beta/rhel/server/5/$releasever/$basearch/cf-tools/1/source/SRPMS
		//		/content/beta/rhel/server/5/$releasever/$basearch/debug
		//		/content/beta/rhel/server/5/$releasever/$basearch/iso
		//		/content/beta/rhel/server/5/$releasever/$basearch/os
		//
		//Subscription:
		//	Name: Red Hat Employee Subscription
		//	Quantity: 2
		//	Created: 2013-01-21T21:22:56.000+0000
		//	Start Date: 2011-10-08T04:00:00.000+0000
		//	End Date: 2022-01-01T04:59:59.000+0000
		//	Suport Level: None
		//	Suport Type: None
		//	Architectures: x86,x86_64,ia64,s390x,ppc,s390,ppc64
		//	Product Id: SYS0395
		//	Contract: 2596950
		//	Subscription Id: 2252576
		//	Entitlement File: export/entitlements/8a99f9833c400fa5013c5efe1a5a4683.json
		//	Certificate File: export/entitlement_certificates/2571369151493658952.pem
		//	Certificate Version: 1.0
		//	Provided Products:
		//		69: Red Hat Enterprise Linux Server
		//		71: Red Hat Enterprise Linux Workstation
		//		83: Red Hat Enterprise Linux High Availability (for RHEL Server)
		//		85: Red Hat Enterprise Linux Load Balancer (for RHEL Server)
		//		90: Red Hat Enterprise Linux Resilient Storage (for RHEL Server)
		//		180: Red Hat Beta
		//	Content Sets:
		//		/content/beta/rhel/power/5/$releasever/$basearch/highavailability/debug
		//		/content/beta/rhel/power/5/$releasever/$basearch/highavailability/os
		//		/content/beta/rhel/power/5/$releasever/$basearch/highavailability/source/SRPMS
		//		/content/beta/rhel/power/5/$releasever/$basearch/resilientstorage/debug
		//		/content/beta/rhel/power/5/$releasever/$basearch/resilientstorage/os
		//
		
		Map<String,String> regexes = new HashMap<String,String>();
/*		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("name",					"Subscription:(?:(?:\\n.+)+)Name: (.*)");
		regexes.put("quantity",				"Subscription:(?:(?:\\n.+)+)Quantity: (.*)");
		regexes.put("created",				"Subscription:(?:(?:\\n.+)+)Created: (.+)");
		regexes.put("startDate",			"Subscription:(?:(?:\\n.+)+)Start Date: (.+)");
		regexes.put("endDate",				"Subscription:(?:(?:\\n.+)+)End Date: (.+)");
		regexes.put("supportLevel",			"Subscription:(?:(?:\\n.+)+)Suport Level: (.*)");		// Bug 913302 - typo in msgid "Suport Level" and msgid "Suport Type"
		regexes.put("supportType",			"Subscription:(?:(?:\\n.+)+)Suport Type: (.*)");		// Bug 913302 - typo in msgid "Suport Level" and msgid "Suport Type"
		regexes.put("architectures",		"Subscription:(?:(?:\\n.+)+)Architectures: (.*)");
		regexes.put("productId",			"Subscription:(?:(?:\\n.+)+)Product Id: (.*)");			// Bug 913703 - rct cat-manifest > Subscription:> "Product Id:" should be labeled as "SKU:"	// Bug 878634 - String Updates: Capitalization of acronyms (URL, ID, HTTP, CPU)
		regexes.put("contract",				"Subscription:(?:(?:\\n.+)+)Contract: (.*)");
		regexes.put("subscriptionId",		"Subscription:(?:(?:\\n.+)+)Subscription Id: (.*)");	// Bug 913720 - rct cat-manifest > Subscription:> "Subscription Id:" should be labeled as "Order Number:"	// Bug 878634 - String Updates: Capitalization of acronyms (URL, ID, HTTP, CPU)
		regexes.put("entitlementFile",		"Subscription:(?:(?:\\n.+)+)Entitlement File: (.+)");
		regexes.put("certificateFile",		"Subscription:(?:(?:\\n.+)+)Certificate File: (.+)");
		regexes.put("certificateVersion",	"Subscription:(?:(?:\\n.+)+)Certificate Version: (.*)");
		regexes.put("providedProducts",		"Subscription:(?:(?:\\n.+)+)Provided Products:(.*(\\n.*?)+)^\\s+Content Sets:");	// assumes "Content Sets:" is the next group
		regexes.put("contentSets",			"Subscription:(?:(?:\\n.+)+)Content Sets:(.*(\\n.*?)+?)\\n(?:\\n|$)");	// assumes one or more content set values.  FIXME this will fail if there are no content sets
		
		List<Map<String,String>> productList = new ArrayList<Map<String,String>>();
		for(String field : regexes.keySet()){
			Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
			addRegexMatchesToList(pat, rawManifest, productList, field);
		}
		
		List<ManifestSubscription> manifestSubscriptions = new ArrayList<ManifestSubscription>();
		for(Map<String,String> productMap : productList)
			manifestSubscriptions.add(new ManifestSubscription(productMap));
		
		return manifestSubscriptions;
*/		
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("name",					"^\\s+Name: (.+)");
		regexes.put("quantity",				"^\\s+Quantity: (.+)");
		regexes.put("created",				"^\\s+Created: (.+)");
		regexes.put("startDate",			"^\\s+Start Date: (.+)");
		regexes.put("endDate",				"^\\s+End Date: (.+)");
		regexes.put("supportLevel",			"^\\s+Service Level: (.+)");		// Bug 913302 - typo in msgid "Suport Level" and msgid "Suport Type"
		regexes.put("supportType",			"^\\s+Service Type: (.+)");			// Bug 913302 - typo in msgid "Suport Level" and msgid "Suport Type"
		regexes.put("architectures",		"^\\s+Architectures: (.+)");
		regexes.put("productId",			"^\\s+SKU: (.+)");					// Bug 913703 - rct cat-manifest > Subscription:> "Product Id:" should be labeled as "SKU:"	// Bug 878634 - String Updates: Capitalization of acronyms (URL, ID, HTTP, CPU)
		regexes.put("contract",				"^\\s+Contract: (.+)");
		regexes.put("subscriptionId",		"^\\s+Order: (.+)");			// Bug 913720 - rct cat-manifest > Subscription:> "Subscription Id:" should be labeled as "Order Number:"	// Bug 878634 - String Updates: Capitalization of acronyms (URL, ID, HTTP, CPU)	// cahnged from Order Number back to Order https://bugzilla.redhat.com/show_bug.cgi?id=913720#c6 
		regexes.put("entitlementFile",		"^\\s+Entitlement File: (.+)");
		regexes.put("certificateFile",		"^\\s+Certificate File: (.+)");
		regexes.put("certificateVersion",	"^\\s+Certificate Version: (.+)");
		regexes.put("providedProducts",		"^\\s+Provided Products:(.*(\\n.*?)+)^\\s+Content Sets:");	// assumes "Content Sets:" is the next group
		regexes.put("contentSets",			"^\\s+Content Sets:(.*(\\n.*?)+)(?:[\\s\\w]+:|$)");

		
		// find all the raw "Subscription:" groupings and then create one ManifestSubscription per raw "Subscription:" grouping
		String rawSubscrtionRegex = "Subscription:((\\n.+)+)";
		List<ManifestSubscription> manifestSubscriptions = new ArrayList<ManifestSubscription>();
		Matcher m = Pattern.compile(rawSubscrtionRegex, Pattern.MULTILINE).matcher(rawManifest);
		while (m.find()) {
			String rawSubscription = m.group(1);
			
			// find a list of all the certData matching the subscription fields in the map of regexes
			List<Map<String,String>> certDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawSubscription, certDataList, field);
			}
			
			// assert that there is only one group of certData found in the list for this subscription grouping
			if (certDataList.size()!=1) Assert.fail("Error when parsing raw subscription group.  Expected to parse only one group of subscription data from:\n"+m.group(0));
			Map<String,String> certData = certDataList.get(0);
			
			// create a new ContentNamespace
			manifestSubscriptions.add(new ManifestSubscription(certData));
		}
		return manifestSubscriptions;
		
		
		
		
	}
}


