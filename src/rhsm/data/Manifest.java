package rhsm.data;

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
public class Manifest extends AbstractCommandLineData {

	// abstraction fields
	public String server;
	public String serverVersion;
	public Calendar dateCreated;
	public String creator;
	public String consumerName;
	public String consumerUUID;
	public String consumerType;
	public List<ManifestSubscription> subscriptions;

	public String rawManifest;


	public Manifest(String rawManifest, Map<String, String> manifestData){
		super(manifestData);		
		this.subscriptions = ManifestSubscription.parse(rawManifest);
		this.rawManifest = rawManifest;
	}
	
	
	@Override
	public String toString() {
		
		String string = "";
		if (server != null)			string += String.format(" %s='%s'", "server",server);
		if (serverVersion != null)	string += String.format(" %s='%s'", "serverVersion",serverVersion);
		if (dateCreated != null)	string += String.format(" %s='%s'", "dateCreated",formatDateString(dateCreated));
		if (creator != null)		string += String.format(" %s='%s'", "creator",creator);
		if (consumerName != null)	string += String.format(" %s='%s'", "consumerName",consumerName);
		if (consumerUUID != null)	string += String.format(" %s='%s'", "consumerUUID",consumerUUID);
		if (consumerType != null)	string += String.format(" %s='%s'", "consumerType",consumerType);
	
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
	 * @param rawManifests - stdout from: # find /tmp/sm-testManifestsDir/auto-services.usersys.redhat.com/test-manifests/ -regex "/.+\.zip" -exec rct cat-manifest {} \;
	 * @return
	 */
	static public List<Manifest> parse(String rawManifests) {
		
		//[root@jsefler-7 ~]# find /tmp/sm-testManifestsDir/auto-services.usersys.redhat.com/test-manifests/ -regex "/.+\.zip" -exec rct cat-manifest {} \;
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
		
		
		Map<String,String> regexes = new HashMap<String,String>();
		
		// abstraction field				regex pattern (with a capturing group) Note: the captured group will be trim()ed
		regexes.put("server",				"General:(?:(?:\\n.+)+)Server: (.+)");
		regexes.put("serverVersion",		"General:(?:(?:\\n.+)+)Server Version: (.+)");
		regexes.put("dateCreated",			"General:(?:(?:\\n.+)+)Date Created: (.+)");
		regexes.put("creator",				"General:(?:(?:\\n.+)+)Creator: (.+)");
		regexes.put("consumerName",			"Consumer:(?:(?:\\n.+)+)Name: (.+)");
		regexes.put("consumerUUID",			"Consumer:(?:(?:\\n.+)+)UUID: (.+)");
		regexes.put("consumerType",			"Consumer:(?:(?:\\n.+)+)Type: (.+)");
		
		// split the rawManifests process each individual rawManifest
		String rawManifestRegex = "\\+-+\\+\\n\\s+Manifest\\n\\+-+\\+";
		List<Manifest> manifests = new ArrayList<Manifest>();
		for (String rawManifest : rawManifests.split(rawManifestRegex)) {
			
			// strip leading and trailing blank lines and skip blank rawManifest
			rawManifest = rawManifest.replaceAll("^\\n*","").replaceAll("\\n*$", "");
			if (rawManifest.length()==0) continue;
			
			List<Map<String,String>> manifestDataList = new ArrayList<Map<String,String>>();
			for(String field : regexes.keySet()){
				Pattern pat = Pattern.compile(regexes.get(field), Pattern.MULTILINE);
				addRegexMatchesToList(pat, rawManifest, manifestDataList, field);
			}
			
			// assert that there is only one group of manifestData found in the list
			if (manifestDataList.size()!=1) Assert.fail("Error when parsing raw manifest.  Expected to parse only one group of manifest data.");
			Map<String,String> manifestData = manifestDataList.get(0);
			
			// create a new Manifest
			manifests.add(new Manifest(rawManifest, manifestData));
		}
		return manifests;
	}
}


