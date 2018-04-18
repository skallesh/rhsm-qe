package rhsm.cli.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.SkipException;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.jul.TestRecords;

import rhsm.base.CandlepinType;
import rhsm.base.ConsumerType;
import rhsm.base.SubscriptionManagerBaseTestScript;
import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.CertStatistics;
import rhsm.data.ConsumerCert;
import rhsm.data.ContentNamespace;
import rhsm.data.EntitlementCert;
import rhsm.data.InstalledProduct;
import rhsm.data.Org;
import rhsm.data.ProductCert;
import rhsm.data.ProductNamespace;
import rhsm.data.ProductSubscription;
import rhsm.data.Repo;
import rhsm.data.SubscriptionPool;
import rhsm.data.YumRepo;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;
import com.redhat.qe.tools.SSHCommandRunner;

/**
 * @author jsefler
 *
 */
public class SubscriptionManagerTasks {

	protected static Logger log = Logger.getLogger(SubscriptionManagerTasks.class.getName());
	public SSHCommandRunner sshCommandRunner = null;
	public final String command				= "subscription-manager";
	public final String redhatRepoFile		= "/etc/yum.repos.d/redhat.repo";
	public final String redhatRepoServerValueFile	= "/var/lib/rhsm/repo_server_val/redhat.repo";
	public final String rhsmConfFile		= "/etc/rhsm/rhsm.conf";
	public final String rhsmLoggingConfFile	= "/etc/rhsm/logging.conf";	// NO LONGER EXISTS in subscription-manager-1.17.10-1	// RHEL7.3 commit d84b15f42c2e4521e130b939039960c0846b849c 1334916: Move logging configuration to rhsm.conf
	public final String factsDir			= "/etc/rhsm/facts";
	public final String rhsmUpdateFile		= "/var/run/rhsm/update"; // during RHEL74: replaced by two files /var/run/rhsm/next_auto_attach_update and /var/run/rhsm/next_cert_check_update by commit e9f8421285fc6541166065a8b55ee89b9a425246 1435013: Add splay option to rhsmcertd, randomize over interval
	public final String yumPluginConfFileForSubscriptionManager	= "/etc/yum/pluginconf.d/subscription-manager.conf"; // "/etc/yum/pluginconf.d/rhsmplugin.conf"; renamed by dev on 11/24/2010
	public final String yumPluginConfFileForProductId			= "/etc/yum/pluginconf.d/product-id.conf";
	public final String yumPluginConfFileForRhn					= "/etc/yum/pluginconf.d/rhnplugin.conf";
	public final String yumPluginConfFileForSearchDisabledRepos	= "/etc/yum/pluginconf.d/search-disabled-repos.conf";
	public final String rhnSystemIdFile		= "/etc/sysconfig/rhn/systemid";
	public final String rhnUp2dateFile		= "/etc/sysconfig/rhn/up2date";
	public final String rhsmFactsJsonFile	= "/var/lib/rhsm/facts/facts.json";
	public final String productIdJsonFile	= "/var/lib/rhsm/productid.js";	// maps a product id to the repository from which it came; managed by subscription-manager's ProductDatabase python class
	public final String rhsmCacheDir		= "/var/lib/rhsm/cache";
	public final String rhsmCacheRepoOverridesFile	= rhsmCacheDir+"/content_overrides.json";
	public final String certVersionFactsFilename	= "automation_forced_certificate_version.facts";
	public final String overrideFactsFilename		= "automation_override.facts";
	public final String brandingDir			= "/usr/share/rhsm/subscription_manager/branding";
	public final String rhsmcertdLogFile	= "/var/log/rhsm/rhsmcertd.log";
	public final String rhsmLogFile			= "/var/log/rhsm/rhsm.log";
	public final String messagesLogFile		= "/var/log/messages";
	public final String auditLogFile		= "/var/log/audit/audit.log";
	public final String rhsmCertD			= "rhsmcertd";
	public final String rhsmCertDWorker		= "/usr/libexec/rhsmcertd-worker";
	public final String rhsmComplianceD		= "/usr/libexec/rhsmd";	// /usr/libexec/rhsm-complianced; RHEL61
	public final String rhnDefinitionsDir	= "/tmp/"+"rhnDefinitionsDir";
	public final String productCertDefaultDir		= "/etc/pki/product-default";	// introduced by Bug 1123029 - Use default product certificates when they are present
																					// [root@jsefler-os6 ~]# rpm -q --whatprovides /etc/pki/product-default
																					// redhat-release-server-6Server-6.7.0.2.el6.x86_64

	
	// will be initialized after installSubscriptionManagerRPMs()
	public String msg_ConsumerNotRegistered			= null;
	public String msg_NeedListOrUpdateOption		= null;
	public String msg_NetworkErrorUnableToConnect	= null;
	public String msg_NetworkErrorCheckConnection	= null;
	public String msg_RemoteErrorCheckConnection	= null;
	public String msg_ProxyConnectionFailed			= null;
	public String msg_ProxyConnectionFailed407		= null;
	public String msg_ProxyErrorUnableToConnect		= null;
	public String msg_ClockSkewDetection			= null;
	public String msg_ContainerMode					= null;
	public String msg_InteroperabilityWarning		= null;
	
	// will be initialized by initializeFieldsFromConfigFile()
	public String productCertDir					= null; // "/etc/pki/product";
	public String entitlementCertDir				= null; // "/etc/pki/entitlement";
	public String consumerCertDir					= null; // "/etc/pki/consumer";
	public String caCertDir							= null; // "/etc/rhsm/ca";
	public String baseurl							= null;
	public String consumerKeyFile()	{				return this.consumerCertDir+"/key.pem";}
	public String consumerCertFile() {				return this.consumerCertDir+"/cert.pem";}
	
	// will be initialized by constructor SubscriptionManagerTasks(SSHCommandRunner runner)
	public String redhatRelease						= null;	// of the client; Red Hat Enterprise Linux Server release 5.8 Beta (Tikanga)
	public String redhatReleaseX					= null;	// of the client; 5
	public String redhatReleaseXY					= null;	// of the client; 5.8
	public String hostname							= null;	// of the client
	public String ipaddr							= null;	// of the client
	public String arch								= null;	// of the client
	public String sockets							= null;	// of the client
	public String coresPerSocket					= null;	// of the client
	public String cores								= null;	// of the client
	public String vcpu								= null;	// of the client
	public String variant							= null;	// of the client
	public String releasever						= null;	// of the client; 5Server 5Client
	public String compose							= "";	// from beaker
	public Boolean isFipsEnabled					= null; // of the client	sysctl crypto.fips_enabled => crypto.fips_enabled = 1
	
	protected String currentlyRegisteredUsername	= null;	// most recent username used during register
	protected String currentlyRegisteredPassword	= null;	// most recent password used during register
	protected String currentlyRegisteredOrg			= null;	// most recent owner used during register
	protected ConsumerType currentlyRegisteredType	= null;	// most recent consumer type used during register
	
	public String candlepinAdminUsername = null;	// hold onto the candlepin admin creds to facilitate calling static CandlepinTasks
	public String candlepinAdminPassword = null;	// hold onto the candlepin admin creds to facilitate calling static CandlepinTasks
	public String candlepinUrl = null;				// hold onto the candlepin url to facilitate calling static CandlepinTasks
	
	public SubscriptionManagerTasks(SSHCommandRunner runner) {
		super();
		sshCommandRunner = runner;
		hostname		= sshCommandRunner.runCommandAndWait("hostname").getStdout().trim();
		//ipaddr			= sshCommandRunner.runCommandAndWait("ip addr show | egrep 'scope global (dynamic )?eth' | cut -d/ -f1 | sed s/inet//g").getStdout().trim();
		//ipaddr			= sshCommandRunner.runCommandAndWait("ip addr show | egrep 'scope global (dynamic )?e' | cut -d/ -f1 | sed s/inet//g").getStdout().trim();
		//ipaddr			= sshCommandRunner.runCommandAndWait("for DEVICE in $(ip addr show | egrep 'state (UP|UNKNOWN)' | cut -f2 -d':' | sed 's/ //'); do ip addr show $DEVICE | egrep 'scope global .*'$DEVICE | cut -d'/' -f1 | sed 's/ *inet *//g'; done;").getStdout().trim();	// state is UNKNOWN on ppc64	// does not know how to choose when you have a physical system with two active ip devices - default and bridge
		ipaddr			= sshCommandRunner.runCommandAndWait("ip addr show $(ip route | awk '$1 == \"default\" {print $5}' | uniq) | egrep 'inet [[:digit:]]+\\.[[:digit:]]+\\.[[:digit:]]+\\.[[:digit:]]+.* scope global' | awk '{print $2}' | cut -d'/' -f1").getStdout().trim();	// Note: this value is identical to facts net.interface.eth0.ipv4_address and network.ipv4_address but NOT on an openstack instance where network.ipv4_address is different
		arch			= sshCommandRunner.runCommandAndWait("uname --machine").getStdout().trim();  // uname -i --hardware-platform :print the hardware platform or "unknown"	// uname -m --machine :print the machine hardware name
		//releasever		= sshCommandRunner.runCommandAndWait("rpm -q --qf \"%{VERSION}\\n\" --whatprovides /etc/redhat-release").getStdout().trim();  // e.g. 5Server		// cut -f 5 -d : /etc/system-release-cpe	// rpm -q --queryformat "%{VERSION}\n" --whatprovides system-release		// rpm -q --queryformat "%{VERSION}\n" --whatprovides /etc/redhat-release	// does not work on RHEL7, returns "7.0" instead of "7Server"
		releasever		= sshCommandRunner.runCommandAndWait("python -c 'import yum, pprint; yb = yum.YumBase(); pprint.pprint(yb.conf.yumvar[\"releasever\"], width=1)' | egrep -v 'Loaded plugins|This system' | cut -f 2 -d \\'").getStdout().trim();  // e.g. 5Server 6Server 7Server	// python -c 'import yum, pprint; yb = yum.YumBase(); pprint.pprint(yb.conf.yumvar["releasever"], width=1)' | grep -v 'Loaded plugins' | cut -f 2 -d \'
		
		//		rhsmComplianceD	= sshCommandRunner.runCommandAndWait("rpm -ql subscription-manager | grep libexec/rhsm").getStdout().trim();
		redhatRelease	= sshCommandRunner.runCommandAndWait("cat /etc/redhat-release").getStdout().trim();
		redhatReleaseXY = sshCommandRunner.runCommandAndWait("cat /etc/redhat-release").getStdout().trim();
		if (redhatRelease.contains("Server")) variant = "Server";	//69.pem
		if (redhatRelease.contains("Client")) variant = "Client";	//68.pem   (aka Desktop)
		if (redhatRelease.contains("Workstation")) variant = "Workstation";	//71.pem
		if (redhatRelease.contains("ComputeNode")) variant = "ComputeNode";	//76.pem
		//if (redhatRelease.contains("IBM POWER")) variant = "IBM Power";	//74.pem	Red Hat Enterprise Linux for IBM POWER	// TODO  Not sure if these are correct or if they are just Server on a different arch
		//if (redhatRelease.contains("IBM System z")) variant = "System Z";	//72.pem	Red Hat Enterprise Linux for IBM System z	// TODO

		Pattern pattern = Pattern.compile("\\d+\\.\\d+"/*,Pattern.DOTALL*/);
		Matcher matcher = pattern.matcher(redhatRelease);
		Assert.assertTrue(matcher.find(),"Extracted RHEL redhatReleaseXY from '"+redhatRelease+"'");
		redhatReleaseXY = matcher.group();
		redhatReleaseX = redhatReleaseXY.replaceFirst("\\..*", "");
		
		// TODO This is a WORKAROUND to treat an ARM Development Preview as a RHEL7.0 system.  Not sure if this is what we ultimately want.
		/* 3/18/2015 NO LONGER NEEDED NOW THAT THE GA PRODUCT Red Hat Enterprise Linux Server for ARM ProductId=294 HAS BEEN CREATED
		// Product:	ID: 261		Name: Red Hat Enterprise Linux Server for ARM Development Preview	Version: Snapshot	Arch: aarch64	Tags: rhsa-dp-server,rhsa-dp-server-7	Brand Type: 	Brand Name: 
		// [root@apm-mustang-ev3-04 ~]# cat /etc/redhat-release 
		// Red Hat Enterprise Linux Server for ARM Development Preview
		// Red Hat Enterprise Linux Server for ARM (Development Preview release 1.6)
		if (redhatRelease.startsWith("Red Hat Enterprise Linux Server for ARM") && Integer.valueOf(redhatReleaseX)<7) {	// Red Hat Enterprise Linux Server for ARM Development Preview release 1.2
			log.warning("Detected an Red Hat Enterprise Linux Server for ARM system.  Automation will effectively assume a RHEL7.0 system.");
			redhatReleaseXY = "7.0";
			redhatReleaseX = "7";
		}
		*/
		
		// compose
		// /etc/yum.repos.d/beaker-Server.repo:baseurl=http://download.eng.rdu.redhat.com/rel-eng/RHEL-ALT-7.5-20180110.1/compose/Server/aarch64/os
		String tree = sshCommandRunner.runCommandAndWait("grep RHEL /etc/yum.repos.d/beaker-*.repo | sort | tail -1").getStdout();
		if (SubscriptionManagerCLITestScript.doesStringContainMatches(tree, "RHEL-.*/compose")) {	// RHEL-ALT-7.5-20180110.1/compose
			compose = SubscriptionManagerCLITestScript.getSubstringMatches(tree, "RHEL-.*/compose").get(0).split("/")[0];	// RHEL-ALT-7.5-20180110.1
		}
		
		// FIPS mode
		isFipsEnabled = sshCommandRunner.runCommandAndWait("sysctl crypto.fips_enabled").getStdout().trim().equals("crypto.fips_enabled = 1")? true:false;
		
		// predict sockets on the system   http://libvirt.org/formatdomain.html#elementsCPU
		/* 5/6/2013: DON'T PREDICT THIS USING lscpu ANY MORE.  IT LEADS TO TOO MANY TEST FAILURES TO TROUBLESHOOT.  INSTEAD, RELY ON FactsTests.MatchingCPUSocketsFact_Test() TO ASSERT BUGZILLA Bug 751205 - cpu_socket(s) facts value occasionally differs from value reported by lscpu (which is correct?)
		if (Float.valueOf(redhatReleaseXY) < 6.0f) {
			sockets = sshCommandRunner.runCommandAndWait("for cpu in `ls -1 /sys/devices/system/cpu/ | egrep cpu[[:digit:]]`; do echo \"cpu `cat /sys/devices/system/cpu/$cpu/topology/physical_package_id`\"; done | grep cpu | sort | uniq | wc -l").getStdout().trim();  // Reference: Bug 707292 - cpu socket detection fails on some 5.7 i386 boxes
		} else if (Float.valueOf(redhatReleaseXY) < 6.4f) {
			sockets = sshCommandRunner.runCommandAndWait("lscpu | grep 'CPU socket(s)'").getStdout().split(":")[1].trim();	// CPU socket(s):         2	
		} else {
			sockets = sshCommandRunner.runCommandAndWait("lscpu | grep 'Socket(s)'").getStdout().split(":")[1].trim();	// Socket(s):             2
		}
		* INSTEAD, CALL initializeRamCoreSockets() */
		
		// copy RHNS-CA-CERT to RHN-ORG-TRUSTED-SSL-CERT on RHEL7 as a workaround for Bug 906875 ERROR: can not find RHNS CA file: /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT 
// FIXME Not convinced that this workaround is needed anymore.  Surrounding by if (false) at the start of the rhel71 test cycle...
// This is a better solution... SubscriptionManagerCLITestScript.setupRhnCACert();		
if (false) {
		if (Integer.valueOf(redhatReleaseX)>=7) {
			log.info("Invoking the following suggestion to enable this rhel7 system to use rhn-client-tools https://bugzilla.redhat.com/show_bug.cgi?id=906875#c2 ");
			sshCommandRunner.runCommandAndWait("cp -n /usr/share/rhn/RHNS-CA-CERT /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT"); 
			// will not work anymore because...
			//   The certificate /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT is expired. Please ensure you have the correct certificate and your system time is correct.
			//   See /var/log/up2date for more information
		}
}
		
		// assert some properties for this instance
		Assert.assertTrue(ipaddr.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"), "Detected ip address '"+ipaddr+"' for client '"+hostname+"' which appears successful.");
	}
	
	
	/**
	 * Initialize/determine the system values for ram cores vcpu and sockets which are all used in the determination of a system's compliance (depending on the is_guest fact)
	 * Must be called after installSubscriptionManagerRPMs(...)
	 * 
	 */
	public void initializeSystemComplianceAttributes() {
		// STORE THE subscription-manager fact for "cpu.cpu_socket(s)".  THIS IS THE VALUE CANDLEPIN USES FOR HARDWARE RULES.
		removeAllFacts();
		
		// sockets
		String cpuSocketsFact = "cpu.cpu_socket(s)";
		sockets = getFactValue(cpuSocketsFact);	//  (will be ingored for compliance on a virtual system)
		//Assert.assertTrue(SubscriptionManagerCLITestScript.isInteger(sockets) && Integer.valueOf(sockets)>0, "Subscription manager facts '"+cpuSocketsFact+"' value '"+sockets+"' is a positive integer.");
		if (!SubscriptionManagerCLITestScript.isInteger(sockets)) {
			log.warning("When no '"+cpuSocketsFact+"' fact is present, the hardware rules should treat this system as a 1 socket system.  Therefore automation will assume this is a one socket system.");
			sockets = "1";
		}
		
		// cores
		String cpuCoresPerSocketFact = "cpu.core(s)_per_socket";
		coresPerSocket = getFactValue(cpuCoresPerSocketFact);
		//Assert.assertTrue(SubscriptionManagerCLITestScript.isInteger(coresPerSocket) && Integer.valueOf(coresPerSocket)>0, "Subscription manager facts '"+cpuCoresPerSocketFact+"' value '"+coresPerSocket+"' is a positive integer.");
		if (!SubscriptionManagerCLITestScript.isInteger(coresPerSocket)) {
			log.warning("When no '"+cpuCoresPerSocketFact+"' fact is present, the hardware rules should treat this system as a 1 core_per_socket system.  Therefore automation will assume this is a one core_per_socket system.");
			coresPerSocket = "1";
		}
		cores = String.valueOf(Integer.valueOf(sockets)*Integer.valueOf(coresPerSocket));	//  (will be ignored for compliance on a virtual system)
		
		// ram
		// ram = getFactValue("memory.memtotal"); //TODO determine what the ram is on the system; is this adequate?
		
		// vcpu
		vcpu = cores;	// vcpu count on a virtual system is treated as equivalent to cores (will be ignored for compliance on a physical system)
	}
	
	
	/**
	 * Must be called after installSubscriptionManagerRPMs(...)
	 */
	public void initializeFieldsFromConfigFile() {
		if (RemoteFileTasks.testExists(sshCommandRunner, rhsmConfFile)) {
			this.consumerCertDir	= getConfFileParameter(rhsmConfFile, "consumerCertDir").replaceFirst("/$", "");
			this.entitlementCertDir	= getConfFileParameter(rhsmConfFile, "entitlementCertDir").replaceFirst("/$", "");
			this.productCertDir		= getConfFileParameter(rhsmConfFile, "productCertDir").replaceFirst("/$", "");
			this.caCertDir			= getConfFileParameter(rhsmConfFile, "ca_cert_dir").replaceFirst("/$", "");
			this.baseurl			= getConfFileParameter(rhsmConfFile, "baseurl").replaceFirst("/$", "");
			log.info(this.getClass().getSimpleName()+".initializeFieldsFromConfigFile() succeeded on '"+sshCommandRunner.getConnection().getRemoteHostname()+"'.");
		} else {
			log.warning("Cannot "+this.getClass().getSimpleName()+".initializeFieldsFromConfigFile() on '"+sshCommandRunner.getConnection().getRemoteHostname()+"' until file exists: "+rhsmConfFile);
		}
	}
	
	
	/**
	 * Must be called after initializeFieldsFromConfigFile(...)
	 * @param repoCaCertUrls
	 */
	public void installRepoCaCerts(List<String> repoCaCertUrls) {
		// transfer copies of CA certs that cane be used when generating yum repo configs 
		for (String repoCaCertUrl : repoCaCertUrls) {
			String repoCaCert = Arrays.asList(repoCaCertUrl.split("/")).get(repoCaCertUrl.split("/").length-1);
			log.info("Copying repo CA cert '"+repoCaCert+"' from "+repoCaCertUrl+"...");
			//File repoCaCertFile = new File(serverCaCertDir.replaceFirst("/$","/")+Arrays.asList(repoCaCertUrl.split("/|=")).get(repoCaCertUrl.split("/|=").length-1));
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"cd "+caCertDir+"; wget --no-clobber --no-check-certificate \""+repoCaCertUrl+"\"",Integer.valueOf(0),null,"."+repoCaCert+". saved|File ."+repoCaCert+". already there");
		}
	}
	
	
	/**
	 * Must be called after initializeFieldsFromConfigFile(...)
	 * @param repoCaCertFile
	 * @param toNewName
	 * @throws IOException
	 */
	public void installRepoCaCert(File repoCaCertFile, String toNewName) throws IOException {
		if (repoCaCertFile==null) return;
		if (toNewName==null) toNewName = repoCaCertFile.getName();
		
		// transfer the CA Cert File from the candlepin server to the clients so we can test in secure mode
		RemoteFileTasks.putFile(sshCommandRunner, repoCaCertFile.getPath(), caCertDir+"/"+toNewName, "0644");
		updateConfFileParameter(rhsmConfFile, "insecure", "0");
	}
	
	
	/**
	 * Must be called after installProductCerts(...)
	 * @param productCerts
	 * @throws IOException
	 */
	public void installProductCerts(List <File> productCerts) throws IOException {
		if (productCerts.size() > 0) {
			// directory must exist otherwise the copy will fail
			sshCommandRunner.runCommandAndWait("mkdir -p "+productCertDir);
		}

		for (File file : productCerts) {
			if (!file.exists()) Assert.fail("Local file '"+file.getPath()+"' does not exist.  Instruct the automator to fix this logic error.");
			RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), productCertDir+"/", "0644");
		}
	}
	
	/**
	 * When the Jenkins parameter UPDATE_ZSTREAM=true, this setup methods is called.<BR>
	 * Jenkins UPDATE_ZSTREAM Description: While setting up the client, configure a rhel-zstream.repo to <A HREF=http://download.devel.redhat.com/nightly/updates/>http://download.devel.redhat.com/nightly/updates/</A>latest-RHEL-X/compose/VARIANT[-optional]/ARCH/os and yum update the UPDATE_ZSTREAM_PACKAGES
	 * @param installOptions
	 * @param updatePackages - specific list of packages, or an empty list implies update all packages
	 * @param composeUrl - example: http://download.devel.redhat.com/nightly/updates/latest-RHEL-7/compose/
	 * @param brewUrl - example: http://download.devel.redhat.com/brewroot/repos/rhel-7.1-z-build/latest/
	 * @param ciMessage - value of CI_MESSAGE returned from the Jenkins CI event trigger
	 * @throws IOException
	 * @throws JSONException
	 */
	public void installZStreamUpdates(String installOptions, List<String> updatePackages, String composeUrl, String brewUrl, String ciMessage) throws IOException, JSONException {
		
		// I doubt these will ever be used
		if (Integer.valueOf(redhatReleaseX)==5) {installZStreamUpdates_THE_OLD_WAY(installOptions, updatePackages); return;}
		if (Float.valueOf(redhatReleaseXY)<6.7) {installZStreamUpdates_THE_OLD_WAY(installOptions, updatePackages); return;}
		
		// FIRST: prepare zstream repos for the updatePackages and then yum update
		
		// make sure installOptions begins with --disablerepo=* to make sure the updates ONLY come from the rhel-zstream repos we are about to define
		if (!installOptions.contains("--disablerepo=*")) installOptions = "--disablerepo=* "+installOptions;
		
		// avoid ERROR: can not find RHNS CA file: /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT
		installOptions += " --disableplugin=rhnplugin";
		
		// locally create a yum.repos.d zstream repos file
	    File file = new File("tmp/rhel-zstream-compose.repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    String archZStream = arch;
	    if (Integer.valueOf(redhatReleaseX)==5 && arch.equals("i686")) archZStream = "i386"; // only i386 arch packages are built in brew for RHEL5
	    if (Integer.valueOf(redhatReleaseX)==6 && arch.equals("i386")) archZStream = "i686"; // only i686 arch packages are built in brew for RHEL6
//THE_OLD_WAY	    String baseurl			= "http://download.devel.redhat.com/rel-eng/repos/RHEL-"+redhatReleaseXY+"-Z/"+archZStream;	// applicable on RHEL5 RHEL6
//THE_OLD_WAY	    if (Integer.valueOf(redhatReleaseX)==7) baseurl = baseurl.replace("/RHEL-", "/rhel-").replace("-Z/", "-z/");
	    // example: http://download.devel.redhat.com/nightly/updates/latest-RHEL-7/compose/Server/x86_64/os/
	    composeUrl = composeUrl.trim().replaceFirst("/$", "");	// strip trailing slash
	    String baseurl			= composeUrl+"/"+variant+"/"+archZStream+"/os";
	    String baseurlOptional	= composeUrl+"/"+variant+"-optional/"+archZStream+"/os";
	    
	    // test the baseurl; log a warning if "Not Found" and abort the ZStream Update
	    // dgregor says: 5.10 isn't EUS, so there wouldn't be an active RHEL-5.10-Z
	    if (sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurl).getStdout().contains("404 Not Found")) {
			log.warning("Skipping the install of ZStream updates since the baseurl is Not Found.");
	    	Assert.fail("The ZStream baseurl '"+baseurl+"' was Not Found.  Instruct the automator to verify the assembly of this baseurl.");
	    }
	    if (sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurlOptional).getStdout().contains("404 Not Found")) {
			log.warning("Skipping the install of ZStream updates since the baseurl is Not Found.");
	    	Assert.fail("The ZStream baseurl '"+baseurlOptional+"' was Not Found.  Instruct the automator to verify the assembly of this baseurl.");
	    }
		
		// write out the rows of the table...
    	Writer output = new BufferedWriter(new FileWriter(file));
	    
    	// write the zstream repo
		output.write("[rhel-zstream-"+variant+"-"+archZStream+"]\n");
		output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" "+archZStream+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
		output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
		output.write("baseurl  = "+baseurl+"\n");
		output.write("\n");
	    installOptions += " --enablerepo=rhel-zstream-"+variant+"-"+archZStream;

	    // write the zstream optional repo
		output.write("[rhel-zstream-"+variant+"-optional-"+archZStream+"]\n");
		output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" optional "+archZStream+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
		output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
		output.write("baseurl  = "+baseurlOptional+"\n");
		//output.write("skip_if_unavailable = 1\n");	// if set to True yum will continue running if this repository cannot be contacted for any reason.
		output.write("\n");
	    installOptions += " --enablerepo=rhel-zstream-"+variant+"-optional-"+archZStream;
	    
	    // TODO I don't think this block will ever be used since this came from installZStreamUpdates_THE_OLD_WAY 
	    // special cases repos based on arch
	    if (archZStream.equals("ppc64") && Integer.valueOf(redhatReleaseX)<7) {
			output.write("[rhel-zstream-"+variant+"-ppc]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" ppc\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurl.replaceAll("ppc64$", "ppc")+"\n");
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-ppc";
		    
			output.write("[rhel-zstream-"+variant+"-optional-ppc]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" optional ppc\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurlOptional+"\n");
			//output.write("skip_if_unavailable = 1\n");	// if set to True yum will continue running if this repository cannot be contacted for any reason.
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-optional-ppc";
	    }
	    
	    if (archZStream.equals("s390x") && Integer.valueOf(redhatReleaseX)<7) {
			output.write("[rhel-zstream-"+variant+"-s390]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" s390\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurl.replaceAll("s390x$", "s390")+"\n");
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-s390";
		    
			output.write("[rhel-zstream-"+variant+"-optional-s390]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" optional s390\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurlOptional+"\n");
			//output.write("skip_if_unavailable = 1\n");	// if set to True yum will continue running if this repository cannot be contacted for any reason.
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-optional-s390";
	    }
	    
	    if (archZStream.equals("x86_64") && Integer.valueOf(redhatReleaseX)==5) {
			output.write("[rhel-zstream-"+variant+"-i386]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" i386\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurl.replaceAll("x86_64$", "i386")+"\n");
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-i386";
		    
			output.write("[rhel-zstream-"+variant+"-optional-i386]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" optional i386\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurlOptional+"\n");
			//output.write("skip_if_unavailable = 1\n");	// if set to True yum will continue running if this repository cannot be contacted for any reason.
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-optional-i386";
	    }
	    
	    if (archZStream.equals("x86_64") && Integer.valueOf(redhatReleaseX)==6) {
			output.write("[rhel-zstream-"+variant+"-i686]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" i686\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurl.replaceAll("x86_64$", "i686")+"\n");
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-i686";
		    
			output.write("[rhel-zstream-"+variant+"-optional-i686]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+variant+" optional i686\n");
			output.write("enabled  = 0\n");
			output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurlOptional+"\n");
			//output.write("skip_if_unavailable = 1\n");	// if set to True yum will continue running if this repository cannot be contacted for any reason.
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+variant+"-optional-i686";
	    }
	    output.close();
		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		// assemble the packages to be updated (note: if the list is empty, then all packages will be updated)
		String updatePackagesAsString = "";
		for (String updatePackage : updatePackages) updatePackagesAsString += updatePackage+" "; updatePackagesAsString=updatePackagesAsString.trim();
		
		// run yum update
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y update "+updatePackagesAsString+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum updated from zstream repo: "+baseurl);	// exclude all redhat-release* packages for safety; rhel5-client will undesirably upgrade from redhat-release-5Client-5.9.0.2 ---> Package redhat-release.x86_64 0:5Server-5.9.0.2 set to be updated
		
		
		// SECOND: prepare zstream repo for brew where the CI_MESSAGE rpms can be found and then yum update each rpm from the CI_MESSAGE
		
		// locally create a yum.repos.d zstream repos file
	    file = new File("tmp/rhel-zstream-brew.repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    
		// example: http://download.devel.redhat.com/brewroot/repos/rhel-7.1-z-build/latest/$basearch/
		brewUrl = brewUrl.trim().replaceFirst("/$", "");	// strip trailing slash
		baseurl = brewUrl+"/"+archZStream; // this should also work: brewUrl+"/$basearch"
		
		// write out the rows of the table...
    	output = new BufferedWriter(new FileWriter(file));
		
    	// write the zstream repo
		output.write("[rhel-zstream-brew]\n");
		output.write("name     = Z-Stream updates from brew\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the z-stream packages may not be signed until on REL_PREP
		output.write("baseurl  = "+baseurl+"\n");
		output.write("\n");
	    installOptions += " --enablerepo=rhel-zstream-brew";
	    output.close();
		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		// no need to continue installing anything when CI_MESSAGE is empty
		if (ciMessage.isEmpty()) return;
		
		// get the list of rpms from  CI_MESSSAGE
		//	CI_MESSAGE:
		//		{
		//		  "tag" : {
		//		    "maven_support" : false,
		//		    "locked" : false,
		//		    "name" : "rhel-7.1-pending",
		//		    "perm" : "trusted",
		//		    "perm_id" : 6,
		//		    "arches" : null,
		//		    "maven_include_all" : false,
		//		    "id" : 6220
		//		  },
		//		  "force" : null,
		//		  "build" : {
		//		    "owner_name" : "lnykryn",
		//		    "package_name" : "systemd",
		//		    "task_id" : 8070969,
		//		    "volume_name" : "DEFAULT",
		//		    "owner_id" : 1615,
		//		    "creation_event_id" : 9961102,
		//		    "creation_time" : "2014-10-07 06:41:30.725054",
		//		    "state" : 1,
		//		    "nvr" : "systemd-208-13.el7",
		//		    "completion_time" : "2014-10-07 06:51:17.991341",
		//		    "epoch" : null,
		//		    "version" : "208",
		//		    "creation_ts" : 1.41267849072505E9,
		//		    "volume_id" : 0,
		//		    "release" : "13.el7",
		//		    "package_id" : 34071,
		//		    "completion_ts" : 1.41267907799134E9,
		//		    "id" : 389811,
		//		    "name" : "systemd"
		//		  },
		//		  "user" : {
		//		    "status" : 0,
		//		    "usertype" : 0,
		//		    "krb_principal" : "host/rcm-rhel7.app.eng.bos.redhat.com@REDHAT.COM",
		//		    "id" : 2504,
		//		    "name" : "host/rcm-rhel7.app.eng.bos.redhat.com"
		//		  },
		//		  "rpms" : {
		//		    "s390x" : [ "libgudev1-208-13.el7.s390x.rpm", "libgudev1-devel-208-13.el7.s390x.rpm", "systemd-208-13.el7.s390x.rpm", "systemd-debuginfo-208-13.el7.s390x.rpm", "systemd-devel-208-13.el7.s390x.rpm", "systemd-journal-gateway-208-13.el7.s390x.rpm", "systemd-libs-208-13.el7.s390x.rpm", "systemd-python-208-13.el7.s390x.rpm", "systemd-sysv-208-13.el7.s390x.rpm" ],
		//		    "i686" : [ "libgudev1-208-13.el7.i686.rpm", "libgudev1-devel-208-13.el7.i686.rpm", "systemd-208-13.el7.i686.rpm", "systemd-debuginfo-208-13.el7.i686.rpm", "systemd-devel-208-13.el7.i686.rpm", "systemd-journal-gateway-208-13.el7.i686.rpm", "systemd-libs-208-13.el7.i686.rpm", "systemd-python-208-13.el7.i686.rpm", "systemd-sysv-208-13.el7.i686.rpm" ],
		//		    "ppc64" : [ "libgudev1-208-13.el7.ppc64.rpm", "libgudev1-devel-208-13.el7.ppc64.rpm", "systemd-208-13.el7.ppc64.rpm", "systemd-debuginfo-208-13.el7.ppc64.rpm", "systemd-devel-208-13.el7.ppc64.rpm", "systemd-journal-gateway-208-13.el7.ppc64.rpm", "systemd-libs-208-13.el7.ppc64.rpm", "systemd-python-208-13.el7.ppc64.rpm", "systemd-sysv-208-13.el7.ppc64.rpm" ],
		//		    "x86_64" : [ "libgudev1-208-13.el7.x86_64.rpm", "libgudev1-devel-208-13.el7.x86_64.rpm", "systemd-208-13.el7.x86_64.rpm", "systemd-debuginfo-208-13.el7.x86_64.rpm", "systemd-devel-208-13.el7.x86_64.rpm", "systemd-journal-gateway-208-13.el7.x86_64.rpm", "systemd-libs-208-13.el7.x86_64.rpm", "systemd-python-208-13.el7.x86_64.rpm", "systemd-sysv-208-13.el7.x86_64.rpm" ],
		//		    "s390" : [ "libgudev1-208-13.el7.s390.rpm", "libgudev1-devel-208-13.el7.s390.rpm", "systemd-208-13.el7.s390.rpm", "systemd-debuginfo-208-13.el7.s390.rpm", "systemd-devel-208-13.el7.s390.rpm", "systemd-journal-gateway-208-13.el7.s390.rpm", "systemd-libs-208-13.el7.s390.rpm", "systemd-python-208-13.el7.s390.rpm", "systemd-sysv-208-13.el7.s390.rpm" ],
		//		    "ppc" : [ "libgudev1-208-13.el7.ppc.rpm", "libgudev1-devel-208-13.el7.ppc.rpm", "systemd-208-13.el7.ppc.rpm", "systemd-debuginfo-208-13.el7.ppc.rpm", "systemd-devel-208-13.el7.ppc.rpm", "systemd-journal-gateway-208-13.el7.ppc.rpm", "systemd-libs-208-13.el7.ppc.rpm", "systemd-python-208-13.el7.ppc.rpm", "systemd-sysv-208-13.el7.ppc.rpm" ],
		//		    "src" : [ "systemd-208-13.el7.src.rpm" ]
		//		  },
		//		  "tags" : [ "rhel-7.1-candidate", "rhel-7.1-pending" ]
		//		}
		JSONObject jsonCIMessage = new JSONObject(ciMessage);
		JSONArray jsonCIMessageRpms;
		if (jsonCIMessage.getJSONObject("rpms").has("noarch")) {
			jsonCIMessageRpms = jsonCIMessage.getJSONObject("rpms").getJSONArray("noarch");
		} else {
			jsonCIMessageRpms = jsonCIMessage.getJSONObject("rpms").getJSONArray(arch);
		}
		List<String> ciMessageRpms = new ArrayList<String>();
		for (int r = 0; r < jsonCIMessageRpms.length(); r++) ciMessageRpms.add(jsonCIMessageRpms.getString(r));
		String jsonCIMessageBuildVersion = jsonCIMessage.getJSONObject("build").getString("version");	// 208
		String jsonCIMessageBuildRelease = jsonCIMessage.getJSONObject("build").getString("release");	// 13.el7
		boolean atLeastOnePkgIsAlreadyInstalled=false;
		for (String rpm : ciMessageRpms) {
			String pkgVersionReleaseArch = rpm.replaceFirst(".rpm$", "");	// libgudev1-208-13.el7.s390x
			String pkg = pkgVersionReleaseArch.split("-"+jsonCIMessageBuildVersion+"-"+jsonCIMessageBuildRelease)[0];	// libgudev1
			
			if (isPackageInstalled(pkg)) {
				atLeastOnePkgIsAlreadyInstalled = true;
				
				// update the already installed package to the one from CI Message
				Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y update "+pkg+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum updated from zstream repo: "+baseurl);
				
				// assert that the CI_MESSAGE build version-release.arch is now installed;
				Assert.assertTrue(isPackageInstalled(pkgVersionReleaseArch),"Package '"+pkgVersionReleaseArch+"' from CI_MESSAGE is now installed.");
			}
		}
		if (!atLeastOnePkgIsAlreadyInstalled) Assert.fail("Regardless of build version-release, expected at least one of these dependent packages "+ciMessageRpms+" from CI_MESSAGE to already be installed.");
	}
	/**
	 * When the Jenkins parameter UPDATE_ZSTREAM=true, this setup methods is called.<BR>
	 * Jenkins UPDATE_ZSTREAM Description: While setting up the client, configure a rhel-zstream.repo to <A HREF=http://download.devel.redhat.com/rel-eng/repos/>http://download.devel.redhat.com/rel-eng/repos/</A>RHEL-X.Y-Z/ARCH and yum update the UPDATE_ZSTREAM_PACKAGES
	 * @param installOptions
	 * @param updatePackages
	 * @throws IOException
	 */
	//@Deprecated	// Deprecating this method with the introduction of Consolidated ZStream process https://mojo.redhat.com/docs/DOC-1021938 starting with RHEL 7.1.z and RHEL 6.7.z
	public void installZStreamUpdates_THE_OLD_WAY(String installOptions, List<String> updatePackages) throws IOException {
		
		// make sure installOptions begins with --disablerepo=* to make sure the updates ONLY come from the rhel-zstream repos we are about to define
		if (!installOptions.contains("--disablerepo=*")) installOptions = "--disablerepo=* "+installOptions;
		
		// avoid ERROR: can not find RHNS CA file: /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT
		installOptions += " --disableplugin=rhnplugin";
		
		// locally create a yum.repos.d zstream repos file
	    File file = new File("tmp/rhel-zstream.repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    String archZStream = arch;
	    if (Integer.valueOf(redhatReleaseX)==5 && arch.equals("i686")) archZStream = "i386"; // only i386 arch packages are built in brew for RHEL5
	    if (Integer.valueOf(redhatReleaseX)==6 && arch.equals("i386")) archZStream = "i686"; // only i686 arch packages are built in brew for RHEL6
	    String baseurl = "http://download.devel.redhat.com/rel-eng/repos/RHEL-"+redhatReleaseXY+"-Z/"+archZStream;
	    if (Integer.valueOf(redhatReleaseX)==7) baseurl = baseurl.replace("/RHEL-", "/rhel-").replace("-Z/", "-z/");
	    
	    // test the baseurl; log a warning if "Not Found" and abort the ZStream Update
	    // dgregor says: 5.10 isn't EUS, so there wouldn't be an active RHEL-5.10-Z
	    if (sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurl).getStdout().contains("404 Not Found")) {
			log.warning("Skipping the install of ZStream updates since the baseurl is Not Found.");
	    	return;
	    }
	    
	    try {
	    	Writer output = new BufferedWriter(new FileWriter(file));
			
			// write out the rows of the table
			output.write("[rhel-zstream-"+archZStream+"]\n");
			output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" "+archZStream+"\n");
			output.write("enabled  = 0\n");
			//output.write("gpgcheck = 0\n");	// not really needed since the z-stream packages are signed
			output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
			output.write("baseurl  = "+baseurl+"\n");
			output.write("\n");
		    installOptions += " --enablerepo=rhel-zstream-"+archZStream;
		    
		    if (archZStream.equals("ppc64")) {
				output.write("[rhel-zstream-ppc]\n");
				output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" ppc\n");
				output.write("enabled  = 0\n");
				//output.write("gpgcheck = 0\n");	// not really needed since the z-stream packages are signed
				output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
				output.write("baseurl  = "+baseurl.replaceAll("ppc64$", "ppc")+"\n");
				output.write("\n");
			    installOptions += " --enablerepo=rhel-zstream-ppc";
		    }
		    
		    if (archZStream.equals("s390x")) {
				output.write("[rhel-zstream-s390]\n");
				output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" s390\n");
				output.write("enabled  = 0\n");
				//output.write("gpgcheck = 0\n");	// not really needed since the z-stream packages are signed
				output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
				output.write("baseurl  = "+baseurl.replaceAll("s390x$", "s390")+"\n");
				output.write("\n");
			    installOptions += " --enablerepo=rhel-zstream-s390";
		    }
		    
		    if (archZStream.equals("x86_64") && Integer.valueOf(redhatReleaseX)==5) {
				output.write("[rhel-zstream-i386]\n");
				output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" i386\n");
				output.write("enabled  = 0\n");
				//output.write("gpgcheck = 0\n");	// not really needed since the z-stream packages are signed
				output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
				output.write("baseurl  = "+baseurl.replaceAll("x86_64$", "i386")+"\n");
				output.write("\n");
			    installOptions += " --enablerepo=rhel-zstream-i386";
		    }
		    
		    if (archZStream.equals("x86_64") && Integer.valueOf(redhatReleaseX)==6) {
				output.write("[rhel-zstream-i686]\n");
				output.write("name     = Z-Stream updates for RHEL"+redhatReleaseXY+" i686\n");
				output.write("enabled  = 0\n");
				//output.write("gpgcheck = 0\n");	// not really needed since the z-stream packages are signed
				output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
				output.write("baseurl  = "+baseurl.replaceAll("x86_64$", "i686")+"\n");
				output.write("\n");
			    installOptions += " --enablerepo=rhel-zstream-i686";
		    }
		    
		    output.close();
		    
		    //log.info(file.getCanonicalPath()+" exists="+file.exists()+" writable="+file.canWrite());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		// assemble the packages to be updated (note: if the list is empty, then all packages will be updated)
		String updatePackagesAsString = "";
		for (String updatePackage : updatePackages) updatePackagesAsString += updatePackage+" "; updatePackagesAsString=updatePackagesAsString.trim();

		// run yum update
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y update "+updatePackagesAsString+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum updated from zstream repo: "+baseurl);	// exclude all redhat-release* packages for safety; rhel5-client will undesirably upgrade from redhat-release-5Client-5.9.0.2 ---> Package redhat-release.x86_64 0:5Server-5.9.0.2 set to be updated
	}
	
	
	
	/**
	 * Configure a rhel-latest-extras.repo to http://download.devel.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/Server/x86_64/os/ and yum update the updatePackages
	 * @param installOptions
	 * @param updatePackages - specific list of packages, or an empty list implies update all packages
	 * @throws IOException
	 * @throws JSONException
	 */
	public void installLatestExtrasUpdates(String installOptions, List<String> updatePackages) throws IOException, JSONException {
		
		// FIRST: prepare a repo for the updatePackages and then yum update
		
		// make sure installOptions begins with --disablerepo=* to make sure the updates ONLY come from the rhel-latest-extras repo we are about to define
		if (false) {	// stop disabling the auto-subscribed base rhel repos because docker 2:1.12.6-38.1 now requires atomic-registries which depends on libyaml which comes from rhel-7-server-rpms or rhel-7-server-htb-rpms 
		if (!installOptions.contains("--disablerepo=*")) installOptions = "--disablerepo=* "+installOptions;
		}
		
		// avoid ERROR: can not find RHNS CA file: /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT
		installOptions += " --disableplugin=rhnplugin";
		
		// locally create a yum.repos.d extras repos file (and include latest-RHEL-7 for dependencies)

	    File file = new File("tmp/latest.repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    // http://download.devel.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/Server/x86_64/os/
	    String baseurlForExtras = "http://download.devel.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/"+variant+"/$basearch/os/";
	    baseurlForExtras = "http://download.devel.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/"+variant+"/x86_64/os/";	// 302 Found // The document has moved <a href="http://download-node-02.eng.bos.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/Workstation/x86_64/os/">here</a>
	    baseurlForExtras = "http://download-node-02.eng.bos.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/"+variant+"/x86_64/os/";
	    baseurlForExtras = "http://download-node-02.eng.bos.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/"+"Server"+"/x86_64/os/";	// "Server" is the ONLY compose for http://download-node-02.eng.bos.redhat.com/rel-eng/latest-EXTRAS-7-RHEL-7/compose/
	    String baseurlForDeps = "http://download-node-02.eng.bos.redhat.com/rel-eng/latest-RHEL-7/compose/"+variant+"/x86_64/os/";
    
	    // check the baseurl for problems
	    SSHCommandResult baseurlTestResult = sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurlForExtras);
	    if (baseurlTestResult.getStdout().contains("404 Not Found") || baseurlTestResult.getStdout().contains("The document has moved")) {
			log.warning("Cannot install updates from latest-EXTRAS-7-RHEL-7 since the baseurl '"+baseurlForExtras+"' is Not Found.");
	    	Assert.fail("The Latest Extras baseurl '"+baseurlForExtras+"' was Not Found.  Instruct the automator to verify the assembly of this baseurl.");
	    }
	    baseurlTestResult = sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurlForDeps);
	    if (baseurlTestResult.getStdout().contains("404 Not Found") || baseurlTestResult.getStdout().contains("The document has moved")) {
			log.warning("Cannot install updates from latest-RHEL-7 since the baseurl '"+baseurlForDeps+"' is Not Found.");
	    	Assert.fail("The Latest baseurl '"+baseurlForDeps+"' was Not Found.  Instruct the automator to verify the assembly of this baseurl.");
	    }
		
		// write out the rows of the table...
    	Writer output = new BufferedWriter(new FileWriter(file));
	    
    	// write the rhel-latest-extras repo
		String repo = "latest-EXTRAS-7-RHEL-7-"+variant;
		output.write("["+repo+"]\n");
		output.write("name     = Latest Extras updates for RHEL"+redhatReleaseX+" "+variant+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the latest extras packages may not be signed until on REL_PREP
		//output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
		output.write("baseurl  = "+baseurlForExtras+"\n");
	    installOptions += " --enablerepo="+repo;
		repo = "latest-RHEL-7-"+variant;
		output.write("["+repo+"]\n");
		output.write("name     = Latest updates for RHEL"+redhatReleaseX+" "+variant+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the latest packages may not be signed until on REL_PREP
		//output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
		output.write("baseurl  = "+baseurlForDeps+"\n");
	    installOptions += " --enablerepo="+repo;
		output.write("\n");
	    output.close();

		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		// assemble the packages to be updated (note: if the list is empty, then all packages will be updated)
		String updatePackagesAsString = "";
		for (String updatePackage : updatePackages) updatePackagesAsString += updatePackage+" "; updatePackagesAsString=updatePackagesAsString.trim();
		
		// run yum update
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y update "+updatePackagesAsString+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum updated from latest extras repo: "+baseurlForExtras);
	}
	
	
	
	/**
	 * Configure a rhel-latest-app-stream.repo to http://download.devel.redhat.com/nightly/latest-AppStream-8/compose/AppStream/x86_64/os/ and return the repo label
	 * @throws IOException
	 * @throws JSONException
	 */
	public String configureLatestAppStreamRepo() throws IOException, JSONException {
		
		// locally create a yum.repos.d AppStream repos file
		
	    File file = new File("tmp/latest-app-stream.repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    // http://download.devel.redhat.com/nightly/latest-AppStream-8/compose/AppStream/x86_64/os/
	    String baseurlForAppStream = "http://download.devel.redhat.com/nightly/latest-AppStream-"+redhatReleaseX+"/compose/AppStream/$basearch/os/";
	    baseurlForAppStream = "http://download-node-02.eng.bos.redhat.com/nightly/latest-AppStream-"+redhatReleaseX+"/compose/AppStream/$basearch/os/";
	    
	    // check the baseurl for problems
	    SSHCommandResult baseurlTestResult = sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurlForAppStream.replace("$basearch", arch));
	    if (baseurlTestResult.getStdout().contains("404 Not Found") || baseurlTestResult.getStdout().contains("The document has moved")) {
			log.warning("Cannot install updates from latest-AppStream-8 since the baseurl '"+baseurlForAppStream+"' is Not Found.");
	    	Assert.fail("The Latest AppStream baseurl '"+baseurlForAppStream+"' was Not Found.  Instruct the automator to verify the assembly of this baseurl.");
	    }
		
		// write out the rows of the table...
    	Writer output = new BufferedWriter(new FileWriter(file));
	    
    	// write the rhel-latest-extras repo
		String repo = "latest-AppStream-8";
		output.write("["+repo+"]\n");
		output.write("name     = Latest AppStream updates for RHEL"+redhatReleaseX+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the latest extras packages may not be signed until on REL_PREP
		//output.write("exclude  = redhat-release*\n");	// avoids unwanted updates of rhel-release server variant to workstation
		output.write("baseurl  = "+baseurlForAppStream+"\n");
		output.write("\n");
	    output.close();
	    
		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		return repo;
	}
	
	public void installReleasedRhnClassicPackages(String installOptions, List<String> installPackages) throws IOException, JSONException {
		
		// FIRST: prepare a repo for the packages and then yum install
		
		// make sure installOptions begins with --disablerepo=* to make sure the installs ONLY come from the released-rhn57-tools repo we are about to define
		if (!installOptions.contains("--disablerepo=*")) installOptions = "--disablerepo=* "+installOptions;
		
		// avoid ERROR: can not find RHNS CA file: /usr/share/rhn/RHN-ORG-TRUSTED-SSL-CERT
		installOptions += " --disableplugin=rhnplugin";
		
		
		
		// locally create a yum.repos.d released-rhn57-tools repo file
		String repo = "released-rhn57-tools";
	    File file = new File("tmp/"+repo+".repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    // http://download.devel.redhat.com/released/RHN-Tools-5.7-RHEL-6/i386/tree/RHNTools/
	    String basearch = arch;
	    if (arch.equals("i686")) basearch = "x86_64"; // all the rhn tools are noarch packages, so it really should not matter what basearch we use so long as the path exists.  I see arches ppc64 s390x x86_64 for RHN-Tools-5.7-RHEL-7.  I see arches i386 ppc64 s390x x86_64 for RHN-Tools-5.7-RHEL-6.
	    if (arch.equals("s390")) basearch = "s390x";
	    if (arch.equals("ppc")) basearch = "ppc64";
	    String baseurl;
	    baseurl = "http://download.devel.redhat.com/released/RHN-Tools-5.7-RHEL-"+redhatReleaseX+"/"+basearch+"/tree/RHNTools/";	// <p>The document has moved <a href="http://download-node-02.eng.bos.redhat.com/released/RHN-Tools-5.7-RHEL-6/i686/tree/RHNTools/">here</a>.</p>
	    baseurl = "http://download-node-02.eng.bos.redhat.com/released/RHN-Tools-5.7-RHEL-"+redhatReleaseX+"/"+basearch+"/tree/RHNTools/";	// Bug 1432642 fails on RHE7 because...  repodata/repomd.xml: [Errno 14] HTTP Error 404 - Not Found
	    baseurl = "http://pulp.dist.prod.ext.phx2.redhat.com/content/dist/rhel/server/"+redhatReleaseX+"/"+redhatReleaseX+"Server/"+basearch+"/rhn-tools/os/";	// recommended by Tomas Mlcoch in https://projects.engineering.redhat.com/browse/RCM-13960

	    // check the baseurl for problems
	    SSHCommandResult baseurlTestResult = sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurl);
	    if (baseurlTestResult.getStdout().contains("404 Not Found") || baseurlTestResult.getStdout().contains("The document has moved")) {
			log.warning("Cannot installReleasedRhnClassicPackages from baseurl '"+baseurl+"'.");
	    	Assert.fail("The Released RHN Tools baseurl '"+baseurl+"' had problems.  Instruct the automator to verify the assembly of this baseurl.");
	    }
		
		// write out the rows of the table...
    	Writer output = new BufferedWriter(new FileWriter(file));
	    
    	// write the released-rhn57-tools repo
		output.write("["+repo+"]\n");
		output.write("name     = Released RHN 5.7 Tools for RHEL"+redhatReleaseX+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the latest extras packages may not be signed until on REL_PREP
		output.write("baseurl  = "+baseurl+"\n");
		output.write("\n");
	    output.close();
	    installOptions += " --enablerepo="+repo;

		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		
		
		// locally create a yum.repos.d latest-rhelX-variant repo file
		repo = "latest-rhel"+redhatReleaseX+"-"+variant;
	    file = new File("tmp/"+repo+".repo"); // this will be in the automation.dir directory on hudson (workspace/automatjon/sm)
	    // http://download.devel.redhat.com/rel-eng/latest-RHEL-6/compose/Server/x86_64/os/
	    basearch = arch;
	    if (arch.equals("i686")) basearch = "i386"; // all the i686 arch packages are found in the i386 base arch path
	    if (arch.equals("s390")) basearch = "s390x"; // all the ppc arch packages are found in the ppc64 base arch path
	    if (arch.equals("ppc")) basearch = "ppc64"; // all the ppc arch packages are found in the ppc64 base arch path
	    baseurl = "http://download.devel.redhat.com/rel-eng/latest-RHEL-"+redhatReleaseX+"/compose/"+variant+"/"+basearch+"/os/";	// <p>The document has moved <a href="http://download-node-02.eng.bos.redhat.com/rel-eng/latest-RHEL-6/compose/Client/i686/os/">here</a>.</p>
	    baseurl = "http://download-node-02.eng.bos.redhat.com/rel-eng/latest-RHEL-"+redhatReleaseX+"/compose/"+variant+"/"+basearch+"/os/";
	    
	    // check the baseurl for problems
	    baseurlTestResult = sshCommandRunner.runCommandAndWait("curl --stderr /dev/null --insecure --request GET "+baseurl);
	    if (baseurlTestResult.getStdout().contains("404 Not Found") || baseurlTestResult.getStdout().contains("The document has moved")) {
			log.warning("Cannot installReleasedRhnClassicPackages from baseurl '"+baseurl+"'.");
	    	Assert.fail("The Released RHN Tools baseurl '"+baseurl+"' had problems.  Instruct the automator to verify the assembly of this baseurl.");
	    }
		
		// write out the rows of the table...
    	output = new BufferedWriter(new FileWriter(file));
	    
    	// write the latest-rhel6 repo
		output.write("["+repo+"]\n");
		output.write("name     = Latest RHEL"+redhatReleaseX+" "+variant+"\n");
		output.write("enabled  = 0\n");
		output.write("gpgcheck = 0\n");	// needed since the latest extras packages may not be signed until on REL_PREP
		output.write("baseurl  = "+baseurl+"\n");
		output.write("\n");
	    output.close();
	    installOptions += " --enablerepo="+repo;

		RemoteFileTasks.putFile(sshCommandRunner, file.getPath(), "/etc/yum.repos.d/", "0644");
		
		
		
		
		
		// assemble the packages to be updated (note: if the list is empty, then all packages will be updated)
		String installPackagesAsString = "";
		for (String installPackage : installPackages) installPackagesAsString += installPackage+" "; installPackagesAsString=installPackagesAsString.trim();
		
		// run yum install
		Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y install "+installPackagesAsString+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum successfully installed RHN Classic packages");
	}
	
	/**
	 * assumes there are already enabled repos that will provide any of the rhsm packages that are not already installed
	 * @param installOptions
	 */
	public void installSubscriptionManagerRPMs(String installOptions) {
		
		// attempt to install all missing packages
		List<String> pkgs = new ArrayList<String>();
		
		if /* >= RHEL5.7 */ (redhatReleaseX.equals("5") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=7) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gnome", "subscription-manager-firstboot"}));
		if /* >= RHEL5.8 */ (redhatReleaseX.equals("5") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=8) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gnome", "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data"}));
		if /* >= RHEL5.9 */ (redhatReleaseX.equals("5") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=9) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data"}));	// "subscription-manager-gnome" => "subscription-manager-gui" bug 818397
		if /* >= RHEL6.1 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=1) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gnome", "subscription-manager-firstboot"}));
		if /* >= RHEL6.3 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=3) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gnome", "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data"}));
		if /* >= RHEL6.4 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=4) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data"}));
		if /* >= RHEL6.7 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=7) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-container"}));
		if /* >= RHEL6.9 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=9) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm-certificates", "python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-container"}));
		if /* >= RHEL6.10 */ (redhatReleaseX.equals("6") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=10) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"subscription-manager-rhsm-certificates", "subscription-manager-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-container"}));
		if /* >= RHEL7.0 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=0) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data"}));
		if /* >= RHEL7.1 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=1) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-firstboot", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-ostree", "subscription-manager-plugin-container"}));
		if /* >= RHEL7.2 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=2) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-initial-setup-addon", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-ostree", "subscription-manager-plugin-container"}));
		if /* >= RHEL7.3 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=3) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"python-rhsm-certificates", "python-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-initial-setup-addon", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-ostree", "subscription-manager-plugin-container"}));
		if /* >= RHEL7.5 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=5) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"subscription-manager-rhsm-certificates", "subscription-manager-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-initial-setup-addon", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-ostree", "subscription-manager-plugin-container", "subscription-manager-cockpit"}));
		if /* >= RHEL8.0 */ (redhatReleaseX.equals("8") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=0) pkgs = new ArrayList<String>(Arrays.asList(new String[]{"subscription-manager-rhsm-certificates", "python3-subscription-manager-rhsm", "subscription-manager", "subscription-manager-gui",   "subscription-manager-initial-setup-addon", "subscription-manager-migration", "subscription-manager-migration-data", "subscription-manager-plugin-ostree", "subscription-manager-plugin-container", "dnf-plugin-subscription-manager.rpm", "subscription-manager-cockpit"}));
		pkgs.add(0,"expect");	// used for interactive cli prompting
		pkgs.add(0,"sos");	// used to create an sosreport for debugging hardware
		pkgs.add(0,"bash-completion");	// used in BashCompletionTests
		pkgs.add(0,"hunspell");	// used for spellcheck testing
		pkgs.add(0,"gettext");	// used for Pofilter and Translation testing - msgunfmt
		pkgs.add(0,"policycoreutils-python");	// used for Docker testing - required by docker-selinux package 
		pkgs.add(0,"net-tools");	// provides netstat which is used to know when vncserver is up
		pkgs.add(0,"ntp");	// used to synchronize a computer's time with another reference time source.
		pkgs.add(0,"git");	// used to clone rcm-metadata.git
		//TODO I don't think we really need to check the version of rhel for the next pkgs, if they don't exist, they just won't be installed. 
		if /* >= RHEL6 */   (Integer.valueOf(redhatReleaseX)>=6) pkgs.add(0,"python-dateutil");	// dependency for python-rhsm
		if /* >= RHEL7.5 */ (redhatReleaseX.equals("7") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=5) pkgs.add(0,"cockpit");	// indirect dependency for subscription-manager-cockpit, but indirectly requires subscription-manager which means when subscription-manager is removed by yum, then cockpit is also removed
		if /* >= RHEL8.0 */ (redhatReleaseX.equals("8") && Integer.valueOf(redhatReleaseXY.split("\\.")[1])>=0) pkgs.add(0,"cockpit");	// indirect dependency for subscription-manager-cockpit, but indirectly requires subscription-manager which means when subscription-manager is removed by yum, then cockpit is also removed
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "790116"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			String pkg = "subscription-manager-migration-data";
			log.warning("Skipping the install of "+pkg+".");
			pkgs.remove(pkg);
		}
		// END OF WORKAROUND
		for (String pkg : pkgs) {
			if (pkg.equals("subscription-manager-gnome") && isPackageInstalled("subscription-manager-gui")) continue;	// avoid downgrading
			if (!isPackageInstalled(pkg)) {
				//Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y install "+pkg+" "+installOptions).getExitCode(),Integer.valueOf(0), "Yum installed package: "+pkg);
				sshCommandRunner.runCommandAndWait("yum -y install "+pkg+" "+installOptions);
			}
		}
	}
	
	
	
	public void installSubscriptionManagerRPMs(List<String> rpmInstallUrls, List<String> rpmUpdateUrls, String installOptions, String jenkinsUsername, String jenkinsPassword) {
		if (rpmInstallUrls==null) rpmInstallUrls = new ArrayList<String>();
		if (rpmUpdateUrls==null) rpmUpdateUrls = new ArrayList<String>();
		if (installOptions==null) installOptions = "";
		List<String> pkgsInstalled = new ArrayList<String>();
		
		// skip installation of packages on an Atomic system
		
		//	-bash-4.2# cat /etc/redhat-release 
		//	Red Hat Atomic Host Preview release 7.0 Beta
		//	-bash-4.2# rpm -q redhat-release-atomic-host
		//	redhat-release-atomic-host-7.0-20140925.0.atomic.el7.x86_64
		
		//	[root@10-16-7-142 ~]# cat /etc/redhat-release
		//	Red Hat Atomic Host release 7.1 
		//	[root@10-16-7-142 ~]# rpm -q redhat-release-atomic-host
		//	redhat-release-atomic-host-7.1-20150113.0.atomic.el7.4.x86_64
		if (redhatRelease.startsWith("Red Hat Atomic Host")) {
			log.warning("Skipping setup procedure installSubscriptionManagerRPMs() on '"+redhatRelease+"'.");
			return;
		}
		
		// make sure the client's time is accurate
		if (Integer.valueOf(redhatReleaseX)>=8)	{	// the RHEL8 way...
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "systemctl stop ntpd.service && ntpd -q clock.redhat.com && systemctl enable ntpd.service && systemctl start ntpd.service && systemctl is-active ntpd.service", Integer.valueOf(0), "^active$", null);
		} else if (Integer.valueOf(redhatReleaseX)>=7)	{	// the RHEL7 / F16+ way...
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "systemctl stop ntpd.service && ntpdate clock.redhat.com && systemctl enable ntpd.service && systemctl start ntpd.service && systemctl is-active ntpd.service", Integer.valueOf(0), "^active$", null);
		} else {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service ntpd stop; ntpdate clock.redhat.com; service ntpd start; chkconfig ntpd on", /*Integer.valueOf(0) DON"T CHECK EXIT CODE SINCE IT RETURNS 1 WHEN STOP FAILS EVEN THOUGH START SUCCEEDS*/null, "Starting ntpd:\\s+\\[  OK  \\]", null);
		}
		
		// yum clean all
		SSHCommandResult sshCommandResult = yumClean("all");
		// this if block was written 2010-10-25 but I cannot remember why I did it - 01/26/2013 jsefler
		if (sshCommandResult.getExitCode().equals(1)) {
			sshCommandRunner.runCommandAndWait("rm -f "+redhatRepoFile);
		}
		
		// 6/15/2013 - including "--disablerepo=*" with yum installOptions to avoid these errors which happen a lot on rhel61 and rhel57
		//	
		//	201306141731:15.632 - FINE: ssh root@rhsm-compat-rhel61.usersys.redhat.com yum remove -y subscription-manager-migration (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201306141731:28.980 - FINE: Stderr: 
		//	This system is not registered to Red Hat Subscription Management. You can use subscription-manager to register.
		//	file://var/cache/yum/x86_64/6Server/rhel-x86_64-server-6/repodata/repomd.xml: [Errno 14] Could not open/read file://var/cache/yum/x86_64/6Server/rhel-x86_64-server-6/repodata/repomd.xml
		//	Trying other mirror.
		//	Error: Cannot retrieve repository metadata (repomd.xml) for repository: rhel-x86_64-server-6. Please verify its path and try again
		//	
		//	201306141719:42.800 - FINE: ssh root@rhsm-compat-rhel57.usersys.redhat.com yum -y localinstall /tmp/python-rhsm.rpm --nogpgcheck (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201306141719:43.170 - FINE: Stderr: 
		//	This system is not registered with RHN.
		//	RHN Satellite or RHN Classic support will be disabled.
		//	file://var/cache/yum/rhel-x86_64-server-5/repodata/repomd.xml: [Errno 5] OSError: [Errno 2] No such file or directory: '/cache/yum/rhel-x86_64-server-5/repodata/repomd.xml'
		//	Trying other mirror.
		//	Error: Cannot retrieve repository metadata (repomd.xml) for repository: rhel-x86_64-server-5. Please verify its path and try again
		installOptions = "--disablerepo=* "+installOptions;

		// remove current rpms
		// http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/python-rhsm.noarch.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-gnome.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-firstboot.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-migration.x86_64.rpm,     http://gibson.usersys.redhat.com/latestrpm/?arch=noarch&version=1&rpmname=subscription-manager-migration-data
		List<String> rpmUrlsReversed = new ArrayList<String>();
		for (String rpmUrl : rpmInstallUrls) rpmUrlsReversed.add(0,rpmUrl);
		for (String rpmUrl : rpmUrlsReversed) {
			rpmUrl = rpmUrl.trim(); if (rpmUrl.isEmpty()) continue;
			String rpm = Arrays.asList(rpmUrl.split("/|=")).get(rpmUrl.split("/|=").length-1);
			String pkg = rpm.replaceFirst("\\.rpm$", "");
			String rpmPath = "/tmp/"+rpm; if (!rpmPath.endsWith(".rpm")) rpmPath+=".rpm";
			
			// remove the existing package first
			log.info("Removing existing package "+pkg+"...");
			if (pkg.startsWith("katello-ca-consumer")) {
				sshCommandRunner.runCommandAndWait("yum -y remove $(rpm -qa | grep katello-ca-consumer) "+installOptions);
				continue;
			}
			// compatibility adjustment for python-rhsm* packages obsoleted by subscription-manager-rhsm* packages
			if (pkg.startsWith("subscription-manager-rhsm")) {	// commit f445b6486a962d12185a5afe69e768d0a605e175 Move python-rhsm build into subscription-manager
				String obsoletedPackage = pkg.replace("subscription-manager-rhsm", "python-rhsm");
				sshCommandRunner.runCommandAndWait("yum -y remove "+obsoletedPackage+" "+installOptions);
			}
			//sshCommandRunner.runCommandAndWait("yum -y remove "+pkg+" "+installOptions);	// inadvertently causes removal of cockpit-system which requires subscription-manager and then there is no way to get it back when subscription-manager-cockpit is installed; therefore let's use rpm --erase --no-deps
			sshCommandRunner.runCommandAndWait("rpm --erase --nodeps "+pkg);
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"rpm -q "+pkg,Integer.valueOf(1),"package "+pkg+" is not installed",null);
		}
		
		// install new rpms
		// http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/python-rhsm.noarch.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-gnome.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-firstboot.x86_64.rpm,      http://hudson.rhq.lab.eng.bos.redhat.com:8080/hudson/view/Entitlement/job/subscription-manager_RHEL5.8/lastSuccessfulBuild/artifact/rpms/x86_64/subscription-manager-migration.x86_64.rpm,     http://gibson.usersys.redhat.com/latestrpm/?arch=noarch&version=1&rpmname=subscription-manager-migration-data
		for (String rpmUrl : rpmInstallUrls) {
			rpmUrl = rpmUrl.trim(); if (rpmUrl.isEmpty()) continue;
			String rpm = Arrays.asList(rpmUrl.split("/|=")).get(rpmUrl.split("/|=").length-1);
			String pkg = rpm.replaceFirst("\\.rpm$", "");
			String rpmPath = "/tmp/"+rpm; if (!rpmPath.endsWith(".rpm")) rpmPath+=".rpm";
			
			// exclude subscription-manager-plugin-ostree-1.15.9-5+ due to Bug 1185958: Make ostree plugin depend on ostree.
			/* reverted commit e6a140d6d995887bd1f6488ef2549f913e4c3790 because https://bugzilla.redhat.com/show_bug.cgi?id=1185958#c7
			if (rpmUrl.contains("subscription-manager-plugin-ostree") && !isPackageInstalled("ostree")) {
				log.warning("Skipping install of '"+rpmUrl+"' due to missing ostree package dependency.");
				continue;
			}
			*/
			
			// install rpmUrl
			log.info("Installing RPM from "+rpmUrl+"...");
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"wget -nv -O "+rpmPath+" --no-check-certificate "+(jenkinsUsername.isEmpty()?"":"--http-user="+jenkinsUsername)+" "+(jenkinsPassword.isEmpty()?"":"--http-password="+jenkinsPassword)+" \""+rpmUrl.trim()+"\"",Integer.valueOf(0),null,"-> \""+rpmPath+"\"");
			Assert.assertEquals(sshCommandRunner.runCommandAndWait("yum -y localinstall "+rpmPath+" "+installOptions).getExitCode(), Integer.valueOf(0), "ExitCode from yum installed local rpm: "+rpmPath);
			
			// assert the local rpm is now installed
			if (!pkg.startsWith("katello-ca-consumer")) {	// skip assertion of katello-ca-consumer
				String rpmPackageVersion = sshCommandRunner.runCommandAndWait("rpm --query --package "+rpmPath).getStdout().trim();
				String rpmInstalledVersion = sshCommandRunner.runCommandAndWait("rpm --query "+pkg).getStdout().trim();
				Assert.assertEquals(rpmInstalledVersion,rpmPackageVersion, "Local rpm package '"+rpmPath+"' is currently installed.");
				pkgsInstalled.add(pkg);
			}
		}
		
		// update new rpms
		// http://gibson.usersys.redhat.com/latestrpm/?arch=x86_64&basegrp=subscription-manager&version=0.98.15&rpmname=subscription-manager,     http://gibson.usersys.redhat.com/latestrpm/?arch=x86_64&basegrp=subscription-manager&version=0.98.15&rpmname=subscription-manager-gnome,     http://gibson.usersys.redhat.com/latestrpm/?arch=x86_64&basegrp=subscription-manager&version=0.98.15&rpmname=subscription-manager-firstboot,     http://gibson.usersys.redhat.com/latestrpm/?arch=x86_64&basegrp=subscription-manager&version=0.98.15&rpmname=subscription-manager-migration,    http://gibson.usersys.redhat.com/latestrpm/?arch=noarch&version=1.11&release=el5&rpmname=subscription-manager-migration-data
		String rpmPaths = "";
		for (String rpmUrl : rpmUpdateUrls) {
			rpmUrl = rpmUrl.trim(); if (rpmUrl.isEmpty()) continue;
			String rpm = Arrays.asList(rpmUrl.split("/|=")).get(rpmUrl.split("/|=").length-1);
			String pkg = rpm.replaceFirst("\\.rpm$", "");
			String rpmPath = "/tmp/"+rpm; if (!rpmPath.endsWith(".rpm")) rpmPath+=".rpm";
			
			// exclude subscription-manager-plugin-ostree-1.15.9-5+ due to Bug 1185958: Make ostree plugin depend on ostree.
			/* reverted commit e6a140d6d995887bd1f6488ef2549f913e4c3790 because https://bugzilla.redhat.com/show_bug.cgi?id=1185958#c7
			if (rpmUrl.contains("subscription-manager-plugin-ostree") && !isPackageInstalled("ostree")) {
				log.warning("Skipping upgrade of '"+rpmUrl+"' due to missing ostree dependency.");
				continue;
			}
			*/
			
			// upgrade rpmUrl
			log.info("Upgrading RPM from "+rpmUrl+"...");
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"wget -nv -O "+rpmPath+" --no-check-certificate \""+rpmUrl.trim()+"\"",Integer.valueOf(0),null,"-> \""+rpmPath+"\"");
			rpmPaths += rpmPath; rpmPaths += " ";
		}
		if (!rpmUpdateUrls.isEmpty()) {
			// TODO: using yum update may cause trouble for subscription-manager-gnome => subscription-manager-gui (Package subscription-manager-gui not installed, cannot update it. Run yum install to install it instead.)

			// using yum to upgrade...
			SSHCommandResult updateResult = sshCommandRunner.runCommandAndWait("yum -y localupdate "+rpmPaths+" "+installOptions);
			if (updateResult.getStdout().contains("does not update installed package")) {
				log.warning("The rpmUpdateUrls does not update installed package(s).");
			}
			Assert.assertEquals(updateResult.getExitCode(), Integer.valueOf(0), "ExitCode from attempt to upgrade packages: "+rpmPaths);
			
			/* using rpm to upgrade...
			SSHCommandResult updateResult = sshCommandRunner.runCommandAndWait("rpm -v --upgrade "+rpmPaths);
			if (updateResult.getExitCode()==3 && updateResult.getStderr().contains("is already installed")) {
				log.warning("The rpmUpdateUrls appear to already be installed.");
			} else {
				Assert.assertEquals(updateResult.getExitCode(), Integer.valueOf(0), "ExitCode from attempt to upgrade packages: "+rpmPaths);
			}
			*/
		}
// DELETEME since exitCode assertion was added above
//		// assert that all of the updated rpms are now installed
//		for (String rpmPath : rpmPaths.split(" ")) {
//			rpmPath = rpmPath.trim(); if (rpmPath.isEmpty()) continue;
//			String pkg = (new File(rpmPath)).getName().replaceFirst("\\.rpm$","");
//			String rpmPackageVersion = sshCommandRunner.runCommandAndWait("rpm --query --package "+rpmPath).getStdout().trim();
//			String rpmInstalledVersion = sshCommandRunner.runCommandAndWait("rpm --query "+pkg).getStdout().trim();
//			Assert.assertEquals(rpmInstalledVersion,rpmPackageVersion, "Local rpm package '"+rpmPath+"' is currently installed.");
//			pkgsInstalled.add(pkg);
//		}
		
		// remember the versions of the packages installed
		for (String pkg : pkgsInstalled) {
			isPackageVersion(pkg, "==", "0.0");	// this will simply populate the cached Map<String,String> installedPackageVersionMap
		}
	}
	
	public void initializeMsgStringsAfterInstallingSubscriptionManagerRPMs() {
		// initialize client fields that depend on the installed package version
		msg_ConsumerNotRegistered		= "Consumer not registered. Please register using --username and --password";
		if (isPackageVersion("subscription-manager", ">=", "0.98.4-1"))		msg_ConsumerNotRegistered = "Error: You need to register this system by running `register` command.  Try register --help.";	// effective after bug fix 749332 subscription-manager commit 6241cd1495b9feac2ed123f60405061b03815721
		if (isPackageVersion("subscription-manager", ">=", "0.99.11-1"))	msg_ConsumerNotRegistered = "This system is not yet registered. Try 'subscription-manager register --help' for more information.";	// effective after bug fix 767790 subscription-manager commit 913ca2f8f3febd210634988162e14426ae4bfe76
		msg_NeedListOrUpdateOption		= "Error: Need either --list or --update, Try facts --help";
		
		msg_NetworkErrorUnableToConnect = "Network error, unable to connect to server.\n Please see "+rhsmLogFile+" for more information.";
		msg_NetworkErrorUnableToConnect = "Network error, unable to connect to server.\nPlease see "+rhsmLogFile+" for more information."; // effective in RHEL58
		if (isPackageVersion("subscription-manager", ">=", "1.10.9-1"))		msg_NetworkErrorUnableToConnect = "Network error, unable to connect to server. Please see "+rhsmLogFile+" for more information."; // effective after subscription-manager commit 3366b1c734fd27faf48313adf60cf051836af115
		msg_NetworkErrorCheckConnection = "Network error. Please check the connection details, or see "+rhsmLogFile+" for more information.";
		msg_ClockSkewDetection			= "Clock skew detected, please check your system time";
		msg_ContainerMode				= "subscription-manager is disabled when running inside a container. Please refer to your host system for subscription management.";
		msg_RemoteErrorCheckConnection	= "Remote server error. Please check the connection details, or see /var/log/rhsm/rhsm.log for more information.";
		msg_ProxyConnectionFailed		= "Proxy connection failed, please check your settings.";
		msg_ProxyConnectionFailed407	= "Proxy connection failed: 407";
		msg_ProxyErrorUnableToConnect	= "Proxy error, unable to connect to proxy server.";

		// TEMPORARY WORKAROUND FOR BUG 1335537 - typo in "Proxy connnection failed, please check your settings."
		String bugId = "1335537"; boolean invokeWorkaroundWhileBugIsOpen = true;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			msg_ProxyConnectionFailed = msg_ProxyConnectionFailed.replace("connection", "connnection");	// pretend that "connnection" is the correct spelling while bug 1335537 is open
		}
		// END OF WORKAROUND

		// msg_InteroperabilityWarning is defined in /usr/share/rhsm/subscription_manager/branding/__init__.py self.REGISTERED_TO_OTHER_WARNING
		msg_InteroperabilityWarning		=
			"WARNING" +"\n\n"+
			"You have already registered with RHN using RHN Classic technology. This tool requires registration using RHN Certificate-Based Entitlement technology." +"\n\n"+
			"Except for a few cases, Red Hat recommends customers only register with RHN once." +"\n\n"+
			"For more information, including alternate tools, consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 730018 - Warning text message is confusing
		msg_InteroperabilityWarning		= 
			"WARNING" +"\n\n"+
			"This system has already been registered with RHN using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using RHN Certificate-Based technology. Red Hat recommends (except in a few cases) that customers only register with RHN once. " +"\n\n"+
			"To learn more about RHN registration and technologies please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// during rhel59, terminology changes were made for "RHN Certificate-Based technology"
		msg_InteroperabilityWarning = 
			"WARNING" +"\n\n"+
			"This system has already been registered with RHN using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends (except in a few cases) that customers only register once. " +"\n\n"+
			"To learn more about RHN registration and technologies please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 847795 - String Update: redhat_branding.py Updates
		msg_InteroperabilityWarning = 
			"WARNING" +"\n\n"+
			"This system has already been registered with Red Hat using RHN Classic technology." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends that customers only register once. " +"\n\n"+
			"To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 859090 - String Update: redhat_branding.py
		msg_InteroperabilityWarning = 
			"WARNING" +"\n\n"+
			"This system has already been registered with Red Hat using RHN Classic." +"\n\n"+
			"The tool you are using is attempting to re-register using Red Hat Subscription Management technology. Red Hat recommends that customers only register once. " +"\n\n"+
			"To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// after Bug 877590 - The tool you are using is attempting to re-register using Red Hat Subscription Management technology.
		msg_InteroperabilityWarning = 
			"WARNING" +"\n\n"+
			"This system has already been registered with Red Hat using RHN Classic." +"\n\n"+
			"Your system is being registered again using Red Hat Subscription Management. Red Hat recommends that customers only register once." +"\n\n"+
			"To learn how to unregister from either service please consult this Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563";
		// during RHEL58, DEV trimmed whitespace from strings...
		msg_InteroperabilityWarning = msg_InteroperabilityWarning.replaceAll(" +(\n|$)", "$1"); 
	}
	
	public void restartCockpitServices() {
		// skip when cockpit is not installed
		if (!isPackageInstalled("cockpit")) return;
		
		// # systemctl restart cockpit.socket cockpit.service
		sshCommandRunner.runCommandAndWait("systemctl restart cockpit.socket cockpit.service");
		if (sshCommandRunner.getExitCode()!=0) log.warning("Encountered problems while restarting cockpit services.");
		
		// # firewall-cmd --query-service=cockpit
		// no
		sshCommandRunner.runCommandAndWait("firewall-cmd --query-service=cockpit");
		if (sshCommandRunner.getStdout().trim().equalsIgnoreCase("no")) {
			// # firewall-cmd --add-service=cockpit
			// success
			sshCommandRunner.runCommandAndWait("firewall-cmd --add-service=cockpit");
			sshCommandRunner.runCommandAndWait("firewall-cmd --query-service=cockpit");
		}
		if (!sshCommandRunner.getStdout().trim().equalsIgnoreCase("yes")) log.warning("Encountered problems while adding firewall-cmd cockpit service.");
		
		// now open https://rhel7.usersys.redhat.com:9090/ in a browser and login with root credentials
	}
	
	public void setupRhnDefinitions(String gitRepository) {
		if (gitRepository.equals("")) return;
		
		// git clone git://git.app.eng.bos.redhat.com/rcm/rhn-definitions.git
		log.info("Cloning Rhn Definitions...");
		/* git may is not always installed (e.g. RHEL5/epel s390,ia64), therefore stop asserting which causes entire setupBeforeSuite to fail.
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "rm -rf "+rhnDefinitionsDir+" && mkdir "+rhnDefinitionsDir, new Integer(0));
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "git clone "+gitRepository+" "+rhnDefinitionsDir, new Integer(0));
		*/
		sshCommandRunner.runCommandAndWait("rm -rf "+rhnDefinitionsDir+" && mkdir "+rhnDefinitionsDir);
		sshCommandRunner.runCommandAndWait("git clone --quiet --depth=1 "+gitRepository+" "+rhnDefinitionsDir);
		if (sshCommandRunner.getExitCode()!=0) log.warning("Encountered problems while cloning "+gitRepository+"; dependent tests will likely fail or skip.");
		
	}
	
	@Deprecated
	public void setupTranslateToolkitFromGitRepo(String gitRepository) {
		if (gitRepository.equals("")) return;
		
		//	[root@jsefler-7 ~]# pip uninstall -y six
		//	Uninstalling six:
		//	  Successfully uninstalled six
		//	
		//	[root@jsefler-7 ~]# easy_install six
		//	Searching for six
		//	Reading https://pypi.python.org/simple/six/
		//	Best match: six 1.5.2
		//	Downloading https://pypi.python.org/packages/source/s/six/six-1.5.2.tar.gz#md5=322b86d0c50a7d165c05600154cecc0a
		//	Processing six-1.5.2.tar.gz
		//	Writing /tmp/easy_install-evlX6g/six-1.5.2/setup.cfg
		//	Running six-1.5.2/setup.py -q bdist_egg --dist-dir /tmp/easy_install-evlX6g/six-1.5.2/egg-dist-tmp-LCKBIt
		//	no previously-included directories found matching 'documentation/_build'
		//	zip_safe flag not set; analyzing archive contents...
		//	six: module references __file__
		//	Adding six 1.5.2 to easy-install.pth file
		//
		//	Installed /usr/lib/python2.7/site-packages/six-1.5.2-py2.7.egg
		//	Processing dependencies for six
		//	Finished processing dependencies for six
		//	[root@jsefler-7 ~]# echo $?
		//	0
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "easy_install six", new Integer(0));
		
		// git clone git://github.com/translate/translate.git
		log.info("Cloning Translate Toolkit...");
		final String translateToolkitDir	= "/tmp/"+"translateToolkitDir";
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "rm -rf "+translateToolkitDir+" && mkdir "+translateToolkitDir, new Integer(0));
		/* git is not always installed (e.g. RHEL5/epel s390,ia64), therefore stop asserting which causes entire setupBeforeSuite to fail.
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "git clone "+gitRepository+" "+translateToolkitDir, new Integer(0));
		sshCommandRunner.runCommandAndWaitWithoutLogging("cd "+translateToolkitDir+" && ./setup.py install --force");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "which pofilter", new Integer(0));
		*/
		sshCommandRunner.runCommandAndWait("git clone --quiet --depth=1 "+gitRepository+" "+translateToolkitDir);
		sshCommandRunner.runCommandAndWait("cd "+translateToolkitDir+" && ./setup.py install --force");
		sshCommandRunner.runCommandAndWait("rm -rf ~/.local");	// 9/27/2013 Fix for the following... Don't know why I started getting Traceback ImportError: cannot import name pofilter
		sshCommandRunner.runCommandAndWait("which pofilter");
		if (sshCommandRunner.getExitCode()!=0) log.warning("Encountered problems while installing pofilter; related tests will likely fail or skip.");
	}
	public void setupTranslateToolkitFromTarUrl(String tarUrl) {
		// nothing to install
		if (tarUrl.isEmpty()) return;
		
		// avoid redundant installation
		if (sshCommandRunner.runCommandAndWait("which pofilter").getExitCode().equals(0)) {
			log.warning("The TranslateToolkit appears to be installed already.  Skipping re-installation.");
			return;
		}
		
		sshCommandRunner.runCommandAndWait("easy_install six");	 // needed for translate-toolkit-1.11.0 and newer
		
		log.info("Getting Translate Toolkit...");
		final String translateToolkitDir	= "/tmp/"+"translateToolkitDir";
		final String translateToolkitTarPath	= translateToolkitDir+"/translate-toolkit.tar";
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "rm -rf "+translateToolkitDir+" && mkdir "+translateToolkitDir, new Integer(0));
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "wget --no-verbose --no-check-certificate --output-document="+translateToolkitTarPath+" "+tarUrl,Integer.valueOf(0),null,"-> \""+translateToolkitTarPath+"\"");
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "tar --extract --directory="+translateToolkitDir+" --file="+translateToolkitTarPath,Integer.valueOf(0));
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "cd "+translateToolkitDir+"/translate-toolkit-* && sudo ./setup.py install --force", Integer.valueOf(0));
		sshCommandRunner.runCommandAndWait("rm -rf ~/.local");	// 9/27/2013 Fix for the following... Traceback ImportError: cannot import name pofilter
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "which pofilter", Integer.valueOf(0));
	}
	
	public void removeAllFacts() {
		log.info("Cleaning out facts directory: "+this.factsDir);
		if (!this.factsDir.startsWith("/etc/rhsm/")) log.warning("UNRECOGNIZED DIRECTORY.  NOT CLEANING FACTS FROM: "+this.factsDir);
		else sshCommandRunner.runCommandAndWait("rm -rf "+this.factsDir+"/*.facts");
	}
	
	public void removeAllCerts(boolean consumers, boolean entitlements, boolean products) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");
		String certDir;
		
		if (consumers) {
			certDir = this.consumerCertDir;	// getConfigFileParameter("consumerCertDir");
			log.info("Cleaning out certs from consumerCertDir: "+certDir);
			if (!certDir.startsWith("/etc/pki/") && !certDir.startsWith("/tmp/")) log.warning("UNRECOGNIZED DIRECTORY.  NOT CLEANING CERTS FROM: "+certDir);
			else {
				sshCommandRunner.runCommandAndWait("rm -rf "+certDir+"/*");
				this.currentlyRegisteredUsername = null;
				this.currentlyRegisteredPassword = null;
				this.currentlyRegisteredOrg = null;
				this.currentlyRegisteredType = null;
			}
		}
		
		if (entitlements) {
			certDir = this.entitlementCertDir;	// getConfigFileParameter("entitlementCertDir");
			log.info("Cleaning out certs from entitlementCertDir: "+certDir);
			if (!certDir.startsWith("/etc/pki/") && !certDir.startsWith("/tmp/")) log.warning("UNRECOGNIZED DIRECTORY.  NOT CLEANING CERTS FROM: "+certDir);
			else sshCommandRunner.runCommandAndWait("rm -rf "+certDir+"/*");
		}
		
		if (products) {
			certDir = this.productCertDir;	// getConfigFileParameter("productCertDir");
			log.info("Cleaning out certs from productCertDir: "+certDir);
			if (!certDir.startsWith("/etc/pki/") && !certDir.startsWith("/tmp/")) log.warning("UNRECOGNIZED DIRECTORY.  NOT CLEANING CERTS FROM: "+certDir);
			else sshCommandRunner.runCommandAndWait("rm -rf "+certDir+"/*");
		}
	}
	
	public void removeRhnSystemIdFile() {
		//RemoteFileTasks.runCommandAndWait(sshCommandRunner, "rm -rf "+rhnSystemIdFile, TestRecords.action());
		sshCommandRunner.runCommandAndWait("rm -rf "+rhnSystemIdFile);
		
		// also do a yum clean all to avoid rhnplugin message: This system may not be registered to RHN Classic or RHN Satellite. SystemId could not be acquired.
		yumClean("all");
	}
	
	public void updateYumRepo(String yumRepoFile, YumRepo yumRepo){
		log.info("Updating yumrepo file '"+yumRepoFile+"' repoid '"+yumRepo.id+"' to: "+yumRepo);
		// first, empty the contents of the current yumRepo
		// sed -i "/\[REPOID\]/,/\[/ s/^[^\[].*//" /etc/yum.repos.d/redhat.repo
		String command = String.format("sed -i \"/\\[%s\\]/,/\\[/ s/^[^\\[].*//\" %s", yumRepo.id, yumRepoFile);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,Integer.valueOf(0));
		// then, add all of the non-null yumRepo parameters
		Map<String,String> parameterValueMap = new HashMap<String,String>();
		if (yumRepo.name!=null)						parameterValueMap.put("name", yumRepo.name);
		if (yumRepo.baseurl!=null)					parameterValueMap.put("baseurl", yumRepo.baseurl);
		if (yumRepo.enabled!=null)					parameterValueMap.put("enabled", yumRepo.enabled.toString());
		if (yumRepo.gpgcheck!=null)					parameterValueMap.put("gpgcheck", yumRepo.gpgcheck.toString());
		if (yumRepo.gpgkey!=null)					parameterValueMap.put("gpgkey", yumRepo.gpgkey);
		if (yumRepo.sslcacert!=null)				parameterValueMap.put("sslcacert", yumRepo.sslcacert);
		if (yumRepo.sslverify!=null)				parameterValueMap.put("sslverify", yumRepo.sslverify.toString());
		if (yumRepo.sslclientcert!=null)			parameterValueMap.put("sslclientcert", yumRepo.sslclientcert);
		if (yumRepo.sslclientkey!=null)				parameterValueMap.put("sslclientkey", yumRepo.sslclientkey);
		if (yumRepo.metadata_expire!=null)			parameterValueMap.put("metadata_expire", yumRepo.metadata_expire);
		if (yumRepo.metalink!=null)					parameterValueMap.put("metalink", yumRepo.metalink);
		if (yumRepo.mirrorlist!=null)				parameterValueMap.put("mirrorlist", yumRepo.mirrorlist);
		if (yumRepo.repo_gpgcheck!=null)			parameterValueMap.put("repo_gpgcheck", yumRepo.repo_gpgcheck.toString());
		if (yumRepo.gpgcakey!=null)					parameterValueMap.put("gpgcakey", yumRepo.gpgcakey);
		if (yumRepo.exclude!=null)					parameterValueMap.put("exclude", yumRepo.exclude);
		if (yumRepo.includepkgs!=null)				parameterValueMap.put("includepkgs", yumRepo.includepkgs);
		if (yumRepo.enablegroups!=null)				parameterValueMap.put("enablegroups", yumRepo.enablegroups.toString());
		if (yumRepo.failovermethod!=null)			parameterValueMap.put("failovermethod", yumRepo.failovermethod);
		if (yumRepo.keepalive!=null)				parameterValueMap.put("keepalive", yumRepo.keepalive.toString());
		if (yumRepo.timeout!=null)					parameterValueMap.put("timeout", yumRepo.timeout);
		if (yumRepo.http_caching!=null)				parameterValueMap.put("http_caching", yumRepo.http_caching);
		if (yumRepo.retries!=null)					parameterValueMap.put("retries", yumRepo.retries);
		if (yumRepo.throttle!=null)					parameterValueMap.put("throttle", yumRepo.throttle);
		if (yumRepo.bandwidth!=null)				parameterValueMap.put("bandwidth", yumRepo.bandwidth);
		if (yumRepo.mirrorlist_expire!=null)		parameterValueMap.put("mirrorlist_expire", yumRepo.mirrorlist_expire);
		if (yumRepo.proxy!=null)					parameterValueMap.put("proxy", yumRepo.proxy);
		if (yumRepo.proxy_username!=null)			parameterValueMap.put("proxy_username", yumRepo.proxy_username);
		if (yumRepo.proxy_password!=null)			parameterValueMap.put("proxy_password", yumRepo.proxy_password);
		if (yumRepo.username!=null)					parameterValueMap.put("username", yumRepo.username);
		if (yumRepo.password!=null)					parameterValueMap.put("password", yumRepo.password);
		if (yumRepo.cost!=null)						parameterValueMap.put("cost", yumRepo.cost);
		if (yumRepo.skip_if_unavailable!=null)		parameterValueMap.put("skip_if_unavailable", yumRepo.skip_if_unavailable.toString());
		if (yumRepo.priority!=null)					parameterValueMap.put("priority", yumRepo.priority.toString());
		addYumRepoParameters(yumRepoFile,yumRepo.id,parameterValueMap);
	}
	public void updateYumRepoParameter(String yumRepoFile, String repoid, String parameter, String value){
		log.info("Updating yumrepo file '"+yumRepoFile+"' repoid '"+repoid+"' parameter '"+parameter+"' value to: "+value);
//		String command = "sed -i \"/\\["+repoid+"\\]/,/\\[/ s/^"+parameter+"\\s*=.*/"+parameter+"="+value+"/\" "+yumRepoFile;
		String command = String.format("sed -i \"/\\[%s\\]/,/\\[/ s/^%s\\s*=.*/%s=%s/\" %s", repoid, parameter, parameter, value, yumRepoFile);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,Integer.valueOf(0));
	}
	public void addYumRepoParameter(String yumRepoFile, String repoid, String parameter, String value){
		Map<String,String> parameterValueMap = new HashMap<String,String>();
		parameterValueMap.put(parameter, value);
		addYumRepoParameters(yumRepoFile,repoid,parameterValueMap);
	}
	public void addYumRepoParameters(String yumRepoFile, String repoid, Map<String,String> parameterValueMap){
		
		String a = "";
		for (String parameter:parameterValueMap.keySet()) {
			String value = parameterValueMap.get(parameter);
			log.info("Adding yumrepo file '"+yumRepoFile+"' repoid '"+repoid+"' option: "+parameter+"="+value);
			a += parameter+"="+value+"\\n";
		}
		if (parameterValueMap.size()>1) a = a.replaceFirst("\\\\n$", "");	// strip the trailing \n
		
		// sed  -i "/\[REPOID\]/ a F=bar\nG=tar\n" /etc/yum.repos.d/redhat.repo
		String command = String.format("sed -i \"/\\[%s\\]/ a %s\" %s", repoid, a, yumRepoFile);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command,Integer.valueOf(0));
	}
	
	public void updateConfFileParameter(String confFile, String parameter, String value){
		log.info("Updating config file '"+confFile+"' parameter '"+parameter+"' value to: "+value);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, confFile, "^"+parameter+"\\s*=.*$", parameter+"="+value.replaceAll("\\/", "\\\\/")),
				0,"Updated '"+confFile+"' parameter '"+parameter+"' to value '"+value+"'.");
		
		// catch the case when the config file parameter is not defined and therefore cannot be updated
		if (getConfFileParameter(confFile, parameter)==null) {
			log.warning("Config file '"+confFile+"' parameter '"+parameter+"' was not found and therefore cannot be updated.");
			return;
		}
		
		// also update this "cached" value for these config file parameters
		if (confFile.equals(this.rhsmConfFile)) {
			if (parameter.equals("consumerCertDir"))	this.consumerCertDir = value;
			if (parameter.equals("entitlementCertDir"))	this.entitlementCertDir = value;
			if (parameter.equals("productCertDir"))		this.productCertDir = value;
			if (parameter.equals("baseurl"))			this.baseurl = value;
			if (parameter.equals("ca_cert_dir"))		this.caCertDir = value;
		}
	}
	
	public void updateConfFileParameter(String confFile, String section, String parameter, String value) throws IOException{
		// TODO: An alternative solution for this task would be to use: https://github.com/pixelb/crudini
		log.info("Updating config file '"+confFile+"' section '"+section+"' parameter: "+parameter+"="+value);

		// get the permission mask on the file
		String mask = sshCommandRunner.runCommandAndWait("stat -c '%a' "+confFile).getStdout().trim();	// returns 644
		if (mask.length()==3) mask="0"+mask;	// prepend "0" as the file type to 0644
		
		// get a local copy of the confFile
		File remoteFile = new File(confFile);
	    File localFile = new File("tmp/"+remoteFile.getName()); // this will be in the automation.dir directory
		RemoteFileTasks.getFile(sshCommandRunner, localFile.getParent(),remoteFile.getPath());
		
		// read the confFile into a single String
		//String contents = new String(Files.readAllBytes(Paths.get("abc.java")));
		//String contents = new Scanner(localFile).useDelimiter("\\Z").next();	// http://stackoverflow.com/questions/3402735/what-is-simplest-way-to-read-a-file-into-string
		Scanner scanner = new Scanner(localFile,"UTF-8");
		scanner.useDelimiter("\\Z");	// assumes that there is no occurrence of \Z in the file otherwise only part of the file contents will be read.  Should assert scanner.hasNext() is false.
		String contents = scanner.next();
		scanner.close();
		
		// use a regex to update the conf value
		String regex = "(\\["+section+"\\](?:(?:\\n.*?)+)"+parameter+"\\s*(?::|=)\\s*)(.+)";
		String updatedContents = contents.replaceFirst(regex, "$1"+value);
		
		// catch the case when the config file parameter is not defined and therefore cannot be updated
		if (!SubscriptionManagerCLITestScript.doesStringContainMatches(contents, regex)) {
			log.warning("Config file '"+confFile+"' parameter '"+parameter+"' was not found and therefore cannot be updated.");
			return;
		}
		
		// inform the user if no update was made and simply return
		if (contents.equals(updatedContents)) {
			log.info("No update to parameter '"+parameter+"' in section '"+section+"' of config file '"+confFile+"' was made.  It may already have value '"+value+"'.");
			return;
		}
		
		// overwrite the local file with the update
    	Writer output = new BufferedWriter(new FileWriter(localFile));
		output.write(updatedContents);	// TODO: will append an extra blank line
	    output.close();
	    
	    // put the updated confFile back onto the client
		RemoteFileTasks.putFile(sshCommandRunner, localFile.getPath(), confFile, mask);

		// also update this "cached" value for these config file parameters
		if (confFile.equals(this.rhsmConfFile)) {
			if (parameter.equals("consumerCertDir"))	this.consumerCertDir = value;
			if (parameter.equals("entitlementCertDir"))	this.entitlementCertDir = value;
			if (parameter.equals("productCertDir"))		this.productCertDir = value;
			if (parameter.equals("baseurl"))			this.baseurl = value;
			if (parameter.equals("ca_cert_dir"))		this.caCertDir = value;
		}
	}
	
	public void removeConfFileParameter(String confFile, String parameter){
		log.info("Removing config file '"+confFile+"' parameter: "+parameter);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, confFile, "^"+parameter+"\\s*=.*", ""),
				0,"Removed '"+confFile+"' parameter: "+parameter);
	}
	
	public void commentConfFileParameter(String confFile, String parameter){
		log.info("Commenting out config file '"+confFile+"' parameter: "+parameter);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, confFile, "^"+parameter+"\\s*=", "#"+parameter+"="),
				0,"Commented '"+confFile+"' parameter: "+parameter);
	}
	
	public void uncommentConfFileParameter(String confFile, String parameter){
		log.info("Uncommenting config file '"+confFile+"' parameter: "+parameter);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, confFile, "^#\\s*"+parameter+"\\s*=", parameter+"="),
				0,"Uncommented '"+confFile+"' parameter: "+parameter);
	}
	
	public void addConfFileParameter(String confFile, String section, String parameter, String value){
		log.info("Adding config file '"+confFile+"' section '"+section+"' parameter: "+parameter+"="+value);
		
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, confFile, "\\["+section+"\\]", "\\["+section+"\\]\\n"+parameter+"="+value),
				0,"Added config file '"+confFile+"' section '"+section+"' parameter '"+parameter+"' value '"+value+"'");
	}
	public void addConfFileParameter(String confFile, String parameter, String value){
		log.info("Adding config file '"+confFile+"' parameter: "+parameter+"="+value);
		
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner, String.format("echo '%s=%s' >> %s", parameter, value, confFile), 0);
	}
	
	/**
	 * return the value of an active configuration parameter from a config file. If not found null is returned.
	 * @param confFile
	 * @param parameter
	 * @return
	 */
	public String getConfFileParameter(String confFile, String parameter){
		// Note: parameter can be case insensitive
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(String.format("grep -iE \"^%s *(=|:)\" %s",parameter,confFile));	// tolerates = or : assignment character
		if (result.getExitCode()!=0) return null;
		String value = result.getStdout().split("=|:",2)[1];
		return value.trim();
	}
	/**
	 * @param confFile
	 * @param section
	 * @param parameter - case insensitive matching will be used
	 * @return value of the section.parameter config (null when not found)
	 */
	public String getConfFileParameter(String confFile, String section, String parameter){
		String confFileContents = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "egrep -v  \"^\\s*(#|$)\" "+confFile, 0).getStdout();
		String value = getSectionParameterFromConfigFileContents(section, parameter, confFileContents);
		if (value==null) log.warning("Did not find section '"+section+"' parameter '"+parameter+"' in conf file '"+confFile+"'.");
		return value;
	}
	/**
	 * @param confFileContents - stdout from egrep -v  "^\s*(#|$)" /etc/rhsm/rhsm.conf
	 * @param section
	 * @param parameter - case insensitive matching will be used
	 * @return value of the section.parameter config (null when not found)
	 */
	protected String getSectionParameterFromConfigFileContents(String section, String parameter, String confFileContents){
		// TODO: A alternative solution for this task would be to use: https://github.com/pixelb/crudini
		
		//	[root@jsefler-onprem-62server ~]# egrep -v  "^\s*(#|$)" /etc/rhsm/rhsm.conf
		//	[server]
		//	hostname=jsefler-onprem-62candlepin.usersys.redhat.com
		//	prefix=/candlepin
		//	port=8443
		//	insecure=0
		//	ssl_verify_depth = 3
		//	ca_cert_dir=/etc/rhsm/ca/
		//	proxy_hostname =
		//	proxy_port = 
		//	proxy_user =
		//	proxy_password =
		//	[rhsm]
		//	baseurl=https://cdn.redhat.com
		//	repo_ca_cert=%(ca_cert_dir)sredhat-uep.pem
		//	productCertDir=/etc/pki/product
		//	entitlementCertDir=/etc/pki/entitlement
		//	consumercertdir=/etc/pki/consumer
		//	certfrequency=2400
		//	proxy_port = BAR
		//	[rhsmcertd]
		//	certFrequency=240
		
		// ^\[rhsm\](?:\n[^\[]*?)+^(?:[c|C][o|O][n|N][s|S][u|U][m|M][e|E][r|R][C|c][e|E][r|R][t|T][d|D][i|I][r|R])\s*[=:](.*)
		// Note: parameterRegex needs to be case insensitive since python will write all lowercase parameter names but read parameter names in mixed case 
		String parameterRegex = "";
		for (int i=0; i<parameter.length(); i++) parameterRegex += "["+parameter.toLowerCase().charAt(i)+"|"+parameter.toUpperCase().charAt(i)+"]";
		parameterRegex = "(?:"+parameterRegex+")";
		String regex = "^\\["+section+"\\](?:\\n[^\\[]*?)+^"+parameterRegex+"\\s*[=:](.*)";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(confFileContents);
		if (!matcher.find()) {
			//log.warning("Did not find section '"+section+"' parameter '"+parameter+"'.");
			return null;
		}
		/*
		log.fine("Matches: ");
		do {
			log.fine(matcher.group());
		} while (matcher.find());
		*/
		return matcher.group(1).trim();	// return the contents of the first capturing group
	}
	
	
	/**
	 * Use subscription-manager config to get the interpolated parameter value.
	 * @param parameter
	 * @return
	 */
	public String getConfParameter(String parameter){
		String parameterValue = sshCommandRunner.runCommandAndWait(command+" config | grep '  "+parameter.toLowerCase()+" = '").getStdout().trim();
		//	[root@jsefler-6 ~]# subscription-manager config
		//	[server]
		//	   hostname = jsefler-f14-candlepin.usersys.redhat.com
		//	   insecure = [0]
		//	   port = 8443
		//	   prefix = /candlepin
		//	   proxy_hostname = []
		//	   proxy_password = []
		//	   proxy_port = []
		//	   proxy_user = []
		//	   ssl_verify_depth = [3]

		return parameterValue.split(" = ")[1].replaceAll("\\[","").replaceAll("\\]","");	// a value wrapped in brackets [] indicates a default value is being used
	}
	
	/**
	 * Update the rhsmcertd frequency configurations in /etc/rhsm/rhsm.conf file and restart the rhsmcertd service.
	 * @param certFrequency - Frequency of certificate refresh (in minutes) (passing null will not change the current value)
	 * @param healFrequency - Frequency of subscription auto healing (in minutes) (passing null will not change the current value)
	 * @param assertCertificatesUpdate if NULL, do not assert the status of Certificate updates in rhsmcertd log; if TRUE, assert rhsmcertd logs that Certificate updates succeeded; if FALSE, assert rhsmcertd logs that Certificate updates failed
	 * NOTE: Due to the implementation of RFE Bug 1435013, calls to this method could potentially take up to a whole day waiting for the default autoAttachInterval to trigger.
	 * To avoid an unfriendly automation delay like this, the splay configuration will be temporarily turned off while restarting rhsmcertd when these frequencies exceed 10 minutes.
	 * If you are trying to explicitly test the certFrequency and healFrequency, you should use values less than 10 minutes. 
	 */
	public void restart_rhsmcertd (Integer certFrequency, Integer healFrequency, Boolean assertCertificatesUpdate) {
		
		// update the configuration for certFrequency and healFrequency
		//updateConfFileParameter(rhsmConfFile, "certFrequency", String.valueOf(certFrequency));
		//updateConfFileParameter(rhsmConfFile, "healFrequency", String.valueOf(healFrequency));
		// save time using the new config module to update the configuration
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		if (certFrequency!=null) listOfSectionNameValues.add(new String[]{"rhsmcertd", /*"certFrequency" was renamed by bug 882459 to*/"certCheckInterval".toLowerCase(), String.valueOf(certFrequency)});
		else certFrequency = Integer.valueOf(getConfFileParameter(rhsmConfFile, "rhsmcertd", /*"certFrequency" was renamed by bug 882459 to*/"certCheckInterval"));
		if (healFrequency!=null) listOfSectionNameValues.add(new String[]{"rhsmcertd", /*"healFrequency" was renamed by bug 882459 to*/"autoAttachInterval".toLowerCase(), String.valueOf(healFrequency)});
		else healFrequency = Integer.valueOf(getConfFileParameter(rhsmConfFile, "rhsmcertd", /*"healFrequency" was renamed by bug 882459 to*/"autoAttachInterval"));
		if (listOfSectionNameValues.size()>0) config(null,null,true,listOfSectionNameValues);
		
		// avoid excessive restart delays by turning off the splay configuration 
		String originalSplayConfig = null;
 		if (isPackageVersion("subscription-manager",">=","1.19.8-1")) {	// commit e9f8421285fc6541166065a8b55ee89b9a425246 RFE Bug 1435013: Add splay option to rhsmcertd, randomize over interval
			int maxSplayDelayMinutes = 10;	// arbitrarily selected to a small number assuming it is greater than any value tested via the automated our rhsm-qe tests and less than the default frequency values of 240 and 1440.
			if (healFrequency>maxSplayDelayMinutes || certFrequency>maxSplayDelayMinutes) {
				originalSplayConfig = getConfFileParameter(rhsmConfFile, "rhsmcertd", "splay");	// get the original rhsmcertd.splay config
				if (originalSplayConfig!=null) {
					log.warning("The rhsmcertd.autoAttachInterval ("+healFrequency+") and/or rhsmcertd.certCheckInterval ("+certFrequency+") exceed an automation friendly limit of '"+maxSplayDelayMinutes+"'.  Temporarily disabling rhsmcertd.splay to avoid long delays waiting for the next update...");
					try {
						updateConfFileParameter(rhsmConfFile, "rhsmcertd", "splay","0");
					} catch (IOException e) {
						Assert.fail(e.getMessage());
					}
				}
			}
		}
		
		// mark the rhsmcertd log file before restarting the deamon
 		RemoteFileTasks.runCommandAndWait(sshCommandRunner, "touch "+rhsmcertdLogFile, TestRecords.action());	// to ensure the file exists before trying to mark it
		String rhsmcertdLogMarker = System.currentTimeMillis()+" Testing service rhsmcertd restart...";
		RemoteFileTasks.markFile(sshCommandRunner, rhsmcertdLogFile, rhsmcertdLogMarker);
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId="804227"; //  Status: 	CLOSED ERRATA
		boolean invokeWorkaroundWhileBugIsOpen = false;
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			throw new SkipException("Restart rhsmcertd has no workaround for Bugzilla "+bugId+".");
		}
		// END OF WORKAROUND
		
		//	[root@jsefler-63server ~]# service rhsmcertd restart
		//	Stopping rhsmcertd                                         [  OK  ]
		//	Starting rhsmcertd 2 1440                                  [  OK  ]

		// TEMPORARY WORKAROUND FOR BUG
		if (this.arch.equals("s390x") || this.arch.equals("ppc64")) {
			bugId="691137";	// Status: 	CLOSED ERRATA
			invokeWorkaroundWhileBugIsOpen = false;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				RemoteFileTasks.runCommandAndWait(sshCommandRunner,"service rhsmcertd restart", TestRecords.action());
			} else {
				/* VALID PRIOR TO BUG 818978:
				 * RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd "+certFrequency+" "+healFrequency+"\\[  OK  \\]$",null);
				 */
			}
		} else {
		// END OF WORKAROUND
			
		/* VALID PRIOR TO BUG 818978:
		 * RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd "+certFrequency+" "+healFrequency+"\\[  OK  \\]$",null);
		 */
		}
		
		// restart rhsmcertd service
		if (Integer.valueOf(redhatReleaseX)>=7)	{	// the RHEL7 / F16+ way...
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "systemctl restart rhsmcertd.service && systemctl is-active rhsmcertd.service", Integer.valueOf(0), "^active$", null);
		} else {
			// NEW SERVICE RESTART FEEDBACK AFTER IMPLEMENTATION OF Bug 818978 - Missing systemD unit file
			//	[root@jsefler-59server ~]# service rhsmcertd restart
			//	Stopping rhsmcertd...                                      [  OK  ]
			//	Starting rhsmcertd...                                      [  OK  ]
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd restart",Integer.valueOf(0),"^Starting rhsmcertd\\.\\.\\.\\[  OK  \\]$",null);	
			
			// # service rhsmcertd restart
			// rhsmcertd (pid 10172 10173) is running...
			
			//RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd status",Integer.valueOf(0),"^rhsmcertd \\(pid \\d+ \\d+\\) is running...$",null);	// RHEL62 branch
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd status",Integer.valueOf(0),"^rhsmcertd \\(pid \\d+\\) is running...$",null);		// master/RHEL58 branch
			//RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"service rhsmcertd status",Integer.valueOf(0),"^rhsmcertd \\(pid( \\d+){1,2}\\) is running...$",null);	// tolerate 1 or 2 pids for RHEL62 or RHEL58; don't really care which it is since the next assert is really sufficient
		}
		
		// # tail -f /var/log/rhsm/rhsmcertd.log
		// Wed Nov  9 15:21:54 2011: started: interval = 1440 minutes
		// Wed Nov  9 15:21:54 2011: started: interval = 240 minutes
		// Wed Nov  9 15:21:55 2011: certificates updated
		// Wed Nov  9 15:21:55 2011: certificates updated
		
		// TEMPORARY WORKAROUND FOR BUG
		/*boolean*/ invokeWorkaroundWhileBugIsOpen = false; // Current bug status is: CLOSED ERRATA; setting invokeWorkaroundWhileBugIsOpen to false to save execution time
		String bugId1="752572";	// Status: 	CLOSED ERRATA
		String bugId2="759199";	// Status: 	CLOSED ERRATA
		invokeWorkaroundWhileBugIsOpen = false;
		try {if (invokeWorkaroundWhileBugIsOpen&&(BzChecker.getInstance().isBugOpen(bugId1)||BzChecker.getInstance().isBugOpen(bugId2))) {log.fine("Invoking workaround for Bugzillas:  https://bugzilla.redhat.com/show_bug.cgi?id="+bugId1+" https://bugzilla.redhat.com/show_bug.cgi?id="+bugId2);SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId1);SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId2);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping assert of the rhsmcertd logging of the started: interval certFrequency and healFrequency while bug "+bugId1+" or "+bugId2+" is open.");
		} else {
		// END OF WORKAROUND
		//RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"tail -4 "+rhsmcertdLogFile,Integer.valueOf(0),"(.*started: interval = "+healFrequency+" minutes\n.*started: interval = "+certFrequency+" minutes)|(.*started: interval = "+certFrequency+" minutes\n.*started: interval = "+healFrequency+" minutes)",null);
		//RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"tail -4 "+rhsmcertdLogFile,Integer.valueOf(0),".* healing check started: interval = "+healFrequency+"\n.* cert check started: interval = "+certFrequency,null);
		/* VALID PRIOR TO BUG 818978:
		 * RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"tail -4 "+rhsmcertdLogFile,Integer.valueOf(0),".* healing check started: interval = "+healFrequency+" minute\\(s\\)\n.* cert check started: interval = "+certFrequency+" minute\\(s\\)",null);
		 */
		}
		
		// give the rhsmcertd time to make its initial check-in with the candlepin server and update the certs
		// I've seen this take 10 to 15 seconds as demonstrated here...
		
		// when registered...
		//	1334786048260 Testing service rhsmcertd restart...
		//	Wed Apr 18 17:54:11 2012: healing check started: interval = 1440
		//	Wed Apr 18 17:54:11 2012: cert check started: interval = 240
		//	Wed Apr 18 17:54:21 2012: certificates updated
		//	Wed Apr 18 17:54:26 2012: certificates updated
		
		// when not registered...
		//	1341610172422 Testing service rhsmcertd restart...
		//	Fri Jul  6 17:30:48 2012: Loading configuration from command line
		//	Fri Jul  6 17:30:48 2012: Cert Frequency: 14400 seconds
		//	Fri Jul  6 17:30:48 2012: Heal Frequency: 86400 seconds
		//	Fri Jul  6 17:30:48 2012: healing check started: interval = 1440 minute(s)
		//	Fri Jul  6 17:30:48 2012: cert check started: interval = 240 minute(s)
		//	Fri Jul  6 17:30:48 2012: update failed (255), retry will occur on next run
		//	Fri Jul  6 17:30:49 2012: update failed (255), retry will occur on next run


		// assert the rhsmcertd log file reflected newly updated certificates...
		/* VALID PRIOR TO BUG 818978:
		int i=0, delay=10;
		String rhsmcertdLogResult,updateRegex;
		if (this.currentlyRegisteredUsername==null)	updateRegex = ".*update failed \\(255\\), retry will occur on next run\\n.*update failed \\(255\\), retry will occur on next run";	// when NOT registered
		else										updateRegex = ".*certificates updated\\n.*certificates updated";
		do {	// retry every 10 seconds (up to a minute) for the expected update certificates regex in the rhsmcertd log
			SubscriptionManagerCLITestScript.sleep(delay*1000);i++;	// wait a few seconds before trying again
			rhsmcertdLogResult = RemoteFileTasks.getTailFromMarkedFile(sshCommandRunner, rhsmcertdLogFile, rhsmcertdLogMarker, "update").trim();
			if (rhsmcertdLogResult.matches(updateRegex)) break;
		} while (delay*i < 60);
		Assert.assertTrue(rhsmcertdLogResult.matches(updateRegex), "Expected certificate update regex '"+updateRegex+"' is being logged to rhsmcertd log during a restart.");
		*/
		
		//	[root@jsefler-59server ~]# tail -f /var/log/rhsm/rhsmcertd.log
		//  1342466941476 Testing service rhsmcertd restart...
		//	Mon Jul 16 13:23:56 2012 [INFO] rhsmcertd is shutting down...
		//	Mon Jul 16 13:23:56 2012 [INFO] Starting rhsmcertd...
		//	Mon Jul 16 13:23:56 2012 [INFO] Healing interval: 1440.0 minute(s) [86400 second(s)]
		//	Mon Jul 16 13:23:56 2012 [INFO] Cert check interval: 2.0 minute(s) [120 second(s)]
		//	Mon Jul 16 13:23:56 2012 [INFO] Waiting 120 second(s) [2.0 minute(s)] before running updates.
		//	Mon Jul 16 13:25:56 2012 [WARN] (Healing) Update failed (255), retry will occur on next run.
		//	Mon Jul 16 13:25:57 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		//	Mon Jul 16 13:27:56 2012 [WARN] (Cert Check) Update failed (255), retry will occur on next run.
		
		//  1342466941944 Testing service rhsmcertd restart...
		//	Mon Jul 16 14:30:20 2012 [INFO] rhsmcertd is shutting down...
		//	Mon Jul 16 14:30:20 2012 [INFO] Starting rhsmcertd...
		//	Mon Jul 16 14:30:20 2012 [INFO] Healing interval: 1440.0 minute(s) [86400 second(s)]
		//	Mon Jul 16 14:30:20 2012 [INFO] Cert check interval: 2.0 minute(s) [120 second(s)]
		//	Mon Jul 16 14:30:20 2012 [INFO] Waiting 120 second(s) [2.0 minute(s)] before running updates.
		//	Mon Jul 16 14:32:25 2012 [INFO] (Healing) Certificates updated.
		//	Mon Jul 16 14:32:29 2012 [INFO] (Cert Check) Certificates updated.
		//	Mon Jul 16 14:34:22 2012 [INFO] (Cert Check) Certificates updated.

		Integer waitSecondsForFirstUpdateCheck = 120; // this is a dev hard coded wait (seconds) before the first check for updates is attempted  REFERENCE BUG 818978#c2
		String rhsmcertdLogExpectedStartingRhsmMsg = String.format(" Starting rhsmcertd...");
		String rhsmcertdLogExpectedHealIntervalMsg = String.format(" Healing interval: %.1f minute(s) [%d second(s)]",healFrequency*1.0,healFrequency*60);/* msg was changed by bug 882459*/
		rhsmcertdLogExpectedHealIntervalMsg = String.format(" Auto-attach interval: %.1f minute(s) [%d second(s)]",healFrequency*1.0,healFrequency*60);
		if (isPackageVersion("subscription-manager",">=","1.19.9-1")) {	// commit e0e1a6b3e80c33e04fe79eaa696a821881f95f35 1443205: Simplify rhsmcertd log message plurality
			rhsmcertdLogExpectedHealIntervalMsg = String.format(" Auto-attach interval: %.1f minutes [%d seconds]",healFrequency*1.0,healFrequency*60);
		}
		String rhsmcertdLogExpectedCertIntervalMsg = String.format(" Cert check interval: %.1f minute(s) [%d second(s)]",certFrequency*1.0,certFrequency*60);
		if (isPackageVersion("subscription-manager",">=","1.19.9-1")) {	// commit e0e1a6b3e80c33e04fe79eaa696a821881f95f35 1443205: Simplify rhsmcertd log message plurality
			rhsmcertdLogExpectedCertIntervalMsg = String.format(" Cert check interval: %.1f minutes [%d seconds]",certFrequency*1.0,certFrequency*60);
		}
		String rhsmcertdLogResult;
		int i=0, delay=5;
		do {	// retry every 5 seconds (up to a 20 seconds) for the expected update messages in the rhsmcertd log
			if (i>0) SubscriptionManagerCLITestScript.sleep(delay*1000);	// wait a few seconds before trying again
			rhsmcertdLogResult = RemoteFileTasks.getTailFromMarkedFile(sshCommandRunner, rhsmcertdLogFile, rhsmcertdLogMarker, null).trim();
			if (rhsmcertdLogResult.contains(rhsmcertdLogExpectedStartingRhsmMsg)) break;
		} while (delay*i++ < 20);	// Note: should wait at least 60+ additional seconds because auto-attach can timeout after 60 seconds.  see bug https://bugzilla.redhat.com/show_bug.cgi?id=964332#c6
		
		Assert.assertTrue(rhsmcertdLogResult.contains(rhsmcertdLogExpectedStartingRhsmMsg),"Tail of rhsmcertd log contains the expected restart message '"+rhsmcertdLogExpectedStartingRhsmMsg+"'.");
		Assert.assertTrue(rhsmcertdLogResult.contains(rhsmcertdLogExpectedHealIntervalMsg),"Tail of rhsmcertd log contains the expected restart message '"+rhsmcertdLogExpectedHealIntervalMsg+"'.");
		Assert.assertTrue(rhsmcertdLogResult.contains(rhsmcertdLogExpectedCertIntervalMsg),"Tail of rhsmcertd log contains the expected restart message '"+rhsmcertdLogExpectedCertIntervalMsg+"'.");

		if (isPackageVersion("subscription-manager",">=","1.19.8-1")) {	// commit e9f8421285fc6541166065a8b55ee89b9a425246 RFE Bug 1435013: Add splay option to rhsmcertd, randomize over interval
			// The new splay algorithm from RFE Bug 1435013 will wait a hard coded 2 minutes...
			// A. plus a random number of seconds up to a maximum of the configured Auto-attach interval before running /usr/libexec/rhsmcertd-worker --autoheal
			// B. plus a random number of seconds up to a maximum of the configured Cert check interval before running /usr/libexec/rhsmcertd-worker
			
			// The new splay delay can be turned on/off using the rhsmcertd.splay configuration.
			// When off (splay=0), the splay seconds will be zero which effectively restores the former behavior.
			String splayConfig = getConfFileParameter(rhsmConfFile, "rhsmcertd", "splay");
			
			//	[root@jsefler-rhel7 ~]# tail -f /var/log/rhsm/rhsmcertd.log
			//	1492537756242 Testing service rhsmcertd restart...
			//	Tue Apr 18 13:49:16 2017 [INFO] rhsmcertd is shutting down...
			//	Tue Apr 18 13:49:16 2017 [INFO] Starting rhsmcertd...
			//	Tue Apr 18 13:49:16 2017 [INFO] Auto-attach interval: 4.0 minute(s) [240 second(s)]
			//	Tue Apr 18 13:49:16 2017 [INFO] Cert check interval: 3.0 minute(s) [180 second(s)]
			//	Tue Apr 18 13:49:16 2017 [INFO] Waiting 2.0 minute(s) plus 129 splay second(s) [249 seconds(s) totals] before performing first auto-attach.
			//	Tue Apr 18 13:49:16 2017 [INFO] Waiting 2.0 minute(s) plus 86 splay second(s) [206 seconds(s) totals] before performing first cert check.
			//	Tue Apr 18 13:52:43 2017 [INFO] (Cert Check) Certificates updated.
			//	Tue Apr 18 13:53:26 2017 [INFO] (Auto-attach) Certificates updated.
			
			// assert the wait time for auto-attach
			String rhsmcertdLogResultExpectedRegex = String.format(" Waiting %.1f minute\\(s\\) plus (\\d+) splay second\\(s\\) \\[(\\d+) seconds\\(s\\) totals\\] before performing first auto-attach\\.",waitSecondsForFirstUpdateCheck/60.0);
			if (isPackageVersion("subscription-manager",">=","1.19.9-1")) {	// commit e0e1a6b3e80c33e04fe79eaa696a821881f95f35 1443205: Simplify rhsmcertd log message plurality
				rhsmcertdLogResultExpectedRegex = String.format(" Waiting %.1f minutes plus (\\d+) splay seconds \\[(\\d+) seconds total\\] before performing first auto-attach\\.",waitSecondsForFirstUpdateCheck/60.0);
			}
			Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex),"Tail of rhsmcertd log contains the expected restart regex message '"+rhsmcertdLogResultExpectedRegex+"'.");
			int splayHealSeconds = Integer.valueOf(SubscriptionManagerCLITestScript.getSubstringMatches(SubscriptionManagerCLITestScript.getSubstringMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex).get(0), "(\\d+) splay").get(0).split(" ")[0]);	// 129 splay second(s)
			int totalHealSeconds = Integer.valueOf(SubscriptionManagerCLITestScript.getSubstringMatches(SubscriptionManagerCLITestScript.getSubstringMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex).get(0), "(\\d+) seconds").get(0).split(" ")[0]);	// 249 seconds(s) totals
			Assert.assertTrue(0<=splayHealSeconds && splayHealSeconds<=healFrequency*60/*seconds*/, "The splay ("+splayHealSeconds+") seconds is greater than or equal to zero and less than or equal to the auto-attach interval ("+healFrequency*60+" seconds) ");
			Assert.assertEquals(totalHealSeconds, waitSecondsForFirstUpdateCheck+splayHealSeconds, "The total wait time in seconds for the first auto-attach after restarting rhsmcertd.");
			if (splayConfig.equals("0")||splayConfig.toLowerCase().equals("false")) Assert.assertEquals(splayHealSeconds,0,"When the rhsmcertd.splay configuration in '"+rhsmConfFile+"' is off, the splay seconds for auto-attach should be zero.");
				
			// assert the wait time for cert check
			rhsmcertdLogResultExpectedRegex = String.format(" Waiting %.1f minute\\(s\\) plus (\\d+) splay second\\(s\\) \\[(\\d+) seconds\\(s\\) totals\\] before performing first cert check\\.",waitSecondsForFirstUpdateCheck/60.0);
			if (isPackageVersion("subscription-manager",">=","1.19.9-1")) {	// commit e0e1a6b3e80c33e04fe79eaa696a821881f95f35 1443205: Simplify rhsmcertd log message plurality
				rhsmcertdLogResultExpectedRegex = String.format(" Waiting %.1f minutes plus (\\d+) splay seconds \\[(\\d+) seconds total\\] before performing first cert check\\.",waitSecondsForFirstUpdateCheck/60.0);
			}
			Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex),"Tail of rhsmcertd log contains the expected restart regex message '"+rhsmcertdLogResultExpectedRegex+"'.");
			int splayCertSeconds = Integer.valueOf(SubscriptionManagerCLITestScript.getSubstringMatches(SubscriptionManagerCLITestScript.getSubstringMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex).get(0), "(\\d+) splay").get(0).split(" ")[0]);	// 86 splay second(s)
			int totalCertSeconds = Integer.valueOf(SubscriptionManagerCLITestScript.getSubstringMatches(SubscriptionManagerCLITestScript.getSubstringMatches(rhsmcertdLogResult, rhsmcertdLogResultExpectedRegex).get(0), "(\\d+) seconds").get(0).split(" ")[0]);	// 206 seconds(s) totals
			Assert.assertTrue(0<=splayCertSeconds && splayCertSeconds<=certFrequency*60/*seconds*/, "The splay ("+splayCertSeconds+") seconds is greater than or equal to zero and less than or equal to the cert check interval ("+certFrequency*60+" seconds) ");
			Assert.assertEquals(totalCertSeconds, waitSecondsForFirstUpdateCheck+splayCertSeconds, "The total wait time in seconds for the first cert check after restarting rhsmcertd.");
			if (splayConfig.equals("0")||splayConfig.toLowerCase().equals("false")) Assert.assertEquals(splayCertSeconds,0,"When the rhsmcertd.splay configuration in '"+rhsmConfFile+"' is off, the splay seconds for cert check should be zero.");
	
			// Waiting 120 second(s) [2.0 minute(s)] PLUS THE MAXIMUM SPLAY DELAYS before asserting updates.
			SubscriptionManagerCLITestScript.sleep(Math.max(totalHealSeconds, totalCertSeconds)*1000/*milliseconds*/);
		} else {
			String rhsmcertdLogExpectedWaitMsg = String.format(" Waiting %d second(s) [%.1f minute(s)] before running updates.",waitSecondsForFirstUpdateCheck,waitSecondsForFirstUpdateCheck/60.0 );
			Assert.assertTrue(rhsmcertdLogResult.contains(rhsmcertdLogExpectedWaitMsg),"Tail of rhsmcertd log contains the expected restart message '"+rhsmcertdLogExpectedWaitMsg+"'.");
			
			// Waiting 120 second(s) [2.0 minute(s)] before running updates.
			SubscriptionManagerCLITestScript.sleep(waitSecondsForFirstUpdateCheck*1000/*milliseconds*/);
		}
		
		// assert the rhsmcertd log for messages stating the cert and heal frequencies have be logged
		if (assertCertificatesUpdate!=null) {
			
			// assert these cert and heal update/fail messages are logged (but give the system up to a minute to do it)
			//String healMsg = assertCertificatesUpdate? "(Healing) Certificates updated.":"(Healing) Update failed (255), retry will occur on next run.";	// msg was changed by bug 882459
			String healMsg = assertCertificatesUpdate? "(Auto-attach) Certificates updated.":"(Auto-attach) Update failed (255), retry will occur on next run.";
			String certMsg = assertCertificatesUpdate? "(Cert Check) Certificates updated.":"(Cert Check) Update failed (255), retry will occur on next run.";
			/*int*/ i=0; delay=10;
			do {	// retry every 10 seconds (up to a 90 seconds) for the expected update messages in the rhsmcertd log
				if (i>0) SubscriptionManagerCLITestScript.sleep(delay*1000);i++;	// wait a few seconds before trying again
				rhsmcertdLogResult = RemoteFileTasks.getTailFromMarkedFile(sshCommandRunner, rhsmcertdLogFile, rhsmcertdLogMarker, null).trim();
				if (rhsmcertdLogResult.contains(healMsg) && rhsmcertdLogResult.contains(certMsg)) break;
			} while (delay*i++ < 90);	// Note: should wait at least 60+ additional seconds because auto-attach can timeout after 60 seconds.  see bug https://bugzilla.redhat.com/show_bug.cgi?id=964332#c6
			
			boolean healMsgAssert = true;
			boolean certMsgAssert = true;
			
			// TEMPORARY WORKAROUND FOR BUG
			bugId="1241247"; // Bug 1241247 - rhsmcertd-worker throws Traceback "ImportError: cannot import name ga"
			invokeWorkaroundWhileBugIsOpen = true;
			try {if (assertCertificatesUpdate&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (assertCertificatesUpdate&&invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Restart rhsmcertd has no workaround for Bugzilla "+bugId+".");
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			bugId="861443"; // Bug 861443 - rhsmcertd logging of Healing shows "Certificates updated." when it should fail.
			invokeWorkaroundWhileBugIsOpen = true;
			try {if (assertCertificatesUpdate&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (!assertCertificatesUpdate&&invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping assertion: "+"Tail of rhsmcertd log contains the expected restart message '"+healMsg+"'.");
				healMsgAssert = false;
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			bugId="1440934"; // Bug 1440934 - rhsmcertd is not starting the Auto-attach interval
			invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping assertion: "+"Tail of rhsmcertd log contains the expected restart message '"+healMsg+"'.");
				healMsgAssert = false;
			}
			// END OF WORKAROUND
			
			if (healMsgAssert) Assert.assertTrue(rhsmcertdLogResult.contains(healMsg),"Tail of rhsmcertd log contains the expected restart message '"+healMsg+"'.");
			if (certMsgAssert) Assert.assertTrue(rhsmcertdLogResult.contains(certMsg),"Tail of rhsmcertd log contains the expected restart message '"+certMsg+"'.");
		}
		
		// restore the original splay configuration 
		if (originalSplayConfig!=null) {
			log.warning("Restoring the disabled rhsmcertd.splay to it original configuration '"+originalSplayConfig+"' ...");
			try {
				updateConfFileParameter(rhsmConfFile, "rhsmcertd", "splay",originalSplayConfig);
			} catch (IOException e) {
				Assert.fail(e.getMessage());
			}
		}
	}
	
	public void stop_rhsmcertd (){
		if (Integer.valueOf(redhatReleaseX)>=7)	{	// the RHEL7+ / Fedora16+ way...
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "systemctl stop rhsmcertd.service && systemctl is-active rhsmcertd.service", Integer.valueOf(3), "^inactive$", null);
		} else {
			RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "service rhsmcertd stop && service rhsmcertd status", Integer.valueOf(3), "^rhsmcertd is stopped$", null);  // exit code 3 = program not running		// reference Bug 232163; Bug 679812
		}
	}
	
	
	/**
	 * run /usr/libexec/rhsmcertd-worker as a faster alternative to restart_rhsmcertd(...) to avoid a two minute sleep delay waiting for the cert check updates.
	 * @param autoheal - passing true will trigger what happens on the autoAttachInterval; otherwise the certCheckInterval events will trigger
	 * @return the command result
	 */
	public SSHCommandResult run_rhsmcertd_worker(Boolean autoheal) {
		String command = this.rhsmCertDWorker;
		
		if (autoheal!=null && autoheal)		command += " --autoheal";
		
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		
		// assert results...
		logRuntimeErrors(sshCommandResult);
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from command '"+command+"' indicates a success.");

		return sshCommandResult;
	}
	
	/**
	 * Wait for the last line appended to the rhsmcertd.log file to contain a match to the given logRegex.
	 * An assertion failure will be thrown if the timeout is reached without finding a match.
	 * @param logRegex
	 * @param timeoutMinutes - do not wait longer than this many minutes for a match 
	 * @return - last line appended to the rhsmcertd.log
	 */
	public String waitForRegexInRhsmcertdLog(String logRegex, int timeoutMinutes) {
		int retryMilliseconds = Integer.valueOf(getConfFileParameter(rhsmConfFile, /*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval"))*60*1000;  // certFrequency is in minutes
		int t = 0;
		
		// get the last line of the rhsmcertd.log
		String lastLine = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"tail -1 "+rhsmcertdLogFile,Integer.valueOf(0)).getStdout().trim();
		
		// check for matches to logRegex within the timeout
		while(t*retryMilliseconds < timeoutMinutes*60*1000) {
			//if (lastLine.matches(logRegex)) return lastLine;	// successfully waited within the timeout
			if (SubscriptionManagerCLITestScript.doesStringContainMatches(lastLine,logRegex)) return lastLine;	// successfully waited within the timeout
			
			// pause for the sleep interval
			SubscriptionManagerCLITestScript.sleep(retryMilliseconds); t++;	
			
			// get the last line of the rhsmcertd.log again
			lastLine = sshCommandRunner.runCommandAndWait("tail -1 "+rhsmcertdLogFile).getStdout().trim();
		}
		//if (lastLine.matches(logRegex)) return lastLine;	// final check for success
		if (SubscriptionManagerCLITestScript.doesStringContainMatches(lastLine,logRegex)) return lastLine;	// final check for success
		
		// we failed to get a match within the timeout
		Assert.fail("Reached the timeout of '"+timeoutMinutes+"' minutes waiting for the last line of "+rhsmcertdLogFile+" to match regex '"+logRegex+"' (Actual time waited was "+t*retryMilliseconds+"' milliseconds.)");
		return lastLine;
	}
	
	
	
	
	/**
	 * @return the current service level returned by subscription-manager service-level --show (must already be registered); will return an empty string when the service level preference is not set.
	 */
	public String getCurrentServiceLevel() {
		
		SSHCommandResult result = service_level(true, false, null, null, null, null, null, null, null, null, null, null, null);
		
		// [root@jsefler-r63-server ~]# subscription-manager service-level --show
		// Current service level: Standard
		//
		// [root@jsefler-r63-server ~]# subscription-manager service-level --show
		// Current service level: 
		
		// [root@jsefler-59server ~]# subscription-manager service-level --show --list
		// Service level preference not set
		// +-------------------------------------------+
		//                Available Service Levels
		// +-------------------------------------------+
		// PREMIUM
		// STANDARD
		// NONE
		// [root@jsefler-59server ~]# 


		String serviceLevel = result.getStdout().split("\\+-+\\+")[0].replaceFirst(".*:", "").trim();
		serviceLevel = serviceLevel.replace("Service level preference not set", "");	// decided not to use serviceLevel=null when the service level is not set because the json value of the consumer's service level will be "" instead of null which effectively means the service level is not set.

		return serviceLevel;
	}
	
	/**
	 * @return the current release returned by subscription-manager release (must already be registered)
	 */
	public String getCurrentRelease() {
		
		SSHCommandResult result = release(null, null, null, null, null, null, null, null);
		
		//	[root@jsefler-r63-server ~]# subscription-manager release
		//	Release: foo
		//	[root@jsefler-r63-server ~]# subscription-manager release
		//	Release not set

		String release = result.getStdout().replaceFirst(".*:", "").replaceFirst("Release not set", "").trim();
		
		return release;
	}
	
	/**
	 * @return list of the service labels returned by subscription-manager service-level --list (must already be registered)
	 */
	public List<String> getCurrentlyAvailableServiceLevels() {
		return getAvailableServiceLevels(null,null,null);
	}
	
	/**
	 * @param username
	 * @param password
	 * @param org
	 * @return list of the service labels returned by subscription-manager service-level --list --username=username --password=password --org=org
	 */
	public List<String> getAvailableServiceLevels(String username, String password, String org) {
		
		SSHCommandResult result = service_level_(false, true, null, null, username, password, org, null, null, null, null, null, null);
		
		List<String> serviceLevels = new ArrayList<String>();
		if (!result.getExitCode().equals(Integer.valueOf(0))) return serviceLevels;

		//	ssh root@margo.idm.lab.bos.redhat.com subscription-manager service-level --list
		//	Stdout: This org does not have any subscriptions with service levels.
		//	Stderr:
		//	ExitCode: 0
		if (result.getStdout().trim().equals("This org does not have any subscriptions with service levels.")) return serviceLevels;

		
		//	[root@jsefler-r63-server ~]# subscription-manager service-level --list
		//	+-------------------------------------------+
		//	          Available Service Levels
		//	+-------------------------------------------+
		//	Standard
		//	None
		//	Premium
		for (String serviceLevel : result.getStdout().split("\\+-+\\+")[result.getStdout().split("\\+-+\\+").length-1].trim().split("\\n")) {
			serviceLevels.add(serviceLevel);
		}
		
		return serviceLevels;
	}
	
	/**
	 * @param proxy TODO
	 * @param proxyusername TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 * @return list of the releases returned by subscription-manager release --list (must already be registered)
	 */
	public List<String> getCurrentlyAvailableReleases(String proxy, String proxyusername, String proxypassword, String noproxy) {
		
		SSHCommandResult result = release_(null,true,null,null,proxy, proxyusername, proxypassword, noproxy);
		String stdout = result.getStdout().trim();
		
		//	[root@jsefler-r63-workstation ~]# subscription-manager release --list
		//	5.7
		//	5.8
		//	5Client
		//	6.0
		//	6.1
		//	6.2
		//	6Workstation
		
		// Bug 824979 - No message for "subscription-manager release --list" with no subscriptions
		// result when no releases are available
		// FINE: ssh root@jsefler-59server.usersys.redhat.com subscription-manager release --list
		// FINE: Stdout: 
		// FINE: Stderr: No release versions available, please check subscriptions.
		// FINE: ExitCode: 255
		
		// Bug 808217 - [RFE] a textural output banner would be nice for subscription-manager release --list
		//	[root@jsefler-6 ~]# subscription-manager release --list
		//	+-------------------------------------------+
		//	          Available Releases
		//	+-------------------------------------------+
		//	6.1
		//	6.2
		//	6.3
		//	6Server
		
		// strip off the banner (added by bug 808217 in RHEL64)
		String bannerRegex = "\\+-+\\+\\n\\s*Available Releases\\s*\\n\\+-+\\+";
		stdout = stdout.replaceFirst(bannerRegex, "");

		List<String> releases =  new ArrayList<String>();
		for (String release : stdout.split("\\s*\\n\\s*")) {
			if (!release.isEmpty())	releases.add(release);
		}
		
		return releases;
	}
	
	
	/**
	 * @return list of the expected releases currently available based on the currently enabled repo content and this major RHEL release
	 */
	public List<String> getCurrentlyExpectedReleases() {
		HashSet<String> expectedReleaseSet = new HashSet<String>();
		String baseurl = getConfFileParameter(rhsmConfFile, "rhsm", "baseurl");
		List<ProductCert> productCerts = getCurrentProductCerts();
		
		// loop through all of the currently entitled repo urls
		for (EntitlementCert entitlementCert : getCurrentEntitlementCerts()) {
			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				if (contentNamespace.type.equalsIgnoreCase("yum")) {
					if (contentNamespace.enabled) {	// Bug 820639 - subscription-manager release --list should exclude listings from disabled repos
						if (areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, productCerts)) {	// Bug 861151 - subscription-manager release doesn't take variant into account 
							if (contentNamespace.downloadUrl.contains("$releasever")) {
								if (contentNamespace.downloadUrl.contains("/"+redhatReleaseX+"/")) {	// Bug 818298 - subscription-manager release --list should not display releasever applicable to rhel-5 when only rhel-6 product is installed
									// example contentNamespace.downloadUrl:  /content/dist/rhel/server/5/$releasever/$basearch/iso
									String listingUrl =  contentNamespace.downloadUrl.startsWith("http")? "":baseurl;
									listingUrl += contentNamespace.downloadUrl.split("/\\$releasever/")[0];
									listingUrl += "/listing";
									String command = String.format("curl --stderr /dev/null --insecure --tlsv1 --cert %s --key %s %s" , entitlementCert.file.getPath(), getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCert.file).getPath(), listingUrl);
									SSHCommandResult result = sshCommandRunner.runCommandAndWaitWithoutLogging(command);
									//	[root@qe-blade-13 ~]# curl --stderr /dev/null --insecure --tlsv1 --cert /etc/pki/entitlement/2013167262444796312.pem --key /etc/pki/entitlement/2013167262444796312-key.pem https://cdn.rcm-qa.redhat.com/content/dist/rhel/server/6/listing
									//	6.1
									//	6.2
									//	6Server
									
									// process exceptional results...
									if (result.getStdout().toUpperCase().contains("<HTML>")) {
										//	[root@jsefler-6 ~]# curl --stderr /dev/null --insecure --tlsv1 --cert /etc/pki/entitlement/3360706382464344965.pem --key /etc/pki/entitlement/3360706382464344965-key.pem https://cdn.redhat.com/content/dist/rhel/server/6/listing
										//	<HTML><HEAD>
										//	<TITLE>Access Denied</TITLE>
										//	</HEAD><BODY>
										//	<H1>Access Denied</H1>
										log.warning("curl result: "+result);	// or should this be a failure?
										Assert.fail("Expected to retrieve a list of available release versions. (Example: 6.1, 6.2, 6Server)");
									} else if (result.getStdout().trim().startsWith("Unable to locate content")) {
										//	[root@jsefler-5 ~]# curl --stderr /dev/null --insecure --tlsv1 --cert /etc/pki/entitlement/6653190787244398414.pem --key /etc/pki/entitlement/6653190787244398414-key.pem https://cdn.qa.redhat.com/content/aus/rhel/server/5/listing
										//	Unable to locate content /content/aus/rhel/server/5/listing
										log.warning("curl result: "+result);	// or should this be a failure?
										log.warning("Query failed to get an expected release listing from ContentNamespace: \n"+contentNamespace);
										log.warning("After setting an older release, a yum repolist of this content set will likely yield: [Errno 14] HTTP Error 404: Not Found");
									} else {
										expectedReleaseSet.addAll(Arrays.asList(result.getStdout().trim().split("\\s*\\n\\s*")));
									}
								}
							}
						}
					}
				}
			}
		}
		return new ArrayList<String>(expectedReleaseSet);
		
		// ^^ TODO On second thought, it would technically be more correct to loop over the current YumRepo object rather than the Entitlement Certs since a repo enablement could have been manually overridden
// TODO work in progress
//		for (YumRepo yumRepo : getCurrentlySubscribedYumRepos()()) {
//					if (yumRepo.enabled) {	// Bug 820639 - subscription-manager release --list should exclude listings from disabled repos
//						if (yumRepo.baseurl.contains("$releasever")) {
//							if (yumRepo.baseurl.contains("/"+redhatReleaseX+"/")) {	// Bug 818298 - subscription-manager release --list should not display releasever applicable to rhel-5 when only rhel-6 product is installed
//								// example contentNamespace.downloadUrl:  /content/dist/rhel/server/5/$releasever/$basearch/iso
//								String listingUrl =  yumRepo.baseurl.startsWith("http")? "":baseurl;
//								listingUrl += yumRepo.baseurl.split("/\\$releasever/")[0];
//								listingUrl += "/listing";
//								String command = String.format("curl --stderr /dev/null --insecure --cert %s --key %s %s" , entitlementCert.file.getPath(), getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(entitlementCert.file).getPath(), listingUrl);
//								SSHCommandResult result = sshCommandRunner.runCommandAndWaitWithoutLogging(command);
//								//	[root@qe-blade-13 ~]# curl --stderr /dev/null --insecure --cert /etc/pki/entitlement/2013167262444796312.pem --key /etc/pki/entitlement/2013167262444796312-key.pem https://cdn.rcm-qa.redhat.com/content/dist/rhel/server/6/listing
//								//	6.1
//								//	6.2
//								//	6Server
//								expectedReleaseSet.addAll(Arrays.asList(result.getStdout().trim().split("\\s*\\n\\s*")));
//							}
//						}
//					}
//		}
	}
	
	
	
	
	
	/**
	 * @return list of objects representing the subscription-manager list --available
	 */
	public List<SubscriptionPool> getCurrentlyAvailableSubscriptionPools() {
		return SubscriptionPool.parse(listAvailableSubscriptionPools().getStdout());
	}
	public List<SubscriptionPool> getCurrentlyAvailableSubscriptionPools(String providingProductId, String serverUrl) throws JSONException, Exception {
		return getCurrentlyAvailableSubscriptionPools(providingProductId, currentlyRegisteredUsername, currentlyRegisteredPassword, serverUrl);	// may encounter "Insufficient permissions"; I suspect this is a consequence of the solution for Bug 994711 - Consumers can consume entitlements from other orgs
	}
	public List<SubscriptionPool> getCurrentlyAvailableSubscriptionPools(String providingProductId, String authenticator, String password, String serverUrl) throws JSONException, Exception {
		List<SubscriptionPool> subscriptionPoolsProvidingProductId = new ArrayList<SubscriptionPool>();
		
		for (SubscriptionPool subscriptionPool : getCurrentlyAvailableSubscriptionPools()) {
			if (CandlepinTasks.getPoolProvidedProductIds(authenticator, password, serverUrl, subscriptionPool.poolId).contains(providingProductId)) {
				subscriptionPoolsProvidingProductId.add(subscriptionPool);
			}
		}
		return subscriptionPoolsProvidingProductId;
	}
	
	/**
	 * @return list of objects representing the subscription-manager list --all --available
	 */
	public List<SubscriptionPool> getCurrentlyAllAvailableSubscriptionPools() {
		return SubscriptionPool.parse(listAllAvailableSubscriptionPools().getStdout());
	}
	
	/**
	 * @return list of objects representing the subscription-manager list --available --matchinstalled
	 */
	public  List<SubscriptionPool> getAvailableSubscriptionsMatchingInstalledProducts() {
		
		return SubscriptionPool.parse(list_(null, true, null, null, null, null, null, true, null, null, null, null, null, null, null).getStdout()); 
	}
	
	/**
	 * @return list of objects representing the subscription-manager list --consumed
	 */
	public List<ProductSubscription> getCurrentlyConsumedProductSubscriptions() {
		return ProductSubscription.parse(listConsumedProductSubscriptions().getStdout());
	}
	/**
	 * @return list of objects representing the subscription-manager list --avail --ondate
	 */
	public List<SubscriptionPool> getAvailableFutureSubscriptionsOndate(String onDateToTest) {
		return SubscriptionPool.parse(list_(null, true, null, null, null, onDateToTest, null, null, null, null, null, null, null, null, null).getStdout());
	}
	
	/**
	 * @return list of objects representing the subscription-manager repos --list
	 */
	public List<Repo> getCurrentlySubscribedRepos() {
		return Repo.parse(listSubscribedRepos().getStdout());
	}
	
	/**
	 * @return list of objects representing the Red Hat Repositories from /etc/yum.repos.d/redhat.repo
	 */
	public List<YumRepo> getCurrentlySubscribedYumRepos() {
		
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1008016";	// Bug 1008016 - The redhat.repo file should be refreshed after a successful subscription
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			// trigger a yum transaction so that subscription-manager yum plugin will refresh redhat.repo
			//sshCommandRunner.runCommandAndWait("killall -9 yum"); // is this needed?
			//sshCommandRunner.runCommandAndWait("yum repolist all --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
			sshCommandRunner.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND
		if (!invokeWorkaroundWhileBugIsOpen) invokeWorkaroundWhileBugIsOpen = true;
		bugId="1090206";	// Bug 1090206 - The redhat.repo file should be refreshed after a successful subscription
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			// trigger a yum transaction so that subscription-manager plugin will refresh redhat.repo
			//sshCommandRunner.runCommandAndWait("killall -9 yum"); // is this needed?
			//sshCommandRunner.runCommandAndWait("yum repolist all --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
			sshCommandRunner.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		}
		// END OF WORKAROUND
		// TODO Fix these two lines if bug 1090206 is rejected for rhel5.11 - choose the second "1.9.8-1"
		//if (isPackageVersion("subscription-manager","<","1.10.3-1")) sshCommandRunner.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// subscription-manager master commit cebde288bbe4005a82345882fcfcce742b49b039
		//if (isPackageVersion("subscription-manager","<","1.9.8-1")) sshCommandRunner.runCommandAndWait("yum -q repolist --disableplugin=rhnplugin");	// subscription-manager RHEL6.5 commit 030a7fca1d48213edb247ca035fdd9143200a41e
		
		return YumRepo.parse(sshCommandRunner.runCommandAndWait("cat "+redhatRepoFile).getStdout());
	}
	
	/**
	 * @return list of objects representing the subscription-manager list --installed
	 */
	public List<InstalledProduct> getCurrentlyInstalledProducts() {
		return InstalledProduct.parse(listInstalledProducts().getStdout());
	}
	
	@Deprecated
	public List<EntitlementCert> getCurrentEntitlementCertsUsingOpensslX509() {

		// THIS ORIGINAL IMPLEMENTATION HAS BEEN THROWING A	java.lang.StackOverflowError
		// REIMPLEMENTING THIS METHOD TO HELP BREAK THE PROBLEM DOWN INTO SMALLER PIECES - jsefler 11/23/2010
//		sshCommandRunner.runCommandAndWait("find "+entitlementCertDir+" -name '*.pem' | grep -v key.pem | xargs -I '{}' openssl x509 -in '{}' -noout -text");
//		String certificates = sshCommandRunner.getStdout();
//		return EntitlementCert.parse(certificates);

		// STACK OVERFLOW PROBLEM FIXED
//		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
//		for (File entitlementCertFile : getCurrentEntitlementCertFiles()) {
//			entitlementCerts.add(getEntitlementCertFromEntitlementCertFile(entitlementCertFile));
//		}
//		return entitlementCerts;
		
		//sshCommandRunner.runCommandAndWait("find "+entitlementCertDir+" -name '*.pem' | grep -v key.pem | xargs -I '{}' openssl x509 -in '{}' -noout -text");
		sshCommandRunner.runCommandAndWaitWithoutLogging("find "+entitlementCertDir+" -regex \".*/[0-9]+.pem\" -exec openssl x509 -in '{}' -noout -text \\; -exec echo \"    File: {}\" \\;");
		String certificates = sshCommandRunner.getStdout();
		return EntitlementCert.parseStdoutFromOpensslX509(certificates);
	}
	public List<EntitlementCert> getCurrentEntitlementCerts() {
		
		if (false) {	// effectively commented out
			// STRANGELY THIS ALGORITHM OCCASIONALY TAKES MORE THAN 30 MIN (sshCommandRunner emergencyTimeout > 18000000lMS); LET'S TRY A LESS AGRESSIVE ALGORITHM
			//	2018-02-22 15:22:57.940  FINE: ssh root@bkr-hv03-guest12.dsal.lab.eng.bos.redhat.com find /etc/pki/entitlement -regex "/.+/[0-9]+.pem" -exec rct cat-cert {} \;
			//	2018-02-22 15:52:58.081  SEVERE: Test Failed: testReposListPreservesSimultaneousEnablementOfRedhatRepos
			//	java.lang.RuntimeException: net.schmizz.sshj.connection.ConnectionException: Timeout expired
			//	        at com.redhat.qe.tools.SSHCommandRunner.run(SSHCommandRunner.java:177)
			//	        at com.redhat.qe.tools.SSHCommandRunner.runCommand(SSHCommandRunner.java:317)
			//	        at com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait(SSHCommandRunner.java:354)
			//	        at com.redhat.qe.tools.SSHCommandRunner.runCommandAndWaitWithoutLogging(SSHCommandRunner.java:337)
			//	        at rhsm.cli.tasks.SubscriptionManagerTasks.getCurrentEntitlementCerts(SubscriptionManagerTasks.java:2305)
		sshCommandRunner.runCommandAndWaitWithoutLogging("find "+entitlementCertDir+" -regex \"/.+/[0-9]+.pem\" -exec rct cat-cert {} \\;");
		String certificates = sshCommandRunner.getStdout();
		return EntitlementCert.parse(certificates);
		}
		
		List<EntitlementCert> entitlementCerts = new ArrayList<EntitlementCert>();
		for (File serialPemFile : getCurrentEntitlementCertFiles()) {
			entitlementCerts.add(getEntitlementCertFromEntitlementCertFile(serialPemFile));
		}
		return entitlementCerts;
	}
	
	/**
	 * @return - Set of unique productIds from all the product certs installed in the rhsm.productCertDir and /etc/pki/product-default/
	 */
	public Set<String> getCurrentProductIds() {
		Set<String> productIds = new HashSet<String>();
		for (ProductCert productCert : getCurrentProductCerts()) productIds.add(productCert.productId);
		return productIds;
	}
	
	/**
	 * @return - List of ProductCerts installed in the rhsm.productCertDir and /etc/pki/product-default/
	 */
	public List<ProductCert> getCurrentProductCerts() {
		/* THIS ORIGINAL IMPLEMENTATION DID NOT INCLUDE THE DEFAULT PRODUCT CERTS PROVIDED BY redhat-release
		 * See Bugs 1080007 1080012 - [RFE] Include default product certificate in redhat-release 
		return getProductCerts(productCertDir);
		*/
		
		List<ProductCert> productCerts = new ArrayList<ProductCert>();
		for (ProductCert productCert : getProductCerts(productCertDir)) productCerts.add(productCert);
		for (ProductCert productCert : getProductCerts(productCertDefaultDir)) productCerts.add(productCert);		
		return productCerts;
	}
	public List<ProductCert> getProductCertsUsingOpensslX509(String fromProductCertDir) {
		/* THIS ORIGINAL IMPLEMENTATION DID NOT INCLUDE THE FILE IN THE OBJECT
		sshCommandRunner.runCommandAndWaitWithoutLogging("find "+productCertDir+" -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text");
		String certificates = sshCommandRunner.getStdout();
		return ProductCert.parse(certificates);
		*/
		/*
		List<ProductCert> productCerts = new ArrayList<ProductCert>();
		for (File productCertFile : getCurrentProductCertFiles()) {
			productCerts.add(ProductCert.parse(sshCommandRunner, productCertFile));
		}
		return productCerts;
		*/
		sshCommandRunner.runCommandAndWaitWithoutLogging("find "+fromProductCertDir+" -name '*.pem' -exec openssl x509 -in '{}' -noout -text \\; -exec echo \"    File: {}\" \\;");
		String certificates = sshCommandRunner.getStdout();
		return ProductCert.parse(certificates);
		
	}
	public List<ProductCert> getProductCerts(String fromProductCertDir) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("find "+fromProductCertDir+" -name '*.pem' -exec rct cat-cert {} \\;");
		String certificates = sshCommandRunner.getStdout();
		return ProductCert.parse(certificates);
		
	}
	
	/**
	 * @param providingTagRegex - examples: rhel5 rhel6 rhel-5-client-workstation rhel-7-client rhel-7-server rhel-7-*
	 * @return a list of ProductCert that provide the given tag (in the case of rhel7, a regex is useful)
	 */
	public List<ProductCert> getCurrentProductCerts(String providingTagRegex) {
		List<ProductCert> prodctCertsProvidingTag = new ArrayList<ProductCert>();
		for (ProductCert productCert : getCurrentProductCerts()) {
			if (productCert.productNamespace.providedTags==null) continue;
			List<String> providedTags = Arrays.asList(productCert.productNamespace.providedTags.split("\\s*,\\s*"));
			for (String providedTag : providedTags) {
				if (providedTag.matches(providingTagRegex)) {
					prodctCertsProvidingTag.add(productCert);
					break;
				}
			}
		}
		return prodctCertsProvidingTag;
	}

	/**
	 * @return the currently installed ProductCert that provides a known base RHEL OS tag "rhel-5", "rhel-6", "rhel-7", "rhel-alt-7", etc.
	 * Also asserts that at most only one RHEL product cert is installed; returns null if not found
	 */
	public ProductCert getCurrentRhelProductCert() {
		// get the current base RHEL product cert that provides a predictable tag
		String providingTag;
		
		// consider the major rhel version tags
		//	Product:
		//		ID: 69
		//		Name: Red Hat Enterprise Linux 6 Server
		//		Version: 6.1
		//		Arch: i386
		//		Tags: rhel-6,rhel-6-server
		//		Brand Type: 
		//		Brand Name: 
		providingTag = "rhel-"+redhatReleaseX;
		
		// also consider rhel-5-client-workstation tag
		//	Product:
		//		ID: 71
		//		Name: Red Hat Enterprise Linux Workstation
		//		Version: 5.10
		//		Arch: i386
		//		Tags: rhel-5-client-workstation,rhel-5-workstation
		providingTag += "|" + "rhel-5-client-workstation";
		
		// also consider tags used for rhel 7 beta
		//	[root@ibm-p8-kvm-09-guest-08 rhel-7.0-beta]# for f in $(ls *.pem); do rct cat-cert $f | egrep -A7 'Product:'; done;
		//	Product:
		//		ID: 227
		//		Name: Red Hat Enterprise Linux 7 for IBM POWER Public Beta
		//		Version: 7.0 Beta
		//		Arch: ppc64
		//		Tags: rhel-7-ibm-power-everything
		//		Brand Type: 
		//		Brand Name: 
		//	Product:
		//		ID: 228
		//		Name: Red Hat Enterprise Linux 7 for IBM System z Public Beta
		//		Version: 7.0 Beta
		//		Arch: s390x
		//		Tags: rhel-7-ibm-system-z-everything
		//		Brand Type: 
		//		Brand Name: 
		//	Product:
		//		ID: 226
		//		Name: Red Hat Enterprise Linux 7 Public Beta
		//		Version: 7.0 Beta
		//		Arch: x86_64
		//		Tags: rhel-7-everything
		//		Brand Type: 
		//		Brand Name: 
		providingTag += "|" + "rhel-7-.*everything";
		
		// also consider tags used for rhelsa-dp Development Preview
		//	[root@ibm-p8-kvm-09-guest-08 rhelsa-dp]# for f in $(ls *.pem); do rct cat-cert $f | egrep -A7 'Product:'; done;
		//	Product:
		//		ID: 261
		//		Name: Red Hat Enterprise Linux Server for ARM Development Preview
		//		Version: Snapshot
		//		Arch: aarch64
		//		Tags: rhsa-dp-server,rhsa-dp-server-7
		//		Brand Type: 
		//		Brand Name: 
		providingTag += "|" + "rhelsa-dp-.+";
		
		// also consider tags used for rhel-alt
		//	[root@ibm-p8-kvm-09-guest-08 tmp]# git clone git://git.host.prod.eng.bos.redhat.com/rcm/rcm-metadata.git
		//	[root@ibm-p8-kvm-09-guest-08 tmp]# cd rcm-metadata/product_ids/rhel-alt-7.4/
		//	[root@ibm-p8-kvm-09-guest-08 rhel-alt-7.4]# for f in $(ls *.pem); do rct cat-cert $f | egrep -A7 'Product:'; done;
		//	Product:
		//		ID: 419
		//		Name: Red Hat Enterprise Linux for ARM 64
		//		Version: 7.4
		//		Arch: aarch64
		//		Tags: rhel-alt-7,rhel-alt-7-armv8-a
		//		Brand Type: 
		//		Brand Name: 
		//	Product:
		//		ID: 420
		//		Name: Red Hat Enterprise Linux for Power 9
		//		Version: 7.4
		//		Arch: ppc64le
		//		Tags: rhel-alt-7,rhel-alt-7-power9
		//		Brand Type: 
		//		Brand Name: 
		providingTag += "|" + "rhel-alt-"+redhatReleaseX;
		
		// also consider tags used for rhel-htb High Touch Beta
		//	[root@dell-pem610-01 tmp]# git clone git://git.host.prod.eng.bos.redhat.com/rcm/rcm-metadata.git
		//	[root@dell-pem610-01 tmp]# cd rcm-metadata/product_ids/rhel-7.5-htb/
		//	[root@dell-pem610-01 rhel-7.5-htb]# for f in $(ls *.pem); do rct cat-cert $f | egrep -A7 'Product:'; done;
		//	Product:
		//		ID: 230
		//		Name: Red Hat Enterprise Linux 7 Server High Touch Beta
		//		Version: 7.5 Beta					// Ref Bug 1538957 - product-default .pem files do not contain expected data
		//		Arch: x86_64
		//		Tags: rhel-7-htb,rhel-7-server
		//		Brand Type: 
		//		Brand Name: 
		//	Product:
		//		ID: 231
		//		Name: Red Hat Enterprise Linux 7 Workstation High Touch Beta
		//		Version: 7.5 Beta					// Ref Bug 1538957 - product-default .pem files do not contain expected data
		//		Arch: x86_64
		//		Tags: rhel-7-htb,rhel-7-workstation
		//		Brand Type: 
		//		Brand Name: 
		providingTag += "|" + "rhel-"+redhatReleaseX+"-htb";
		
		// get the product certs matching the rhel regex tag
		List<ProductCert> rhelProductCerts = getCurrentProductCerts(providingTag);
		
		
		// due to the implementation of Bug 1123029 - [RFE] Use default product certificates when they are present
		// we should now purge the product-default certs from rhelProductCerts that are being trumped
		// by a rhel product cert in /etc/pki/product, otherwise the subsequent asserts will fail
		for (ProductCert productDefaultCert : getProductCerts(productCertDefaultDir)) {
			if (ProductCert.findAllInstancesWithCaseInsensitiveMatchingFieldFromList("productId", productDefaultCert.productId, rhelProductCerts).size()>=2) {
				rhelProductCerts.remove(productDefaultCert);
			}
		}
		
		// log a warning when more than one product cert providing tag rhel-X is installed 
		if (rhelProductCerts.size()>1) log.warning("These "+rhelProductCerts.size()+" RHEL tagged '"+providingTag+"' product certs are installed: "+rhelProductCerts); 

/* TODO FIX THIS LOGIC WHICH ASSUMES providingTag IS NOT A REGEX
		// HTB Product Certs (230, 231) also provide the base rhel-7 tags and the content sets for *rhel-7-<variant>-htb-rpms repos require tag rhel-7-<variant>; hence all of the rhel7 htb products are "OS" branded products  (not the same for rhel6)
		// let's ignore it since it could have been legitimately added by installing a package from an htb repo 
		//	Product:
		//		ID: 230
		//		Name: Red Hat Enterprise Linux 7 Server High Touch Beta
		//		Version: 7.2 HTB
		//		Arch: x86_64
		//		Tags: rhel-7,rhel-7-server
		//		Brand Type: 
		//		Brand Name: 
		for (String htbProductId : Arrays.asList(new String[]{"230","231"})) {
			ProductCert htbProductCert = ProductCert.findFirstInstanceWithCaseInsensitiveMatchingFieldFromList("productId", htbProductId, rhelProductCerts);
			if (htbProductCert!=null && htbProductCert.productNamespace.providedTags.contains(providingTag)) {
				log.warning("Ignoring this installed HTB product cert prior to assertion that only one installed product provides RHEL tag '"+providingTag+"': "+htbProductCert);
				rhelProductCerts.remove(htbProductCert);
			}
		}
*/
		
		// TEMPORARY WORKAROUND
		if (rhelProductCerts.size()>1) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1506271"; // Bug 1506271 - redhat-release is providing more than 1 variant specific product cert
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("The remainder of this test is blocked by bug '"+bugId+"' because multiple base RHEL product certs are installed.");
			}
		}
		// END OF WORKAROUND
		
		// assert that only one rhel product cert is installed (after purging HTB and an untrumped product-default cert)
		Assert.assertEquals(rhelProductCerts.size(), 1, "Only one product cert is installed that provides RHEL tag '"+providingTag+"' (this assert tolerates /etc/pki/product-default/ certs that are trumped by /etc/pki/product/ certs)");
		
		// return it
		if (rhelProductCerts.isEmpty()) return null;
		return rhelProductCerts.get(0);
	}
	
	public boolean isRhelProductCertSubscribed() {
		ProductCert rhelProductCert=getCurrentRhelProductCert();
		if (rhelProductCert==null) return false;	// rhel product cert cannot be subscribed if a rhel product cert is not installed
		InstalledProduct installedRhelProduct = InstalledProduct.findFirstInstanceWithMatchingFieldFromList("productId", rhelProductCert.productId, getCurrentlyInstalledProducts());
		if (installedRhelProduct==null) Assert.fail("Could not find the installed product corresponding to the current RHEL product cert: "+rhelProductCert);
		return installedRhelProduct.status.equals("Subscribed");
	}
	
	
	
	/**
	 * Given a list of the currently installed product certs (that could include duplicate productIds), filter
	 * out the product certs from /etc/pki/product-default/ that are trumped by a duplicate productId.
	 * @param productCerts
	 * @return 
	 */
	public List<ProductCert> filterTrumpedDefaultProductCerts(List<ProductCert> productCerts) {
		List<ProductCert> filteredProductCerts = new ArrayList<ProductCert>();
		for (ProductCert productCert : productCerts) {
			if (!productCert.file.getPath().startsWith(productCertDefaultDir)) {
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", productCert.productId, filteredProductCerts) == null) {
					filteredProductCerts.add(productCert);
				}
			}
		}
		for (ProductCert productCert : productCerts) {
			if (productCert.file.getPath().startsWith(productCertDefaultDir)) {
				if (ProductCert.findFirstInstanceWithMatchingFieldFromList("productId", productCert.productId, filteredProductCerts) == null) {
					filteredProductCerts.add(productCert);
				}
			}
		}

		return filteredProductCerts;
	}
	
	/**
	 * @return a ConsumerCert object corresponding to the current identity certificate parsed from the output of: openssl x509 -noout -text -in /etc/pki/consumer/cert.pem
	 */
	public ConsumerCert getCurrentConsumerCertUsingOpensslX509() {
		if (!RemoteFileTasks.testExists(sshCommandRunner, this.consumerCertFile())) {
			log.info("Currently, there is no consumer registered.");
			return null;
		}
		sshCommandRunner.runCommandAndWaitWithoutLogging("openssl x509 -noout -text -in "+this.consumerCertFile());
		String certificate = sshCommandRunner.getStdout();
		return ConsumerCert.parseStdoutFromOpensslX509(certificate);
	}
	/**
	 * @return A ConsumerCert object corresponding to the current identity certificate parsed from the output of: rct cat-cert /etc/pki/consumer/cert.pem is returned.
	 * If no consumer cert exists, null is returned.
	 */
	public ConsumerCert getCurrentConsumerCert() {
		if (!RemoteFileTasks.testExists(sshCommandRunner, this.consumerCertFile())) {
			log.info("Currently, there is no consumer registered.");
			return null;
		}
		sshCommandRunner.runCommandAndWaitWithoutLogging("rct cat-cert "+this.consumerCertFile());
		String certificate = sshCommandRunner.getStdout();
		return ConsumerCert.parse(certificate);
	}
	
	/**
	 * @return consumerid from the Subject CN of the current /etc/pki/consumer/cert.pem identity x509 certificate
	 */
	public String getCurrentConsumerId() {
		ConsumerCert currentConsumerCert = getCurrentConsumerCert();
		if (currentConsumerCert==null) return null;
		return currentConsumerCert.consumerid;
	}
	

	public String getCurrentlyRegisteredOwnerKey() throws JSONException, Exception {
		if (this.currentlyRegisteredOrg!=null) return this.currentlyRegisteredOrg;
		if (this.currentlyRegisteredUsername==null) return null;
//		String hostname = getConfFileParameter(rhsmConfFile, "hostname");
//		String port = getConfFileParameter(rhsmConfFile, "port");
//		String prefix = getConfFileParameter(rhsmConfFile, "prefix");
		
		return (CandlepinTasks.getOwnerKeyOfConsumerId(this.currentlyRegisteredUsername, this.currentlyRegisteredPassword, candlepinUrl, getCurrentConsumerId()));
	}
	
	/**
	 * @return from the contents of the current /etc/pki/consumer/cert.pem
	 */
	public List<Org> getOrgs(String username, String password) {
//		List<String> orgs = new ArrayList<String>();
//		SSHCommandResult result = orgs(username, password, null, null, null);
//		for (String line : result.getStdout().split("\n")) {
//			orgs.add(line);
//		}
//		if (orgs.size()>0) orgs.remove(0); // exclude the first title line of output...  orgs:
//		return orgs;
		
		return Org.parse(orgs(username, password, null, null, null, null, null, null).getStdout());
	}
	
	/**
	 * @param registerResult
	 * @return from the stdout of the register command
	 */
	public String getCurrentConsumerId(SSHCommandResult registerResult) {
		
		// Example stdout:
		// ca3f9b32-61e7-44c0-94c1-ce328f7a15b0 jsefler.usersys.redhat.com
		
		// Example stdout:
		// The system with UUID 4e3675b1-450a-4066-92da-392c204ca5c7 has been unregistered
		// ca3f9b32-61e7-44c0-94c1-ce328f7a15b0 testuser1
		
		/*
		Pattern pattern = Pattern.compile("^[a-f,0-9,\\-]{36} [^ ]*$", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(registerResult.getStdout());
		Assert.assertTrue(matcher.find(),"Found the registered UUID in the register result."); 
		return matcher.group().split(" ")[0];
		*/
		
		// The example output and code above is from RHEL61 and RHEL57, it has changed in RHEL62 to:
		// The system with UUID 080ee4f9-736e-4195-88e1-8aff83250e7d has been unregistered
		// The system has been registered with id: 3bc07645-781f-48ef-b3d4-8821dae438f8 

		//Pattern pattern = Pattern.compile("^The system has been registered with id: [a-f,0-9,\\-]{36} *$", Pattern.MULTILINE/* | Pattern.DOTALL*/);	// msgid changed by bug 878634
		Pattern pattern = Pattern.compile("^The system has been registered with ID: [a-f,0-9,\\-]{36} *$", Pattern.MULTILINE/* | Pattern.DOTALL*/);
		Matcher matcher = pattern.matcher(registerResult.getStdout());
		Assert.assertTrue(matcher.find(),"Found the registered UUID in the register result."); 
		return matcher.group().split(":")[1].trim();
	}
	
	/**
	 * @param factName
	 * @return The fact value that subscription-manager lists for factName is returned.  If factName is not listed, null is returned.
	 */
	public String getFactValue(String factName) {
		
		// FIXME: SIMPLE WORKAROUND FOR NEW SERVER-SIDE COMPLIANCE CHECK ON "system.entitlements_valid" CompliantTests.factNameForSystemCompliance 3/11/2013
		// maybe the new solution should get the compliance status from the new cache file /var/lib/rhsm/cache/entitlement_status.json
		if (factName.equals("system.entitlements_valid")) {
			log.warning("The former \""+factName+"\" fact is no longer used.  Employing a WORKAROUND by getting the system compliance status directly from the candlepin server...");
			String complianceStatus = "UNKNOWN_COMPLIANCE_STATUS";
			try {
				complianceStatus = CandlepinTasks.getConsumerComplianceStatus(currentlyRegisteredUsername, currentlyRegisteredPassword, candlepinUrl, getCurrentConsumerId());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return complianceStatus;
		}
		// END OF WORKAROUND
		
		Map<String,String> factsMap = getFacts(factName);
		if (!factsMap.containsKey(factName)) {
			log.warning("Did not find fact '"+factName+"' in the facts list on system '"+hostname+"'.");
			return null;
		}
		return (factsMap.get(factName));
	}
	
	/**
	 * @return Map of the system's facts
	 */
	public Map<String,String> getFacts() {
		return getFacts(null);
	}
	/**
	 * @param grepFilter
	 * @return Map of the system's facts filtered by grepping for specific values
	 */
	public Map<String,String> getFacts(String grepFilter) {
		return getFacts(null,null,grepFilter);
	}

	/**
	 * @param localeVariable - either "LANG" or "LC_ALL" or something else.  run locale on a command line to see variables; passing null will default to LANG
	 * @param lang - "as_IN","bn_IN","de_DE", etc  See TranslationTests.supportedLangs
	 * @param grepFilter
	 * @return Map of the system's facts filtered by grepping for specific values
	 */
	public Map<String,String> getFacts(String localeVariable, String lang, String grepFilter) {
		Map<String,String> factsMap = new HashMap<String,String>();
		List<String> factNames = new ArrayList<String>();
		
		//SSHCommandResult factsList = facts_(true, false, null, null, null);
		String rhsmCommand = factsCommand(true, null, null, null, null, null);
		if (grepFilter!=null) rhsmCommand+=" | grep --text \""+grepFilter+"\"";		// add --text option to avoid "Binary file (standard input) matches"
		SSHCommandResult factsList = runCommandWithLang(localeVariable, lang, rhsmCommand);
		String factsListAsString = factsList.getStdout().trim();
		// # subscription-manager facts --list
		// cpu.architecture: x86_64
		// cpu.bogomips: 4600.03
		// cpu.core(s)_per_socket: 1
		// cpu.cpu(s): 2
		// dmi.system.uuid: a2e71856-6778-7975-772a-21750aa3eeb0
		// dmi.system.version: Not Specified
		// uname.sysname: Linux
		// uname.version: #1 SMP Mon Mar 21 10:20:35 EDT 2011
		// virt.host_type: ibm_systemz
		// ibm_systemz-zvm
		// uname.sysname: Linux
		// network.ipaddr: 10.16.66.203
		// system.entitlements_valid: invalid
		// system.name: jsefler-r63-server.usersys.redhat.com
		// system.uuid: 1c404f7f-a77b-4afa-8748-0532f05435b5
		// uname.machine: x86_64
		// dmi.slot.slotlength: Short
		// dmi.slot.type:slotbuswidth: x16
		// dmi.slot.type:slottype: PCI Express
		
		String factNameRegex="^[\\w\\.\\(\\)-:]+: ";
		Pattern pattern = Pattern.compile(factNameRegex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(factsListAsString);
		while (matcher.find()) {
			matcher.group();
			factNames.add(matcher.group());
		}
		
		int fromIndex=0;
		for (int f = 0; f < factNames.size(); f++) {
			String thisFactName = factNames.get(f);
			String thisFactValue;
			String nextFactName;
			if (f==factNames.size()-1) {
				thisFactValue = factsListAsString.substring(factsListAsString.indexOf(thisFactName,fromIndex)+thisFactName.length());	
			} else {
				nextFactName = factNames.get(f+1);
				thisFactValue = factsListAsString.substring(factsListAsString.indexOf(thisFactName,fromIndex)+thisFactName.length(), factsListAsString.indexOf(nextFactName,fromIndex));
			}
			fromIndex = factsListAsString.indexOf(thisFactName,fromIndex)+thisFactName.length();
			thisFactName = thisFactName.replaceFirst(": $", "");
			thisFactValue = thisFactValue.replaceFirst("\n$","");
			factsMap.put(thisFactName, thisFactValue);
		}
		
		return factsMap;
	}
	
	/**
	 * @param factsMap - map of key/values pairs that will get written as JSON to a facts file that will override the true facts on the system.  Note: subscription-manager facts --update may need to be called after this method to realize the override.
	 */
	public void createFactsFileWithOverridingValues (Map<String,String> factsMap) {
		createFactsFileWithOverridingValues (overrideFactsFilename, factsMap);
	}
	/**
	 * @param factsFilename - name for the facts file (e.g. sockets.facts); must end in ".facts" to be recognized by rhsm; do NOT prepend with /etc/rhsm/facts
	 * @param factsMap
	 */
	public void createFactsFileWithOverridingValues (String factsFilename, Map<String,String> factsMap) {
		
		// assemble an echo command and run it to create a facts file
		String keyvaluesString = "";
		for (String key : factsMap.keySet()) {
			if (factsMap.get(key)==null)
				keyvaluesString += String.format("\"%s\":%s, ", key, factsMap.get(key));
			else
				keyvaluesString += String.format("\"%s\":\"%s\", ", key, factsMap.get(key));
		}
		keyvaluesString = keyvaluesString.replaceFirst(", *$", "");
		String echoCommand = String.format("echo '{%s}' > %s", keyvaluesString, (factsDir+"/"+factsFilename).replaceAll("/{2,}", "/"));	// join the dir and filename and make sure there are not too many /'s
        sshCommandRunner.runCommandAndWait(echoCommand);	// create an override facts file
	}
	public void deleteFactsFileWithOverridingValues () {
		deleteFactsFileWithOverridingValues(overrideFactsFilename);	// delete the override facts file
	}
	public void deleteFactsFileWithOverridingValues (String factsFilename) {
		String deleteCommand = String.format("rm -f %s", (factsDir+"/"+factsFilename).replaceAll("/{2,}", "/"));
		sshCommandRunner.runCommandAndWait(deleteCommand);
	}
	
	public String getIPV4Address () {
		String ipv4_address = ipaddr;	// stopped working when we started using open stack instances of RHEL7
		ipv4_address = getFactValue("network.ipv4_address");	// stopped working when we started using openstack instances of RHEL6

		// when client is an openstack instance, the ipv4_address is private and we need the public address when asserting the proxy logs
		//	[root@rhel6-openstack-instance ~]# dmidecode --string system-product-name
		//	OpenStack Compute
		//	[root@rhel7-openstack-instance ~]# dmidecode --string system-product-name
		//	RHEV Hypervisor
		String dmiSystemProduct_name = getFactValue("dmi.system.product_name");
//		if (dmiSystemProduct_name!=null && !dmiSystemProduct_name.equals("Not Specified")) {	// then this is likely an openstack instance		// WRONG, fails on SGI hardware which returns dmi.system.product_name: UV2000
		if ((redhatReleaseX.equals("6") && "OpenStack Compute".equals(dmiSystemProduct_name)) ||
			(redhatReleaseX.equals("7") && "RHEV Hypervisor".equals(dmiSystemProduct_name))	) {	// then this is likely an openstack instance
			SSHCommandResult ipv4_addressResult = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "curl --stderr /dev/null http://169.254.169.254/latest/meta-data/public-ipv4", 0);		// will timeout on a non-openstack instance and then fail the exit code assert (probably with code 7)
			ipv4_address = ipv4_addressResult.getStdout().trim();
			Assert.assertMatch(ipv4_address, "\\d+\\.\\d+\\.\\d+\\.\\d+", "Validated format of ipv4 address '"+ipv4_address+"' detected from openstack curl query above.");
		}
		
		// some beaker systems (e.g. cloud-qe-05.idmqe.lab.eng.bos.redhat.com and tigger.idmqe.lab.eng.bos.redhat.com) started failing with a network.ipv4_address=127.0.0.1 which leads to test failures
		// Solution: https://github.com/martinp/ipd has been installed on auto-services.usersys.redhat.com:5555
		//	[root@jsefler-rhel7 ~]# curl auto-services.usersys.redhat.com:5555
		//	10.16.7.221
		ipv4_address = "127.0.0.1";
		if (ipv4_address.equals("127.0.0.1")) {
			log.warning("This system could not determine it's own network.ipv4_address fact.  Assuming https://github.com/martinp/ipd is installed on "+SubscriptionManagerBaseTestScript.getProperty("sm.noauthproxy.hostname","auto-services.usersys.redhat.comXX")+":5555 to detect the ipaddress of this system.");
			ipv4_address = RemoteFileTasks.runCommandAndAssert(sshCommandRunner, "curl --stderr /dev/null "+SubscriptionManagerBaseTestScript.getProperty("sm.noauthproxy.hostname","auto-services.usersys.redhat.comXX")+":5555", 0).getStdout().trim();
		}
		
		return ipv4_address;
	}
	
	
	/**
	 * @return a Map equivalent to the contents of "/var/lib/rhsm/productid.js"
	 * @throws JSONException
	 */
	public Map<String,List<String>> getProductIdToReposMap() throws JSONException {
		Map<String,List<String>> productIdToReposMap = new HashMap<String,List<String>>();
		sshCommandRunner.runCommandAndWait/*WithoutLogging*/("cat "+productIdJsonFile);
		JSONObject productIdToReposJSON = new JSONObject(sshCommandRunner.getStdout());

		Iterator<String> productIdKeysIter = productIdToReposJSON.keys();
		while (productIdKeysIter.hasNext()) {
			String productId = productIdKeysIter.next();
			List<String> repos = new ArrayList<String>();
			// two possibilities for backward compatibility...
			
			// OLD	
			//	[root@rhsm-compat-rhel58 ~]# cat /var/lib/rhsm/productid.js
			//	{
			//	  "69": "anaconda-base-201202021136.x86_64"
			//	}
			
			// NEW - after bug 859197 fix https://bugzilla.redhat.com/show_bug.cgi?id=859197#c15
			//	[root@jsefler-5 ~]# cat /var/lib/rhsm/productid.js
			//	{
			//	  "69": [
			//	    "anaconda-base-201211291318.x86_64", 
			//	    "rel-eng-latest"
			//	  ]
			//	}
			
			try { // first possibility for backward compatibility 
				repos.add(productIdToReposJSON.getString(productId));
			} catch (JSONException e) {	// org.json.JSONException: JSONObject["69"] not a string.
				// second possibility is the new way
				JSONArray reposJSON = productIdToReposJSON.getJSONArray(productId);
				for (int r = 0; r < reposJSON.length(); r++) {
					String repo = (String) reposJSON.get(r);
					repos.add(repo);
				}
			}
						
			productIdToReposMap.put(productId, repos);
		}
		return productIdToReposMap;
	}
	
	
	/**
	 * @return a map of serialNumber to SubscriptionPool pairs.  The SubscriptionPool is the source from where the serialNumber for the currentlyConsumedProductSubscriptions came from.
	 * @throws Exception 
	 */
//	public Map<Long, SubscriptionPool> getCurrentSerialMapToSubscriptionPools() {
//		sshCommandRunner.runCommandAndWait("find "+entitlementCertDir+" -name '*.pem' | xargs -I '{}' openssl x509 -in '{}' -noout -text");
//		String certificates = sshCommandRunner.getStdout();
//		return SubscriptionPool.parseCerts(certificates);
//	}
	public Map<BigInteger, SubscriptionPool> getCurrentSerialMapToSubscriptionPools(String username, String password) throws Exception  {
		
		Map<BigInteger, SubscriptionPool> serialMapToSubscriptionPools = new HashMap<BigInteger, SubscriptionPool>();
//		String hostname = getConfFileParameter(rhsmConfFile, "hostname");
//		String port = getConfFileParameter(rhsmConfFile, "port");
//		String prefix = getConfFileParameter(rhsmConfFile, "prefix");
		for (EntitlementCert entitlementCert : getCurrentEntitlementCerts()) {
			JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(username,password,candlepinUrl,entitlementCert.id);
			String poolHref = jsonEntitlement.getJSONObject("pool").getString("href");
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(username,password,candlepinUrl,poolHref));
			String subscriptionName = jsonPool.getString("productName");
			String productId = jsonPool.getString("productId");
			String poolId = jsonPool.getString("id");
			String quantity = Integer.toString(jsonPool.getInt("quantity"));	// = jsonPool.getString("quantity");
			if (jsonPool.getInt("quantity")<0)  quantity = "Unlimited";	// a pool quantity of -1 provided unlimited entitlements
			String endDate = jsonPool.getString("endDate");
			Boolean multiEntitlement = CandlepinTasks.isPoolProductMultiEntitlement(username,password, candlepinUrl, poolId);
			SubscriptionPool fromPool = new SubscriptionPool(subscriptionName,productId,poolId,quantity,null,multiEntitlement,endDate);
			serialMapToSubscriptionPools.put(entitlementCert.serialNumber, fromPool);
		}
		return serialMapToSubscriptionPools;
	}
	
	/**
	 * @param lsOptions - options used when calling ls to populate the order of the returned List (man ls for more info)
	 * <br>Possibilities:
	 * <br>"" no sort order preferred
	 * <br>"-t" sort by modification time
	 * <br>"-v" natural sort of (version) numbers within text
	 * @return List of /etc/pki/entitlement/*.pem files sorted using lsOptions (excluding a key.pem file)
	 */
	public List<File> getCurrentEntitlementCertFiles(String lsOptions) {
		List<File> files = new ArrayList<File>();
		if (entitlementCertDir==null) {log.warning("The entitlementCertDir has not yet been defined."); return files;}
		if (lsOptions==null) lsOptions = "";
		//sshCommandRunner.runCommandAndWait("find /etc/pki/entitlement/ -name '*.pem'");
		//sshCommandRunner.runCommandAndWait("ls -1 "+lsOptions+" "+entitlementCertDir+"/*.pem");
		sshCommandRunner.runCommandAndWait("ls -1 "+lsOptions+" "+entitlementCertDir+"/*.pem | grep -v key.pem");
		String lsFiles = sshCommandRunner.getStdout().trim();
		if (!lsFiles.isEmpty()) {
			for (String lsFile : Arrays.asList(lsFiles.split("\n"))) {
				
				// exclude the the key.pem file
				if (lsFile.endsWith("key.pem")) continue;
				
				// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=640338 - jsefler 10/7/2010
				if (lsFile.matches(".*\\(\\d+\\)\\.pem")) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="640338"; 
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
					if (invokeWorkaroundWhileBugIsOpen) {
						continue;
					}
				}
				// END OF WORKAROUND
				
				files.add(new File(lsFile));
			}
		}
		return files;
	}
	/**
	 * @return List of /etc/pki/entitlement/*.pem files (excluding a key.pem file)
	 */
	public List<File> getCurrentEntitlementCertFiles() {
		return getCurrentEntitlementCertFiles("-v");
	}

	

	/**
	 * @param lsOptions - options used when calling ls to populate the order of the returned List (man ls for more info)
	 * <br>Possibilities:
	 * <br>"" no sort order preferred
	 * <br>"-t" sort by modification time
	 * <br>"-v" natural sort of (version) numbers within text
	 * @return List of /etc/pki/product/*.pem files sorted using lsOptions AND the /etc/pki/product-default/*.pem files
	 */
	public List<File> getCurrentProductCertFiles(String lsOptions) {
		/* THIS ORIGINAL IMPLEMENTATION DID NOT INCLUDE THE DEFAULT PRODUCT CERTS PROVIDED BY redhat-release
		 * See Bugs 1080007 1080012 - [RFE] Include default product certificate in redhat-release 
		return getProductCertFiles(lsOptions,productCertDir);
		*/
		List<File> certFiles = new ArrayList<File>();
		for (File certFile : getProductCertFiles(lsOptions,productCertDir)) certFiles.add(certFile);
		for (File certFile : getProductCertFiles(lsOptions,productCertDefaultDir)) certFiles.add(certFile);
		return certFiles;
	}
	public List<File> getProductCertFiles(String lsOptions, String fromProductCertDir) {
		if (lsOptions==null) lsOptions = "";
		//sshCommandRunner.runCommandAndWait("find /etc/pki/product/ -name '*.pem'");
		sshCommandRunner.runCommandAndWait("ls -1 "+lsOptions+" "+fromProductCertDir+"/*.pem");
		String lsFiles = sshCommandRunner.getStdout().trim();
		List<File> files = new ArrayList<File>();
		if (!lsFiles.isEmpty()) {
			for (String lsFile : Arrays.asList(lsFiles.split("\n"))) {
				files.add(new File(lsFile));
			}
		}
		return files;
	}
	
	/**
	 * @return List of /etc/pki/product/*.pem files
	 */
	public List<File> getCurrentProductCertFiles() {
		return getCurrentProductCertFiles("-v");
	}
	public List<File> getProductCertFiles(String fromProductCertDir) {
		return getProductCertFiles("-v", fromProductCertDir);
	}
	
// replaced by getYumListOfAvailablePackagesFromRepo(...)
//	/**
//	 * @return
//	 * @author ssalevan
//	 */
//	public HashMap<String,String[]> getPackagesCorrespondingToSubscribedRepos(){
//		int min = 3;
//		sshCommandRunner.runCommandAndWait("killall -9 yum");
//		log.info("timeout of "+min+" minutes for next command");
//		sshCommandRunner.runCommandAndWait("yum list available",Long.valueOf(min*60000));
//		HashMap<String,String[]> pkgMap = new HashMap<String,String[]>();
//		
//		String[] packageLines = sshCommandRunner.getStdout().split("\\n");
//		
//		int pkglistBegin = 0;
//		
//		for(int i=0;i<packageLines.length;i++){
//			pkglistBegin++;
//			if(packageLines[i].contains("Available Packages"))
//				break;
//		}
//		
//		for(ProductSubscription sub : getCurrentlyConsumedProductSubscriptions()){
//			ArrayList<String> pkgList = new ArrayList<String>();
//			for(int i=pkglistBegin;i<packageLines.length;i++){
//				String[] splitLine = packageLines[i].split(" ");
//				String pkgName = splitLine[0];
//				String repoName = splitLine[splitLine.length - 1];
//				if(repoName.toLowerCase().contains(sub.productName.toLowerCase()))
//					pkgList.add(pkgName);
//			}
//			pkgMap.put(sub.productName, (String[])pkgList.toArray());
//		}
//		
//		return pkgMap;
//	}

	/**
	 * @param productSubscription
	 * @param username	- owner of the subscription pool (will be used in a REST api call to the candlepin server)
	 * @param password
	 * @return the SubscriptionPool from which this consumed ProductSubscription came from
	 * @throws Exception
	 */
	public SubscriptionPool getSubscriptionPoolFromProductSubscription(ProductSubscription productSubscription, String username, String password) throws Exception {
		
		// if already known, return the SubscriptionPool from which ProductSubscription came
		if (productSubscription.fromSubscriptionPool != null) return productSubscription.fromSubscriptionPool;
		
		productSubscription.fromSubscriptionPool = getCurrentSerialMapToSubscriptionPools(username, password).get(productSubscription.serialNumber);

		return productSubscription.fromSubscriptionPool;
	}
	
//DELETEME
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param subscriptionPools - usually getCurrentlyAvailableSubscriptionPools()
//	 * @return - the SubscriptionPool from subscriptionPools that has a matching field (if not found, null is returned)
//	 */
//	public SubscriptionPool findSubscriptionPoolWithMatchingFieldFromList(String fieldName, Object fieldValue, List<SubscriptionPool> subscriptionPools) {
//		
//		SubscriptionPool subscriptionPoolWithMatchingField = null;
//		for (SubscriptionPool subscriptionPool : subscriptionPools) {
//			try {
//				if (SubscriptionPool.class.getField(fieldName).get(subscriptionPool).equals(fieldValue)) {
//					subscriptionPoolWithMatchingField = subscriptionPool;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return subscriptionPoolWithMatchingField;
//	}
//	
//	
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param productSubscriptions - usually getCurrentlyConsumedProductSubscriptions()
//	 * @return - the ProductSubscription from productSubscriptions that has a matching field (if not found, null is returned)
//	 */
//	public ProductSubscription findProductSubscriptionWithMatchingFieldFromList(String fieldName, Object fieldValue, List<ProductSubscription> productSubscriptions) {
//		ProductSubscription productSubscriptionWithMatchingField = null;
//		for (ProductSubscription productSubscription : productSubscriptions) {
//			try {
//				if (ProductSubscription.class.getField(fieldName).get(productSubscription).equals(fieldValue)) {
//					productSubscriptionWithMatchingField = productSubscription;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return productSubscriptionWithMatchingField;
//	}
//	
//	
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param installedProducts - usually getCurrentProductCerts()
//	 * @return - the InstalledProduct from installedProducts that has a matching field (if not found, null is returned)
//	 */
//	public InstalledProduct findInstalledProductWithMatchingFieldFromList(String fieldName, Object fieldValue, List<InstalledProduct> installedProducts) {
//		InstalledProduct installedProductWithMatchingField = null;
//		for (InstalledProduct installedProduct : installedProducts) {
//			try {
//				if (InstalledProduct.class.getField(fieldName).get(installedProduct).equals(fieldValue)) {
//					installedProductWithMatchingField = installedProduct;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return installedProductWithMatchingField;
//	}
//	
//	
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param productCerts - usually getCurrentlyProductCerts()
//	 * @return - the ProductCert from productCerts that has a matching field (if not found, null is returned)
//	 */
//	public ProductCert findProductCertWithMatchingFieldFromList(String fieldName, Object fieldValue, List<ProductCert> productCerts) {
//		ProductCert productCertWithMatchingField = null;
//		for (ProductCert productCert : productCerts) {
//			try {
//				if (ProductCert.class.getField(fieldName).get(productCert).equals(fieldValue)) {
//					productCertWithMatchingField = productCert;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return productCertWithMatchingField;
//	}
//	
//	
//	/**
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param entitlementCerts - usually getCurrentEntitlementCerts()
//	 * @return - the EntitlementCert from entitlementCerts that has a matching field (if not found, null is returned)
//	 */
//	public EntitlementCert findEntitlementCertWithMatchingFieldFromList(String fieldName, Object fieldValue, List<EntitlementCert> entitlementCerts) {
//		EntitlementCert entitlementCertWithMatchingField = null;
//		for (EntitlementCert entitlementCert : entitlementCerts) {
//			try {
//				if (EntitlementCert.class.getField(fieldName).get(entitlementCert).equals(fieldValue)) {
//					entitlementCertWithMatchingField = entitlementCert;
//				}
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return entitlementCertWithMatchingField;
//	}
	


	
//KEEPME FOR FUTURE USAGE SOMEWHERE ELSE	
//	/**
//	 * Given a List of instances of some class (e.g. getCurrentEntitlementCerts()), this
//	 * method is useful for finding the first instance (e.g. an EntitlementCert) whose public
//	 * field by the name "fieldName" has a value of fieldValue.  If no match is found, null is returned.
//	 * @param <T>
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param dataInstances
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	public <T> T findFirstInstanceWithMatchingFieldFromList(String fieldName, Object fieldValue, List<T> dataInstances) {
//		Collection<T> dataInstancesWithMatchingFieldFromList = Collections2.filter(dataInstances, new ByValuePredicate(fieldName,fieldValue));
//		if (dataInstancesWithMatchingFieldFromList.isEmpty()) return null;
//		return (T) dataInstancesWithMatchingFieldFromList.toArray()[0];
//	}
//	
//	/**
//	 * Given a List of instances of some class (e.g. getAllAvailableSubscriptionPools()), this
//	 * method is useful for finding a subset of instances whose public field by the name "fieldName"
//	 * has a value of fieldValue.  If no match is found, an empty list is returned.
//	 * @param <T>
//	 * @param fieldName
//	 * @param fieldValue
//	 * @param dataInstances
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	public <T> List<T> findAllInstancesWithMatchingFieldFromList(String fieldName, Object fieldValue, List<T> dataInstances) {
//		Collection<T> dataInstancesWithMatchingFieldFromList = Collections2.filter(dataInstances, new ByValuePredicate(fieldName,fieldValue));
//		return (List<T>) Arrays.asList(dataInstancesWithMatchingFieldFromList.toArray());
//	}
//	
//	class ByValuePredicate implements Predicate<Object> {
//		Object value;
//		String fieldName;
//		public ByValuePredicate(String fieldName, Object value) {
//			this.value=value;
//			this.fieldName=fieldName;
//		}
//		public boolean apply(Object toTest) {
//			try {
//				return toTest.getClass().getField(fieldName).get(toTest).equals(value);
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (NoSuchFieldException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return false;
//		}
//	}

	
	/**
	 * @return a list of the currently granted EntitlementCerts that are within the warningPeriod (days) of its endDate
	 */
	public List<EntitlementCert> getCurrentEntitlementCertsWithinWarningPeriod() {
		List<EntitlementCert> entitlementCertsWithinWarningPeriod = new ArrayList<EntitlementCert>();
		Calendar now = new GregorianCalendar();	now.setTimeInMillis(System.currentTimeMillis());
		
		// assemble all of the current entitlementCerts that are within the warning period
		for (EntitlementCert entitlementCert : getCurrentEntitlementCerts()) {
			
			// find the warning period
			int warningPeriod = 0;	// assume zero
			try {warningPeriod = Integer.valueOf(entitlementCert.orderNamespace.warningPeriod);}
			catch (NumberFormatException e) {
				log.warning("The OrderNamespace's warningPeriod is non-numeric or non-existing in EntitlementCert: "+entitlementCert);
			}
			
			// subtract the warningPeriod number of days from the endDate
			entitlementCert.orderNamespace.endDate.add(Calendar.DATE, -1*warningPeriod);
			
			// check if we are now inside the warningPeriod
			if (entitlementCert.orderNamespace.endDate.before(now)) {
				entitlementCertsWithinWarningPeriod.add(entitlementCert);
			}
		}
		return entitlementCertsWithinWarningPeriod;
	}
	
	/**
	 * For the given consumed ProductSubscription, get the corresponding EntitlementCert
	 * @param productSubscription
	 * @return
	 */
	@Deprecated
	public EntitlementCert getEntitlementCertCorrespondingToProductSubscriptionUsingOpensslX509(ProductSubscription productSubscription) {
		String serialPemFile = entitlementCertDir+"/"+productSubscription.serialNumber+".pem";
		sshCommandRunner.runCommandAndWaitWithoutLogging("openssl x509 -text -noout -in "+serialPemFile+"; echo \"    File: "+serialPemFile+"\"");	// openssl x509 -text -noout -in /etc/pki/entitlement/5066044962491605926.pem; echo "    File: /etc/pki/entitlement/5066044962491605926.pem"
		String certificate = sshCommandRunner.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parseStdoutFromOpensslX509(certificate);
		Assert.assertEquals(entitlementCerts.size(), 1,"Only one EntitlementCert corresponds to ProductSubscription: "+productSubscription);
		return entitlementCerts.get(0);
	}
	/**
	 * For the given consumed ProductSubscription, get the corresponding EntitlementCert
	 * @param productSubscription
	 * @return
	 */
	public EntitlementCert getEntitlementCertCorrespondingToProductSubscription(ProductSubscription productSubscription) {
		String serialPemFile = entitlementCertDir+"/"+productSubscription.serialNumber+".pem";
		sshCommandRunner.runCommandAndWaitWithoutLogging("rct cat-cert "+serialPemFile);
		String certificate = sshCommandRunner.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parse(certificate);
		Assert.assertEquals(entitlementCerts.size(), 1,"Only one EntitlementCert corresponds to ProductSubscription: "+productSubscription);
		return entitlementCerts.get(0);
	}
	
	/**
	 * For the given ProductCert installed in /etc/pki/product, get the corresponding InstalledProduct from subscription-manager list --installed
	 * @param productCert
	 * @return instance of InstalledProduct (null if not found)
	 */
	public InstalledProduct getInstalledProductCorrespondingToProductCert(ProductCert productCert) {
		return getInstalledProductCorrespondingToProductCert(productCert,getCurrentlyInstalledProducts());
	}
	public InstalledProduct getInstalledProductCorrespondingToProductCert(ProductCert productCert, List<InstalledProduct> fromInstalledProducts) {
		for (InstalledProduct installedProduct : fromInstalledProducts) {
			
			/* IMPLEMENTATION BEFORE THE PRODUCT ID WAS INCLUDED AS A FIELD FOR InstalledProduct
			// when a the product cert is missing OIDS, "None" is rendered in the list --installed
			String name = productCert.productNamespace.name==null?"None":productCert.productNamespace.name;
			String version = productCert.productNamespace.version==null?"None":productCert.productNamespace.version;
			String arch = productCert.productNamespace.arch==null?"None":productCert.productNamespace.arch;
			
			if (installedProduct.productName.equals(name) &&
				installedProduct.version.equals(version) &&
				installedProduct.arch.equals(arch)) {
				return installedProduct;
			}
			*/
			if (productCert.productId.equals(installedProduct.productId)) return installedProduct;
		}
		return null; // not found
	}
	
	
	public EntitlementCert getEntitlementCertCorrespondingToSubscribedPool(SubscriptionPool subscribedPool) {
//		String hostname = getConfFileParameter(rhsmConfFile, "hostname");
//		String port = getConfFileParameter(rhsmConfFile, "port");
//		String prefix = getConfFileParameter(rhsmConfFile, "prefix");
		
		for (File entitlementCertFile : getCurrentEntitlementCertFiles("-t")) {
			EntitlementCert entitlementCert = getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
			try {
				JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,entitlementCert.id);
				JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,jsonEntitlement.getJSONObject("pool").getString("href")));
				if (jsonPool.getString("id").equals(subscribedPool.poolId)) {
					return entitlementCert;
				}
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		return null;	// not found
	}
	
	/**
	 * From amongst the currently installed product certs, return those that are provided for by the given pool.
	 * @param pool
	 * @return
	 * @throws JSONException
	 * @throws Exception
	 */
	public List<ProductCert> getCurrentProductCertsProvidedBySubscriptionPool(SubscriptionPool pool) throws JSONException, Exception {
		return getProductCertsProvidedBySubscriptionPool(getCurrentProductCerts(), pool);
	}
	
	/**
	 * From amongst the given product certs, return those that are provided for by the given pool.
	 * @param productCerts
	 * @param pool
	 * @return
	 * @throws JSONException
	 * @throws Exception
	 */
	public List<ProductCert> getProductCertsProvidedBySubscriptionPool(List<ProductCert> productCerts, SubscriptionPool pool) throws JSONException, Exception {
		List<ProductCert> productCertsProvidedBySubscriptionPool = new ArrayList<ProductCert>();
//		String hostname = getConfFileParameter(rhsmConfFile, "hostname");
//		String port = getConfFileParameter(rhsmConfFile, "port");
//		String prefix = getConfFileParameter(rhsmConfFile, "prefix");

		JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,"/pools/"+pool.poolId));
		JSONArray jsonProvidedProducts = (JSONArray) jsonPool.getJSONArray("providedProducts");
		for (int k = 0; k < jsonProvidedProducts.length(); k++) {
			JSONObject jsonProvidedProduct = (JSONObject) jsonProvidedProducts.get(k);
			String providedProductId = jsonProvidedProduct.getString("productId");
			
			// is this productId among the installed ProductCerts? if so, add them all to the currentProductCertsCorrespondingToSubscriptionPool
			productCertsProvidedBySubscriptionPool.addAll(ProductCert.findAllInstancesWithMatchingFieldFromList("productId", providedProductId, productCerts));
		}
		
		return productCertsProvidedBySubscriptionPool;
	}
	
	public List <EntitlementCert> getEntitlementCertsProvidingProductCert(ProductCert productCert) {
		List<EntitlementCert> correspondingEntitlementCerts = new ArrayList<EntitlementCert>();
		ProductNamespace productNamespaceMatchingProductCert = null;
		for (EntitlementCert entitlementCert : getCurrentEntitlementCerts()) {
			productNamespaceMatchingProductCert = ProductNamespace.findFirstInstanceWithMatchingFieldFromList("id", productCert.productId, entitlementCert.productNamespaces);	
			if (productNamespaceMatchingProductCert!=null) {
				correspondingEntitlementCerts.add(entitlementCert);
			}
		}
		return correspondingEntitlementCerts;
	}
	
	public EntitlementCert getEntitlementCertFromEntitlementCertFileUsingOpensslX509(File serialPemFile) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("openssl x509 -text -noout -in "+serialPemFile+"; echo \"    File: "+serialPemFile+"\"");	// openssl x509 -text -noout -in /etc/pki/entitlement/5066044962491605926.pem; echo "    File: /etc/pki/entitlement/5066044962491605926.pem"
		String certificates = sshCommandRunner.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parseStdoutFromOpensslX509(certificates);
		
		// assert that only one EntitlementCert was parsed and return it
		Assert.assertEquals(entitlementCerts.size(), 1, "Entitlement cert file '"+serialPemFile+"' parsed only one EntitlementCert.");
		return entitlementCerts.get(0);
	}
	public EntitlementCert getEntitlementCertFromEntitlementCertFile(File serialPemFile) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("rct cat-cert "+serialPemFile);
		String certificates = sshCommandRunner.getStdout();
		List<EntitlementCert> entitlementCerts = EntitlementCert.parse(certificates);
		
		// assert that only one EntitlementCert was parsed and return it
		Assert.assertEquals(entitlementCerts.size(), 1, "Entitlement cert file '"+serialPemFile+"' parsed only one EntitlementCert.");
		return entitlementCerts.get(0);
	}
	
	public BigInteger getSerialNumberFromEntitlementCertFile(File serialPemFile) {
		// example serialPemFile: /etc/pki/entitlement/196.pem
		// extract the serial number from the certFile name
		// Note: probably a more robust way to do this is to get it from inside the file
		//Integer serialNumber = Integer.valueOf(serialPemFile.getName().split("\\.")[0]);
		String serialNumber = serialPemFile.getName().split("\\.")[0];
		//return Long.parseLong(serialNumber, 10);
		//return new Long(serialNumber);
		return new BigInteger(serialNumber);
	}
	
	public File getEntitlementCertFileFromEntitlementCert(EntitlementCert entitlementCert) {
		File serialPemFile = new File(entitlementCertDir+File.separator+entitlementCert.serialNumber+".pem");
		return serialPemFile;
	}
	
	public File getEntitlementCertKeyFileFromEntitlementCert(EntitlementCert entitlementCert) {
		File serialKeyPemFile = new File(entitlementCertDir+File.separator+entitlementCert.serialNumber+"-key.pem");
		return serialKeyPemFile;
	}
	
	public File getEntitlementCertKeyFileCorrespondingToEntitlementCertFile(File entitlementCertFile) {
		// 239223656620993791.pem  => 239223656620993791-key.pem
		String serialKeyPem = entitlementCertFile.getPath().replaceAll("(\\.\\w*)$", "-key$1");
		// 239223656620993791      => 239223656620993791-key
		if (!serialKeyPem.contains("-key.")) serialKeyPem += "-key";

		return new File(serialKeyPem);
	}
	
	@Deprecated
	public ProductCert getProductCertFromProductCertFileUsingOpensslX509(File productPemFile) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("openssl x509 -noout -text -in "+productPemFile.getPath());
		String certificates = sshCommandRunner.getStdout();
		List<ProductCert> productCerts = ProductCert.parse(certificates);
		
		// assert that only one ProductCert was parsed and return it
		Assert.assertEquals(productCerts.size(), 1, "Product cert file '"+productPemFile+"' parsed only one ProductCert.");
		return productCerts.get(0);
	}
	public ProductCert getProductCertFromProductCertFile(File productPemFile) {
		sshCommandRunner.runCommandAndWaitWithoutLogging("rct cat-cert "+productPemFile.getPath());
		String certificates = sshCommandRunner.getStdout();
		List<ProductCert> productCerts = ProductCert.parse(certificates);
		
		// assert that only one ProductCert was parsed and return it
		Assert.assertEquals(productCerts.size(), 1, "Product cert file '"+productPemFile+"' parsed only one ProductCert.");
		return productCerts.get(0);
	}
	
	public CertStatistics getCertStatisticsFromCertFile(File certPemFile) {
		sshCommandRunner.runCommandAndWait/*WithoutLogging*/("rct stat-cert "+certPemFile.getPath());
		String rawStatistics = sshCommandRunner.getStdout();
		CertStatistics certStatistics = CertStatistics.parse(rawStatistics);

		return certStatistics;
	}
	
	
	
	// register module tasks ************************************************************

	/**
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String registerCommand(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, List<String> activationkeys, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;											command += " register";
		if (username!=null)														command += " --username="+String.format(username.contains(" ")? "\"%s\"":"%s", username);	// quote username containing spaces
		if (password!=null)														command += " --password="+String.format(password.contains("(")||password.contains(")")? "\"%s\"":"%s", password);	// quote password containing ()
		if (org!=null)															command += " --org="+org;
		if (environment!=null)													command += " --environment="+environment;
		if (type!=null)															command += " --type="+type;
		if (name!=null)															command += " --name="+String.format(name.contains("\"")? "'%s'":"\"%s\"", name./*escape backslashes*/replace("\\", "\\\\")./*escape backticks*/replace("`", "\\`"));
		if (consumerid!=null)													command += " --consumerid="+consumerid;
		if (autosubscribe!=null && autosubscribe)								command += " --autosubscribe";
		if (servicelevel!=null)													command += " --servicelevel="+String.format(servicelevel.contains(" ")||servicelevel.isEmpty()? "\"%s\"":"%s", servicelevel);	// quote a value containing spaces or is empty
		if (release!=null)														command += " --release="+release;
		if (activationkeys!=null)	for (String activationkey : activationkeys)	command += " --activationkey="+String.format(activationkey.contains(" ")? "\"%s\"":"%s", activationkey);	// quote activationkey containing spaces
		if (serverurl!=null)													command += " --serverurl="+serverurl;
		if (insecure!=null && insecure)											command += " --insecure";
		if (baseurl!=null)														command += " --baseurl="+baseurl;
		if (force!=null && force)												command += " --force";
		if (proxy!=null)														command += " --proxy="+proxy;
		if (proxyuser!=null)													command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)												command += " --proxypassword="+proxypassword;
		if (noproxy!=null)														command += " --noproxy="+noproxy;
		
		return command;
	}
	public String registerCommand(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, String activationkey, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> activationkeys = activationkey==null?null:Arrays.asList(new String[]{activationkey});
		return registerCommand(username, password, org, environment, type, name, consumerid, autosubscribe, servicelevel, release, activationkeys, serverurl, insecure, baseurl, force, autoheal, proxy, proxyuser, proxypassword, noproxy);
	}
	

	/**
	 * register WITHOUT asserting results.
	 * @param insecure TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult register_(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, List<String> activationkeys, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		String command = registerCommand(username, password, org, environment, type, name, consumerid, autosubscribe, servicelevel, release, activationkeys, serverurl, insecure, baseurl, force, autoheal, proxy, proxyuser, proxypassword, noproxy);	
		/* this workaround should no longer be needed after rhel70 fixes by ckozak similar to bugs 1052297 1048325 commit 6fe57f8e6c3c35ac7761b9fa5ac7a6014d69ce20 that employs #!/usr/bin/python -S    sys.setdefaultencoding('utf-8')    import site
		if (!SubscriptionManagerCLITestScript.isStringSimpleASCII(command)) command = "PYTHONIOENCODING=ascii "+command;	// workaround for bug 800323 after master commit 1bc25596afaf294cd217200c605737a43112a378 to avoid stderr: 'ascii' codec can't decode byte 0xe5 in position 13: ordinal not in range(128)
		*/
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		
		// copy the current consumer cert and key to allRegisteredConsumerCertsDir for recollection by deleteAllRegisteredConsumerEntitlementsAfterSuite()
		ConsumerCert consumerCert = getCurrentConsumerCert();
		if (consumerCert!=null) {
			sshCommandRunner.runCommandAndWait/*WithoutLogging*/("cp -f "+consumerCert.file+" "+consumerCert.file.getPath().replace(consumerCertDir, SubscriptionManagerCLITestScript.allRegisteredConsumerCertsDir).replaceFirst("cert.pem$", consumerCert.consumerid+"_cert.pem"));
			sshCommandRunner.runCommandAndWait/*WithoutLogging*/("cp -f "+consumerCert.file.getPath().replace("cert", "key")+" "+consumerCert.file.getPath().replace(consumerCertDir, SubscriptionManagerCLITestScript.allRegisteredConsumerCertsDir).replaceFirst("cert.pem$", consumerCert.consumerid+"_key.pem"));
		}
		
		// reset this.currentlyRegistered values
		if (isPackageVersion("subscription-manager",">=","1.19.11-1")) {	// commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide feedback for force register
			if (sshCommandResult.getStdout().contains("All local data removed")) {
				//	Unregistering from: jsefler-candlepin.usersys.redhat.com:8443/candlepin
				//	The system with UUID 2618dc5e-8407-4933-a945-da2a3fa1ccd6 has been unregistered
				//	All local data removed
				//	Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
				//	The system has been registered with ID: 2618dc5e-8407-4933-a945-da2a3fa1ccd6 
				this.currentlyRegisteredUsername = null;
				this.currentlyRegisteredPassword = null;
				this.currentlyRegisteredOrg = null;
				this.currentlyRegisteredType = null;
			}
		}
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(0))) {
			// The system has been registered with id: 660faf39-a8f2-4311-acf2-5c1bb3c141ef
			this.currentlyRegisteredUsername = username;
			this.currentlyRegisteredPassword = password;
			this.currentlyRegisteredOrg = org;
			this.currentlyRegisteredType = type;
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && autosubscribe!=null && autosubscribe) {
			// Bug 689608 - Return error code when auto subscribing doesn't find any subscriptions (reported by dgregor)
			this.currentlyRegisteredUsername = username;
			this.currentlyRegisteredPassword = password;
			this.currentlyRegisteredOrg = org;
			this.currentlyRegisteredType = type;	
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && consumerid!=null) {
			this.currentlyRegisteredUsername = username;
			this.currentlyRegisteredPassword = password;
			this.currentlyRegisteredOrg = org;
			this.currentlyRegisteredType = type;	
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && activationkeys!=null && !activationkeys.isEmpty()) {
			this.currentlyRegisteredUsername = username;
			this.currentlyRegisteredPassword = password;
			this.currentlyRegisteredOrg = org;
			this.currentlyRegisteredType = type;	
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && (force==null || !force) && isPackageVersion("subscription-manager","<","1.13.8-1")) {	// pre commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			// stdout= This system is already registered. Use --force to override
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(64)/*EX_USAGE*/) && (force==null || !force) && isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
			// stdout= This system is already registered. Use --force to override
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && (environment!=null)) {
			// Server does not support environments.
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(1)) && sshCommandResult.getStderr().trim().endsWith("cannot register with any organizations.") && isPackageVersion("subscription-manager",">=","1.14.7-1")) {	// post commit 270f2a3e5f7d55b69a6f98c160d38362961b3059
			// stderr= testuser3 cannot register with any organizations.
		} else
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(64)) ||	// EX_USAGE			Error: Activation keys cannot be used with --auto-attach.
			sshCommandResult.getExitCode().equals(Integer.valueOf(69)) ||	// EX_UNAVAILABLE	Unable to reach the server
			sshCommandResult.getExitCode().equals(Integer.valueOf(70)) ||	// EX_SOFTWARE		Error parsing serverurl:
			sshCommandResult.getExitCode().equals(Integer.valueOf(78)) ||	// EX_CONFIG		Error: CA certificate for subscription service has not been installed.
			sshCommandResult.getExitCode().equals(Integer.valueOf(255))){	// EX-CODES			http://docs.thefoundry.co.uk/nuke/63/pythonreference/os-module.html
			// Traceback/Error
			/* The current system consumer actually remains unchanged when these errors are encountered by the register attempt, stop nullifying the currentlyRegistered consumer.  TODO not confident that this is always true
			this.currentlyRegisteredUsername = null;
			this.currentlyRegisteredPassword = null;
			this.currentlyRegisteredOrg = null;
			this.currentlyRegisteredType = null;
			*/
		} else {
			Assert.fail("Encountered an unexpected exitCode '"+sshCommandResult.getExitCode()+"' during a attempt to register.");
		}
		
		// set autoheal attribute of the consumer
		if (autoheal!=null && sshCommandResult.getExitCode().equals(Integer.valueOf(0))) {
			try {
				// Note: NullPointerException will likely occur when activationKeys are used because null will likely be passed for username/password
				CandlepinTasks.setAutohealForConsumer(currentlyRegisteredUsername, currentlyRegisteredPassword, candlepinUrl, getCurrentConsumerId(sshCommandResult), autoheal);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} 
		}
		
		return sshCommandResult;
	}
	
	/**
	 * register WITHOUT asserting results.
	 * @param insecure TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult register_(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, String activationkey, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		List<String> activationkeys = activationkey==null?null:Arrays.asList(new String[]{activationkey});

		return register_(username, password, org, environment, type, name, consumerid, autosubscribe, servicelevel, release, activationkeys, serverurl, insecure, baseurl, force, autoheal, proxy, proxyuser, proxypassword, noproxy);
	}
	
	

	
	public SSHCommandResult register(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, List<String> activationkeys, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		boolean alreadyRegistered = this.currentlyRegisteredUsername==null? false:true;
		String currentConsumerId = alreadyRegistered? getCurrentConsumerId():null;
		if (alreadyRegistered && currentConsumerId==null) {
			log.warning("AUTOMATION ERROR: Detected a bad state of the SubscriptionManagerTasks.  Conflicting variables: alreadyRegistered='"+alreadyRegistered+"' && currentConsumerId='"+currentConsumerId+"'.  Instruct the automator of this testware to troubleshoot the cause.  Proceeding with the assumption that currentConsumerId is correct.");
			alreadyRegistered=false;
			this.currentlyRegisteredType=null;
			this.currentlyRegisteredUsername=null;
			this.currentlyRegisteredPassword=null;
			this.currentlyRegisteredOrg=null;
		}
		String msg;
		SSHCommandResult sshCommandResult = register_(username, password, org, environment, type, name, consumerid, autosubscribe, servicelevel, release, activationkeys, serverurl, insecure, baseurl, force, autoheal, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results when already registered
		if (alreadyRegistered) {
			if (force==null || !force) { // already registered while attempting to register without using force
				if (isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(64), "The exit code from the register command indicates we are already registered.");
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "This system is already registered. Use --force to override");	
					Assert.assertEquals(sshCommandResult.getStdout().trim(), "");	
				} else {
					Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(1), "The exit code from the register command indicates we are already registered.");
					Assert.assertEquals(sshCommandResult.getStdout().trim(), "This system is already registered. Use --force to override");	
					Assert.assertEquals(sshCommandResult.getStderr().trim(), "");	
				}
				return sshCommandResult;
			}
			if (force!=null && force) { // already registered while attempting to register with force
				//	201705041325:40.743 - FINE: ssh root@jsefler-rhel7.usersys.redhat.com subscription-manager register --username=testuser1 --password=REDACTED --org=admin --force
				//	201705041325:42.367 - FINE: Stdout: 
				//	Unregistering from: jsefler-candlepin.usersys.redhat.com:8443/candlepin
				//	The system with UUID 855e98bb-e44e-4acc-92b8-e772e3364411 has been unregistered
				//	All local data removed
				//	Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
				//	The system has been registered with ID: ba4d8ea1-8be2-40d0-b77e-f0c9ed2c0ce8 
				//	201705041325:42.374 - FINE: Stderr: 
				//	201705041325:42.376 - FINE: ExitCode: 0
				if (isPackageVersion("subscription-manager",">=","1.19.11-1")) {	// commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide feedback for force register
					String unregisterFromServer = getConfFileParameter(rhsmConfFile, "server", "hostname")+":"+ getConfFileParameter(rhsmConfFile, "server", "port")+ getConfFileParameter(rhsmConfFile, "server", "prefix");
					String unregisterFromMsg = String.format("Unregistering from: %s\nThe system with UUID %s has been unregistered\nAll local data removed",unregisterFromServer, currentConsumerId);	// introduced by commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide feedback for force register 
					if (isRhnSystemRegistered()) {
						String unregisterFromMsgWithInteroperabilityWarning = msg_InteroperabilityWarning+"\n"+unregisterFromMsg;
						Assert.assertTrue(sshCommandResult.getStdout().trim().startsWith(unregisterFromMsgWithInteroperabilityWarning), "Stdout from an attempt to register with force while already being registered (to both RHN Classic and RHSM) starts with message '"+unregisterFromMsgWithInteroperabilityWarning+"'.");
					} else {
						Assert.assertTrue(sshCommandResult.getStdout().trim().startsWith(unregisterFromMsg), "Stdout from an attempt to register with force while already being registered starts with message '"+unregisterFromMsg+"'.");
					}
				}
			}
		}

		// assert results for a successful registration exit code
		if (isPackageVersion("subscription-manager", "<", "1.10")) {
			if (autosubscribe==null || !autosubscribe)	// https://bugzilla.redhat.com/show_bug.cgi?id=689608
				Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the register command indicates a success.");
		} else {
			
			// Bug 689608 - Return error code when auto subscribing doesn't find any subscriptions
			if ((autosubscribe!=null && Boolean.valueOf(autosubscribe)) ||
				(consumerid!=null && isPackageVersion("subscription-manager", "<", "1.13.4-1")) ||		// Bug 1145835 - subscription-manager register --consumerid throws return code 1 even though it was successful	// commit 6f82c03f05804dcc28eb66d8126453f73c250488
				(activationkeys!=null && !activationkeys.isEmpty())) {
				// skip exit code assertion
			} else {
				Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the register command indicates a success.");
			}
		}
		
		// assert the heading for the current status of the installed products (applicable to register with autosubscribe|consumerid|activationkey)
		msg = "Installed Product Current Status:";
		if (isPackageVersion("subscription-manager", "<", "1.10")) {
			if (autosubscribe==null || !autosubscribe)
				Assert.assertFalse(sshCommandResult.getStdout().contains(msg),
						"register without autosubscribe should not show a list of the \""+msg+"\".");
			else
				Assert.assertTrue(sshCommandResult.getStdout().contains(msg),
						"register with autosubscribe should show a list of the \""+msg+"\".");
		} else {
			if (getCurrentProductCertFiles().isEmpty()) msg = "No products installed.";	// bug 962545
			if (isPackageVersion("subscription-manager", ">=", "1.13.4-1")) {	// bug 1122001 bug 1145835	// commit 6f82c03f05804dcc28eb66d8126453f73c250488
				// applicable to register with autosubscribe|activationkey - is no longer applicable to consumerid after bugs 1122001 bug 1145835
				if ((autosubscribe!=null && Boolean.valueOf(autosubscribe)) || (activationkeys!=null && !activationkeys.isEmpty())) {
					// TODO subscription-manager commit afd16e96d89ff38f74c60fd23613b67e27da17c5 for 1132981: Fixed exit code when registering system with no products installed is causing the following assert to fail.  Need resolution for comment https://bugzilla.redhat.com/show_bug.cgi?id=1132981#c10 
					Assert.assertTrue(sshCommandResult.getStdout().contains(msg),
							"register with autosubscribe|activationkey should list \""+msg+"\".");
				} else {
					Assert.assertTrue(!sshCommandResult.getStdout().contains(msg),
							"register without autosubscribe|activationkey should NOT list \""+msg+"\".");
				}
			} else {
				// applicable to register with autosubscribe|consumerid|activationkey
				if ((autosubscribe!=null && Boolean.valueOf(autosubscribe)) || (consumerid!=null) || (activationkeys!=null && !activationkeys.isEmpty())) {
					Assert.assertTrue(sshCommandResult.getStdout().contains(msg),
							"register with autosubscribe|consumerid|activationkey should list \""+msg+"\".");
				} else {
					Assert.assertTrue(!sshCommandResult.getStdout().contains(msg),
							"register without autosubscribe|consumerid|activationkey should NOT list \""+msg+"\".");
				}
			}
		}
		
		// assert stdout results for a successful registration id
		if (type==ConsumerType.person) name = username;		// https://bugzilla.redhat.com/show_bug.cgi?id=661130
		if (name==null) name = this.hostname;				// https://bugzilla.redhat.com/show_bug.cgi?id=669395
		//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "[a-f,0-9,\\-]{36} "+name);	// applicable to RHEL61 and RHEL57. changed in RHEL62 due to feedback from mmccune https://engineering.redhat.com/trac/kalpana/wiki/SubscriptionManagerReview - jsefler 6/28/2011
		//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "The system has been registered with id: [a-f,0-9,\\-]{36}");
		//msg = "The system has been registered with id: [a-f,0-9,\\-]{36}";	// msgid changed by bug 878634
		msg = "The system has been registered with ID: [a-f,0-9,\\-]{36}";
		Assert.assertTrue(Pattern.compile(".*"+msg+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from register contains a match to expected msg: "+msg);
		
		// assert that register with consumerId returns the expected uuid
		if (consumerid!=null) {
			//Assert.assertEquals(sshCommandResult.getStdout().trim(), consumerId+" "+username, "register to an exiting consumer was a success");
			//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "^"+consumerId, "register to an exiting consumer was a success");	// removed name from assert to account for https://bugzilla.redhat.com/show_bug.cgi?id=669395	// applicable to RHEL61 and RHEL57.
			//Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "The system has been registered with id: "+consumerid, "register to an exiting consumer was a success");	// removed name from assert to account for https://bugzilla.redhat.com/show_bug.cgi?id=669395
			//msg = "The system has been registered with id: "+consumerid;	// msgid changed by bug 878634
			msg = "The system has been registered with ID: "+consumerid;
			Assert.assertTrue(sshCommandResult.getStdout().contains(msg), "Stdout from register contains a match to expected msg: "+msg);
		}
		
		// assert certificate files are installed into /etc/pki/consumer
		Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner,this.consumerKeyFile()), "Consumer key file '"+this.consumerKeyFile()+"' must exist after register.");
		Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner,this.consumerCertFile()), "Consumer cert file '"+this.consumerCertFile()+"' must exist after register.");
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=639417 - jsefler 10/1/2010
		boolean invokeWorkaroundWhileBugIsOpen = false;	// Status: 	CLOSED CURRENTRELEASE
		String bugId="639417"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			restart_rhsmcertd(Integer.valueOf(getConfFileParameter(rhsmConfFile, "certFrequency")), null, null);
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR Bug 797243 - manual changes to redhat.repo are too sticky
		invokeWorkaroundWhileBugIsOpen = true;
		bugId="797243"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Triggering a yum transaction to insure the redhat.repo file is wiped clean");
			sshCommandRunner.runCommandAndWait("yum repolist --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		}
		// END OF WORKAROUND
		
		return sshCommandResult; // from the register command
	}
	
	public SSHCommandResult register(String username, String password, String org, String environment, ConsumerType type, String name, String consumerid, Boolean autosubscribe, String servicelevel, String release, String activationkey, String serverurl, Boolean insecure, String baseurl, Boolean force, Boolean autoheal, String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> activationkeys = activationkey==null?null:Arrays.asList(new String[]{activationkey});

		return register(username, password, org, environment, type, name, consumerid, autosubscribe, servicelevel, release, activationkeys, serverurl, insecure, baseurl, force, autoheal, proxy, proxyuser, proxypassword, noproxy);
	}
	
	public SSHCommandResult register(String username, String password, String org) {
		return register(username, password, org, null, null, null, null, null, null, null, (String)null, null, null, null, null, null, null, null, null, null);
	}
	
	/**
	 * Useful workaround to create a candlepin consumer on a RHEL client when the version of
	 * candlepin is greater that 2.1.1-1 due to a change in candlepin behavior blocking the
	 * creation of a "candlepin" consumer from subscription-manager.
	 * Reference candlepin commit 739b51a0d196d9d3153320961af693a24c0b826f
	 * 1455361: Disallow candlepin consumers to be registered via Subscription Manager
	 * @param username
	 * @param password
	 * @param org
	 * @param serverUrl
	 * @param consumerName
	 * @throws Exception
	 */
	public void registerCandlepinConsumer(String username, String password, String org, String serverUrl, String consumerName) throws Exception {
	    JSONObject jsonCandlepinConsumer = CandlepinTasks.createCandlepinConsumerUsingRESTfulAPI(username, password, serverUrl, org, consumerName);
		String cert = jsonCandlepinConsumer.getJSONObject("idCert").getString("cert");
		String key = jsonCandlepinConsumer.getJSONObject("idCert").getString("key");
		
		// write the cert and key to a local file
	    File certFile = new File("test-output/cert.pem");
    	Writer certWriter = new BufferedWriter(new FileWriter(certFile));
		certWriter.write(cert);
		certWriter.close();
	    File keyFile = new File("test-output/key.pem");
    	Writer keyWriter = new BufferedWriter(new FileWriter(keyFile));
		keyWriter.write(key);
		keyWriter.close();
		
		// transfer the cert and key file to the client
		RemoteFileTasks.putFile(sshCommandRunner, certFile.getPath(), consumerCertFile(), "0640");
		RemoteFileTasks.putFile(sshCommandRunner, keyFile.getPath(), consumerKeyFile(), "0640");
		
		//return jsonCandlepinConsumer.getString("uuid");
	}
	
	
	// reregister module tasks ************************************************************

//	/**
//	 * reregister without asserting results
//	 */
//	public SSHCommandResult reregister_(String username, String password, String consumerid) {
//
//		// assemble the command
//		String					command  = "subscription-manager-cli reregister";	
//		if (username!=null)		command += " --username="+username;
//		if (password!=null)		command += " --password="+password;
//		if (consumerid!=null)	command += " --consumerid="+consumerid;
//		
//		// register without asserting results
//		return sshCommandRunner.runCommandAndWait(command);
//	}
//	
//	/**
//	 * "subscription-manager-cli reregister"
//	 */
//	public SSHCommandResult reregister(String username, String password, String consumerid) {
//		
//		// get the current ConsumerCert
//		ConsumerCert consumerCertBefore = null;
//		if (consumerid==null) {	//if (RemoteFileTasks.testFileExists(sshCommandRunner, consumerCertFile)==1) {
//			consumerCertBefore = getCurrentConsumerCert();
//			log.fine("Consumer cert before reregistering: "+consumerCertBefore);
//		}
//		
//		SSHCommandResult sshCommandResult = reregister_(username,password,consumerid);
//		
//		// assert results for a successful reregistration
//		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the reregister command indicates a success.");
//		String regex = "[a-f,0-9,\\-]{36}";			// consumerid regex
//		if (consumerid!=null) regex=consumerid;		// consumerid
//		if (username!=null) regex+=" "+username;	// username
//		Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), regex);
//
//		// get the new ConsumerCert
//		ConsumerCert consumerCertAfter = getCurrentConsumerCert();
//		log.fine("Consumer cert after reregistering: "+consumerCertAfter);
//		
//		// assert the new ConsumerCert from a successful reregistration
//		if (consumerCertBefore!=null) {
//			Assert.assertEquals(consumerCertAfter.consumerid, consumerCertBefore.consumerid,
//				"The consumer cert userid remains unchanged after reregistering.");
//			Assert.assertEquals(consumerCertAfter.username, consumerCertBefore.username,
//				"The consumer cert username remains unchanged after reregistering.");
//			Assert.assertTrue(consumerCertAfter.validityNotBefore.after(consumerCertBefore.validityNotBefore),
//				"The consumer cert validityNotBefore date has been changed to a newer date after reregistering.");
//		}
//		
//		// assert the new consumer certificate contains the reregistered credentials...
//		if (consumerid!=null) {
//			Assert.assertEquals(consumerCertAfter.consumerid, consumerid,
//				"The reregistered consumer cert belongs to the requested consumerid.");
//		}
//		if (username!=null) {
//			Assert.assertEquals(consumerCertAfter.username, username,
//				"The reregistered consumer cert belongs to the authenticated username.");
//		}
//		
//		return sshCommandResult; // from the reregister command
//	}
	
	public SSHCommandResult reregisterToExistingConsumer(String username, String password, String consumerId) {
		log.warning("The subscription-manager-cli reregister module has been eliminated and replaced by register --consumerid (10/4/2010 git hash b3c728183c7259841100eeacb7754c727dc523cd)...");
		//RemoteFileTasks.runCommandAndWait(sshCommandRunner, "rm -f "+consumerCertFile, TestRecords.action());
		//removeAllCerts(true, true);
		clean();
		return register(username,password,null,null,null,null,consumerId, null, null, null, new ArrayList<String>(), null, null, null, null, null, null, null, null, null);
	}
	
	
	
	// clean module tasks ************************************************************

	/**
	 * clean without asserting results
	 */
	public SSHCommandResult clean_() {
		
		// assemble the command
		String command = this.command;	command += " clean";
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		
		// keep the currentlyRegistered class variables accurate
		// when clean is successful (exitCode==0), the system is no longer registered
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(0))) {
			this.currentlyRegisteredUsername = null;
			this.currentlyRegisteredPassword = null;
			this.currentlyRegisteredOrg = null;
			this.currentlyRegisteredType = null;
		}
		
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager-cli clean"
	 */
	public SSHCommandResult clean() {
		
		SSHCommandResult sshCommandResult = clean_();
		
		// assert results for a successful clean
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the clean command indicates a success.");
		Assert.assertEquals(sshCommandResult.getStdout().trim(), "All local data removed");
		
		// assert that the consumer cert directory is gone
		if (isPackageVersion("subscription-manager", "<", "1.10.14-1")) {
			Assert.assertFalse(RemoteFileTasks.testExists(sshCommandRunner,consumerCertDir), consumerCertDir+" does NOT exist after clean.");	// this was valid before Bug 1026501 - deleting consumer will move splice identity cert
		} else {
			Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner,consumerCertDir), consumerCertDir+" still exists after clean.");	// this is now valid after Bug 1026501 - deleting consumer will move splice identity cert
		}
		Assert.assertFalse(RemoteFileTasks.testExists(sshCommandRunner,consumerCertDir+"/cert.pem"), consumerCertDir+"/cert.pem"+" does NOT exist after clean.");
		Assert.assertFalse(RemoteFileTasks.testExists(sshCommandRunner,consumerCertDir+"/key.pem"), consumerCertDir+"/key.pem"+" does NOT exist after clean.");
		
		// assert that the entitlement cert directory is gone
		//Assert.assertFalse(RemoteFileTasks.testFileExists(sshCommandRunner,entitlementCertDir)==1, entitlementCertDir+" does NOT exist after clean.");
		// assert that the entitlement cert directory is gone (or is empty)
		if (RemoteFileTasks.testExists(sshCommandRunner,entitlementCertDir)) {
			Assert.assertEquals(sshCommandRunner.runCommandAndWait("ls "+entitlementCertDir).getStdout(), "", "The entitlement cert directory is empty after running clean.");
		}

		return sshCommandResult; // from the clean command
	}
	
	
	
	// import module tasks ************************************************************

	/**
	 * import WITHOUT asserting results
	 * @param certificates - list of paths to certificate files to be imported
	 * @return
	 */
	public SSHCommandResult importCertificate_(List<String> certificates) {
		
		// assemble the command
		String command = this.command;									command += " import";
		if (certificates!=null)	for (String certificate : certificates)	command += " --certificate="+certificate;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * import WITHOUT asserting results.
	 */
	public SSHCommandResult importCertificate_(String certificate) {
		
		List<String> certificates = certificate==null?null:Arrays.asList(new String[]{certificate});
		
		return importCertificate_(certificates);
	}
	
	/**
	 * import with assertions that the results are a success"
	 * @param certificates - list of paths to certificates file to be imported
	 * @return
	 */
	public SSHCommandResult importCertificate(List<String> certificates/*, String proxy, String proxyuser, String proxypassword*/) {
		
		SSHCommandResult sshCommandResult = importCertificate_(certificates/*, proxy, proxyuser, proxypassword*/);
		
		// assert results for a successful import
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the import command indicates a success.");
		
		// Successfully imported certificate {0}
		for (String certificate: certificates) {
			String successMsg = "Successfully imported certificate "+(new File(certificate)).getName();
			Assert.assertTrue(sshCommandResult.getStdout().contains(successMsg),"The stdout from the import command contains expected message: "+successMsg);		
		}
	
		// {0} is not a valid certificate file. Please use a valid certificate.
		
		// assert that the entitlement certificate has been extracted to /etc/pki/entitlement
		//Assert.assertTrue(RemoteFileTasks.testFileExists(sshCommandRunner,consumerCertDir)==1, consumerCertDir+" does NOT exist after clean.");

		// assert that the key has been extracted to /etc/pki/entitlement
		//Assert.assertTrue(RemoteFileTasks.testFileExists(sshCommandRunner,consumerCertDir)==1, consumerCertDir+" does NOT exist after clean.");

		return sshCommandResult; // from the import command
	}
	
	/**
	 * import including assertion that the result is a success
	 * @param certificate - path to certificate file to be imported (file should contain both the entitlement and key)
	 * @return
	 */
	public SSHCommandResult importCertificate(String certificate) {
		
		List<String> certificates = certificate==null?null:Arrays.asList(new String[]{certificate});
		
		return importCertificate(certificates);
	}
	
	// refresh module tasks ************************************************************

	/**
	 * refresh without asserting results
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult refresh_(String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " refresh";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager-cli refresh"
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult refresh(String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = refresh_(proxy, proxyuser, proxypassword, noproxy);
		String refreshStdoutMsg = sshCommandResult.getStdout().trim();
		
		refreshStdoutMsg = workaroundForBug906550(refreshStdoutMsg);
		
		// assert results for a successful clean
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the refresh command indicates a success.");
		Assert.assertEquals(refreshStdoutMsg, "All local data refreshed");
		
		return sshCommandResult; // from the refresh command
	}
	
	
	
	// identity module tasks ************************************************************

	/**
	 * identity without asserting results
	 * @param username
	 * @param password
	 * @param regenerate
	 * @param force
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult identity_(String username, String password, Boolean regenerate, Boolean force, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;		command += " identity";
		if (username!=null)					command += " --username="+username;
		if (password!=null)					command += " --password="+password;
		if (regenerate!=null && regenerate)	command += " --regenerate";
		if (force!=null && force)			command += " --force";
		if (proxy!=null)					command += " --proxy="+proxy;
		if (proxyuser!=null)				command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)			command += " --proxypassword="+proxypassword;
		if (noproxy!=null)					command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager-cli identity"
	 * @param username
	 * @param password
	 * @param regenerate
	 * @param force
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult identity(String username, String password, Boolean regenerate, Boolean force, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = identity_(username, password, regenerate, force, proxy, proxyuser, proxypassword, noproxy);
		regenerate = regenerate==null? false:regenerate;	// the non-null default value for regenerate is false

		// assert results for a successful identify
		/* Example sshCommandResult.getStdout():
		 * Current identity is: 8f4dd91a-2c41-4045-a937-e3c8554a5701 name: testuser1
		 */
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the identity command indicates a success.");
		
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=719109 - jsefler 7/05/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="719109"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			// skip the assertion of user feedback in stdout
			return sshCommandResult;
		}
		// END OF WORKAROUND
		
		
		if (regenerate) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Identity certificate has been regenerated.");
		}
// DELETEME
// DON'T ASSERT THIS HERE.  WILL BE ASSERTED IN OTHER TESTS. IT'S POSSIBLE THAT THIS IS NOT EXPECTED EVEN THOUGH EXIT CODE IS 0 (e.g when registered classically)
//		else {
//			Assert.assertContainsMatch(sshCommandResult.getStdout().trim(), "Current identity is: [a-f,0-9,\\-]{36}");
//		}
		
		return sshCommandResult; // from the identity command
	}
	
	
	// orgs module tasks ************************************************************

	/**
	 * orgs without asserting results
	 * @param username
	 * @param password
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult orgs_(String username, String password, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " orgs";
		if (username!=null)				command += " --username="+username;
		if (password!=null)				command += " --password="+password;
		if (serverurl!=null)			command += " --serverurl="+serverurl;
		if (insecure!=null && insecure)	command += " --insecure";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager orgs"
	 * @param username
	 * @param password
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult orgs(String username, String password, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = orgs_(username, password, serverurl, insecure, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		/*
		[root@jsefler-r63-server ~]# subscription-manager orgs --username testuser1 --password password
		+-------------------------------------------+
		          testuser1 Organizations
		+-------------------------------------------+

		OrgName: 	Admin Owner              
		OrgKey: 	admin                    

		OrgName: 	Snow White               
		OrgKey: 	snowwhite                
		*/

		// assert the banner
		String bannerRegex = "\\+-+\\+\\n\\s*"+username+" Organizations\\s*\\n\\+-+\\+";
		Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from orgs contains the expected banner regex: "+bannerRegex);

		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the orgs command indicates a success.");

		return sshCommandResult; // from the orgs command
	}
	
	
	// service-level module tasks ************************************************************

	/**
	 * service_level without asserting results
	 * @param show
	 * @param list
	 * @param set
	 * @param unset TODO
	 * @param username
	 * @param password
	 * @param org
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult service_level_(Boolean show, Boolean list, String set, Boolean unset, String username, String password, String org, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " service-level";
		if (show!=null && show)			command += " --show";
		if (list!=null && list)			command += " --list";
		if (set!=null)					command += " --set="+String.format("\"%s\"", set);
		if (unset!=null && unset)		command += " --unset";
		if (username!=null)				command += " --username="+username;
		if (password!=null)				command += " --password="+password;
		if (org!=null)					command += " --org="+org;
		if (serverurl!=null)			command += " --serverurl="+serverurl;
		if (insecure!=null && insecure)	command += " --insecure";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager service-level"
	 * @param show
	 * @param list
	 * @param set
	 * @param unset TODO
	 * @param username
	 * @param password
	 * @param org
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult service_level(Boolean show, Boolean list, String set, Boolean unset, String username, String password, String org, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = service_level_(show, list, set, unset, username, password, org, serverurl, insecure, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		/*
		[root@jsefler-r63-server ~]# subscription-manager service-level --show --list
		Current service level: 
		+-------------------------------------------+
          			Available Service Levels
		+-------------------------------------------+
		Standard
		None
		Premium
		*/
		
				
		if (Boolean.valueOf(System.getProperty("sm.server.old","false"))) {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "ERROR: The service-level command is not supported by the server.");
			throw new SkipException(sshCommandResult.getStderr().trim());
 		}
 		 			
		// assert the banner
		String bannerRegex = "\\+-+\\+\\n\\s*Available Service Levels\\s*\\n\\+-+\\+";
		if (list!=null && list) {	// when explicitly asked to list
			Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from service-level (with option --list) contains the expected banner regex: "+bannerRegex);
		} else {
			Assert.assertTrue(!Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from service-level (without option --list) does not contain the banner regex: "+bannerRegex);	
		}
		
		// assert the "Current service level: "
		String serviceLevelMsg = "Current service level: ";
		String serviceLevelNotSetMsg = "Service level preference not set";	// Bug 825286 - subscription-manager service-level --show
		if ((show!=null && show) // when explicitly asked to show
			|| ((show==null || !show) && (list==null || !list) && (set==null) && (unset==null || !unset)) ){	// or when no options are explicity asked, then the default behavior is --show
			if (!sshCommandResult.getStdout().contains(serviceLevelNotSetMsg)) {
				Assert.assertTrue(sshCommandResult.getStdout().contains(serviceLevelMsg),"Stdout from service-level (with option --show) contains the expected feedback: "+serviceLevelMsg);
			} else {
				Assert.assertTrue(!sshCommandResult.getStdout().contains(serviceLevelMsg),"Stdout from service-level (without option --show) does not contain feedback: "+serviceLevelNotSetMsg);
			}
		}
		
		// assert the "Service level set to: "
		String serviceLevelSetMsg = "Service level set to: ";
		if (set!=null && !set.isEmpty()) {
			Assert.assertTrue(sshCommandResult.getStdout().contains(serviceLevelSetMsg+set),"Stdout from service-level (with option --set) contains the expected feedback: "+serviceLevelSetMsg+set);
		} else {
			// TEMPORARY WORKAROUND FOR BUG
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {String bugId="835050"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping service-level unset feedback message while this bug is open.");
			} else
			// END OF WORKAROUND
			Assert.assertTrue(!sshCommandResult.getStdout().contains(serviceLevelSetMsg),"Stdout from service-level (without option --set) does not contain feedback: "+serviceLevelSetMsg);
		}
		
		// assert the "Service level preference has been unset"
		String serviceLevelUnsetMsg = "Service level preference has been unset";
		if ((unset!=null && unset) || (set!=null && set.isEmpty())) {
			Assert.assertTrue(sshCommandResult.getStdout().contains(serviceLevelUnsetMsg),"Stdout from service-level (with option --unset) contains the expected feedback: "+serviceLevelUnsetMsg);
		} else {
			Assert.assertTrue(!sshCommandResult.getStdout().contains(serviceLevelUnsetMsg),"Stdout from service-level (without option --unset) does not contain the feedback: "+serviceLevelUnsetMsg);
		}
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the service-level command indicates a success.");
		
		return sshCommandResult; // from the service-level command
	}
	
	
	// release module tasks ************************************************************

	/**
	 * SSHCommand subscription-manager release [parameters] without asserting any results
	 * @param show
	 * @param list
	 * @param set
	 * @param unset
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return SSHCommandResult stdout, stderr, exitCode
	 */
	public SSHCommandResult release_(Boolean show, Boolean list, String set, Boolean unset, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " release";
		if (show!=null && show)			command += " --show";
		if (list!=null && list)			command += " --list";
		if (set!=null)					command += " --set="+(set.equals("")?"\"\"":set);	// quote an empty string
		if (unset!=null && unset)		command += " --unset";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * SSHCommand subscription-manager release [parameters]
	 * @param show
	 * @param list
	 * @param set
	 * @param unset
	 * @param proxy
	 * @param proxyuser
	 * @param proxypassword
	 * @param noproxy TODO
	 * @return SSHCommandResult stdout, stderr, exitCode
	 */
	public SSHCommandResult release(Boolean show, Boolean list, String set, Boolean unset, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = release_(show, list, set, unset, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		if (list!=null)
		if (Boolean.valueOf(System.getProperty("sm.server.old","false"))) {
			Assert.assertEquals(sshCommandResult.getStderr().trim(), "ERROR: The 'release' command is not supported by the server.");
			throw new SkipException(sshCommandResult.getStderr().trim());
 		}
		
		if (set!=null) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Release set to: %s", set).trim(),"Stdout from release --set with a value.");
		}
		if (unset!=null) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Release preference has been unset","Stdout from release --unset.");
		}
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the release command indicates a success.");
		
		return sshCommandResult; // from the release command
	}
	
	
	// auto-heal module tasks ************************************************************

	/**
	 * SSHCommand subscription-manager auto-heal [parameters] without asserting any results
	 * @param noproxy TODO
	 * @return SSHCommandResult stdout, stderr, exitCode
	 */
	public SSHCommandResult autoheal_(Boolean show, Boolean enable, Boolean disable, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " auto-attach";		// command += " autoheal"; changed by Bug 976867
		if (show!=null && show)			command += " --show";
		if (enable!=null && enable)		command += " --enable";
		if (disable!=null && disable)	command += " --disable";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * SSHCommand subscription-manager auto-attach [parameters]
	 * @param noproxy TODO
	 * @return SSHCommandResult stdout, stderr, exitCode
	 */
	public SSHCommandResult autoheal(Boolean show, Boolean enable, Boolean disable, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = autoheal_(show, enable, disable, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		
		if (enable!=null) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Auto-attach preference: enabled").trim(),"Stdout from auto-attach --enable");
		}
		if (disable!=null) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(), String.format("Auto-attach preference: disabled").trim(),"Stdout from auto-attach --disable");
		}
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the auto-attach command indicates a success.");
		
		return sshCommandResult; // from the auto-attach command
	}
	
	
	// config module tasks ************************************************************
	
	/**
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String configCommand(Boolean list, Boolean remove, Boolean set, List<String[]> listOfSectionNameValues) {
		
		// assemble the command
		String command = this.command;				command += " config";
		if (list!=null && list)						command += " --list";
		if (listOfSectionNameValues!=null) for (String[] section_name_value : listOfSectionNameValues) {
			// double quote the value when necessary
			if (section_name_value.length>2 && section_name_value[2].equals("")) section_name_value[2] = "\"\"";	// double quote blank values
			if (section_name_value.length>2 && section_name_value[2].contains(" ")) section_name_value[2] = "\""+section_name_value[2]+"\"";	// double quote value containing spaces (probably never used)

			if (remove!=null && remove)				command += String.format(" --remove=%s.%s", section_name_value[0],section_name_value[1]);  // expected format section.name
			if (set!=null && set)					command += String.format(" --%s.%s=%s", section_name_value[0],section_name_value[1],section_name_value[2]);  // expected format section.name=value
		}
		
		return command;
	}
	public String configCommand(Boolean list, Boolean remove, Boolean set, String[] section_name_value) {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		if (section_name_value!=null) listOfSectionNameValues.add(section_name_value);
		return configCommand(list, remove, set, listOfSectionNameValues);
	}
	
	/**
	 * config without asserting results
	 */
	public SSHCommandResult config_(Boolean list, Boolean remove, Boolean set, List<String[]> listOfSectionNameValues) {
		
		// assemble the command
		String command = configCommand(list, remove, set, listOfSectionNameValues);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * config without asserting results
	 */
	public SSHCommandResult config_(Boolean list, Boolean remove, Boolean set, String[] section_name_value) {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		if (section_name_value!=null) listOfSectionNameValues.add(section_name_value);
		return config_(list, remove, set, listOfSectionNameValues);
	}
	
	/**
	 * "subscription-manager config"
	 */
	public SSHCommandResult config(Boolean list, Boolean remove, Boolean set, List<String[]> listOfSectionNameValues) {
		
		// store what is currently configured to assist during assertion of remove stdout
		String rhsmConfFileContents = sshCommandRunner.runCommandAndWaitWithoutLogging("egrep -v  \"^\\s*(#|$)\" "+rhsmConfFile).getStdout();
		
		SSHCommandResult sshCommandResult = config_(list, remove, set, listOfSectionNameValues);
		
		// assert results...
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the config command indicates a success.");

		/*
		[root@jsefler-onprem-62server ~]# subscription-manager config --list
		[server]
		   ca_cert_dir = [/etc/rhsm/ca/]
		   hostname = jsefler-onprem-62candlepin.usersys.redhat.com
		   insecure = [0]
		   port = [8443]
		   prefix = [/candlepin]
		   proxy_hostname = []
		   proxy_password = []
		   proxy_port = []
		   proxy_user = []
		   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		   ssl_verify_depth = [3]

		[rhsm]
		   baseurl = https://cdn.redhat.com
		   ca_cert_dir = [/etc/rhsm/ca/]
		   certfrequency = 2400
		   consumercertdir = /etc/pki/consumer
		   entitlementcertdir = /etc/pki/entitlement
		   hostname = [localhost]
		   insecure = [0]
		   port = [8443]
		   prefix = [/candlepin]
		   productcertdir = /etc/pki/product
		   proxy_hostname = []
		   proxy_password = []
		   proxy_port = BAR
		   proxy_user = []
		   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		   ssl_verify_depth = [3]

		[rhsmcertd]
		   ca_cert_dir = [/etc/rhsm/ca/]
		   certfrequency = 240
		   hostname = [localhost]
		   insecure = [0]
		   port = [8443]
		   prefix = [/candlepin]
		   proxy_hostname = []
		   proxy_password = []
		   proxy_port = []
		   proxy_user = []
		   repo_ca_cert = [/etc/rhsm/ca/redhat-uep.pem]
		   ssl_verify_depth = [3]

		[] - Default value in use

		[root@jsefler-onprem-62server ~]# echo $?
		0
		[root@jsefler-onprem-62server ~]# subscription-manager config --remove=rhsmcertd.certfrequency
		You have removed the value for section rhsmcertd and name certfrequency.
		[root@jsefler-onprem-62server ~]# echo $?
		0
		[root@jsefler-onprem-62server ~]# subscription-manager config --rhsmcertd.certfrequency=240
		[root@jsefler-onprem-62server ~]# echo $?
		0
		[root@jsefler-onprem-62server ~]# 
		 */
		
		// assert remove stdout indicates a success
		if (remove!=null && remove) {
			for (String[] section_name_value : listOfSectionNameValues) {
				String section	= section_name_value[0];
				String name		= section_name_value[1];
				//String value	= section_name_value[2];
				
				//	[root@jsefler-6 ~]# subscription-manager config --remove=server.repo_ca_cert
				//	You have removed the value for section server and name repo_ca_cert.
				//	The default value for repo_ca_cert will now be used.
				//OR
				//	[root@jsefler-7 ~]# subscription-manager config --remove=server.repo_ca_cert
				//	Section server and name repo_ca_cert cannot be removed.
				String value = getSectionParameterFromConfigFileContents(section, name, rhsmConfFileContents);
				// if (value!=null) {
				if (value!=null || Integer.valueOf(redhatReleaseX)<7) {  // added OR check after Bug 927350 was CLOSED NOTABUG
					Assert.assertTrue(sshCommandResult.getStdout().contains(String.format("You have removed the value for section %s and name %s.",section,name.toLowerCase())), "The stdout indicates the removal of config parameter name '"+name+"' from section '"+section+"'.");
					Assert.assertEquals(sshCommandResult.getStdout().contains(String.format("The default value for %s will now be used.",name.toLowerCase())), defaultConfFileParameterNames(section,true).contains(name), "The stdout indicates the default value for '"+name+"' will now be used after having removed it from section '"+section+"'.");
				} else {
					Assert.assertTrue(sshCommandResult.getStdout().contains(String.format("Section %s and name %s cannot be removed.",section,name.toLowerCase())), "The stdout indicates that config parameter name '"+name+"' from section '"+section+"' cannot be removed since it is not set.");
				}
			}
		}

		
		return sshCommandResult; // from the orgs command
	}
	
	public SSHCommandResult config(Boolean list, Boolean remove, Boolean set, String[] section_name_value) {
		List<String[]> listOfSectionNameValues = new ArrayList<String[]>();
		if (section_name_value!=null) listOfSectionNameValues.add(section_name_value);
		return config(list, remove, set, listOfSectionNameValues);
	}
	
	@Deprecated
	public List<String> defaultConfFileParameterNames(Boolean toLowerCase) {
		if (toLowerCase==null) toLowerCase=false;	// return the defaultConfFileParameterNames in lowerCase
		
		// hard-coded list of parameter called DEFAULTS in /usr/lib/python2.6/site-packages/rhsm/config.py
		// this list of hard-coded parameter names have a hard-coded value (not listed here) that will be used
		// after a user calls subscription-manager --remove section.name otherwise the remove will set the value to ""
		List<String> defaultNames = new ArrayList<String>();

		// BEFORE FIX FOR BUG 807721
		// initialize defaultNames (will appear in all config sections and have a default value)
		//	DEFAULTS = {
		//	        'hostname': 'localhost',
		//	        'prefix': '/candlepin',
		//	        'port': '8443',
		//	        'ca_cert_dir': '/etc/rhsm/ca/',
		//	        'repo_ca_cert': '/etc/rhsm/ca/redhat-uep.pem',
		//	        'ssl_verify_depth': '3',
		//	        'proxy_hostname': '',
		//	        'proxy_port': '',
		//	        'proxy_user': '',
		//	        'proxy_password': '',
		//	        'insecure': '0'
		//	        }
		/*
		defaultNames.add("hostname");
		defaultNames.add("prefix");
		defaultNames.add("port");
		defaultNames.add("ca_cert_dir");
		defaultNames.add("repo_ca_cert");
		defaultNames.add("ssl_verify_depth");
		defaultNames.add("proxy_hostname");
		defaultNames.add("proxy_port");
		defaultNames.add("proxy_user");
		defaultNames.add("proxy_password");
		defaultNames.add("insecure");
		*/
		
		// AFTER FIX FOR BUG 807721
		//# Defaults are applied to each section in the config file.
		//DEFAULTS = {
		//                'hostname': 'localhost',
		//                'prefix': '/candlepin',
		//                'port': '8443',
		//                'ca_cert_dir': '/etc/rhsm/ca/',
		//                'repo_ca_cert': '/etc/rhsm/ca/redhat-uep.pem',
		//                'ssl_verify_depth': '3',
		//                'proxy_hostname': '',
		//                'proxy_port': '',
		//                'proxy_user': '',
		//                'proxy_password': '',
		//                'insecure': '0',
		//                'baseurl': 'https://cdn.redhat.com',
		//                'manage_repos': '1',
		//                'productCertDir': '/etc/pki/product',
		//                'entitlementCertDir': '/etc/pki/entitlement',
		//                'consumerCertDir': '/etc/pki/consumer',
		//                'certFrequency': '240',
		//                'healFrequency': '1440',
		//            }
		defaultNames.add("hostname");
		defaultNames.add("prefix");
		defaultNames.add("port");
		defaultNames.add("ca_cert_dir");
		defaultNames.add("repo_ca_cert");
		defaultNames.add("ssl_verify_depth");
		defaultNames.add("proxy_hostname");
		defaultNames.add("proxy_port");
		defaultNames.add("proxy_user");
		defaultNames.add("proxy_password");
		defaultNames.add("insecure");
		defaultNames.add("baseurl");
		defaultNames.add("manage_repos");
		defaultNames.add("productCertDir");
		defaultNames.add("entitlementCertDir");
		defaultNames.add("consumerCertDir");
		defaultNames.add(/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval");
		defaultNames.add(/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval");
		defaultNames.add("report_package_profile");
		defaultNames.add("pluginDir");
		defaultNames.add("pluginConfDir");
		
		// lowercase all of the defaultNames when requested
		if (toLowerCase) for (String defaultName : defaultNames) {
			defaultNames.set(defaultNames.indexOf(defaultName), defaultName.toLowerCase());
		}
		
		return defaultNames;
	}
	
	public List<String> defaultConfFileParameterNames(String section, Boolean toLowerCase) {
		if (toLowerCase==null) toLowerCase=false;	// return the defaultConfFileParameterNames in lowerCase
		
		// hard-coded list of parameter called DEFAULTS in: 
		//    /usr/lib/python2.6/site-packages/rhsm/config.py
		//    /usr/lib64/python2.4/site-packages/rhsm/config.py
		// this list of hard-coded parameter names have a hard-coded value (not listed here) that will be used
		// after a user calls subscription-manager --remove section.name otherwise the remove will set the value to ""
		List<String> defaultNames = new ArrayList<String>();

		// BEFORE FIX FOR BUG 807721
		// initialize defaultNames (will appear in all config sections and have a default value)
		//	DEFAULTS = {
		//	        'hostname': 'localhost',
		//	        'prefix': '/candlepin',
		//	        'port': '8443',
		//	        'ca_cert_dir': '/etc/rhsm/ca/',
		//	        'repo_ca_cert': '/etc/rhsm/ca/redhat-uep.pem',
		//	        'ssl_verify_depth': '3',
		//	        'proxy_hostname': '',
		//	        'proxy_port': '',
		//	        'proxy_user': '',
		//	        'proxy_password': '',
		//	        'insecure': '0'
		//	        }
		/*
		defaultNames.add("hostname");
		defaultNames.add("prefix");
		defaultNames.add("port");
		defaultNames.add("ca_cert_dir");
		defaultNames.add("repo_ca_cert");
		defaultNames.add("ssl_verify_depth");
		defaultNames.add("proxy_hostname");
		defaultNames.add("proxy_port");
		defaultNames.add("proxy_user");
		defaultNames.add("proxy_password");
		defaultNames.add("insecure");
		*/
		
		// AFTER FIX FOR BUG 807721
		//# Defaults are applied to each section in the config file.
		//DEFAULTS = {
		//                'hostname': 'localhost',
		//                'prefix': '/candlepin',
		//                'port': '8443',
		//                'ca_cert_dir': '/etc/rhsm/ca/',
		//                'repo_ca_cert': '/etc/rhsm/ca/redhat-uep.pem',
		//                'ssl_verify_depth': '3',
		//                'proxy_hostname': '',
		//                'proxy_port': '',
		//                'proxy_user': '',
		//                'proxy_password': '',
		//                'insecure': '0',
		//                'baseurl': 'https://cdn.redhat.com',
		//                'manage_repos': '1',
		//                'productCertDir': '/etc/pki/product',
		//                'entitlementCertDir': '/etc/pki/entitlement',
		//                'consumerCertDir': '/etc/pki/consumer',
		//                'certFrequency': '240',
		//                'healFrequency': '1440',
		//            }
		
		// AFTER FIX FOR BUG 988476
		if (section.equalsIgnoreCase("server")) {
			defaultNames.add("hostname");
			defaultNames.add("prefix");
			defaultNames.add("port");
			defaultNames.add("insecure");
			defaultNames.add("ssl_verify_depth");
			defaultNames.add("proxy_hostname");
			defaultNames.add("proxy_port");
			defaultNames.add("proxy_user");
			defaultNames.add("proxy_password");
			if (isPackageVersion("python-rhsm",">=","1.19.4-1")) defaultNames.add("no_proxy");	// subscription-manager/python-rhsm commit bd8b0538d7b0be7ee1e666ad5a66df80962c67d9 1420533: Add no_proxy option to API, config, UI
			if (isPackageVersion("python-rhsm",">=","1.17.3-1")) defaultNames.add("server_timeout");	// python-rhsm commit 5780140650a59d45a03372a0390f92fd7c3301eb Allow users to set socket timeout.	// Bug 1346417 - Allow users to set socket timeout.

		}
		if (section.equalsIgnoreCase("rhsm")) {
			defaultNames.add("baseurl");
			defaultNames.add("ca_cert_dir");
			defaultNames.add("repo_ca_cert");
			defaultNames.add("productCertDir");
			defaultNames.add("entitlementCertDir");
			defaultNames.add("consumerCertDir");
			defaultNames.add("manage_repos");
			defaultNames.add("report_package_profile");
			defaultNames.add("pluginDir");
			defaultNames.add("pluginConfDir");
			if (isPackageVersion("subscription-manager",">=","1.10.7-1")) defaultNames.add("full_refresh_on_yum");	// was added as part of RFE Bug 803746
			if (isPackageVersion("subscription-manager",">=","1.20.2-1")) defaultNames.add("auto_enable_yum_plugins");	// commit 29a9a1db08a2ee920c43891daafdf858082e5d8b	// Bug 1319927 - [RFE] subscription-manager should automatically enable yum plugins
			if (isPackageVersion("subscription-manager",">=","1.20.2-1"/*TODO change to 1.20.3-1*/)) defaultNames.add("inotify");	// commit db7f92dd2a29071eddd3b8de5beedb0fe46352f9	// Bug 1477958: Use inotify for checking changes of consumer certs
		}
		if (section.equalsIgnoreCase("rhsmcertd")) {
			defaultNames.add(/*"certFrequency" CHANGED BY BUG 882459 TO*/"certCheckInterval");
			defaultNames.add(/*"healFrequency" CHANGED BY BUG 882459 TO*/"autoAttachInterval");
			if (isPackageVersion("subscription-manager",">=","1.19.8-1")) defaultNames.add("splay");	// commit e9f8421285fc6541166065a8b55ee89b9a425246 1435013: Add splay option to rhsmcertd, randomize over interval
		}
		if (section.equalsIgnoreCase("logging")) {
			defaultNames.add("default_log_level");
		}


		
		// lowercase all of the defaultNames when requested
		if (toLowerCase) for (String defaultName : defaultNames) {
			defaultNames.set(defaultNames.indexOf(defaultName), defaultName.toLowerCase());
		}
		
		return defaultNames;
	}
	
	// environments module tasks ************************************************************

	/**
	 * environments without asserting results
	 * @param username
	 * @param password
	 * @param org
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult environments_(String username, String password, String org, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " environments";
		if (username!=null)				command += " --username="+username;
		if (password!=null)				command += " --password="+password;
		if (org!=null)					command += " --org="+org;
		if (serverurl!=null)			command += " --serverurl="+serverurl;
		if (insecure!=null && insecure)	command += " --insecure";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager environments"
	 * @param username
	 * @param password
	 * @param org
	 * @param serverurl TODO
	 * @param insecure TODO
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult environments(String username, String password, String org, String serverurl, Boolean insecure, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = environments_(username, password, org, serverurl, insecure, proxy, proxyuser, proxypassword, noproxy);
		
		// TODO assert results...
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the environments command indicates a success.");
		
		return sshCommandResult; // from the environments command
	}
	
	
	// unregister module tasks ************************************************************

	/**
	 * unregister without asserting results
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unregister_(String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " unregister";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		workaroundForBug844455();
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		
		// reset this.currentlyRegistered values
		if (sshCommandResult.getExitCode().equals(Integer.valueOf(0))) {			// success
			this.currentlyRegisteredUsername = null;
			this.currentlyRegisteredPassword = null;
			this.currentlyRegisteredOrg = null;
			this.currentlyRegisteredType = null;
		} else if (sshCommandResult.getExitCode().equals(Integer.valueOf(1))) {		// already unregistered
			// as insurance, reset these (they should already be null)
			this.currentlyRegisteredUsername = null;
			this.currentlyRegisteredPassword = null;
			this.currentlyRegisteredOrg = null;
			this.currentlyRegisteredType = null;
		} else if (sshCommandResult.getExitCode().equals(Integer.valueOf(255))) {	// failure
		}
		
		// return the results
		return sshCommandResult;
	}
	
	/**
	 * "subscription-manager-cli unregister"
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unregister(String proxy, String proxyuser, String proxypassword, String noproxy) {
		SSHCommandResult sshCommandResult = unregister_(proxy, proxyuser, proxypassword, noproxy);
		
		// assert results for a successful de-registration
		if (sshCommandResult.getExitCode()==0) {
			String unregisterSuccessMsg = "System has been unregistered.";
			String unregisterFromMsg = "Unregistering from: "/*hostname:port/prefix*/;	// introduced by commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide feedback for force register 
			
			// TEMPORARY WORKAROUND FOR BUG
			boolean invokeWorkaroundWhileBugIsOpen = false;	// Status: 	VERIFIED
			try {String bugId="878657"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) unregisterSuccessMsg = "System has been un-registered.";
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			invokeWorkaroundWhileBugIsOpen = false;	// Status: 	CLOSED ERRATA
			try {String bugId="800121"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("If 'NoneType' object message was thrown to stdout during unregister, we will ignore it while this bug is open.");
				Assert.assertTrue(sshCommandResult.getStdout().trim().contains(unregisterSuccessMsg), "The unregister command was a success.");
			} else
			// END OF WORKAROUND
			if (isPackageVersion("subscription-manager",">=","1.19.11-1")) {	// commit 217c3863448478d06c5008694e327e048cc54f54 Bug 1443101: Provide feedback for force register
				Assert.assertTrue(sshCommandResult.getStdout().trim().startsWith(unregisterFromMsg), "Stdout from an attempt to unregister starts with message '"+unregisterFromMsg+"'.");
				Assert.assertTrue(sshCommandResult.getStdout().trim().endsWith(unregisterSuccessMsg), "Stdout from an attempt to unregister ends with successful message '"+unregisterSuccessMsg+"'.");
			} else {
				Assert.assertTrue(sshCommandResult.getStdout().trim().equals(unregisterSuccessMsg), "Stdout from an attempt to unregister equals successful message '"+unregisterSuccessMsg+"'.");
			}
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "Exit code from an attempt to unregister");
		} else if (sshCommandResult.getExitCode()==1) {
			if (isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// commit df95529a5edd0be456b3528b74344be283c4d258 bug 1119688
				Assert.assertTrue(sshCommandResult.getStderr().startsWith("This system is currently not registered."), "The unregister command was not necessary.  Stderr indicates it was already unregistered.");
			} else {
				Assert.assertTrue(sshCommandResult.getStdout().startsWith("This system is currently not registered."), "The unregister command was not necessary.  Stdout indicates it was already unregistered.");
			}
		} else {
			Assert.fail("Unexpected exit code '"+sshCommandResult.getExitCode()+"' was returned when attempting to unregister.");		
		}
		
		// assert that the consumer cert and key have been removed
		Assert.assertTrue(!RemoteFileTasks.testExists(sshCommandRunner,this.consumerKeyFile()), "Consumer key file '"+this.consumerKeyFile()+"' does NOT exist after unregister.");
		Assert.assertTrue(!RemoteFileTasks.testExists(sshCommandRunner,this.consumerCertFile()), "Consumer cert file '"+this.consumerCertFile()+" does NOT exist after unregister.");
		
		// assert that all of the entitlement certs have been removed
		Assert.assertTrue(getCurrentEntitlementCertFiles().size()==0, "All of the entitlement certificates have been removed after unregister.");
		
		// assert the entitlementCertDir is removed
		// CLOSED WONTFIX Bug 852685 - Folder "/etc/pki/entitlement/" cannot be removed after unregistering with subscription-manager via CLI
		// Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, entitlementCertDir),0,"Entitlement Cert directory '"+entitlementCertDir+"' should not exist after unregister.");
		
		return sshCommandResult; // from the unregister command
	}
	
	
	
	// list module tasks ************************************************************
	
	public String listCommand(Boolean all, Boolean available, Boolean consumed, Boolean installed, String servicelevel, String ondate, String after, Boolean matchInstalled, Boolean noOverlap, String matches, Boolean poolOnly, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;				command += " list";	
		if (all!=null && all)						command += " --all";
		if (available!=null && available)			command += " --available";
		if (consumed!=null && consumed)				command += " --consumed";
		if (installed!=null && installed)			command += " --installed";
		if (ondate!=null)							command += " --ondate="+ondate;
		if (after!=null)							command += " --after="+after;
		if (servicelevel!=null)						command += " --servicelevel="+String.format(servicelevel.contains(" ")||servicelevel.isEmpty()?"\"%s\"":"%s", servicelevel);	// quote a value containing spaces or is empty
		if (matchInstalled!=null && matchInstalled)	command += " --match-installed";
		if (noOverlap!=null && noOverlap)			command += " --no-overlap";
		if (matches!=null)							command += " --matches="+String.format(matches.contains(" ")||matches.isEmpty()?"\"%s\"":"%s", matches.replaceAll("\"","\\\\\""));	// quote a value containing spaces or is empty and escape double quotes
		if (poolOnly!=null && poolOnly)				command += " --pool-only";
		if (proxy!=null)							command += " --proxy="+proxy;
		if (proxyuser!=null)						command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)					command += " --proxypassword="+proxypassword;
		if (noproxy!=null)							command += " --noproxy="+noproxy;
		
		return command;
	}
	
	/**
	 * list without asserting results
	 * @param all TODO
	 * @param available TODO
	 * @param consumed TODO
	 * @param installed TODO
	 * @param servicelevel TODO
	 * @param ondate TODO
	 * @param after TODO
	 * @param matchInstalled TODO
	 * @param noOverlap TODO
	 * @param matches TODO
	 * @param poolOnly TODO
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult list_(Boolean all, Boolean available, Boolean consumed, Boolean installed, String servicelevel, String ondate, String after, Boolean matchInstalled, Boolean noOverlap, String matches, Boolean poolOnly, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		String command = listCommand(all, available, consumed, installed, servicelevel, ondate, after, matchInstalled, noOverlap, matches, poolOnly, proxy, proxyuser, proxypassword, noproxy);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	public SSHCommandResult list(Boolean all, Boolean available, Boolean consumed, Boolean installed, String servicelevel, String ondate, String after, Boolean matchInstalled, Boolean noOverlap, String matches, Boolean poolOnly, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = list_(all, available, consumed, installed, servicelevel, ondate, after, matchInstalled, noOverlap, matches, poolOnly, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		
		// assert the exit code was a success
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the list module indicates a success.");
		
		return sshCommandResult; // from the list command
	}
	
	/**
	 * @return SSHCommandResult from "subscription-manager-cli list --installed"
	 */
	public SSHCommandResult listInstalledProducts() {
		
		SSHCommandResult sshCommandResult = list(null,null,null,Boolean.TRUE, null, null, null, null, null, null, null, null, null, null, null);
		
		if (getCurrentProductCertFiles().isEmpty() /*&& getCurrentEntitlementCertFiles().isEmpty() NOT NEEDED AFTER DESIGN CHANGE FROM BUG 736424*/) {
			Assert.assertTrue(sshCommandResult.getStdout().trim().equals("No installed products to list"), "No installed products to list");
		} else {
			//Assert.assertContainsMatch(sshCommandResult.getStdout(), "Installed Product Status"); // produces too much logging
			String title = "Installed Product Status";
			Assert.assertTrue(sshCommandResult.getStdout().contains(title),"The list of installed products is entitled '"+title+"'.");
		}

		return sshCommandResult;
	}
	
	/**
	 * @return SSHCommandResult from "subscription-manager-cli list --available"
	 */
	public SSHCommandResult listAvailableSubscriptionPools() {

		SSHCommandResult sshCommandResult = list(null,Boolean.TRUE,null, null, null, null, null, null, null, null, null, null, null, null, null);

		//Assert.assertContainsMatch(sshCommandResult.getStdout(), "Available Subscriptions"); // produces too much logging

		return sshCommandResult;
	}
	
	/**
	 * @return SSHCommandResult from "subscription-manager-cli list --all --available"
	 */
	public SSHCommandResult listAllAvailableSubscriptionPools() {

		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=638266 - jsefler 9/28/2010
		boolean invokeWorkaroundWhileBugIsOpen = false;
		String bugId="638266"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			return list_(Boolean.FALSE,Boolean.TRUE,null, null, null, null, null, null, null, null, null, null, null, null, null);
		}
		// END OF WORKAROUND
		
		SSHCommandResult sshCommandResult = list(Boolean.TRUE,Boolean.TRUE,null, null, null, null, null, null, null, null, null, null, null, null, null);
		
		//Assert.assertContainsMatch(sshCommandResult.getStdout(), "Available Subscriptions"); // produces too much logging

		return sshCommandResult;
		
	}
	
	/**
	 * @return SSHCommandResult from "subscription-manager-cli list --consumed"
	 */
	public SSHCommandResult listConsumedProductSubscriptions() {

		SSHCommandResult sshCommandResult = list(null,null,Boolean.TRUE, null, null, null, null, null, null, null, null, null, null, null, null);
		
		List<File> entitlementCertFiles = getCurrentEntitlementCertFiles();
		String expectedConsumedListMessage="No consumed subscription pools to list";
		if (isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit da72dfcbbb2c3a44393edb9e46e1583d05cc140a
			expectedConsumedListMessage="No consumed subscription pools were found.";
		}

		if (entitlementCertFiles.isEmpty()) {
			Assert.assertEquals(sshCommandResult.getStdout().trim(),expectedConsumedListMessage, "Expected message when there are no consumed subscription pools.");
		} else {
			String title = "Consumed Product Subscriptions";
			title = "Consumed Subscriptions";	// changed in https://bugzilla.redhat.com/show_bug.cgi?id=806986#c10
			//Assert.assertContainsMatch(sshCommandResult.getStdout(), title); // produces too much logging
			Assert.assertTrue(sshCommandResult.getStdout().contains(title),"The list of consumed products is entitled '"+title+"'.");
		}

		return sshCommandResult;
	}
	
	
	
	
	
	
	
	// status module tasks ************************************************************

	/**
	 * status without asserting results
	 * @param ondate TODO
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult status_(String ondate, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " status";
		if (ondate!=null)				command += " --ondate="+ondate;
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * status with asserting results
	 * @param ondate TODO
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult status(String ondate, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = status_(ondate, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results for a successful call to plugins
		if (isPackageVersion("subscription-manager",">=","1.13.8-1")) {	// post commit 7957b8df95c575e6e8713c2f1a0f8f754e32aed3 bug 1119688
			// exit code of 0 indicates valid compliance, otherwise exit code is 1
			Assert.assertTrue(sshCommandResult.getExitCode().equals(0)||sshCommandResult.getExitCode().equals(1), "Expecting an exit code of 0 to indicate a valid compliance, otherwise an exit code of 1 is expected.");
		} else
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the status command indicates a successful call to get the system's status.");
		Assert.assertEquals(sshCommandResult.getStderr(), "", "Stderr from the status command indicates a successful call to get the system's status.");
		
		// assert the banner
		String bannerRegex;
		bannerRegex = "\\+-+\\+\\n\\s*System Status Details\\s*\\n\\+-+\\+";
		Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from status contains the expected banner regex: "+bannerRegex);
		
		return sshCommandResult; // from the plugins command
	}
	
	
	
	

	
	// redeem module tasks ************************************************************

	/**
	 * redeem without asserting results
	 * @param email TODO
	 * @param locale TODO
	 * @param serverurl TODO
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult redeem_(String email, String locale, String serverurl, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " redeem";
		if (email!=null)				command += " --email="+email;
		if (locale!=null)				command += " --locale="+locale;
		if (serverurl!=null)			command += " --serverurl="+serverurl;
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}

	public SSHCommandResult redeem(String email, String locale, String serverurl, String proxy, String proxyuser, String proxypassword, String noproxy) {

		SSHCommandResult sshCommandResult = redeem_(email, locale, serverurl, proxy, proxyuser, proxypassword, noproxy);
		
		// TODO assert results...
		
		return sshCommandResult;
	}
	
	
	
	// repos module tasks ************************************************************
	
	/**
	 * @param listEnabled TODO
	 * @param listDisabled TODO
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String reposCommand(Boolean list, Boolean listEnabled, Boolean listDisabled, List<String> enableRepos,List<String> disableRepos,String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;									command += " repos";
		if (list!=null && list)											command += " --list";
		if (listEnabled!=null && listEnabled)							command += " --list-enabled";
		if (listDisabled!=null && listDisabled)							command += " --list-disabled";
		if (enableRepos!=null)	for (String enableRepo : enableRepos)	command += " --enable="+enableRepo;
		if (disableRepos!=null)	for (String disableRepo : disableRepos)	command += " --disable="+disableRepo;
		if (proxy!=null)												command += " --proxy="+proxy;
		if (proxyuser!=null)											command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)										command += " --proxypassword="+proxypassword;
		if (noproxy!=null)												command += " --noproxy="+noproxy;
		
		return command;
	}
	
	/**
	 * @param listEnabled TODO
	 * @param listDisabled TODO
	 * @param noproxy TODO
	 * @return SSHCommandResult from subscription-manager repos [parameters] without asserting any results
	 */
	public SSHCommandResult repos_(Boolean list, Boolean listEnabled, Boolean listDisabled, List<String> enableRepos,List<String> disableRepos,String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		String command = reposCommand(list, listEnabled, listDisabled,  enableRepos, disableRepos, proxy, proxyuser, proxypassword, noproxy);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	public SSHCommandResult repos_(Boolean list, Boolean listEnabled, Boolean listDisabled, String enableRepo,String disableRepo,String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> enableRepos = enableRepo==null?null:Arrays.asList(new String[]{enableRepo});
		List<String> disableRepos = disableRepo==null?null:Arrays.asList(new String[]{disableRepo});
		return repos_(list, listEnabled, listDisabled, enableRepos, disableRepos, proxy, proxyuser, proxypassword, noproxy);
	}

	/**
	 * @param listEnabled TODO
	 * @param listDisabled TODO
	 * @param noproxy TODO
	 * @return SSHCommandResult from subscription-manager repos [parameters]
	 */
	public SSHCommandResult repos(Boolean list, Boolean listEnabled, Boolean listDisabled, List<String> enableRepos,List<String> disableRepos,String proxy, String proxyuser, String proxypassword, String noproxy) {

		SSHCommandResult sshCommandResult = repos_(list, listEnabled, listDisabled, enableRepos,disableRepos,proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the repos command indicates a success.");
		
		// when rhsm.manage_repos is off, this feedback overrides all operations
		String manage_repos = getConfFileParameter(rhsmConfFile, "rhsm", "manage_repos");
		if (manage_repos==null || manage_repos.isEmpty() /*see bug 1251853*/) manage_repos="1";	// default configuration
		if (manage_repos.equals("0") ) {
			//Assert.assertEquals(sshCommandResult.getStdout().trim(), "Repositories disabled by configuration.","Stdout when rhsm.manage_repos is configured to 0.");
			//Assert.assertEquals(sshCommandResult.getStdout().trim(), "Repositories disabled by configuration.\nThe system is not entitled to use any repositories.","Stdout when rhsm.manage_repos is configured to 0.");
			//Assert.assertEquals(sshCommandResult.getStdout().trim(), "Repositories disabled by configuration.\nThis system has no repositories available through subscriptions.","Stdout when rhsm.manage_repos is configured to 0.");	// changed by bug 895462
			Assert.assertEquals(sshCommandResult.getStdout().trim(), "Repositories disabled by configuration.","Stdout when rhsm.manage_repos is configured to 0.");
			return sshCommandResult;
		}
		
		// assert list feedback
		String bannerRegex;
		bannerRegex = "\\+-+\\+\\n\\s*Entitled Repositories in "+redhatRepoFile+"\\s*\\n\\+-+\\+";	// changed by bug 846834
		bannerRegex = "\\+-+\\+\\n\\s*Available Repositories in "+redhatRepoFile+"\\s*\\n\\+-+\\+";
		//bannerRegex += "|The system is not entitled to use any repositories.";	// changed by bug 846834
		bannerRegex += "|This system has no repositories available through subscriptions.";
		if (list!=null && list) {	// when explicitly asked to list
			Assert.assertTrue(Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from repos (with option --list) contains the expected banner regex: "+bannerRegex);
		}
//		else {
//			Assert.assertTrue(!Pattern.compile(".*"+bannerRegex+".*",Pattern.DOTALL).matcher(sshCommandResult.getStdout()).find(),"Stdout from repos (without option --list) does not contain the banner regex: "+bannerRegex);	
//		}
		
		// assert the enable feedback
		if (enableRepos!=null) for (String enableRepo : enableRepos) {
			if (enableRepo.contains("*")) {log.info("Skipping assertion of feedback when enabling a repo with a wildcard.");continue;} 
			String expectedStdout = String.format("Repo %s is enabled for this system.",enableRepo);
			if (isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is enabled for this system.",enableRepo);
			if (isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Repository '%s' is enabled for this system.",enableRepo);	// 1122530: Improved grammar and abbreviation usage.	// commit add5a9b746f9f2af147a7e4622b897a46b5ef132
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedStdout), "Stdout from repos --enable includes expected feedback '"+expectedStdout+"'.");
		}
		
		// assert the disable feedback
		if (disableRepos!=null) for (String disableRepo : disableRepos) {
			if (disableRepo.contains("*")) {log.info("Skipping assertion of feedback when disabling a repo with a wildcard.");continue;} 
			String expectedStdout = String.format("Repo %s is disabled for this system.",disableRepo);
			if (isPackageVersion("subscription-manager", ">=", "1.10.7-1")) expectedStdout = String.format("Repo '%s' is disabled for this system.",disableRepo);
			if (isPackageVersion("subscription-manager", ">=", "1.13.6-1")) expectedStdout = String.format("Repository '%s' is disabled for this system.",disableRepo);	// 1122530: Improved grammar and abbreviation usage.	// commit add5a9b746f9f2af147a7e4622b897a46b5ef132
			Assert.assertTrue(sshCommandResult.getStdout().contains(expectedStdout), "Stdout from repos --disable includes expected feedback '"+expectedStdout+"'.");
		}
		
		return sshCommandResult;
	}
	public SSHCommandResult repos(Boolean list, Boolean listEnabled, Boolean listDisabled, String enableRepo,String disableRepo,String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> enableRepos = enableRepo==null?null:Arrays.asList(new String[]{enableRepo});
		List<String> disableRepos = disableRepo==null?null:Arrays.asList(new String[]{disableRepo});
		return repos(list, listEnabled, listDisabled, enableRepos, disableRepos, proxy, proxyuser, proxypassword, noproxy);
	}
	
	
	/**
	 * @return SSHCommandResult from "subscription-manager repos --list"
	 */
	public SSHCommandResult listSubscribedRepos() {

		Calendar now = new GregorianCalendar();
		now.setTimeInMillis(System.currentTimeMillis());
		
		SSHCommandResult sshCommandResult = repos(true, null, null, (String)null,(String)null,null, null, null, null);
		//Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the repos --list command indicates a success.");
		
		//List<File> entitlementCertFiles = getCurrentEntitlementCertFiles();
		List<ProductCert> productCerts = getCurrentProductCerts();
		List<EntitlementCert> entitlementCerts = getCurrentEntitlementCerts();
		int numContentNamespaces = 0;
		for (EntitlementCert entitlementCert : entitlementCerts) {
			
			// we should NOT count contentNamespaces from entitlement certs that are not valid now
			if (entitlementCert.validityNotBefore.after(now) || entitlementCert.validityNotAfter.before(now)) continue;

			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
				// we should NOT count contentNamespaces from for which all required tags are not provided by the installed product certs
				if (!areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace, productCerts)) {
					log.warning("None of the currently installed product certs provide the required tags '"+contentNamespace.requiredTags+"' for entitled content namespace: "+contentNamespace.name);
					continue;
				}
				
				// we should NOT count contentNamespaces that are not of type=yum
				if (!contentNamespace.type.equals("yum")) {
					log.warning("Encountered the following Content Namespace whose type is not \"yum\" (it should not contribute to the list of yum repos):  "+contentNamespace);
					continue;
				}

				numContentNamespaces++;
			}
			
		}

		if (numContentNamespaces==0) {
			//Assert.assertTrue(sshCommandResult.getStdout().trim().equals("The system is not entitled to use any repositories."), "The system is not entitled to use any repositories.");	// changed by bug 846834
			Assert.assertTrue(sshCommandResult.getStdout().trim().equals("This system has no repositories available through subscriptions."), "This system has no repositories available through subscriptions.");
		} else {
			String title;
			title = "Entitled Repositories in "+redhatRepoFile;	// changed by bug 846834
			title = "Available Repositories in "+redhatRepoFile;
			Assert.assertTrue(sshCommandResult.getStdout().contains(title),"The list of repositories is entitled '"+title+"'.");
		}

		return sshCommandResult;
	}
	
	
	
	// repo-override module tasks ************************************************************

	/**
	 * @param noproxy TODO
	 * @return SSHCommandResult from subscription-manager repo-override [parameters] without asserting any results
	 */
	public SSHCommandResult repo_override_(Boolean list, Boolean removeAll, List<String> repoIds, List<String> removeNames, Map<String,String> addNameValueMap, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;											command += " repo-override";
		if (list!=null && list)													command += " --list";
		if (removeAll!=null && removeAll)										command += " --remove-all";
		if (repoIds!=null) for (String repoId : repoIds)						command += " --repo="+repoId;
		if (removeNames!=null) for (String removeName : removeNames)			command += " --remove="+removeName;
		if (addNameValueMap!=null) for (String name:addNameValueMap.keySet())	command += " --add="+String.format(addNameValueMap.get(name).contains(" ")||addNameValueMap.get(name).isEmpty()?"%s:\"%s\"":"%s:%s", name,addNameValueMap.get(name)); 	// quote a value containing spaces or is empty
		if (proxy!=null)														command += " --proxy="+proxy;
		if (proxyuser!=null)													command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)												command += " --proxypassword="+proxypassword;
		if (noproxy!=null)														command += " --noproxy="+noproxy;
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	/**
	 * @param noproxy TODO
	 * @return SSHCommandResult from subscription-manager repo-override [parameters] without asserting any results
	 */
	public SSHCommandResult repo_override_(Boolean list, Boolean removeAll, String repoId, String removeName, Map<String,String> addNameValueMap, String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> repoIds = repoId==null?null:Arrays.asList(new String[]{repoId});
		List<String> removeNames = removeName==null?null:Arrays.asList(new String[]{removeName});
		return repo_override_(list, removeAll, repoIds, removeNames, addNameValueMap, proxy, proxyuser, proxypassword, noproxy);
	}
	
	
	/**
	 * @param noproxy TODO
	 * @return SSHCommandResult from subscription-manager repo-override [parameters]
	 */
	public SSHCommandResult repo_override(Boolean list, Boolean removeAll, List<String> repoIds, List<String> removeNames, Map<String,String> addNameValueMap, String proxy, String proxyuser, String proxypassword, String noproxy) {

		SSHCommandResult sshCommandResult = repo_override_(list, removeAll, repoIds, removeNames, addNameValueMap, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results...
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the repo-override command indicates a success.");
		
		//	[root@jsefler-7 ~]# subscription-manager repo-override --list
		//	Repository: content-label-10
		//	  enabled:        1
		//	  exclude:        foo-bar
		//	  gpgcheck:       0
		//	  name:           Repo content-label-10
		//	  retries:        5
		//	  ui_repoid_vars: releasever basearch foo
		//
		//	Repository: content-label-11
		//	  enabled:        1
		//	  exclude:        foo-bar
		//	  gpgcheck:       0
		//	  name:           Repo content-label-11
		//	  retries:        5
		//	  ui_repoid_vars: releasever basearch foo
		
		// assert the removeNames are actually removed from the list
		if (removeNames!=null && !removeNames.isEmpty()) {
			SSHCommandResult listResult = repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
			for (String repoId : repoIds) {
				for (String name : removeNames) {
					Assert.assertTrue(!SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), String.format(repoOverrideListRepositoryNameValueRegexFormat,repoId,name,"")),"After a repo-override removal, the subscription-manager repo-override list no longer reports repo override repo='"+repoId+"' name='"+name+"'.");
				}
			}			
		}
		
		// assert the addNameValueMap are actually added to the list
		if (addNameValueMap!=null && !addNameValueMap.isEmpty()) {
			SSHCommandResult listResult = repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
			for (String repoId : repoIds) {
				for (String name : addNameValueMap.keySet()) {
					String value = addNameValueMap.get(name);
					String regex = String.format(repoOverrideListRepositoryNameValueRegexFormat,repoId,name,value.replace("*", "\\*").replace("?", "\\?"));	// notice that we have to escape glob characters from the value so they don't get interpreted as regex chars
					Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(listResult.getStdout(), regex),"After adding a repo-override, the subscription-manager repo-override reports override repo='"+repoId+"' name='"+name+"' value='"+value+"'.");
				}
			}
		}
		
		if (removeAll!=null && removeAll) {
			SSHCommandResult listResult = repo_override(true,null,(String)null,(String)null,null,null,null,null, null);
			Assert.assertEquals(listResult.getStdout().trim(),"This system does not have any content overrides applied to it.","After removing all repo-overrides, this is the subscription-manager repo-override report.");
		}
		
		return sshCommandResult;
	}
	public SSHCommandResult repo_override(Boolean list, Boolean removeAll, String repoId, String removeName, Map<String,String> addNameValueMap, String proxy, String proxyuser, String proxypassword, String noproxy) {
		List<String> repoIds = repoId==null?null:Arrays.asList(new String[]{repoId});
		List<String> removeNames = removeName==null?null:Arrays.asList(new String[]{removeName});
		return repo_override(list, removeAll, repoIds, removeNames, addNameValueMap, proxy, proxyuser, proxypassword, noproxy);
	}
	public static final String repoOverrideListRepositoryNameValueRegexFormat = "Repository: %s(\\n.+)+%s:(\\s)*%s";
	
	// plugins module tasks ************************************************************

	/**
	 * plugins without asserting results
	 */
	public SSHCommandResult plugins_(Boolean list, Boolean listslots, Boolean listhooks, Boolean verbose) {

		// assemble the command
		String command = this.command;		command += " plugins";
		if (list!=null && list)				command += " --list";
		if (listslots!=null && listslots)	command += " --listslots";
		if (listhooks!=null && listhooks)	command += " --listhooks";
		if (verbose!=null && verbose)		command += " --verbose";
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * plugins with asserting results
	 */
	public SSHCommandResult plugins(Boolean list, Boolean listslots, Boolean listhooks, Boolean verbose) {
		
		SSHCommandResult sshCommandResult = plugins_(list, listslots, listhooks, verbose);
		
		// assert results for a successful call to plugins
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the plugins command indicates a success.");
		Assert.assertEquals(sshCommandResult.getStderr(), "", "Stderr from the plugins command indicates a success.");
		
		return sshCommandResult; // from the plugins command
	}
	
	
	

	// subscribe module tasks ************************************************************
	
	/**
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String subscribeCommand(Boolean auto, String servicelevel, List<String> poolIds, List<String> productIds, List<String> regtokens, String quantity, String email, String locale, String file, String proxy, String proxyuser, String proxypassword, String noproxy) {
		// assemble the command
		String command = this.command;									command += " subscribe";
		if (auto!=null && auto)											command += " --auto";
		if (servicelevel!=null)											command += " --servicelevel="+String.format(servicelevel.contains(" ")||servicelevel.isEmpty()?"\"%s\"":"%s", servicelevel);	// quote a value containing spaces or is empty
		if (poolIds!=null)		for (String poolId : poolIds)			command += " --pool="+poolId;
		if (productIds!=null)	for (String productId : productIds)		command += " --product="+productId;
		if (regtokens!=null)	for (String regtoken : regtokens)		command += " --regtoken="+regtoken;
		if (quantity!=null)												command += " --quantity="+quantity;
		if (email!=null)												command += " --email="+email;
		if (locale!=null)												command += " --locale="+locale;
		if (file!=null)													command += " --file="+file;
		if (proxy!=null)												command += " --proxy="+proxy;
		if (proxyuser!=null)											command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)										command += " --proxypassword="+proxypassword;
		if (noproxy!=null)												command += " --noproxy="+noproxy;
		
		return command;
	}
	
	/**
	 * subscribe WITHOUT asserting results
	 * @param noproxy TODO
	 */
	public SSHCommandResult subscribe_(Boolean auto, String servicelevel, List<String> poolIds, List<String> productIds, List<String> regtokens, String quantity, String email, String locale, String file, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = subscribeCommand(auto, servicelevel, poolIds, productIds, regtokens, quantity, email, locale, file, proxy, proxyuser, proxypassword, noproxy);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		
		// TEMPORARY WORKAROUND FOR BUG
		String bugId = "981689"; // 'SubscribeCommand' object has no attribute 'sorter'
		boolean invokeWorkaroundWhileBugIsOpen = true;
		if (sshCommandResult.getStderr().trim().equals("'SubscribeCommand' object has no attribute 'sorter'") ||
			sshCommandResult.getStderr().trim().equals("'AttachCommand' object has no attribute 'sorter'")	) {
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("All tests that attempt to subscribe are blockedByBug '"+bugId+"'.");
			}
		}
		// END OF WORKAROUND
		
		return sshCommandResult;
	}

	/**
	 * subscribe WITHOUT asserting results.
	 * @param noproxy TODO
	 */
	public SSHCommandResult subscribe_(Boolean auto, String servicelevel, String poolId, String productId, String regtoken, String quantity, String email, String locale, String file, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		List<String> poolIds	= poolId==null?null:Arrays.asList(new String[]{poolId});
		List<String> productIds	= productId==null?null:Arrays.asList(new String[]{productId});
		List<String> regtokens	= regtoken==null?null:Arrays.asList(new String[]{regtoken});

		return subscribe_(auto, servicelevel, poolIds, productIds, regtokens, quantity, email, locale, file, proxy, proxyuser, proxypassword, noproxy);
	}


	
	/**
	 * subscribe and assert all results are successful
	 * @param noproxy TODO
	 */
	public SSHCommandResult subscribe(Boolean auto, String servicelevel, List<String> poolIds, List<String> productIds, List<String> regtokens, String quantity, String email, String locale, String file, String proxy, String proxyuser, String proxypassword, String noproxy) {

		SSHCommandResult sshCommandResult = subscribe_(auto, servicelevel, poolIds, productIds, regtokens, quantity, email, locale, file, proxy, proxyuser, proxypassword, noproxy);
		auto = auto==null? false:auto;	// the non-null default value for auto is false

		// assert results...
		String stdoutMessage;
		
		// just return the result for any of the following cases:
		if (sshCommandResult.getStdout().trim().startsWith("This consumer is already subscribed") ||		// This consumer is already subscribed to the product matching pool with id 'ff8080812c71f5ce012c71f6996f0132'.
			sshCommandResult.getStdout().trim().startsWith("This unit has already had the subscription") ||	// This unit has already had the subscription matching pool ID '8a99f98340114f880140766376dc00cf' attached.
			sshCommandResult.getStdout().trim().startsWith("No entitlements are available") ||				// No entitlements are available from the pool with id '8a90f8143611c33f013611c4797b0456'.   (Bug 719743)
			sshCommandResult.getStdout().trim().startsWith("No subscriptions are available") ||				// No subscriptions are available from the pool with id '8a90f8303c98703a013c98715ca80494'.   (Bug 846758)
			sshCommandResult.getStdout().trim().startsWith("Pool is restricted") ||							// Pool is restricted to virtual guests: '8a90f85734205a010134205ae8d80403'.
																											// Pool is restricted to physical systems: '8a9086d3443c043501443c052aec1298'.
																											// Pool is restricted to unmapped virtual guests: '8a9087e34c2f214a014c2f22a7d11ad0'
//DON'T TOLERATE STDERR RESULTS HERE
//			sshCommandResult.getStderr().trim().startsWith("Development units may only be used on") ||		// Development units may only be used on hosted servers and with orgs that have active subscriptions.
			sshCommandResult.getStdout().trim().startsWith("All installed products are covered") ||			// All installed products are covered by valid entitlements. No need to update subscriptions at this time.
			sshCommandResult.getStdout().trim().startsWith("No Installed products on system.") ||			// No Installed products on system. No need to attach subscriptions.
			sshCommandResult.getStdout().trim().startsWith("Unable to entitle consumer")) {					// Unable to entitle consumer to the pool with id '8a90f8b42e3e7f2e012e3e7fc653013e'.: rulefailed.virt.only
																											// Unable to entitle consumer to the pool with id '8a90f85734160df3013417ac68bb7108'.: Entitlements for awesomeos-virt-4 expired on: 12/7/11 3:43 AM
			log.warning(sshCommandResult.getStdout().trim());
			return sshCommandResult;	
		}
		
		// assert the subscribe does NOT report "The system is unable to complete the requested transaction"
		//Assert.assertContainsNoMatch(sshCommandResult.getStdout(), "The system is unable to complete the requested transaction","The system should always be able to complete the requested transaction.");
		stdoutMessage = "The system is unable to complete the requested transaction";
		Assert.assertFalse(sshCommandResult.getStdout().contains(stdoutMessage), "The subscribe stdout should NOT report: "+stdoutMessage);
	
		// assert the subscribe does NOT report "Entitlement Certificate\\(s\\) update failed due to the following reasons:"
		//Assert.assertContainsNoMatch(sshCommandResult.getStdout(), "Entitlement Certificate\\(s\\) update failed due to the following reasons:","Entitlement Certificate updates should be successful when subscribing.");
		stdoutMessage = "Entitlement Certificate(s) update failed due to the following reasons:";
		Assert.assertFalse(sshCommandResult.getStdout().contains(stdoutMessage), "The subscribe stdout should NOT report: "+stdoutMessage);

		// assert that the entitlement pool was found for subscribing
		//Assert.assertContainsNoMatch(sshCommandResult.getStdout(),"No such entitlement pool:", "The subscription pool was found.");
		//Assert.assertContainsNoMatch(sshCommandResult.getStdout(), "Subscription pool .* does not exist.","The subscription pool was found.");
		//stdoutMessage = "Subscription pool "+(poolId==null?"null":poolId)+" does not exist.";	// Subscription pool {0} does not exist.
		//Assert.assertFalse(sshCommandResult.getStdout().contains(stdoutMessage), "The subscribe stdout should NOT report: "+stdoutMessage);
		if (poolIds!=null) {
			for (String poolId : poolIds) {
				stdoutMessage = "Subscription pool "+poolId+" does not exist.";	// Subscription pool {0} does not exist.
				Assert.assertFalse(sshCommandResult.getStdout().contains(stdoutMessage), "The subscribe stdout should NOT report: "+stdoutMessage);
			}
		}
		
		// assert the stdout msg was a success
		if (servicelevel!=null && !servicelevel.equals(""))
			Assert.assertTrue(sshCommandResult.getStdout().contains("Service level set to: "+servicelevel), "The autosubscribe stdout reports: Service level set to: "+servicelevel);
		if (auto)
			Assert.assertTrue(sshCommandResult.getStdout().contains("Installed Product Current Status:"), "The autosubscribe stdout reports: Installed Product Current Status");
		else if (!auto && file==null && (poolIds==null||poolIds.isEmpty()) && isPackageVersion("subscription-manager",">=","1.14.1-1"))	// defaults to auto
			Assert.assertTrue(sshCommandResult.getStdout().contains("Installed Product Current Status:"), "The subscribe stdout reports: Installed Product Current Status (when the default behavior implies to autosubscribe)");
		else {
			//Assert.assertTrue(sshCommandResult.getStdout().startsWith("Success"), "The subscribe stdout reports 'Success'.");
			if (file!=null || poolIds.size()==1) {
				Assert.assertTrue(workaroundForBug906550(sshCommandResult.getStdout()).startsWith("Success"), "The subscribe stdout reports a 'Success'fully attached subscription.");
			} else {
				Assert.assertTrue(workaroundForBug906550(sshCommandResult.getStdout()).contains("Success"), "The subscribe stdout reports at least one 'Success'fully attached subscription.");			
			}
		}
		
		// assert the exit code was not a failure
		if (auto)
			Assert.assertTrue(Integer.valueOf(sshCommandResult.getExitCode())<=1, "The exit code ("+sshCommandResult.getExitCode()+") from the subscribe --auto command does not indicate a failure (exit code 0 indicates an entitlement was granted, 1 indicates an entitlement was not granted, 255 indicates a failure).");
		else if (!auto && file==null && (poolIds==null||poolIds.isEmpty()) && isPackageVersion("subscription-manager",">=","1.14.1-1"))	// defaults to auto
			Assert.assertTrue(Integer.valueOf(sshCommandResult.getExitCode())<=1, "The exit code ("+sshCommandResult.getExitCode()+") from the subscribe command (defaulting to autosubscribe) does not indicate a failure (exit code 0 indicates an entitlement was granted, 1 indicates an entitlement was not granted, 255 indicates a failure).");
		else {
			// TEMPORARY WORKAROUND
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1287610"; 	// Bug 1287610 - yum message in output when FIPS is enabled
			try {if (redhatReleaseX.equals("7")&&isFipsEnabled&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping the stderr assertion from subscribe on rhel '"+redhatReleaseXY+"' while FIPS bug '"+bugId+"' is open");
			} else
			// END OF WORKAROUND
			Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the subscribe command indicates a success.");
		}
		return sshCommandResult;
	}
	
	/**
	 * subscribe and assert all results are successful
	 * @param noproxy TODO
	 */
	public SSHCommandResult subscribe(Boolean auto, String servicelevel, String poolId, String productId, String regtoken, String quantity, String email, String locale, String file, String proxy, String proxyuser, String proxypassword, String noproxy) {

		List<String> poolIds	= poolId==null?null:Arrays.asList(new String[]{poolId});
		List<String> productIds	= productId==null?null:Arrays.asList(new String[]{productId});
		List<String> regtokens	= regtoken==null?null:Arrays.asList(new String[]{regtoken});

		return subscribe(auto, servicelevel, poolIds, productIds, regtokens, quantity, email, locale, file, proxy, proxyuser, proxypassword, noproxy);
	}
	
	
//	public SSHCommandResult subscribe(List<String> poolIds, List<String> productIds, List<String> regtokens, String quantity, String email, String locale, String proxy, String proxyuser, String proxypassword) {
//
//		SSHCommandResult sshCommandResult = subscribe_(null, poolIds, productIds, regtokens, quantity, email, locale, proxy, proxyuser, proxypassword);
//		
//		// assert results
//		Assert.assertContainsNoMatch(sshCommandResult.getStdout(), "Entitlement Certificate\\(s\\) update failed due to the following reasons:","Entitlement Certificate updates should be successful when subscribing.");
//		if (sshCommandResult.getStderr().startsWith("This consumer is already subscribed")) return sshCommandResult;
//		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the subscribe command indicates a success.");
//		return sshCommandResult;
//	}
	
	
	public void subscribeToProductId(String productId) {
		//RemoteFileTasks.runCommandExpectingNonzeroExit(sshCommandRunner,"subscription-manager-cli subscribe --product="+product);
		
		SubscriptionPool pool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("productId", productId, getCurrentlyAvailableSubscriptionPools());
		Assert.assertNotNull(pool,"Found an available pool to subscribe to productId '"+productId+"': "+pool);
		subscribeToSubscriptionPool(pool);
	}
	
	
	/**
	 * subscribe to the given SubscriptionPool (assumes pool came from the list of available pools)
	 * @return the newly installed EntitlementCert file to the newly consumed ProductSubscriptions 
	 */
	public File subscribeToSubscriptionPool(SubscriptionPool pool, String quantity, String authenticator, String password, String serverUrl)  {
		
		List<ProductSubscription> beforeProductSubscriptions = getCurrentlyConsumedProductSubscriptions();
		List<File> beforeEntitlementCertFiles = getCurrentEntitlementCertFiles();
		log.info("Subscribing to subscription pool: "+pool);
		SSHCommandResult sshCommandResult = subscribe(null, null, pool.poolId, null, null, quantity, null, null, null, null, null, null, null);

		// is this pool multi-entitleable?
		/* This information is now in the SubscriptionPool itself
		boolean isPoolMultiEntitlement = false;
		try {
			isPoolMultiEntitlement = CandlepinTasks.isPoolProductMultiEntitlement(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,SubscriptionManagerBaseTestScript.sm_serverUrl,pool.poolId);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		*/
		
		// get the pool's product "arch" attribute that this subscription pool supports
		String poolProductAttributeArch = "";
		List<String> poolProductAttributeArches = new ArrayList<String>();
		if (authenticator!=null && password!=null && serverUrl!=null) {
			try {
				poolProductAttributeArch = CandlepinTasks.getPoolProductAttributeValue(authenticator, password, serverUrl, pool.poolId, "arch");
				if (poolProductAttributeArch!=null && !poolProductAttributeArch.trim().isEmpty()) {				
					poolProductAttributeArches.addAll(Arrays.asList(poolProductAttributeArch.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
					if (poolProductAttributeArches.contains("x86")) poolProductAttributeArches.addAll(Arrays.asList("i386","i486","i586","i686"));  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
					//if (productSupportedArches.contains("ALL")) productSupportedArches.add(arch);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		

		// assert that the remaining SubscriptionPools does NOT contain the pool just subscribed to (unless it is multi-entitleable)
		List<SubscriptionPool> afterSubscriptionPools = getCurrentlyAvailableSubscriptionPools();
	    if (pool.subscriptionType!=null && pool.subscriptionType.equals("Other") ) {
	    	Assert.fail("Encountered a subscription pool of type '"+pool.subscriptionType+"'.  Do not know how to assert the remaining availability of this pool after subscribing to it: "+pool);
	    } else if (pool.multiEntitlement==null && pool.subscriptionType!=null && pool.subscriptionType.isEmpty()) {
	    	log.warning("Encountered a pool with an empty value for subscriptionType (indicative of an older candlepin server): "+pool);
	    	log.warning("Skipping assertion of the pool's expected availability after having subscribed to it.");
	    } else if (!pool.quantity.equalsIgnoreCase("unlimited") && Integer.valueOf(pool.quantity)<=1) {
			Assert.assertTrue(!afterSubscriptionPools.contains(pool),
					"When the final quantity from the pool was consumed, the remaining available subscription pools no longer contains the just subscribed to pool: "+pool);
		} else if (pool.multiEntitlement!=null && !pool.multiEntitlement) {
			Assert.assertTrue(!afterSubscriptionPools.contains(pool),
					"When the pool is not multi-entitleable, the remaining available subscription pools no longer contains the just subscribed to pool: "+pool);
		} else if (pool.subscriptionType!=null && (!pool.subscriptionType.equals("Stackable") && !pool.subscriptionType.equals("Multi-Entitleable") && !pool.subscriptionType.equals("Instance Based") && !pool.subscriptionType.equals("Stackable (Temporary)") && !pool.subscriptionType.equals("Multi-Entitleable (Temporary)") && !pool.subscriptionType.equals("Instance Based (Temporary)")) ) {	// see https://bugzilla.redhat.com/show_bug.cgi?id=1029968#c2
			Assert.assertTrue(!afterSubscriptionPools.contains(pool),
					"When the pool is not multi-entitleable (not Stackable && not Multi-Entitleable && not Instance Based), the remaining available subscription pools no longer contains the just subscribed to pool: "+pool);
		} else if (!poolProductAttributeArches.isEmpty() && !poolProductAttributeArches.contains("ALL") && !poolProductAttributeArches.contains(arch)) {
			Assert.assertTrue(!afterSubscriptionPools.contains(pool),
					"When the pools product attribute arch '"+poolProductAttributeArch+"' does not support this system arch '"+arch+"', the remaining available subscription pools should never contain the just subscribed to pool: "+pool);
		} else {
			Assert.assertTrue(afterSubscriptionPools.contains(pool),
					"When the pool is multi-entitleable, the remaining available subscription pools still contains the just subscribed to pool: "+pool+" (TODO: if this fails, then we likely attached the final entitlements from the pool)");	// TODO fix the assertions for "if this fails"
		}
		
		// assert that the remaining SubscriptionPools do NOT contain the same productId just subscribed to
		//log.warning("We will no longer assert that the remaining available pools do not contain the same productId ("+pool.productId+") as the pool that was just subscribed.  Reference: https://bugzilla.redhat.com/show_bug.cgi?id=663455");
		/*
		for (SubscriptionPool afterSubscriptionPool : afterSubscriptionPools) {
			Assert.assertTrue(!afterSubscriptionPool.productId.equals(pool.productId),
					"This remaining available pool "+afterSubscriptionPool+" does NOT contain the same productId ("+pool.productId+") after subscribing to pool: "+pool);
		}
		*/

		// is this a personal subpool?
		String poolProductId = pool.productId;
		boolean isSubpool = false; 
		try {
			JSONArray personSubscriptionPoolProductData;
//			personSubscriptionPoolProductData = new JSONArray(System.getProperty("sm.person.subscriptionPoolProductData", "<>").replaceAll("<", "[").replaceAll(">", "]")); // hudson parameters use <> instead of []
			personSubscriptionPoolProductData = new JSONArray(SubscriptionManagerBaseTestScript.getProperty("sm.person.subscriptionPoolProductData", "[]").replaceFirst("^\"", "").replaceFirst("\"$", "").replaceAll("<", "[").replaceAll(">", "]")); // hudson JSONArray parameters get surrounded with double quotes that need to be stripped
			for (int j=0; j<personSubscriptionPoolProductData.length(); j++) {
				JSONObject poolProductDataAsJSONObject = (JSONObject) personSubscriptionPoolProductData.get(j);
				String personProductId = poolProductDataAsJSONObject.getString("personProductId");
				JSONObject subpoolProductDataAsJSONObject = poolProductDataAsJSONObject.getJSONObject("subPoolProductData");
				String systemProductId = subpoolProductDataAsJSONObject.getString("systemProductId");
				if (poolProductId.equals(systemProductId)) { // special case when pool's productId is really a personal subpool
					poolProductId = personProductId;
					isSubpool = true;
					break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 
		
		// figure out which entitlement cert file has been newly installed into /etc/pki/entitlement after attempting to subscribe to pool
		/* OLD - THIS ALGORITHM BREAKS DOWN WHEN MODIFIER ENTITLEMENTS ARE IN PLAY
		File newCertFile = null;
		List<File> afterEntitlementCertFiles = getCurrentEntitlementCertFiles();
		for (File file : afterEntitlementCertFiles) {
			if (!beforeEntitlementCertFiles.contains(file)) {
				newCertFile = file; break;
			}
		}
		*/
		/* VALID BUT INEFFICIENT
		List<File> afterEntitlementCertFiles = getCurrentEntitlementCertFiles();
		File newCertFile = null;
		Map<BigInteger, SubscriptionPool> map = new HashMap<BigInteger, SubscriptionPool>();
		try {
			map = getCurrentSerialMapToSubscriptionPools(this.currentAuthenticator,this.currentAuthenticatorPassword);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		for (BigInteger serial: map.keySet()) {
			if (map.get(serial).poolId.equals(pool.poolId)) {
				newCertFile = new File(this.entitlementCertDir+"/"+serial+".pem");
				break;
			}
		}
		*/
		// NOTE: this block of code is somewhat duplicated in getEntitlementCertCorrespondingToSubscribedPool(...)
		File newCertFile = null;
		List<File> afterEntitlementCertFiles = getCurrentEntitlementCertFiles("-t");
		if (authenticator!=null && password!=null && serverUrl!=null) {
			for (File entitlementCertFile : afterEntitlementCertFiles) {
				if (!beforeEntitlementCertFiles.contains(entitlementCertFile)) {
					EntitlementCert entitlementCert = getEntitlementCertFromEntitlementCertFile(entitlementCertFile);
					try {
						//JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(authenticator,password,serverUrl,entitlementCert.id);	// is throwing a 500 in stage, but only for qa@redhat.com credentials - I don't know why
						JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(currentlyRegisteredUsername,currentlyRegisteredPassword,serverUrl,entitlementCert.id);
						JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(authenticator,password,serverUrl,jsonEntitlement.getJSONObject("pool").getString("href")));
						if (jsonPool.getString("id").equals(pool.poolId)) {
							newCertFile = entitlementCertFile; break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						Assert.fail(e.getMessage());
					}
				}
			}
		}
		
		// when the pool is already subscribed to...
		if (sshCommandResult.getStdout().startsWith("This consumer is already subscribed")) {
			
			// assert that NO new entitlement cert file has been installed in /etc/pki/entitlement
			/*Assert.assertNull(newCertFile,
					"A new entitlement certificate has NOT been installed after attempting to subscribe to an already subscribed to pool: "+pool);
			*/
			Assert.assertEquals(beforeEntitlementCertFiles.size(), afterEntitlementCertFiles.size(),
					"The existing entitlement certificate count remains unchanged after attempting to subscribe to an already subscribed to pool: "+pool);

			// find the existing entitlement cert file corresponding to the already subscribed pool
			/* ALREADY FOUND USING ALGORITHM ABOVE 
			EntitlementCert entitlementCert = null;
			for (File thisEntitlementCertFile : getCurrentEntitlementCertFiles()) {
				EntitlementCert thisEntitlementCert = getEntitlementCertFromEntitlementCertFile(thisEntitlementCertFile);
				if (thisEntitlementCert.orderNamespace.productId.equals(poolProductId)) {
					entitlementCert = thisEntitlementCert;
					break;
				}
			}
			Assert.assertNotNull(entitlementCert, isSubpool?
					"Found an already existing Entitlement Cert whose personal productId matches the system productId from the subscription pool: "+pool:
					"Found an already existing Entitlement Cert whose productId matches the productId from the subscription pool: "+pool);
			newCertFile = getEntitlementCertFileFromEntitlementCert(entitlementCert); // not really new, just already existing
			*/
			
			// assert that consumed ProductSubscriptions has NOT changed
			List<ProductSubscription> afterProductSubscriptions = getCurrentlyConsumedProductSubscriptions();
			Assert.assertTrue(afterProductSubscriptions.size() == beforeProductSubscriptions.size() && afterProductSubscriptions.size() > 0,
					"The list of currently consumed product subscriptions has not changed (from "+beforeProductSubscriptions.size()+" to "+afterProductSubscriptions.size()+") since the productId of the pool we are trying to subscribe to is already consumed.");

		// when no free entitlements exist...		// No entitlements are available from the pool with id '8a90f8143611c33f013611c4797b0456'.	// No subscriptions are available from the pool with id '8a90f8303c98703a013c98715ca80494'.  Bug 876758
		} else if (sshCommandResult.getStdout().startsWith("No entitlements are available") || sshCommandResult.getStdout().startsWith("No subscriptions are available")) {
			
			// assert that the depleted pool Quantity is zero
			SubscriptionPool depletedPool = SubscriptionPool.findFirstInstanceWithMatchingFieldFromList("poolId", pool.poolId, getCurrentlyAllAvailableSubscriptionPools());
			/* behavior changed on list --all --available  (3/4/2011)
			Assert.assertNotNull(depletedPool,
					"Found the depleted pool amongst --all --available after having consumed all of its available entitlements: ");
			*/
			Assert.assertNull(depletedPool,
					"Should no longer find the depleted pool amongst --all --available after having consumed all of its available entitlements: ");
//			Assert.assertEquals(depletedPool.quantity, "0",
//					"Asserting the pool's quantity after having consumed all of its available entitlements is zero.");
			if (authenticator!=null && password!=null && serverUrl!=null) {
				JSONObject jsonPool = null;
				int consumed = 0;
				int quantityAvailable = Integer.valueOf(pool.quantity);
				try {
					jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(authenticator,password,serverUrl,"/pools/"+pool.poolId));
					consumed = jsonPool.getInt("consumed");
					quantityAvailable = jsonPool.getInt("quantity");
				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				} 
				Assert.assertEquals(consumed, quantityAvailable,
						"Asserting the pool's consumed attribute equals it's total quantity after having consumed all of its available entitlements.");
			}
			
			//  assert that NO new entitlement cert file has been installed in /etc/pki/entitlement
			Assert.assertNull(newCertFile,
					"A new entitlement certificate has NOT been installed after attempting to subscribe to depleted pool: "+depletedPool);
			Assert.assertEquals(beforeEntitlementCertFiles.size(), afterEntitlementCertFiles.size(),
					"The existing entitlement certificate count remains unchanged after attempting to subscribe to depleted pool: "+depletedPool);

			
		// otherwise, the pool is NOT already subscribe to...
		} else {
	
			// assert that only ONE new entitlement cert file has been installed in /etc/pki/entitlement
			// https://bugzilla.redhat.com/show_bug.cgi?id=640338
			Assert.assertTrue(afterEntitlementCertFiles.size()==beforeEntitlementCertFiles.size()+1,
					"Only ONE new entitlement certificate has been installed (count was '"+beforeEntitlementCertFiles.size()+"'; is now '"+afterEntitlementCertFiles.size()+"') after subscribing to pool: "+pool);

			// assert that the other cert files remain unchanged
			/* CANNOT MAKE THIS ASSERT/ASSUMPTION ANYMORE BECAUSE ADDITION OF AN ENTITLEMENT CAN AFFECT A MODIFIER PRODUCT THAT PROVIDES EXTRA CONTENT FOR THIS PRODUCT (A MODIFIER PRODUCT IS ALSO CALLED EUS) 2/21/2011 jsefler
			if (!afterEntitlementCertFiles.remove(newCertFile)) Assert.fail("Failed to remove certFile '"+newCertFile+"' from list.  This could be an automation logic error.");
			Assert.assertEquals(afterEntitlementCertFiles,beforeEntitlementCertFiles,"After subscribing to pool id '"+pool+"', the other entitlement cert serials remain unchanged");
			*/
			
			if (authenticator!=null && password!=null && serverUrl!=null) {
				
				// assert the new entitlement cert file has been installed in /etc/pki/entitlement
				Assert.assertNotNull(newCertFile, "A new entitlement certificate has been installed after subscribing to pool: "+pool);
				log.info("The new entitlement certificate file is: "+newCertFile);
				
				// assert that the productId from the pool matches the entitlement productId
				// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=650278 - jsefler 11/05/2010
				// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=806986 - jsefler 06/28/2012
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId1="650278"; 
				String bugId2="806986"; 
				try {if (invokeWorkaroundWhileBugIsOpen&&(BzChecker.getInstance().isBugOpen(bugId1)||BzChecker.getInstance().isBugOpen(bugId2))) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId1).toString()+" Bugzilla "+bugId1+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId1+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId1); log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId2).toString()+" Bugzilla "+bugId2+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId2+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId2);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Skipping assert that the productId from the pool matches the entitlement productId");
				} else {
				// END OF WORKAROUND
				EntitlementCert entitlementCert = getEntitlementCertFromEntitlementCertFile(newCertFile);
				File newCertKeyFile = getEntitlementCertKeyFileFromEntitlementCert(entitlementCert);
				Assert.assertEquals(entitlementCert.orderNamespace.productId, poolProductId, isSubpool?
						"New EntitlementCert productId '"+entitlementCert.orderNamespace.productId+"' matches originating Personal SubscriptionPool productId '"+poolProductId+"' after subscribing to the subpool.":
						"New EntitlementCert productId '"+entitlementCert.orderNamespace.productId+"' matches originating SubscriptionPool productId '"+poolProductId+"' after subscribing to the pool.");
				Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner, newCertFile.getPath()),"New EntitlementCert file exists after subscribing to SubscriptionPool '"+pool.poolId+"'.");
				Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner, newCertKeyFile.getPath()),"New EntitlementCert key file exists after subscribing to SubscriptionPool '"+pool.poolId+"'.");
				}
			}
		
			// assert that consumed ProductSubscriptions has NOT decreased
			List<ProductSubscription> afterProductSubscriptions = getCurrentlyConsumedProductSubscriptions();
			//this assertion was valid prior to bug Bug 801187 - collapse list of provided products for subscription-manager list --consumed
			//Assert.assertTrue(afterProductSubscriptions.size() >= beforeProductSubscriptions.size() && afterProductSubscriptions.size() > 0,
			//		"The list of currently consumed product subscriptions has increased (from "+beforeProductSubscriptions.size()+" to "+afterProductSubscriptions.size()+"), or has remained the same after subscribing (using poolID="+pool.poolId+") to pool: "+pool+"  Note: The list of consumed product subscriptions can remain the same when all the products from this subscription pool are a subset of those from a previously subscribed pool.");
			Assert.assertTrue(afterProductSubscriptions.size() == beforeProductSubscriptions.size()+1,
					"The list of currently consumed product subscriptions has increased by 1 (from "+beforeProductSubscriptions.size()+" to "+afterProductSubscriptions.size()+"), after subscribing to pool: "+pool);
		}
		
		return newCertFile;
	}
	/**
	 * @param pool
	 * @return null; WARNING this overloaded method ALWAYS RETURNS NULL (on purpose)!
	 */
	public File subscribeToSubscriptionPool(SubscriptionPool pool)  {
		return subscribeToSubscriptionPool(pool, null, null, null);
	}
	public File subscribeToSubscriptionPool(SubscriptionPool pool, String authenticator, String password, String serverUrl)  {
		return subscribeToSubscriptionPool(pool, null, authenticator, password, serverUrl);
	}
	
	/**
	 * subscribe to the given SubscriptionPool without asserting results
	 * @param pool
	 * @return the newly installed EntitlementCert file to the newly consumed ProductSubscriptions (null if there was a problem)
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public File subscribeToSubscriptionPool_(SubscriptionPool pool, String quantity) throws JSONException, Exception  {
		
//		String hostname = getConfFileParameter(rhsmConfFile, "hostname");
//		String port = getConfFileParameter(rhsmConfFile, "port");
//		String prefix = getConfFileParameter(rhsmConfFile, "prefix");
		
		log.info("Subscribing to subscription pool: "+pool);
		SSHCommandResult sshCommandResult = subscribe(null, null, pool.poolId, null, null, quantity, null, null, null, null, null, null, null);

		// get the serial of the entitlement that was granted from this pool
		//BigInteger serialNumber = CandlepinTasks.getOwnersNewestEntitlementSerialCorrespondingToSubscribedPoolId(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,SubscriptionManagerBaseTestScript.sm_serverUrl,getCurrentlyRegisteredOwnerKey(),pool.poolId);
		BigInteger serialNumber = CandlepinTasks.getConsumersNewestEntitlementSerialCorrespondingToSubscribedPoolId(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,getCurrentConsumerId(),pool.poolId);
		//Assert.assertNotNull(serialNumber, "Found the serial number of the entitlement that was granted after subscribing to pool id '"+pool.poolId+"'.");
		if (serialNumber==null) return null;
		File serialPemFile = new File(entitlementCertDir+File.separator+serialNumber+".pem");
		//Assert.assertEquals(RemoteFileTasks.testFileExists(sshCommandRunner, serialPemFile.getPath()),1, "Found the EntitlementCert file ("+serialPemFile+") that was granted after subscribing to pool id '"+pool.poolId+"'.");

		return serialPemFile;
	}
	public File subscribeToSubscriptionPool_(SubscriptionPool pool) throws JSONException, Exception  {
		return subscribeToSubscriptionPool_(pool,null);
	}
	
	//@Deprecated
	public void subscribeToSubscriptionPoolUsingProductId(SubscriptionPool pool) {
		log.warning("Subscribing to a Subscription Pool using --product Id has been removed in subscription-manager-0.71-1.el6.i686.  Forwarding this subscribe request to use --pool Id...");
		subscribeToSubscriptionPoolUsingPoolId(pool);
		
		/* jsefler 7/22/2010
		List<ProductSubscription> before = getCurrentlyConsumedProductSubscriptions();
		log.info("Subscribing to subscription pool: "+pool);
		subscribe(null, pool.productId, null, null, null);
		String stderr = sshCommandRunner.getStderr().trim();
		
		List<ProductSubscription> after = getCurrentlyConsumedProductSubscriptions();
		if (stderr.equals("This consumer is already subscribed to the product '"+pool.productId+"'.")) {
			Assert.assertTrue(after.size() == before.size() && after.size() > 0,
					"The list of currently consumed product subscriptions has remained the same (from "+before.size()+" to "+after.size()+") after subscribing (using productID="+pool.productId+") to pool: "+pool+"   Note: The list of consumed product subscriptions can remain the same when this product is already a subset from a previously subscribed pool.");
		} else {
			Assert.assertTrue(after.size() >= before.size() && after.size() > 0,
					"The list of currently consumed product subscriptions has increased (from "+before.size()+" to "+after.size()+"), or has remained the same after subscribing (using productID="+pool.productId+") to pool: "+pool+"  Note: The list of consumed product subscriptions can remain the same when this product is already a subset from a previously subscribed pool.");
			Assert.assertTrue(!getCurrentlyAvailableSubscriptionPools().contains(pool),
					"The available subscription pools no longer contains pool: "+pool);
		}
		*/
	}
	
	public void subscribeToSubscriptionPoolUsingPoolId(SubscriptionPool pool/*, boolean withPoolID*/) {
		subscribeToSubscriptionPool(pool);
		
		/* jsefler 11/22/2010
		if(withPoolID){
			log.info("Subscribing to pool with poolId: "+ pool.poolId);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --pool="+pool.poolId);
		}
		else{
			log.info("Subscribing to pool with productId: "+ pool.productId);
			sshCommandRunner.runCommandAndWait("subscription-manager-cli subscribe --product=\""+pool.productId+"\"");
		}
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size() > 0,
				"Successfully subscribed to pool with pool ID: "+ pool.poolId +" and pool name: "+ pool.subscriptionName);
		//TODO: add in more thorough product subscription verification
		// first improvement is to assert that the count of consumedProductIDs is at least one greater than the count of consumedProductIDs before the new pool was subscribed to.
		*/
	}
	
	public void subscribeToRegToken(String regtoken) {
		log.info("Subscribing to registration token: "+ regtoken);
		RemoteFileTasks.runCommandExpectingNoTracebacks(sshCommandRunner, "subscription-manager-cli subscribe --regtoken="+regtoken);
		Assert.assertTrue((getCurrentlyConsumedProductSubscriptions().size() > 0),
				"At least one entitlement consumed by regtoken subscription");
	}
	
	/**
	 * Individually subscribe to each of the currently available subscription pools one at a time 
	 * @return SubscriptionPools that were available for subscribing 
	 */
	public List <SubscriptionPool> subscribeToTheCurrentlyAvailableSubscriptionPoolsIndividually() {

		// individually subscribe to each available subscription pool
		List <SubscriptionPool> pools = getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : pools) {
			subscribeToSubscriptionPool(pool);
		}
		
		// assert
		assertNoAvailableSubscriptionPoolsToList(true, "Asserting that no available subscription pools remain after individually subscribing to them all.");
		return pools;
	}
	
	
	/**
	 * Collectively subscribe to the currently available subscription pools in one command call
	 * 
	 * @return SubscriptionPools that were available for subscribing 
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public List<SubscriptionPool> subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively() throws JSONException, Exception {
		
		// assemble a list of all the available SubscriptionPool ids
		List <String> poolIds = new ArrayList<String>();
		List <SubscriptionPool> poolsBeforeSubscribe = getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : poolsBeforeSubscribe) {
			poolIds.add(pool.poolId);
		}
		if (!poolIds.isEmpty()) subscribe(null,null, poolIds, null, null, null, null, null, null, null, null, null, null);
		
		// assert results when assumingRegisterType="system"
		if (currentlyRegisteredType==null || currentlyRegisteredType.equals(ConsumerType.system)) {
			assertNoAvailableSubscriptionPoolsToList(true, "Asserting that no available subscription pools remain after collectively subscribing to them all.");
			return poolsBeforeSubscribe;
		}
		
		// assert results when assumingRegisterType="candlepin"
		else if (currentlyRegisteredType.equals(ConsumerType.candlepin)) {
			List <SubscriptionPool> poolsAfterSubscribe = getCurrentlyAvailableSubscriptionPools();
			for (SubscriptionPool beforePool : poolsBeforeSubscribe) {
				boolean foundPool = false;
				for (SubscriptionPool afterPool : poolsAfterSubscribe) {
					if (afterPool.equals(beforePool)) {
						foundPool = true;
						
						// determine how much the quantity should have decremented
						int expectedDecrement = 1;
						String virt_only = CandlepinTasks.getPoolAttributeValue(currentlyRegisteredUsername, currentlyRegisteredPassword, candlepinUrl, afterPool.poolId, "virt_only");
						String virt_limit = CandlepinTasks.getPoolProductAttributeValue(currentlyRegisteredUsername, currentlyRegisteredPassword, candlepinUrl, afterPool.poolId, "virt_limit");
						if (virt_only!=null && Boolean.valueOf(virt_only) && virt_limit!=null) expectedDecrement += Integer.valueOf(virt_limit);	// the quantity consumed on a virt pool should be 1 (from the subscribe on the virtual pool itself) plus virt_limit (from the subscribe by the candlepin consumer on the physical pool)

						// assert the quantity has decremented;
						Assert.assertEquals(Integer.valueOf(afterPool.quantity).intValue(), Integer.valueOf(beforePool.quantity).intValue()-expectedDecrement,
								"The quantity of entitlements from subscription pool id '"+afterPool.poolId+"' has decremented by "+expectedDecrement+".");
						break;
					}
				}
				if (!foundPool) {
					Assert.fail("Could not find subscription pool "+beforePool+" listed after subscribing to it as a registered "+currentlyRegisteredType+" consumer.");
				}
			}
			return poolsBeforeSubscribe;
		}
		
		Assert.fail("Do not know how to assert subscribeToTheCurrentlyAvailableSubscriptionPoolsCollectively when registered as type="+currentlyRegisteredType);
		return poolsBeforeSubscribe;
	}
	/**
	 * @return list of all the currently available subscription pools that about to be subscribed
	 */
	public List<SubscriptionPool> subscribeToTheCurrentlyAllAvailableSubscriptionPoolsCollectively() {

		// assemble a list of all the available SubscriptionPool ids
		List<String> poolIds = new ArrayList<String>();
		List<SubscriptionPool> subscriptionPools = getCurrentlyAllAvailableSubscriptionPools();
		for (SubscriptionPool pool : subscriptionPools) poolIds.add(pool.poolId);

		if (!poolIds.isEmpty()) subscribe(null,null,poolIds, null, null, null, null, null,null,null,null, null, null);
		
		// assert
		assertNoAvailableSubscriptionPoolsToList(true,"Asserting that no available subscription pools remain after simultaneously subscribing to all available.");
		
		return subscriptionPools;
	}
	
	public void assertNoAvailableSubscriptionPoolsToList(boolean ignoreMuliEntitlementSubscriptionPools, String assertMsg) {
		boolean invokeWorkaroundWhileBugIsOpen = true;
		
		// TEMPORARY WORKAROUND FOR BUG
		invokeWorkaroundWhileBugIsOpen = false; // true;	// Status: CLOSED ERRATA	// Bug 613635 - “connection.UEPConnection instance “ displays while availability check
		try {String bugId="613635"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			Assert.assertContainsMatch(listAvailableSubscriptionPools().getStdout(),"^No available subscription pools to list$",assertMsg);
			return;
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		invokeWorkaroundWhileBugIsOpen = false; // true;	// Status: CLOSED ERRATA	// Bug 622839 - extraneous user hash code appears in stdout after executing list --available
		try {String bugId="622839"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			Assert.assertContainsMatch(listAvailableSubscriptionPools().getStdout(),"^No available subscription pools to list$",assertMsg);
			return;
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		invokeWorkaroundWhileBugIsOpen = false; // true;	// Status: CLOSED DUPLICATE of bug 623481	// Bug 623657 - extraneous self.conn output appears in stdout after executing list --available
		try {String bugId="623657"; if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			Assert.assertContainsMatch(listAvailableSubscriptionPools().getStdout(),"^No available subscription pools to list$",assertMsg);
			return;
		}
		// END OF WORKAROUND
		
		
		// determine which available pools are multi-entitlement pools
		List<SubscriptionPool> poolsAvailableExcludingMuliEntitlement = new ArrayList<SubscriptionPool>();
		List<SubscriptionPool> poolsAvailable = getCurrentlyAvailableSubscriptionPools();
		for (SubscriptionPool pool : poolsAvailable) {
			try {
				String authenticator = this.currentlyRegisteredUsername!=null?this.currentlyRegisteredUsername:candlepinAdminUsername;
				String password = this.currentlyRegisteredPassword!=null?this.currentlyRegisteredPassword:candlepinAdminPassword;
				if (!CandlepinTasks.isPoolProductMultiEntitlement(authenticator,password,candlepinUrl,pool.poolId)) {
					poolsAvailableExcludingMuliEntitlement.add(pool);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}
		}
		
		// assert
		if (ignoreMuliEntitlementSubscriptionPools) {
			Assert.assertEquals(poolsAvailableExcludingMuliEntitlement.size(),0,
					assertMsg+" (muti-entitlement pools were excluded.)");
		} else {
			Assert.assertEquals(poolsAvailable.size(),0,
					assertMsg+" (muti-entitlement pools were excluded.)");
			Assert.assertEquals(listAvailableSubscriptionPools().getStdout().trim(),
				"No available subscription pools to list",assertMsg);
		}
	}
	
	
	
	// unsubscribe module tasks ************************************************************
	/**
	 * @param poolIds TODO
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String unsubscribeCommand(Boolean all, List<BigInteger> serials, List<String> poolIds, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;							command += " unsubscribe";
		if (all!=null && all)									command += " --all";
		if (serials!=null)	for (BigInteger serial : serials)	command += " --serial="+serial;
		if (poolIds!=null)	for (String poolId : poolIds)		command += " --pool="+poolId;
		if (proxy!=null)										command += " --proxy="+proxy;
		if (proxyuser!=null)									command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)								command += " --proxypassword="+proxypassword;
		if (noproxy!=null)										command += " --noproxy="+noproxy;
		
		return command;
	}
	
	/**
	 * unsubscribe without asserting results
	 * @param poolIds TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unsubscribe_(Boolean all, List<BigInteger> serials, List<String> poolIds, String proxy, String proxyuser, String proxypassword, String noproxy) {

		// assemble the command
		String command = unsubscribeCommand(all, serials, poolIds, proxy, proxyuser, proxypassword, noproxy);	

		if (all!=null && all && serials==null) workaroundForBug844455();
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	/**
	 * unsubscribe without asserting results
	 * @param poolId TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unsubscribe_(Boolean all, BigInteger serial, String poolId, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		List<BigInteger> serials = serial==null?null:Arrays.asList(new BigInteger[]{serial});
		List<String> poolIds = poolId==null?null:Arrays.asList(new String[]{poolId});

		return unsubscribe_(all, serials, poolIds, proxy, proxyuser, proxypassword, noproxy);
	}
	
	/**
	 * unsubscribe and assert all results are successful
	 * @param poolIds TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unsubscribe(Boolean all, List<BigInteger> serials, List<String> poolIds, String proxy, String proxyuser, String proxypassword, String noproxy) {

		SSHCommandResult sshCommandResult = unsubscribe_(all, serials, poolIds, proxy, proxyuser, proxypassword, noproxy);
		
		// assert results
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the unsubscribe command indicates a success.");
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1287610"; 	// Bug 1287610 - yum message in output when FIPS is enabled
		try {if (redhatReleaseX.equals("7")&&isFipsEnabled&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the stderr assertion from unsubscribe on rhel '"+redhatReleaseXY+"' while FIPS bug '"+bugId+"' is open");
		} else
		// END OF WORKAROUND
		Assert.assertEquals(sshCommandResult.getStderr(), "", "Stderr from the unsubscribe.");
		return sshCommandResult;
	}
	
	/**
	 * unsubscribe and assert all results are successful
	 * @param poolId TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult unsubscribe(Boolean all, BigInteger serial, String poolId, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		List<BigInteger> serials = serial==null?null:Arrays.asList(new BigInteger[]{serial});
		List<String> poolIds = poolId==null?null:Arrays.asList(new String[]{poolId});
		
		return unsubscribe(all, serials, poolIds, proxy, proxyuser, proxypassword, noproxy);
	}
	
	/**
	 * unsubscribe from entitlement certificate serial and assert results
	 * @param serialNumber
	 * @return - false when no unsubscribe took place
	 */
	public boolean unsubscribeFromSerialNumber(BigInteger serialNumber) {
		String certFilePath = entitlementCertDir+"/"+serialNumber+".pem";
		String certKeyFilePath = entitlementCertDir+"/"+serialNumber+"-key.pem";
		File certFile = new File(certFilePath);
		boolean certFileExists = RemoteFileTasks.testExists(sshCommandRunner,certFilePath);
		if (certFileExists) Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner,certKeyFilePath),
				"Entitlement Certificate file with serial '"+serialNumber+"' ("+certFilePath+") and corresponding key file ("+certKeyFilePath+") exist before unsubscribing.");
		List<File> beforeEntitlementCertFiles = getCurrentEntitlementCertFiles();

		log.info("Attempting to unsubscribe from certificate serial: "+ serialNumber);
		SSHCommandResult result = unsubscribe_(false, serialNumber, null, null, null, null, null);
		
		// assert the results
		String expectedStdoutMsg;
		if (!certFileExists) {
			String regexForSerialNumber = serialNumber.toString();
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=639320 - jsefler 10/1/2010
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="639320"; 
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				regexForSerialNumber = "[\\d,]*";
			}
			// END OF WORKAROUND
			
			//Assert.assertContainsMatch(result.getStderr(), "Entitlement Certificate with serial number "+regexForSerialNumber+" could not be found.",
			//		"Stderr from an attempt to unsubscribe from Entitlement Certificate serial "+serialNumber+" that was not found in "+entitlementCertDir);
			//Assert.assertContainsMatch(result.getStdout(), "Entitlement Certificate with serial number "+regexForSerialNumber+" could not be found.",
			//		"Stdout from an attempt to unsubscribe from Entitlement Certificate serial "+serialNumber+" that was not found in "+entitlementCertDir);
			//Assert.assertEquals(result.getExitCode(), Integer.valueOf(255), "The unsubscribe should fail when its corresponding entitlement cert file ("+certFilePath+") does not exist.");
			expectedStdoutMsg = "Unsuccessfully unsubscribed serial numbers:";	// added by bug 867766
			expectedStdoutMsg = "Unsuccessfully removed serial numbers:";	// changed by bug 874749
			expectedStdoutMsg = "Serial numbers unsuccessfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
			if (isPackageVersion("subscription-manager", ">=", "1.17.8-1")) expectedStdoutMsg = "The entitlement server failed to remove these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
			Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from unsubscribe contains expected message: "+expectedStdoutMsg);
			if (isPackageVersion("subscription-manager", ">=", "1.16.6-1")) {	// commit 0d80caacf5e9483d4f10424030d6a5b6f472ed88 1285004: Adds check for access to the required manager capabilty
				//	[root@jsefler-6 ~]# subscription-manager remove --serial 8375727538260415740 --serial 2872676222813362535
				//	Serial numbers unsuccessfully removed at the server:
				//	   2872676222813362535
				//	   8375727538260415740
				String expectedStdoutRegex = expectedStdoutMsg+"(?:\\n   \\d+)*?\\n   "+serialNumber;
				Assert.assertContainsMatch(result.getStdout(), expectedStdoutRegex);
			} else {
				expectedStdoutMsg = "   Entitlement Certificate with serial number "+serialNumber+" could not be found.";
				expectedStdoutMsg = "   Entitlement Certificate with serial number '"+serialNumber+"' could not be found.";
				Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from unsubscribe contains expected message: "+expectedStdoutMsg);
			}
			// TEMPORARY WORKAROUND
			invokeWorkaroundWhileBugIsOpen = true;
			bugId="1287610"; 	// Bug 1287610 - yum message in output when FIPS is enabled
			try {if (redhatReleaseX.equals("7")&&isFipsEnabled&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Skipping the stderr assertion from unsubscribe on rhel '"+redhatReleaseXY+"' while FIPS bug '"+bugId+"' is open");
			} else
			// END OF WORKAROUND
			Assert.assertEquals(result.getStderr(),"", "Stderr from unsubscribe.");
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(1), "ExitCode from unsubscribe when the serial's entitlement cert file ("+certFilePath+") does not exist.");	// changed by bug 873791
			return false;
		}
		
		// assert the entitlement certFilePath is removed
		Assert.assertTrue(!RemoteFileTasks.testExists(sshCommandRunner,certFilePath),
				"Entitlement Certificate with serial '"+serialNumber+"' ("+certFilePath+") has been removed.");

		// assert the entitlement certKeyFilePath is removed
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=708362 - jsefler 08/25/2011
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="708362"; 
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		boolean assertCertKeyFilePathIsRemoved = true;
		if (invokeWorkaroundWhileBugIsOpen) log.warning("Skipping the assertion that the Entitlement Certificate key with serial '"+serialNumber+"' ("+certKeyFilePath+") has been removed while bug is open."); else
		// END OF WORKAROUND
		Assert.assertTrue(!RemoteFileTasks.testExists(sshCommandRunner,certKeyFilePath),
				"Entitlement Certificate key with serial '"+serialNumber+"' ("+certKeyFilePath+") has been removed.");

		// assert that only ONE entitlement cert file was removed
		List<File> afterEntitlementCertFiles = getCurrentEntitlementCertFiles();
		Assert.assertTrue(afterEntitlementCertFiles.size()==beforeEntitlementCertFiles.size()-1,
				"Only ONE entitlement certificate has been removed (count was '"+beforeEntitlementCertFiles.size()+"'; is now '"+afterEntitlementCertFiles.size()+"') after unsubscribing from serial: "+serialNumber);
		
		// assert that the other cert files remain unchanged
		/* CANNOT MAKE THIS ASSERT/ASSUMPTION ANYMORE BECAUSE REMOVAL OF AN ENTITLEMENT CAN AFFECT A MODIFIER PRODUCT THAT PROVIDES EXTRA CONTENT FOR THIS SERIAL (A MODIFIER PRODUCT IS ALSO CALLED EUS) 2/21/2011 jsefler
		if (!beforeEntitlementCertFiles.remove(certFile)) Assert.fail("Failed to remove certFile '"+certFile+"' from list.  This could be an automation logic error.");
		Assert.assertEquals(afterEntitlementCertFiles,beforeEntitlementCertFiles,"After unsubscribing from serial '"+serialNumber+"', the other entitlement cert serials remain unchanged");
		*/
		
		expectedStdoutMsg = "Successfully unsubscribed serial numbers:";	// added by bug 867766
		expectedStdoutMsg = "Successfully removed serial numbers:";	// changed by bug 874749
		expectedStdoutMsg = "Serial numbers successfully removed at the server:";	// changed by bug 895447 subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		if (isPackageVersion("subscription-manager", ">=", "1.17.8-1")) expectedStdoutMsg = "The entitlement server successfully removed these serial numbers:";	// commit f64d5a6b012f49bb4d6d6653441d4de9bf373660  1319678: Alter the return message for removing entitlements at server
		Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from unsubscribe contains expected message: "+expectedStdoutMsg);
		expectedStdoutMsg = "   "+serialNumber;	// added by bug 867766
		Assert.assertTrue(result.getStdout().contains(expectedStdoutMsg), "Stdout from unsubscribe contains expected message: "+expectedStdoutMsg);
		// TEMPORARY WORKAROUND
		invokeWorkaroundWhileBugIsOpen = true;
		bugId="1287610"; 	// Bug 1287610 - yum message in output when FIPS is enabled
		try {if (redhatReleaseX.equals("7")&&isFipsEnabled&&invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			log.warning("Skipping the stderr assertion from unsubscribe on rhel '"+redhatReleaseXY+"' while FIPS bug '"+bugId+"' is open");
		} else
		// END OF WORKAROUND
		Assert.assertEquals(result.getStderr(),"", "Stderr from unsubscribe.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from unsubscribe when the serial's entitlement cert file ("+certFilePath+") does exist.");	// added by bug 873791

		return true;
	}
	
	/**
	 * Unsubscribe from the given product subscription using its serial number.
	 * @param productSubscription
	 * @return - false when the productSubscription has already been unsubscribed at a previous time
	 */
	public boolean unsubscribeFromProductSubscription(ProductSubscription productSubscription) {
		
		log.info("Unsubscribing from product subscription: "+ productSubscription);
		boolean unsubscribed = unsubscribeFromSerialNumber(productSubscription.serialNumber);
		
		Assert.assertTrue(!getCurrentlyConsumedProductSubscriptions().contains(productSubscription),
				"The currently consumed product subscriptions does not contain product: "+productSubscription);

		return unsubscribed;
	}
	
	/**
	 * Issues a call to "subscription-manager unsubscribe --all" which will unsubscribe from
	 * all currently consumed product subscriptions and then asserts the list --consumed is empty.
	 */
	public void unsubscribeFromAllOfTheCurrentlyConsumedProductSubscriptions() {

		unsubscribe(true, (BigInteger)null, null, null, null, null, null);

		// assert that there are no product subscriptions consumed
		String expectedConsumedListMessage="No consumed subscription pools to list";
		if (isPackageVersion("subscription-manager", ">=", "1.20.2-1")) {	// commit da72dfcbbb2c3a44393edb9e46e1583d05cc140a
		    expectedConsumedListMessage="No consumed subscription pools were found.";
		}
		Assert.assertEquals(listConsumedProductSubscriptions().getStdout().trim(),
			expectedConsumedListMessage,"Successfully unsubscribed from all consumed products.");
		
		// assert that there are no entitlement cert files
		Assert.assertTrue(sshCommandRunner.runCommandAndWait("find "+entitlementCertDir+" -name '*.pem' | grep -v key.pem").getStdout().equals(""),
				"No entitlement cert files exist after unsubscribing from all subscription pools.");

		// assert that the yum redhat repo file is gone
		/* bad assert...  the repo file is present but empty
		Assert.assertTrue(RemoteFileTasks.testFileExists(sshCommandRunner, redhatRepoFile)==0,
				"The redhat repo file '"+redhatRepoFile+"' has been removed after unsubscribing from all subscription pools.");
		*/
	}
	
	/**
	 * Individually unsubscribe from each of the currently consumed product subscriptions.
	 * This will ultimately issue multiple calls to unsubscribe --serial SERIAL for each of the product subscriptions being consumed. 
	 */
	public void unsubscribeFromTheCurrentlyConsumedProductSubscriptionSerialsIndividually() {
		log.info("Unsubscribing from each of the currently consumed product subscription serials one at a time...");
		for(ProductSubscription sub : getCurrentlyConsumedProductSubscriptions())
			unsubscribeFromProductSubscription(sub);
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size()==0,
				"Currently no product subscriptions are consumed.");
		Assert.assertTrue(getCurrentEntitlementCertFiles().size()==0,
				"This machine has no entitlement certificate files.");			
	}
	
	/**
	 * Collectively unsubscribe from all of the currently consumed product subscriptions.
	 * This will ultimately issue a single call to unsubscribe --serial SERIAL1 --serial SERIAL2 --serial SERIAL3 for each of the product subscriptions being consumed. 
	 * @throws Exception 
	 */
	public SSHCommandResult unsubscribeFromTheCurrentlyConsumedProductSubscriptionSerialsCollectively() throws Exception {
		log.info("Unsubscribing from all of the currently consumed product subscription serials in one collective call...");
		List<BigInteger> serials = new ArrayList<BigInteger>();
	
		// THIS CREATES PROBLEMS WHEN MODIFIER ENTITLEMENTS ARE BEING CONSUMED; ENTITLEMENTS FROM MODIFIER POOLS COULD REMAIN AFTER THE COLLECTIVE UNSUBSCRIBE
		//for(ProductSubscription productSubscription : getCurrentlyConsumedProductSubscriptions()) serials.add(sub.serialNumber);
		
		// THIS AVOIDS PROBLEMS WHEN MODIFIER ENTITLEMENTS ARE BEING CONSUMED
		for(ProductSubscription productSubscription : getCurrentlyConsumedProductSubscriptions()) {
			EntitlementCert entitlementCert = getEntitlementCertCorrespondingToProductSubscription(productSubscription);
			JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,entitlementCert.id);
			String poolHref = jsonEntitlement.getJSONObject("pool").getString("href");
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,poolHref));
			String poolId = jsonPool.getString("id");
				
			if (CandlepinTasks.isPoolAModifier(this.currentlyRegisteredUsername, this.currentlyRegisteredPassword, poolId,  candlepinUrl)) {
				serials.add(0,productSubscription.serialNumber);	// serials to entitlements that modify others should be at the front of the list to be removed, otherwise they will get re-issued under a new serial number when the modified entitlement is removed first.
			} else {
				serials.add(productSubscription.serialNumber);
			}
		}
		
		// unsubscribe from all serials collectively
		SSHCommandResult result = unsubscribe(false,serials,null,null,null, null, null);
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size()==0,
				"Currently no product subscriptions are consumed.");
		Assert.assertTrue(getCurrentEntitlementCertFiles().size()==0,
				"This machine has no entitlement certificate files.");
		return result;
	}
	
	
	/**
	 * Collectively unsubscribe from all of the currently consumed serials (in newest to oldest order).
	 * This will ultimately issue a single call to unsubscribe --serial SERIAL1 --serial SERIAL2 --serial SERIAL3 (where each serial is listed in reverse order that they were granted). 
	 * 
	 * If there are no serials, then unsubscribe --all is called (should effectively do nothing).
	 * 
	 * @return SSHCommandResult result from the call to unsubscribe
	 */
	public SSHCommandResult unsubscribeFromTheCurrentlyConsumedSerialsCollectively() {
		log.info("Unsubscribing from all of the currently consumed serials in one collective call (in reverse order that they were granted)...");
		List<BigInteger> serials = new ArrayList<BigInteger>();
		
		// assemble the serials in reverse order that they were granted to avoid...
		// Runtime Error No row with the given identifier exists: [org.candlepin.model.PoolAttribute#8a99f98a46b4fa990146ba9494032318] at org.hibernate.UnresolvableObjectException.throwIfNull:64
		for (File serialPemFile : getCurrentEntitlementCertFiles("-t")) {
			EntitlementCert entitlementCert = getEntitlementCertFromEntitlementCertFile(serialPemFile);
			serials.add(entitlementCert.serialNumber);
		}
		
		// return unsubscribe --all when no serials are currently consumed
		if (serials.isEmpty()) return unsubscribe(true,serials,null,null,null, null, null); 
		
		// unsubscribe from all serials collectively
		SSHCommandResult result = unsubscribe(false,serials,null,null,null, null, null);
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size()==0,
				"Currently no product subscriptions are consumed.");
		Assert.assertTrue(getCurrentEntitlementCertFiles().size()==0,
				"This machine has no entitlement certificate files.");
		return result;
	}
	
	
	/**
	 * Collectively unsubscribe from all of the currently consumed product subscriptions.
	 * This will ultimately issue a single call to unsubscribe --pool POOLID1 --pool POOLID2 --pool POOLID3 for each of the product subscriptions being consumed. 
	 * @throws Exception 
	 */
	public SSHCommandResult unsubscribeFromTheCurrentlyConsumedProductSubscriptionPoolIdsCollectively() throws Exception {
		log.info("Unsubscribing from all of the currently consumed product subscription poolids in one collective call...");
		List<BigInteger> serials = new ArrayList<BigInteger>();
		List<String> poolIds = new ArrayList<String>();
	
		// THIS CREATES PROBLEMS WHEN MODIFIER ENTITLEMENTS ARE BEING CONSUMED; ENTITLEMENTS FROM MODIFIER POOLS COULD REMAIN AFTER THE COLLECTIVE UNSUBSCRIBE
		//for(ProductSubscription productSubscription : getCurrentlyConsumedProductSubscriptions()) serials.add(sub.serialNumber);
		
		// THIS AVOIDS PROBLEMS WHEN MODIFIER ENTITLEMENTS ARE BEING CONSUMED
		for(ProductSubscription productSubscription : getCurrentlyConsumedProductSubscriptions()) {
			EntitlementCert entitlementCert = getEntitlementCertCorrespondingToProductSubscription(productSubscription);
			JSONObject jsonEntitlement = CandlepinTasks.getEntitlementUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,entitlementCert.id);
			String poolHref = jsonEntitlement.getJSONObject("pool").getString("href");
			JSONObject jsonPool = new JSONObject(CandlepinTasks.getResourceUsingRESTfulAPI(this.currentlyRegisteredUsername,this.currentlyRegisteredPassword,candlepinUrl,poolHref));
			String poolId = jsonPool.getString("id");
				
			if (CandlepinTasks.isPoolAModifier(this.currentlyRegisteredUsername, this.currentlyRegisteredPassword, poolId,  candlepinUrl)) {
				serials.add(0,productSubscription.serialNumber);	// serials to entitlements that modify others should be at the front of the list to be removed, otherwise they will get re-issued under a new serial number when the modified entitlement is removed first.
				poolIds.add(0,productSubscription.poolId);
			} else {
				serials.add(productSubscription.serialNumber);
				poolIds.add(productSubscription.poolId);
			}
		}
		
		// unsubscribe from all poolIds collectively
		SSHCommandResult result = unsubscribe(false,null,poolIds,null,null, null, null);
		Assert.assertTrue(getCurrentlyConsumedProductSubscriptions().size()==0,
				"Currently no product subscriptions are consumed.");
		Assert.assertTrue(getCurrentEntitlementCertFiles().size()==0,
				"This machine has no entitlement certificate files.");
		return result;
	}
	
	// facts module tasks ************************************************************
	/**
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String factsCommand(Boolean list, Boolean update, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		// assemble the command
		String command = this.command;	command += " facts";	
		if (list!=null && list)			command += " --list";
		if (update!=null && update)		command += " --update";
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		return command;
	}
	
	
	/**
	 * facts without asserting results
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 */
	public SSHCommandResult facts_(Boolean list, Boolean update, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		String command =factsCommand(list, update, proxy, proxyuser, proxypassword, noproxy);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * @param list
	 * @param update
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 * @return
	 */
	public SSHCommandResult facts(Boolean list, Boolean update, String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = facts_(list, update, proxy, proxyuser, proxypassword, noproxy);

		// assert results for a successful facts
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the facts command indicates a success.");
		String regex = "";
		if (list!=null && list)	{
			regex=".*:.*";	// list of the current facts
			//Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(sshCommandResult.getStdout().trim(), regex), "The list of facts contains matches to regex '"+regex+"'.");
			List<String> facts = SubscriptionManagerCLITestScript.getSubstringMatches(sshCommandResult.getStdout().trim(), regex);
			Assert.assertTrue(facts.size()>1, "A list of facts matching regex '"+regex+"' was reported.");
		}
		if (update!=null && update)	{
			String expectedMsg="Successfully updated the system facts.";	// expectedMsg=getCurrentConsumerCert().consumerid;	// consumerid	// RHEL57 RHEL61
			Assert.assertTrue(sshCommandResult.getStdout().trim().contains(expectedMsg), "The facts update feedback contains expected message '"+expectedMsg+"'.");
		}
		
		return sshCommandResult; // from the facts command
	}
	
	
	
	// version module tasks ************************************************************
	
	/**
	 * @param noproxy TODO
	 * @return the command line syntax for calling this subscription-manager module with these options
	 */
	public String versionCommand(String proxy, String proxyuser, String proxypassword, String noproxy) {

		// assemble the command
		String command = this.command;	command += " version";	
		if (proxy!=null)				command += " --proxy="+proxy;
		if (proxyuser!=null)			command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)		command += " --proxypassword="+proxypassword;
		if (noproxy!=null)				command += " --noproxy="+noproxy;
		
		return command;
	}
	
	/**
	 * version without asserting results
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 * @return result of the command line call to subscription-manager version
	 */
	public SSHCommandResult version_(String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		String command = versionCommand(proxy,proxyuser,proxypassword, noproxy);
		
		// run command without asserting results
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		return sshCommandResult;
	}
	
	/**
	 * version with asserting results
	 * @param proxy TODO
	 * @param proxyuser TODO
	 * @param proxypassword TODO
	 * @param noproxy TODO
	 * @return result of the command line call to subscription-manager version
	 */
	public SSHCommandResult version(String proxy, String proxyuser, String proxypassword, String noproxy) {
		
		SSHCommandResult sshCommandResult = version_(proxy, proxyuser, proxypassword, noproxy);

		// assert results for a successful version
		Assert.assertEquals(sshCommandResult.getExitCode(), Integer.valueOf(0), "The exit code from the version command indicates a success.");
		
		return sshCommandResult; // from the version command
	}
	
	
	
	
	
	
	
	
	
	
	
//	public boolean areAllRequiredTagsInContentNamespaceProvidedByProductCerts(ContentNamespace contentNamespace, List<ProductCert> productCerts) {
//
//		// get all of the provided tags from the productCerts
//		List<String> providedTags = new ArrayList<String>();
//		for (ProductCert productCert : productCerts) {
//			for (ProductNamespace productNamespace : productCert.productNamespaces) {
//				if (productNamespace.providedTags!=null) {
//					for (String providedTag : productNamespace.providedTags.split("\\s*,\\s*")) {
//						providedTags.add(providedTag);
//					}
//				}
//			}
//		}
//		
//		// get all of the required tags from the contentNamespace
//		List<String> requiredTags = new ArrayList<String>();
//		if (contentNamespace.requiredTags!=null) {
//			for (String requiredTag : contentNamespace.requiredTags.split("\\s*,\\s*")) {
//				requiredTags.add(requiredTag);
//			}
//		}
//		
//		// are ALL of the requiredTags provided?  Note: true is returned (and should be) when requiredTags.isEmpty()
//		return providedTags.containsAll(requiredTags);
//	}
	public boolean areAllRequiredTagsInContentNamespaceProvidedByProductCerts(ContentNamespace contentNamespace, List<ProductCert> productCerts) {
		return areAllRequiredTagsProvidedByProductCerts(contentNamespace.requiredTags, productCerts);
	}
	
	public boolean isArchCoveredByArchesInContentNamespace(String arch, ContentNamespace contentNamespace) {
		List<String> contentNamespaceArches = new ArrayList<String>();
		contentNamespaceArches.addAll(Arrays.asList(contentNamespace.arches.trim().split(" *, *")));	// Note: the arch attribute can be a comma separated list of values
		if (contentNamespaceArches.contains("x86")) {contentNamespaceArches.addAll(Arrays.asList("i386","i486","i586","i686"));}  // Note: x86 is a general arch to cover all 32-bit intel microprocessors 
		if (contentNamespaceArches.contains("ALL")) return true;
		if (contentNamespaceArches.contains("All")) return true;
		if (contentNamespaceArches.contains("all")) return true;
		return contentNamespaceArches.contains(arch);
	}
	
	public boolean areAllRequiredTagsProvidedByProductCerts(String requiredTagsAsString, List<ProductCert> productCerts) {
		// same some time...  if requiredTagsAsString is null, then effectively the requiredTags are provided by any list of product certs
		if (requiredTagsAsString==null) return true;
		
		// get all of the provided tags from the productCerts
		List<String> providedTags = new ArrayList<String>();
		for (ProductCert productCert : productCerts) {
			if (productCert.productNamespace.providedTags!=null) {
				for (String providedTag : productCert.productNamespace.providedTags.split("\\s*,\\s*")) {
					providedTags.add(providedTag);
				}
			}
		}
		
		// get all of the required tags from the contentNamespace
		List<String> requiredTags = new ArrayList<String>();
		if (requiredTagsAsString!=null) {
			for (String requiredTag : requiredTagsAsString.split("\\s*,\\s*")) {
				if (!requiredTag.isEmpty()) requiredTags.add(requiredTag);
			}
		}
		
		// are ALL of the requiredTags provided?  Note: true is returned (and should be) when requiredTags.isEmpty()
		return providedTags.containsAll(requiredTags);
	}
	
	public boolean isPackageInstalled(String pkg) {
		// [root@dell-pe2800-01 ~]# rpm -q subscription-manager-migration-data
		// package subscription-manager-migration-data is not installed
		// [root@dell-pe2800-01 ~]# echo $?
		// 1
		// [root@dell-pe2800-01 ~]# rpm -q subscription-manager
		// subscription-manager-0.98.14-1.el5
		// [root@dell-pe2800-01 ~]# echo $?
		// 0
		// [root@dell-pe2800-01 ~]# 

		return sshCommandRunner.runCommandAndWait("rpm -q "+pkg).getExitCode()==0? true:false;
	}
	
	/**
	 * Assert that the given entitlement certs are displayed in the stdout from "yum repolist all".
	 * @param entitlementCerts
	 */
	public void assertEntitlementCertsInYumRepolist(List<EntitlementCert> entitlementCerts, boolean areReported) {
		// # yum repolist all
		//	Loaded plugins: refresh-packagekit, rhnplugin, rhsmplugin
		//	Updating Red Hat repositories.
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	http://redhat.com/foo/path/never/repodata/repomd.xml: [Errno 14] HTTP Error 404 : http://www.redhat.com/foo/path/never/repodata/repomd.xml 
		//	Trying other mirror.
		//	repo id                      repo name                                                      status
		//	always-enabled-content       always-enabled-content                                         disabled
		//	content-label                content                                                        disabled
		//	never-enabled-content        never-enabled-content                                          enabled: 0
		//	rhel-beta                    Red Hat Enterprise Linux 5.90Workstation Beta - x86_64         disabled
		//	rhel-beta-debuginfo          Red Hat Enterprise Linux 5.90Workstation Beta - x86_64 - Debug disabled
		//	rhel-beta-optional           Red Hat Enterprise Linux 5.90Workstation Beta (Optional) - x86 disabled
		//	rhel-beta-optional-debuginfo Red Hat Enterprise Linux 5.90Workstation Beta (Optional) - x86 disabled
		//	rhel-beta-optional-source    Red Hat Enterprise Linux 5.90Workstation Beta (Optional) - x86 disabled
		//	rhel-beta-source             Red Hat Enterprise Linux 5.90Workstation Beta - x86_64 - Sourc disabled
		//	rhel-latest                  Latest RHEL 6                                                  enabled: 0
		//	repolist: 0
		
		// [root@jsefler-itclient01 product]# yum repolist all
		//	Loaded plugins: pidplugin, refresh-packagekit, rhnplugin, rhsmplugin
		//	Updating Red Hat repositories.
		//	INFO:repolib:repos updated: 0
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms                                                                         | 4.0 kB     00:00     
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms-updates                                                                 |  951 B     00:00     
		//	repo id                                                                        repo name                                           status
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms                        Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms-updates                Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms               Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms-updates       Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-rpms                     Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-rpms-updates             Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-source-rpms              Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-source-rpms-updates      Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms                              Red Hat Enterprise Linux 6 Entitlement Alpha (RPMs) enabled: 3,394
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms-updates                      Red Hat Enterprise Linux 6 Entitlement Alpha (RPMs) enabled:     0
		//	red-hat-enterprise-linux-6-entitlement-alpha-source-rpms                       Red Hat Enterprise Linux 6 Entitlement Alpha (Sourc disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-source-rpms-updates               Red Hat Enterprise Linux 6 Entitlement Alpha (Sourc disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-debug-rpms          Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-debug-rpms-updates  Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-rpms                Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-rpms-updates        Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-source-rpms         Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-supplementary-source-rpms-updates Red Hat Enterprise Linux 6 Entitlement Alpha - Supp disabled
		//	repolist: 3,394
		
		List<ProductCert> currentProductCerts = this.getCurrentProductCerts();
		
		// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=697087 - jsefler 04/27/2011
		if (this.redhatRelease.contains("release 5")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="697087"; 
			// NOTE: LET'S MAKE THIS A PERMANENT WORKAROUND FOR THIS METHOD
			// try {if (invokeWorkaroundWhileBugIsOpen/*&&BzChecker.getInstance().isBugOpen(bugId)*/) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				
				List<String> yumRepoListAll			= this.getYumRepolist("all");
				List<String> yumRepoListEnabled		= this.getYumRepolist("enabled");
				List<String> yumRepoListDisabled	= this.getYumRepolist("disabled");
				
		 		for (EntitlementCert entitlementCert : entitlementCerts) {
		 			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
		 				if (!contentNamespace.type.equalsIgnoreCase("yum")) continue;
		 				if (areReported && areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts)) {
							if (contentNamespace.enabled) {
								Assert.assertTrue(yumRepoListEnabled.contains(contentNamespace.label),
										"Yum repolist enabled includes repo id/label '"+contentNamespace.label+"' that comes from entitlement cert "+entitlementCert.id+"'s content namespace: "+contentNamespace);
							} else {
								Assert.assertTrue(yumRepoListDisabled.contains(contentNamespace.label),
										"Yum repolist disabled includes repo id/label '"+contentNamespace.label+"' that comes from entitlement cert "+entitlementCert.id+"'s content namespace: "+contentNamespace);
							}
		 				}
						else
							Assert.assertFalse(yumRepoListAll.contains(contentNamespace.label),
									"Yum repolist all excludes repo id/label '"+contentNamespace.label+"'.");
			 		}
		 		}
		 		return;
			}
		}
		// END OF WORKAROUND
		
		
		// assert all of the entitlement certs are reported in the stdout from "yum repolist all"
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");
		List<String> yumRepolistAll = getYumRepolist("all");
 		for (EntitlementCert entitlementCert : entitlementCerts) {
 			for (ContentNamespace contentNamespace : entitlementCert.contentNamespaces) {
 				
 				// Note: When the repo id and repo name are really long, the repo name in the yum repolist all gets crushed (hence the reason for .* in the regex)
 				boolean isReported = yumRepolistAll.contains(contentNamespace.label.trim());
 				
				boolean areAllRequiredTagsInstalled = areAllRequiredTagsInContentNamespaceProvidedByProductCerts(contentNamespace,currentProductCerts);
				if (!contentNamespace.type.equalsIgnoreCase("yum")) {
					Assert.assertTrue(!isReported, "ContentNamespace label '"+contentNamespace.label.trim()+"' from EntitlementCert '"+entitlementCert.serialNumber+"' is NOT reported in yum repolist all since its type '"+contentNamespace.type+"' is non-yum.");
				} else if (areReported && areAllRequiredTagsInstalled) {
					Assert.assertTrue(isReported, "ContentNamespace label '"+contentNamespace.label.trim()+"' from EntitlementCert '"+entitlementCert.serialNumber+"' is reported in yum repolist all.");
				} else {
					Assert.assertTrue(!isReported, "ContentNamespace label '"+contentNamespace.label.trim()+"' from EntitlementCert '"+entitlementCert.serialNumber+"' is NOT reported in yum repolist all"+((areReported&&!areAllRequiredTagsInstalled)?" since all its required tags '"+contentNamespace.requiredTags+"' are NOT found among the currently installed product certs.":"."));
				}
 			}
 		}

		// assert that the sshCommandRunner.getStderr() does not contains an error on the entitlementCert.download_url e.g.: http://redhat.com/foo/path/never/repodata/repomd.xml: [Errno 14] HTTP Error 404 : http://www.redhat.com/foo/path/never/repodata/repomd.xml 
		// FIXME EVENTUALLY WE NEED TO UNCOMMENT THIS ASSERT
		//Assert.assertContainsNoMatch(result.getStderr(), "HTTP Error \\d+", "HTTP Errors were encountered when runnning yum repolist all.");
	}
	
	/**
	 * @param options [all|enabled|disabled] [--option=...]
	 * @return array of repo labels returned from a call to yum repolist [options]
	 */
	public ArrayList<String> getYumRepolist(String options){
		if (options==null) options="";
		ArrayList<String> repoList = new ArrayList<String>();
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");
		sshCommandRunner.runCommandAndWait("yum repolist "+options+" --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
				
		// TEMPORARY WORKAROUND FOR BUG
		if (this.redhatReleaseX.equals("5")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="697087"; // Bug 697087 - yum repolist is not producing a list when one of the repo baseurl causes a forbidden 403
			try {if (invokeWorkaroundWhileBugIsOpen && BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				
				// avoid "yum repolist" and assemble the list of repos directly from the redhat repo file
				List<YumRepo> yumRepoList =   getCurrentlySubscribedYumRepos();
				for (YumRepo yumRepo : yumRepoList) {
					if		(options.startsWith("all"))													repoList.add(yumRepo.id);
					else if (options.startsWith("enabled")	&& yumRepo.enabled.equals(Boolean.TRUE))	repoList.add(yumRepo.id);
					else if (options.startsWith("disabled")	&& yumRepo.enabled.equals(Boolean.FALSE))	repoList.add(yumRepo.id);
					else if (options.equals("")				&& yumRepo.enabled.equals(Boolean.TRUE))	repoList.add(yumRepo.id);
				}
				sshCommandRunner.runCommandAndWait("yum repolist "+options+" --disableplugin=rhnplugin"); // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
				return repoList;
			}
		}
		// END OF WORKAROUND
		
//		// TEMPORARY WORKAROUND FOR BUG
//		if (this.redhatReleaseX.equals("7") && (options.startsWith("all") || options.startsWith("disabled"))) {
//			boolean invokeWorkaroundWhileBugIsOpen = true;
//			String bugId="905546"; // Bug 905546 - yum repolist all|disabled is throwing a traceback 
//			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
//			if (invokeWorkaroundWhileBugIsOpen) {
//				throw new SkipException("There is no workaround for yum repolist "+options+" bug "+bugId+".");
//			}
//		}
//		// END OF WORKAROUND
		
		// WARNING: DO NOT MAKE ANYMORE CALLS TO sshCommandRunner.runCommand* DURING EXECUTION OF THE REMAINDER OF THIS METHOD.
		// getYumRepolistPackageCount() ASSUMES sshCommandRunner.getStdout() CAME FROM THE CALL TO yum repolist

		// Example sshCommandRunner.getStdout()
		//	[root@jsefler-itclient01 product]# yum repolist all
		//	Loaded plugins: pidplugin, refresh-packagekit, rhnplugin, rhsmplugin
		//	Updating Red Hat repositories.
		//	INFO:repolib:repos updated: 0
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms                                                                         | 4.0 kB     00:00     
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms-updates                                                                 |  951 B     00:00     
		//	repo id                                                                        repo name                                           status
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms                        Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms-updates                Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms               Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms-updates       Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	repolist: 3,394
		
		//	[root@athlon6 ~]# yum repolist enabled --disableplugin=rhnplugin
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating certificate-based repositories.
		//	repolist: 0
		
		//	[root@localhost ~]# yum repolist all
		//	Loaded plugins: product-id, subscription-manager
		//	This system is receiving updates from Red Hat Subscription Management.
		//	repo id                         repo name                                              status
		//	rhel-7-public-beta-debug-rpms   Red Hat Enterprise Linux 7 Public Beta (Debug RPMs)    disabled
		//	!rhel-7-public-beta-rpms        Red Hat Enterprise Linux 7 Public Beta (RPMs)          enabled: 9,497
		//	rhel-7-public-beta-source-rpms  Red Hat Enterprise Linux 7 Public Beta (Source RPMs)   disabled
		//	repolist: 9,497
		return getYumRepolistFromSSHCommandResult(new SSHCommandResult(sshCommandRunner.getExitCode(), sshCommandRunner.getStdout(), sshCommandRunner.getStderr()));
	}
	public ArrayList<String> getYumRepolistFromSSHCommandResult(SSHCommandResult yumRepoListResult){
		ArrayList<String> repoList = new ArrayList<String>();
		String[] availRepos = yumRepoListResult.getStdout().split("\\n");
		int repolistStartLn = 0;
		int repolistEndLn = 0;
		for(int i=0;i<availRepos.length;i++)
			if (availRepos[i].startsWith("repo id")) // marks the start of the list
				repolistStartLn = i + 1;
			else if (availRepos[i].startsWith("repolist:")) // marks the end of the list
				repolistEndLn = i;
		if (repolistStartLn>0)
			for(int i=repolistStartLn;i<repolistEndLn;i++)
				repoList.add(availRepos[i].split(" ")[0]);
		
		// the repo id may be appended with a /$releasever/$basearch suffix on RHEL7; strip it off!
		// see NOTABUG Bug 905544 - yum repolist is including /$releasever/$basearch as a suffix to the repo id
		for (int i=0; i<repoList.size(); i++) repoList.set(i, repoList.get(i).split("/")[0]);
		
		// the repo id may be prefixed with an exclamation point on RHEL7; strip it off!
		// Explanation from James Antill
		//	 Damn, I was sure I'd documented this but it's not there :(.
		//
		//	 It was added around the same time as "metadata_expire_filter" because
		//	after that feature was added if you just did "simple" yum commands then
		//	yum wouldn't trigger new metadata downloads.
		//	 In repolist the '!' signifies that new metadata would have been checked
		//	for, if metadata_expire_filter=never (in other words the cache check for
		//	the repo. is older than the metadata_expire time for the repo.). But it
		//	hasn't, because of metadata_expire_filter, hence "not" before the repo.
		//
		//	 /me off to write that again in man page.
		for (int i=0; i<repoList.size(); i++) repoList.set(i, repoList.get(i).replaceFirst("^!", ""));
		
		return repoList;
	}
	
	
	/**
	 * @param options [all|enabled|disabled] [--option=...]
	 * @return the value reported at the bottom of a call to yum repolist [options] (repolist: value)
	 */
	public Integer getYumRepolistPackageCount(String options){
		getYumRepolist(options);

		// Example sshCommandRunner.getStdout()
		//	[root@jsefler-itclient01 product]# yum repolist all
		//	Loaded plugins: pidplugin, refresh-packagekit, rhnplugin, rhsmplugin
		//	Updating Red Hat repositories.
		//	INFO:repolib:repos updated: 0
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms                                                                         | 4.0 kB     00:00     
		//	red-hat-enterprise-linux-6-entitlement-alpha-rpms-updates                                                                 |  951 B     00:00     
		//	repo id                                                                        repo name                                           status
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms                        Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-debug-rpms-updates                Red Hat Enterprise Linux 6 Entitlement Alpha (Debug disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms               Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	red-hat-enterprise-linux-6-entitlement-alpha-optional-debug-rpms-updates       Red Hat Enterprise Linux 6 Entitlement Alpha - Opti disabled
		//	repolist: 3,394
		
		// Example sshCommandRunner.getStderr()
		//	INFO:rhsm-app.repolib:repos updated: 63
		//	https://cdn.redhat.com/FOO/content/beta/rhel/server/6/6Server/x86_64/os/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 403"
		//	https://cdn.redhat.com/content/beta/rhel/client/6/x86_64/supplementary/source/SRPMS/repodata/repomd.xml: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404"

		Assert.assertTrue(!sshCommandRunner.getStderr().contains("The requested URL returned error:"),"The requested URL did NOT return an error.");
		
		// parse out the value from repolist: value
		String regex="repolist:(.*)";
		
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(sshCommandRunner.getStdout());
		//Assert.assertTrue(matcher.find(),"Found fact "+factName);
		if (!matcher.find()) {
			log.warning("Did not find repolist package count.");
			return null;
		}
		
		Integer packageCount = Integer.valueOf(matcher.group(1).replaceAll(",","").trim());

		return packageCount;
	}
	
	
	//@Deprecated	// replaced by public ArrayList<String> getYumListAvailable (String options)
	public ArrayList<String> getYumListOfAvailablePackagesFromRepo (String repoLabel) {
		if (true) return getYumListAvailable("--disablerepo=* --enablerepo="+repoLabel);
		// the deprecated implementation of this method follows...
		
		ArrayList<String> packages = new ArrayList<String>();
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");

		int min = 5;
		log.fine("Using a timeout of "+min+" minutes for next ssh command...");
		//SSHCommandResult result = sshCommandRunner.runCommandAndWait("yum list available",Long.valueOf(min*60000));
		SSHCommandResult result = sshCommandRunner.runCommandAndWait("yum list available --disablerepo=* --enablerepo="+repoLabel+" --disableplugin=rhnplugin",Long.valueOf(min*60000));  // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError

		// Example result.getStdout()
		//xmltex.noarch                             20020625-16.el6                      red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//xmlto.x86_64                              0.0.23-3.el6                         red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//xmlto-tex.noarch                          0.0.23-3.el6                         red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//xorg-x11-apps.x86_64                      7.4-10.el6                           red-hat-enterprise-linux-6-entitlement-alpha-rpms

		String regex="(\\S+) +(\\S+) +"+repoLabel+"$";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		if (!matcher.find()) {
			log.fine("Did NOT find any available packages from repoLabel: "+repoLabel);
			return packages;
		}

		// assemble the list of packages and return them
		do {
			packages.add(matcher.group(1)); // group(1) is the pkg,  group(2) is the version
		} while (matcher.find());
		return packages;		
	}
	
//	public ArrayList<String> yumListAvailable (String disableplugin, String disablerepo, String enablerepo, String globExpression) {
	/**
	 * @param options
	 * @return array of packages returned from a call to yum list available [options]
	 */
	public ArrayList<String> getYumListAvailable (String options) {
		ArrayList<String> packages = new ArrayList<String>();
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");
		if (options==null) options="";

//		String							command  = "yum list available";
//		if (disableplugin!=null)		command += " --disableplugin="+disableplugin;
//		if (disablerepo!=null)			command += " --disablerepo="+disablerepo;
//		if (enablerepo!=null)			command += " --enablerepo="+enablerepo;
//		if (globExpression!=null)		command += " "+globExpression;
		String							command  = "yum list available "+options+" --disableplugin=rhnplugin"; // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		
		// execute the yum command to list available packages
		int min = 5;
		log.fine("Using a timeout of "+min+" minutes for next ssh command...");
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command,Long.valueOf(min*60000));
		
		// Example result
		//	FINE: ssh root@ibm-x3550m3-13.lab.eng.brq.redhat.com yum list available --disablerepo=rhel-5-workstation-desktop-rpms selinux-policy-mls.noarch --disableplugin=rhnplugin
		//	FINE: Stdout: 
		//	Loaded plugins: product-id, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	FINE: Stderr: 
		//	This system is receiving updates from Red Hat Subscription Management.
		//	https://cdn.rcm-qa.redhat.com/content/dist/rhel/workstation/5/5Client/x86_64/os/repodata/repomd.xml: [Errno 14] HTTP Error 500: Internal Server Error
		//	Trying other mirror.
		//	Error: Cannot retrieve repository metadata (repomd.xml) for repository: rhel-5-workstation-rpms. Please verify its path and try again
		//	FINE: ExitCode: 1
		if (result.getStderr().contains("Error 500: Internal Server Error")) {
			log.warning(result.getStderr());
			Assert.fail("Encountered an Internal Server Error while running: "+command);
		}
		
		// Example result.getStderr() 
		//	INFO:repolib:repos updated: 0
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	Error: No matching Packages to list
		if (result.getStderr().contains("Error: No matching Packages to list")) {
			log.info("No matching Packages to list from: "+command);
			return packages;
		}
		
		// Example result.getStdout()
		//  Loaded plugins: product-id, refresh-packagekit, subscription-manager
		//  No plugin match for: rhnplugin
		//  Updating certificate-based repositories.
		//  Available Packages
		//	xmltex.noarch                 20020625-16.el6     red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//	xmlto.x86_64                  0.0.23-3.el6        red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//	xmlto-tex.noarch              0.0.23-3.el6        red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//	xorg-x11-apps.x86_64          7.4-10.el6          red-hat-enterprise-linux-6-entitlement-alpha-rpms
		//	pacemaker-libs-devel.i686     1.1.7-6.el6         rhel-ha-for-rhel-6-server-rpms
		//	pacemaker-libs-devel.x86_64   1.1.7-6.el6         rhel-ha-for-rhel-6-server-rpms
		//	perl-Net-Telnet.noarch        3.03-11.el6         rhel-ha-for-rhel-6-server-rpms
		//	pexpect.noarch                2.3-6.el6           rhel-ha-for-rhel-6-server-rpms
		//	python-repoze-what-plugins-sql.noarch
		//	                              1.0-0.6.rc1.el6     rhel-ha-for-rhel-6-server-rpms
		//	python-repoze-what-quickstart.noarch
		//	                              1.0.1-1.el6         rhel-ha-for-rhel-6-server-rpms
		//	python-repoze-who-friendlyform.noarch
		//	                              1.0-0.3.b3.el6      rhel-ha-for-rhel-6-server-rpms
		//	python-repoze-who-plugins-sa.noarch
		//	                              1.0-0.4.rc1.el6     rhel-ha-for-rhel-6-server-rpms
		//	python-suds.noarch            0.4.1-3.el6         rhel-ha-for-rhel-6-server-rpms
		//	python-tw-forms.noarch        0.9.9-1.el6         rhel-ha-for-rhel-6-server-rpms
		//	resource-agents.x86_64        3.9.2-12.el6_3.2    rhel-ha-for-rhel-6-server-rpms
		//	cluster-cim.x86_64                  0.12.1-8.el5_9
		//	        rhel-ha-for-rhel-5-server-rpms
		//	cluster-snmp.x86_64                 0.12.1-8.el5_9
		//	        rhel-ha-for-rhel-5-server-rpms
		//	ipvsadm.x86_64                      1.24-13.el5   rhel-ha-for-rhel-5-server-rpms

		String availablePackadesTable = result.getStdout();	String prefix = "Available Packages";
		if (availablePackadesTable.contains(prefix)) {
			availablePackadesTable = availablePackadesTable.substring(availablePackadesTable.indexOf(prefix)+prefix.length(), availablePackadesTable.length()).trim();	// strip leading info before the list of "Availabile Packages"
		}
		
		//if (enablerepo==null||enablerepo.equals("*")) enablerepo="(\\S+)";
		//String regex="^(\\S+) +(\\S+) +"+enablerepo+"$";
		//String regex="^(\\S+) +(\\S+) +(\\S+)$";	// assume all the packages are on a line with three words
		//String regex="^(\\S+)(?:\\n)? +(\\S+) +(\\S+)$";	// works when the second and third word are on the next line, but not just the third 
		String regex="^(\\S+)(?:\\n)? +(\\S+)(?: +(\\S+)$|\\n +(\\S+)$)";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(availablePackadesTable);
		if (!matcher.find()) {
			log.info("Did NOT find any available packages from: "+command);
			return packages;
		}

		// assemble the list of packages and return them
		do {
			packages.add(matcher.group(1)); // group(1) is the pkg,  group(2) is the version,  group(3) is the repo
		} while (matcher.find());
		
		// flip the packages since the ones at the end of the list are usually easier to install 
		ArrayList<String> packagesCloned = (ArrayList<String>) packages.clone(); packages.clear();
		for (int p=packagesCloned.size()-1; p>=0; p--) packages.add(packagesCloned.get(p));

		return packages;
	}
	
	
	
	public String getYumPackageInfo(String pkg, String info) {
		
		String command = "yum info "+pkg;
		if (info!=null) command += " | grep \""+info+"\""; 
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		
		//	[root@jsefler-7 ~]# yum info ghostscript | grep "From repo"
		//	From repo   : rhel-7-server-eus-rpms
		if (info!=null) return result.getStdout().split(":")[1].trim();
		return result.getStdout().trim();
	}
	
	
	public void yumConfigManagerEnableRepos(List<String> repos, String options) {
		
		// use yum-config-manager to enable all the requested repos
		if (options==null) options="";	// optional options like --disablerepo=beaker*
		String command = "yum-config-manager "+options+" --enable";
		for (String repo : repos) command += " "+repo;
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from command '"+command+"'.");
		
		if (this.isPackageVersion("subscription-manager", ">=", "1.20.1-1")) {	// commit afbc7fd31a27c7eba26fe4b0cc6840228678bfa8	RFE Bug 1329349: Add subscription-manager plugin to yum-config-manager
			//	FINE: ssh root@jsefler-rhel7.usersys.redhat.com yum-config-manager  --enable rhel-7-server-openstack-7.0-tools-source-rpms rhel-7-server-optional-beta-rpms
			//	FINE: Stdout: 
			//	Loaded plugins: langpacks, product-id, subscription-manager
			//	============
			Assert.assertTrue(SubscriptionManagerCLITestScript.doesStringContainMatches(result.getStdout(),"Loaded plugins: .*subscription-manager"),"yum-config-manager loaded plugins includes subscription-manager");
		}
	}
	
	
	/**
	 * Disable all of the repos in /etc/yum.repos.d
	 * NOTE: On RHEL5, yum-utils must be installed first.
	 */
	public void yumDisableAllRepos() {
		yumDisableAllRepos(null);
	}
	
	/**
	 * Disable all of the repos in /etc/yum.repos.d
	 * NOTE: On RHEL5, yum-utils must be installed first.
	 * @param options - any additional options that you want appended when calling "yum repolist enabled" and "yum-config-manager --disable REPO"
	 */
	public void yumDisableAllRepos(String options) {
		
		// use brute force to disable all repos
		if (redhatReleaseX.equals("5")) {	// yum-config-manager does not exist on rhel5
			for (String repoFilepath : Arrays.asList(sshCommandRunner.runCommandAndWait("find /etc/yum.repos.d/ -name '*.repo'").getStdout().trim().split("\n"))) {
				if (repoFilepath.isEmpty()) continue;
				updateConfFileParameter(repoFilepath, "enabled", "0");
			}
			return;
		}
		
		// use yum-config-manager to disable all the currently enabled repos
		if (options==null) options="";
		for (String repo : getYumRepolist(("enabled"+" "+options).trim())) {
			String command = ("yum-config-manager --disable "+repo+" "+options).trim();
			SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from command '"+command+"'.");
		}
	}
	
	public ArrayList<String> yumGroupList (String Installed_or_Available, String options) {
		ArrayList<String> groups = new ArrayList<String>();
		sshCommandRunner.runCommandAndWaitWithoutLogging("killall -9 yum");

		String command = "yum grouplist "+options+" --disableplugin=rhnplugin"; // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		
		// execute the yum command to list available packages
		int min = 5;
		log.fine("Using a timeout of "+min+" minutes for next ssh command...");
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command,Long.valueOf(min*60000));
		
		// Example result.getStdout()
		//	[root@jsefler-betaqa-1 product]# yum grouplist --disablerepo=* --enablerepo=rhel-entitlement-beta
		//	Loaded plugins: product-id, refresh-packagekit, rhnplugin, subscription-manager
		//	Updating Red Hat repositories.
		//	INFO:rhsm-app.repolib:repos updated: 0
		//	This system is not registered with RHN.
		//	RHN support will be disabled.
		//	Setting up Group Process
		//	rhel-entitlement-beta                                                                                                                                 | 4.0 kB     00:00     
		//	rhel-entitlement-beta/group_gz                                                                                                                        | 190 kB     00:00     
		//	Installed Groups:
		//	   Additional Development
		//	   Assamese Support
		//	   Base
		//	Available Groups:
		//	   Afrikaans Support
		//	   Albanian Support
		//	   Amazigh Support
		//	Done

		String regex = Installed_or_Available+" Groups:((\\n\\s{3}.*)+)";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		if (!matcher.find()) {
			log.info("Did NOT find any "+Installed_or_Available+" Groups from: "+command);
			return groups;
		}

		// assemble the list of groups and return them
		for (String group : matcher.group(1).trim().split("\\n\\s{3}")) groups.add(group);

		return groups;		
	}
	
	
	/**
	 * Find an available package for install that is unique to the specified repo label.
	 * @param repo
	 * @return
	 * Note: You should consider calling yumDisableAllRepos() before using this method especially when this client was provisioned by Beaker.
	 */
	public String findUniqueAvailablePackageFromRepo (String repo) {
		for (String pkg : getYumListAvailable("--disablerepo=* --enablerepo="+repo)) {
			if (!getYumListAvailable("--disablerepo="+repo+" "+pkg).contains(pkg)) {
				if (!isPackageInstalled(pkg)) {	// only consider packages that are not installed otherwise we may inadvertantly upgrade an already installed package rather than install a new one
					if (yumCanInstallPackageFromRepo(pkg,repo,null)) {
						return pkg;
					}
				}
			}
		}
		return null;
		
		// repoquery --whatrequires --recursive <pkg> // TODO CONSIDERATION: may want to make use of this query to skip pkgs that will inadvertently remove packages starting with string: rhn-client-tools rhn-setup subscription-manager
	}
	
	public String findRandomAvailablePackageFromRepo (String repo) {
		// TODO: consider re-implementing this method using...
		// repoquery --pkgnarrow=available --all --repoid=<REPO_ID> --qf "%{name}"

		ArrayList<String> pkgs = getYumListAvailable("--disablerepo=* --enablerepo="+repo);
		if (pkgs.isEmpty()) return null;
		return pkgs.get(SubscriptionManagerCLITestScript.randomGenerator.nextInt(pkgs.size()));
	}
	
	public String findAnAvailableGroupFromRepo(String repo) {
		List <String> groups = yumGroupList("Available", "--disablerepo=* --enablerepo="+repo);
		for (int i=0; i<groups.size(); i++) {
			String group = groups.get(i);

			// choose a group that has "Mandatory Packages:"
			String mandatoryPackages = "Mandatory Packages:";
			if (sshCommandRunner.runCommandAndWait("yum groupinfo \""+groups.get(i)+"\" | grep \""+mandatoryPackages+"\"").getStdout().trim().equals(mandatoryPackages)) {
				return group;
			}
		}
		return null;
	}

	public String findAnInstalledGroupFromRepo(String repo) {
		List <String> groups = yumGroupList("Installed", "--disablerepo=* --enablerepo="+repo);
		for (int i=0; i<groups.size(); i++) {
			String group = groups.get(i);
			// don't consider these very important groups
			if (group.equals("Base")) continue;
			if (group.equals("X Window System")) continue;
			if (group.startsWith("Network")) continue;	// Network Infrastructure Server, Network file system client, Networking Tools
			
			return group;
		}
		return null;
	}
	
	/**
	 * @param pkg
	 * @param repoLabel
	 * @param installOptions
	 * @return true - when pkg can be cleanly installed from repolLabel with installOptions. <br>
	 *         false - when the user is not prompted with "Is this ok [y/N]:" to Complete! the install
	 */
	public boolean yumCanInstallPackageFromRepo (String pkg, String repoLabel, String installOptions) {
		
		// attempt to install the pkg from repo with the installOptions, but say N at the prompt: Is this ok [y/N]: N
		if (installOptions==null) installOptions=""; installOptions = installOptions.replaceFirst("-y", "");
		String command = "echo N | yum install "+pkg+" --enablerepo="+repoLabel+" --disableplugin=rhnplugin "+installOptions; // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		SSHCommandResult result;
		if (false) {
			// extremely verbose, consumes a lot of diskspace in the log file.
			result = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 1);
		} else {
			// less verbose, but harder to troubleshoot
			result = sshCommandRunner.runCommandAndWaitWithoutLogging(command);
			Assert.assertEquals(result.getExitCode(), new Integer(1),"ExitCode from: "+command);
			if (result.getStdout().contains("Is this ok [y/")) log.fine("Package '"+pkg+"' appears to be installable from repo '"+repoLabel+"'. (Stdout/Stderr/ExitCode was not logged to conserve disk space)");
			else log.fine("Package '"+pkg+"' does NOT appear to be installable from repo '"+repoLabel+"'. (Stdout/Stderr/ExitCode was not logged to conserve disk space)");
		}

		// disregard the package if it was obsoleted...
		
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating certificate-based repositories.
		//	Setting up Install Process
		//	Package gfs-pcmk is obsoleted by cman, trying to install cman-3.0.12.1-21.el6.x86_64 instead
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package cman.x86_64 0:3.0.12.1-21.el6 will be installed
		String regex="Package "+pkg.split("\\.")[0]+".* is obsoleted by (.+), trying to install .+ instead";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		String obsoletedByPkg = null;
		if (matcher.find()) {
			obsoletedByPkg = matcher.group(1);
			// can the obsoletedByPkg be installed from repoLabel instead? 
			//return yumCanInstallPackageFromRepo (obsoletedByPkg, repoLabel, installOptions);
			log.fine("Disregarding package '"+pkg+"' as installable from repo '"+repoLabel+"' because it has been obsoleted.");
			return false;
		}
		
        // RHEL6...
		//	Total download size: 2.1 M
		//	Installed size: 4.8 M
		//	Is this ok [y/N]: N
		//	Exiting on user Command
		
        // RHEL7...
		//	Total download size: 80 k
		//	Installed size: 210 k
		//	Is this ok [y/d/N]: N
		//	Exiting on user command
		//	Your transaction was saved, rerun it with:
		//	 yum load-transaction /tmp/yum_save_tx.2014-03-28.18-55.EY43Gr.yumtx
		
		return result.getStdout().contains("Is this ok [y/");
	}
	
	// 
	/**
	 * @param pkg
	 * @param repoLabel
	 * @param destdir
	 * @param downloadOptions
	 * @return the actual downloaded package File (null if there was an error)
	 * TODO: on RHEL5, the yum-utils package must be installed first to get yumdownloader
	 */
	public File yumDownloadPackageFromRepo (String pkg, String repoLabel, String destdir, String downloadOptions) {
		
		// use yumdownloader the package with repoLabel enabled
		if (downloadOptions==null) downloadOptions=""; //downloadOptions += " -y";
		String command = "yumdownloader "+pkg+" --destdir="+destdir+" --disablerepo=* --enablerepo="+repoLabel+" --noplugins "+downloadOptions; // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		//SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 0, "^Complete!$",null);
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command+"; "+command); // the second command is needed to populate stdout
		Assert.assertTrue(!result.getStderr().toLowerCase().contains("error"), "Stderr from command '"+command+"' did not report an error.");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from command '"+command+"'.");

		//[root@jsefler-stage-6server ~]# yumdownloader --disablerepo=* ricci.x86_64 --enablerepo=rhel-ha-for-rhel-6-server-rpms --destdir /tmp
		//Loaded plugins: product-id, refresh-packagekit
		//ricci-0.16.2-35.el6_1.1.x86_64.rpm                                                                                                                                     | 614 kB     00:00     
		//[root@jsefler-stage-6server ~]# yumdownloader --disablerepo=* ricci.x86_64 --enablerepo=rhel-ha-for-rhel-6-server-rpms --destdir /tmp
		//Loaded plugins: product-id, refresh-packagekit
		///tmp/ricci-0.16.2-35.el6_1.1.x86_64.rpm already exists and appears to be complete
		//[root@jsefler-stage-6server ~]# 
		
		// extract the name of the downloaded pkg
		// ([/\w\.\-+]*\.rpm)
		String regex = "([/\\w\\.\\-+]*\\.rpm)";	
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result.getStdout());
		if (!matcher.find()) {
			log.warning("Did not find the name of the downloaded pkg using regex '"+regex+"'.");
			return null;
		}
		String rpm = matcher.group(1).trim();	// return the contents of the first capturing group
		
		//File pkgFile = new File(destdir+File.separatorChar+rpm);
		File pkgFile = new File(rpm);
		
		// assert the downloaded file exists
		Assert.assertTrue(RemoteFileTasks.testExists(sshCommandRunner,pkgFile.getPath()),"Package '"+pkg+"' exists in destdir '"+destdir+"' after yumdownloading.");
		
		return pkgFile;
	}
	
	/**
	 * @param command - "install" or "update" or "downgrade" or "remove" - without asserting any results
	 * @param pkg
	 * @param repoLabel
	 * @param options
	 * @return SSHCommandResult result
	 */
	public SSHCommandResult yumDoPackageFromRepo_ (String command, String pkg, String repoLabel, String options) {
		
		// extract pkgName=devtoolset-1.1-valgrind-openmpi from pkg=devtoolset-1.1-valgrind-openmpi.i386
		String pkgName = pkg;
		if (pkg.lastIndexOf(".")!=-1) pkgName=pkg.substring(0,pkg.lastIndexOf("."));
		
		// install or update the package with repoLabel enabled
		String yumCommand = "yum -y "+command+" "+pkg;
		yumCommand += " --disableplugin=rhnplugin";	// --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		if (repoLabel!=null) yumCommand += " --enablerepo="+repoLabel;
		if (options!=null) yumCommand += " "+options; 
		//SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 0, "^Complete!$",null);
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(yumCommand);
		return (result);
	}
	
	public SSHCommandResult yumDoPackageFromRepo (String installUpdateOrDowngrade, String pkg, String repoLabel, String options) {
		// extract pkgName=devtoolset-1.1-valgrind-openmpi from pkg=devtoolset-1.1-valgrind-openmpi.i386
		String pkgName = pkg;
		if (pkg.lastIndexOf(".")!=-1) pkgName=pkg.substring(0,pkg.lastIndexOf("."));
		
		SSHCommandResult result = yumDoPackageFromRepo_(installUpdateOrDowngrade, pkg, repoLabel, options);
		Assert.assertTrue(!result.getStderr().toLowerCase().contains("error"), "Stderr from command '"+command+"' did not report an error.");
		Assert.assertTrue(result.getStdout().contains("\nComplete!"), "Stdout from command '"+command+"' reported a successful \"Complete!\".");
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from command '"+command+"'.");
		
		//	201104051837:12.757 - FINE: ssh root@jsefler-betastage-server.usersys.redhat.com yum -y install cairo-spice-debuginfo.x86_64 --enablerepo=rhel-6-server-beta-debug-rpms --disableplugin=rhnplugin (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201104051837:18.156 - FINE: Stdout: 
		//	Loaded plugins: product-id, refresh-packagekit, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating Red Hat repositories.
		//	Setting up Install Process
		//	Package cairo-spice-debuginfo is obsoleted by spice-server, trying to install spice-server-0.7.3-2.el6.x86_64 instead
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package spice-server.x86_64 0:0.7.3-2.el6 will be installed
		//	--> Finished Dependency Resolution
		//
		//	Dependencies Resolved
		//
		//	================================================================================
		//	 Package          Arch       Version          Repository                   Size
		//	================================================================================
		//	Installing:
		//	 spice-server     x86_64     0.7.3-2.el6      rhel-6-server-beta-rpms     245 k
		//
		//	Transaction Summary
		//	================================================================================
		//	Install       1 Package(s)
		//
		//	Total download size: 245 k
		//	Installed size: 913 k
		//	Downloading Packages:
		//	Running rpm_check_debug
		//	Running Transaction Test
		//	Transaction Test Succeeded
		//	Running Transaction
		//
		//	  Installing : spice-server-0.7.3-2.el6.x86_64                              1/1 
		//	duration: 205(ms)
		//
		//	Installed:
		//	  spice-server.x86_64 0:0.7.3-2.el6                                             
		//
		//	Complete!
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201104051837:18.180 - FINE: Stderr: 
		//	INFO:rhsm-app.repolib:repos updated: 63
		//	Installed products updated.
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201104051837:18.182 - FINE: ExitCode: 0 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)

		// EXAMPLE FROM RHEL62
		//	ssh root@tyan-gt24-03.rhts.eng.bos.redhat.com yum install gfs-pcmk.x86_64 --enablerepo=rhel-rs-for-rhel-6-server-rpms --disableplugin=rhnplugin -y
		//	Stdout:
		//	Loaded plugins: product-id, refresh-packagekit, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating certificate-based repositories.
		//	Setting up Install Process
		//	Package gfs-pcmk is obsoleted by cman, trying to install cman-3.0.12.1-19.el6.x86_64 instead
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package cman.x86_64 0:3.0.12.1-19.el6 will be installed
		//	--> Processing Dependency: clusterlib = 3.0.12.1-19.el6 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: modcluster >= 0.15.0-3 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: fence-virt >= 0.2.3-1 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: fence-agents >= 3.1.5-1 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: openais >= 1.1.1-1 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: ricci >= 0.15.0-4 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: corosync >= 1.4.1-3 for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libcpg.so.4(COROSYNC_CPG_1.0)(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libconfdb.so.4(COROSYNC_CONFDB_1.0)(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libSaCkpt.so.3(OPENAIS_CKPT_B.01.01)(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libcman.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libfenced.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: liblogthread.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libdlm.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libfence.so.4()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libccs.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libcpg.so.4()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libconfdb.so.4()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libdlmcontrol.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Processing Dependency: libSaCkpt.so.3()(64bit) for package: cman-3.0.12.1-19.el6.x86_64
		//	--> Running transaction check
		//	---> Package clusterlib.x86_64 0:3.0.12.1-19.el6 will be installed
		//	---> Package corosync.x86_64 0:1.4.1-3.el6 will be installed
		//	--> Processing Dependency: libnetsnmp.so.20()(64bit) for package: corosync-1.4.1-3.el6.x86_64
		//	---> Package corosynclib.x86_64 0:1.4.1-3.el6 will be installed
		//	--> Processing Dependency: librdmacm.so.1(RDMACM_1.0)(64bit) for package: corosynclib-1.4.1-3.el6.x86_64
		//	--> Processing Dependency: libibverbs.so.1(IBVERBS_1.0)(64bit) for package: corosynclib-1.4.1-3.el6.x86_64
		//	--> Processing Dependency: libibverbs.so.1(IBVERBS_1.1)(64bit) for package: corosynclib-1.4.1-3.el6.x86_64
		//	--> Processing Dependency: libibverbs.so.1()(64bit) for package: corosynclib-1.4.1-3.el6.x86_64
		//	--> Processing Dependency: librdmacm.so.1()(64bit) for package: corosynclib-1.4.1-3.el6.x86_64
		//	---> Package fence-agents.x86_64 0:3.1.5-9.el6 will be installed
		//	--> Processing Dependency: perl(Net::Telnet) for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: /usr/bin/ipmitool for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: perl-Net-Telnet for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: pexpect for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: python-suds for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: telnet for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: net-snmp-utils for package: fence-agents-3.1.5-9.el6.x86_64
		//	--> Processing Dependency: sg3_utils for package: fence-agents-3.1.5-9.el6.x86_64
		//	---> Package fence-virt.x86_64 0:0.2.3-4.el6 will be installed
		//	---> Package modcluster.x86_64 0:0.16.2-13.el6 will be installed
		//	--> Processing Dependency: oddjob for package: modcluster-0.16.2-13.el6.x86_64
		//	---> Package openais.x86_64 0:1.1.1-7.el6 will be installed
		//	---> Package openaislib.x86_64 0:1.1.1-7.el6 will be installed
		//	---> Package ricci.x86_64 0:0.16.2-42.el6 will be installed
		//	--> Processing Dependency: nss-tools for package: ricci-0.16.2-42.el6.x86_64
		//	--> Running transaction check
		//	---> Package ipmitool.x86_64 0:1.8.11-12.el6 will be installed
		//	---> Package libibverbs.x86_64 0:1.1.5-3.el6 will be installed
		//	---> Package librdmacm.x86_64 0:1.0.14.1-3.el6 will be installed
		//	---> Package net-snmp-libs.x86_64 1:5.5-37.el6 will be installed
		//	--> Processing Dependency: libsensors.so.4()(64bit) for package: 1:net-snmp-libs-5.5-37.el6.x86_64
		//	---> Package net-snmp-utils.x86_64 1:5.5-37.el6 will be installed
		//	---> Package nss-tools.x86_64 0:3.12.10-4.el6 will be installed
		//	---> Package oddjob.x86_64 0:0.30-5.el6 will be installed
		//	---> Package perl-Net-Telnet.noarch 0:3.03-11.el6 will be installed
		//	---> Package pexpect.noarch 0:2.3-6.el6 will be installed
		//	---> Package python-suds.noarch 0:0.4.1-3.el6 will be installed
		//	---> Package sg3_utils.x86_64 0:1.28-4.el6 will be installed
		//	---> Package telnet.x86_64 1:0.17-47.el6 will be installed
		//	--> Running transaction check
		//	---> Package lm_sensors-libs.x86_64 0:3.1.1-10.el6 will be installed
		//	--> Finished Dependency Resolution
		//	
		//	Dependencies Resolved
		//	
		//	================================================================================
		//	Package Arch Version Repository Size
		//	================================================================================
		//	Installing:
		//	cman x86_64 3.0.12.1-19.el6 beaker-HighAvailability 427 k
		//	Installing for dependencies:
		//	clusterlib x86_64 3.0.12.1-19.el6 beaker-HighAvailability 92 k
		//	corosync x86_64 1.4.1-3.el6 beaker-HighAvailability 185 k
		//	corosynclib x86_64 1.4.1-3.el6 beaker-HighAvailability 169 k
		//	fence-agents x86_64 3.1.5-9.el6 beaker-HighAvailability 147 k
		//	fence-virt x86_64 0.2.3-4.el6 beaker-HighAvailability 34 k
		//	ipmitool x86_64 1.8.11-12.el6 beaker-Server 323 k
		//	libibverbs x86_64 1.1.5-3.el6 beaker-Server 43 k
		//	librdmacm x86_64 1.0.14.1-3.el6 beaker-Server 26 k
		//	lm_sensors-libs x86_64 3.1.1-10.el6 beaker-Server 36 k
		//	modcluster x86_64 0.16.2-13.el6 beaker-HighAvailability 184 k
		//	net-snmp-libs x86_64 1:5.5-37.el6 beaker-Server 1.5 M
		//	net-snmp-utils x86_64 1:5.5-37.el6 beaker-Server 168 k
		//	nss-tools x86_64 3.12.10-4.el6 beaker-Server 747 k
		//	oddjob x86_64 0.30-5.el6 beaker-Server 59 k
		//	openais x86_64 1.1.1-7.el6 beaker-HighAvailability 191 k
		//	openaislib x86_64 1.1.1-7.el6 beaker-HighAvailability 81 k
		//	perl-Net-Telnet noarch 3.03-11.el6 beaker-HighAvailability 54 k
		//	pexpect noarch 2.3-6.el6 beaker-Server 146 k
		//	python-suds noarch 0.4.1-3.el6 beaker-HighAvailability 217 k
		//	ricci x86_64 0.16.2-42.el6 beaker-HighAvailability 614 k
		//	sg3_utils x86_64 1.28-4.el6 beaker-Server 470 k
		//	telnet x86_64 1:0.17-47.el6 beaker-Server 57 k
		//	
		//	Transaction Summary
		//	================================================================================
		//	Install 23 Package(s)
		//	
		//	Total download size: 5.9 M
		//	Installed size: 19 M
		//	Downloading Packages:
		//	--------------------------------------------------------------------------------
		//	Total 8.2 MB/s | 5.9 MB 00:00
		//	Running rpm_check_debug
		//	Running Transaction Test
		//	Transaction Test Succeeded
		//	Running Transaction
		//	
		//	Installing : libibverbs-1.1.5-3.el6.x86_64 1/23
		//	
		//	Installing : oddjob-0.30-5.el6.x86_64 2/23
		//	
		//	Installing : librdmacm-1.0.14.1-3.el6.x86_64 3/23
		//	
		//	Installing : fence-virt-0.2.3-4.el6.x86_64 4/23
		//	
		//	Installing : lm_sensors-libs-3.1.1-10.el6.x86_64 5/23
		//	
		//	Installing : 1:net-snmp-libs-5.5-37.el6.x86_64 6/23
		//	
		//	Installing : corosync-1.4.1-3.el6.x86_64 7/23
		//	
		//	Installing : corosynclib-1.4.1-3.el6.x86_64 8/23
		//	
		//	Installing : openais-1.1.1-7.el6.x86_64 9/23
		//	
		//	Installing : openaislib-1.1.1-7.el6.x86_64 10/23
		//	
		//	Installing : clusterlib-3.0.12.1-19.el6.x86_64 11/23
		//	
		//	Installing : modcluster-0.16.2-13.el6.x86_64 12/23
		//	
		//	Installing : 1:net-snmp-utils-5.5-37.el6.x86_64 13/23
		//	
		//	Installing : pexpect-2.3-6.el6.noarch 14/23
		//	
		//	Installing : perl-Net-Telnet-3.03-11.el6.noarch 15/23
		//	
		//	Installing : 1:telnet-0.17-47.el6.x86_64 16/23
		//	
		//	Installing : python-suds-0.4.1-3.el6.noarch 17/23
		//	
		//	Installing : nss-tools-3.12.10-4.el6.x86_64 18/23
		//	
		//	Installing : ricci-0.16.2-42.el6.x86_64 19/23
		//	
		//	Installing : sg3_utils-1.28-4.el6.x86_64 20/23
		//	
		//	Installing : ipmitool-1.8.11-12.el6.x86_64 21/23
		//	
		//	Installing : fence-agents-3.1.5-9.el6.x86_64 22/23
		//	Stopping kdump:[ OK ]
		//	Starting kdump:[ OK ]
		//	
		//	Installing : cman-3.0.12.1-19.el6.x86_64 23/23
		//	
		//	Installed:
		//	cman.x86_64 0:3.0.12.1-19.el6
		//	
		//	Dependency Installed:
		//	clusterlib.x86_64 0:3.0.12.1-19.el6 corosync.x86_64 0:1.4.1-3.el6
		//	corosynclib.x86_64 0:1.4.1-3.el6 fence-agents.x86_64 0:3.1.5-9.el6
		//	fence-virt.x86_64 0:0.2.3-4.el6 ipmitool.x86_64 0:1.8.11-12.el6
		//	libibverbs.x86_64 0:1.1.5-3.el6 librdmacm.x86_64 0:1.0.14.1-3.el6
		//	lm_sensors-libs.x86_64 0:3.1.1-10.el6 modcluster.x86_64 0:0.16.2-13.el6
		//	net-snmp-libs.x86_64 1:5.5-37.el6 net-snmp-utils.x86_64 1:5.5-37.el6
		//	nss-tools.x86_64 0:3.12.10-4.el6 oddjob.x86_64 0:0.30-5.el6
		//	openais.x86_64 0:1.1.1-7.el6 openaislib.x86_64 0:1.1.1-7.el6
		//	perl-Net-Telnet.noarch 0:3.03-11.el6 pexpect.noarch 0:2.3-6.el6
		//	python-suds.noarch 0:0.4.1-3.el6 ricci.x86_64 0:0.16.2-42.el6
		//	sg3_utils.x86_64 0:1.28-4.el6 telnet.x86_64 1:0.17-47.el6
		//	
		//	Complete!
		//	Stderr: Installed products updated.
		//	ExitCode: 0

		//	201111171056:22.839 - FINE: ssh root@jsefler-stage-6server.usersys.redhat.com echo N | yum install ricci-debuginfo.x86_64 --enablerepo=rhel-ha-for-rhel-6-server-htb-debug-rpms --disableplugin=rhnplugin  (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201111171056:24.774 - FINE: Stdout: 
		//	Loaded plugins: product-id, refresh-packagekit, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating certificate-based repositories.
		//	Setting up Install Process
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ricci-debuginfo.x86_64 0:0.16.2-35.el6 will be installed
		//	--> Finished Dependency Resolution
		//	
		//	Dependencies Resolved
		//	
		//	================================================================================
		//	 Package    Arch   Version       Repository                                Size
		//	================================================================================
		//	Installing:
		//	 ricci-debuginfo
		//	            x86_64 0.16.2-35.el6 rhel-ha-for-rhel-6-server-htb-debug-rpms 4.4 M
		//	
		//	Transaction Summary
		//	================================================================================
		//	Install       1 Package(s)
		//	
		//	Total download size: 4.4 M
		//	Installed size: 27 M
		//	Is this ok [y/N]: Exiting on user Command
		//	Complete!
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201111171056:24.775 - FINE: Stderr:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201111171056:24.775 - FINE: ExitCode: 1 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201111171056:24.775 - INFO: Asserted: 1 is present in the list [1] (com.redhat.qe.auto.testng.Assert.pass)
		//	201111171056:28.183 - FINE: ssh root@jsefler-stage-6server.usersys.redhat.com yum install ricci-debuginfo.x86_64 --enablerepo=rhel-ha-for-rhel-6-server-htb-debug-rpms --disableplugin=rhnplugin  -y (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201111171056:30.752 - FINE: Stdout: 
		//	Loaded plugins: product-id, refresh-packagekit, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating certificate-based repositories.
		//	Setting up Install Process
		//	Resolving Dependencies
		//	--> Running transaction check
		//	---> Package ricci-debuginfo.x86_64 0:0.16.2-35.el6 will be installed
		//	--> Finished Dependency Resolution
		//	
		//	Dependencies Resolved
		//	
		//	================================================================================
		//	 Package    Arch   Version       Repository                                Size
		//	================================================================================
		//	Installing:
		//	 ricci-debuginfo
		//	            x86_64 0.16.2-35.el6 rhel-ha-for-rhel-6-server-htb-debug-rpms 4.4 M
		//	
		//	Transaction Summary
		//	================================================================================
		//	Install       1 Package(s)
		//	
		//	Total download size: 4.4 M
		//	Installed size: 27 M
		//	Downloading Packages:
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201111171056:30.767 - FINE: Stderr: 
		//	https://cdn.redhat.com/content/htb/rhel/server/6/6Server/x86_64/highavailability/debug/Packages/ricci-debuginfo-0.16.2-35.el6.x86_64.rpm: [Errno 14] PYCURL ERROR 22 - "The requested URL returned error: 404"
		//	Trying other mirror.
		//	
		//	
		//	Error Downloading Packages:
		//	  ricci-debuginfo-0.16.2-35.el6.x86_64: failure: Packages/ricci-debuginfo-0.16.2-35.el6.x86_64.rpm from rhel-ha-for-rhel-6-server-htb-debug-rpms: [Errno 256] No more mirrors to try.
		//	
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201111171056:30.775 - FINE: ExitCode: 1 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)

		// check if the package was obsoleted:
		// Package cairo-spice-debuginfo is obsoleted by spice-server, trying to install spice-server-0.7.3-2.el6.x86_64 instead
		String regex="Package "+pkgName+".* is obsoleted by (.+), trying to install .+ instead";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(sshCommandRunner.getStdout());
		String obsoletedByPkg = null;
		if (matcher.find()) {
			obsoletedByPkg = matcher.group(1);
			log.warning("Package '"+pkg+"' was obsoleted by '"+obsoletedByPkg+"'. The replacement package may NOT get installed from repository '"+repoLabel+"'.");
			pkg = obsoletedByPkg;
		}
		
		// FIXME, If the package is obsoleted, then the obsoletedByPkg may not come from the same repo and the following assert will fail
		
		// assert the installed package came from repoLabel
		// =======================================================================================
		//  Package          Arch       Version           Repository                         Size
		// =======================================================================================
		// Installing:
		//	spice-server     x86_64     0.7.3-2.el6       rhel-6-server-beta-rpms            245 k
		//  cman             x86_64     3.0.12.1-19.el6   beaker-HighAvailability            427 k
		//  cmirror          x86_64     7:2.02.103-5.el7  rhel-rs-for-rhel-7-server-htb-rpms 166 k
		
		// Installing:
		//	ricci-debuginfo
		//	            x86_64 0.16.2-35.el6 rhel-ha-for-rhel-6-server-htb-debug-rpms 4.4 M
		
		// Installing:
		//	jline-eap6 noarch 0.9.94-10.GA_redhat_2.ep6.el6.4
		//	                                       jb-eap-6-for-rhel-6-for-power-rpms 135 k
		if (repoLabel==null) {
			regex=pkgName+"\\n? +(\\w+) +([\\w:\\.-]+) +([\\w-]+)";
			regex=pkgName+"\\n? +(\\w+) +([\\w:\\.-]+)\\n? +([\\w-]+) +(\\d+(\\.\\d+)? (k|M|G))";		
		} else {
			regex=pkgName+"\\n? +(\\w+) +([\\w:\\.-]+) +("+repoLabel+")";
			regex=pkgName+"\\n? +(\\w+) +([\\w:\\.-]+)\\n? +("+repoLabel+") +(\\d+(\\.\\d+)? (k|M|G))";
		}
		pattern = Pattern.compile(regex, Pattern.MULTILINE);
		matcher = pattern.matcher(sshCommandRunner.getStdout());
		Assert.assertTrue(matcher.find(), "Attempted "+installUpdateOrDowngrade+" of package '"+pkg+"' "+(repoLabel==null?"":" from repository '"+repoLabel+"'")+" appears to have been successful.");
		String arch = matcher.group(1);
		String version = matcher.group(2);
		String repo = matcher.group(3);
		String size = matcher.group(4);
		

		// finally assert that the package is actually installed
		//
		// RHEL 5...
		//	201106061840:40.270 - FINE: ssh root@jsefler-stage-5server.usersys.redhat.com yum list installed GConf2-debuginfo.x86_64 --disableplugin=rhnplugin (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201106061840:41.529 - FINE: Stdout: 
		//	Loaded plugins: product-id, security, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating Red Hat repositories.
		//	Installed Packages
		//	GConf2-debuginfo.x86_64                  2.14.0-9.el5                  installed
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201106061840:41.530 - FINE: Stderr:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201106061840:41.530 - FINE: ExitCode: 0 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//
		// RHEL 6...
		//	201104051839:15.836 - FINE: ssh root@jsefler-betastage-server.usersys.redhat.com yum list installed spice-server --disableplugin=rhnplugin (com.redhat.qe.tools.SSHCommandRunner.run)
		//	201104051839:16.447 - FINE: Stdout: 
		//	Loaded plugins: product-id, refresh-packagekit, subscription-manager
		//	No plugin match for: rhnplugin
		//	Updating Red Hat repositories.
		//	Installed Packages
		//	spice-server.x86_64             0.7.3-2.el6             @rhel-6-server-beta-rpms
		//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201104051839:16.453 - FINE: Stderr: INFO:rhsm-app.repolib:repos updated: 63	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
		//	201104051839:16.455 - FINE: ExitCode: 0 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
//		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"yum list installed "+pkg+" --disableplugin=rhnplugin", 0, "^"+pkg.split("\\.")[0]+"."+arch+" +"+version+" +(installed|@"+repo+")$",null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"yum list installed "+pkg+" --disableplugin=rhnplugin", 0, "^"+pkgName+"."+arch+" +"+version+" +(installed|@"+repo+")$",null);
			
		return result;
	}
	
	public SSHCommandResult yumInstallPackageFromRepo (String pkg, String repoLabel, String installOptions) {
		return yumDoPackageFromRepo ("install", pkg, repoLabel, installOptions);
	}
	/**
	 * yum -y install pkg installOptions <br>
	 * Assert the install is Complete! and pkg is installed.
	 * @param pkg
	 * @param installOptions
	 * @return
	 */
	public SSHCommandResult yumInstallPackage (String pkg, String installOptions) {
		return yumInstallPackageFromRepo(pkg,null,installOptions);
	}
	/**
	 * yum -y install pkg<br>
	 * Assert the install is Complete! and pkg is installed.
	 * @param pkg
	 * @return
	 */
	public SSHCommandResult yumInstallPackage (String pkg) {
		return yumInstallPackageFromRepo(pkg,null,null);
	}
	
	public SSHCommandResult yumUpdatePackageFromRepo (String pkg, String repoLabel, String updateOptions) {
		return yumDoPackageFromRepo ("update", pkg, repoLabel, updateOptions);
	}
	/**
	 * yum -y update pkg<br>
	 * Assert the update is Complete! and pkg is installed.
	 * @param pkg
	 * @return
	 */
	public SSHCommandResult yumUpdatePackage (String pkg) {
		return yumUpdatePackageFromRepo(pkg,null,null);
	}
	
	public SSHCommandResult yumDowngradePackageFromRepo (String pkg, String repoLabel, String downgradeOptions) {
		return yumDoPackageFromRepo ("downgrade", pkg, repoLabel, downgradeOptions);
	}
	/**
	 * yum -y downgrade pkg<br>
	 * Assert the downgrade is Complete! and pkg is installed.
	 * @param pkg
	 * @return
	 */
	public SSHCommandResult yumDowngradePackage (String pkg) {
		return yumDowngradePackageFromRepo(pkg,null,null);
	}
	
	
	/**
	 * yum -y remove pkg --disableplugin=rhnplugin<br>
	 * Assert the removal is Complete! and no longer installed.
	 * @param pkg
	 * @param options - additional yum options to append
	 * @return
	 */
	public SSHCommandResult yumRemovePackage (String pkg, String options) {
		String command = "yum -y remove "+pkg;
		command += " --disableplugin=rhnplugin";	// --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		if (options!=null) command += " "+options; 
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 0, "^Complete!$",null);
		RemoteFileTasks.runCommandAndAssert(sshCommandRunner,"yum list installed "+pkg+" --disableplugin=rhnplugin", 1, null,"Error: No matching Packages to list");
		return result;
	}
	/**
	 * yum -y remove pkg<br>
	 * Assert the removal is Complete! and no longer installed.
	 * @param pkg
	 * @return
	 */
	public SSHCommandResult yumRemovePackage (String pkg) {
		return yumRemovePackage (pkg, null);
	}
	
	/**
	 * Attempt to: yum -y groupinstall "group" AND assert a "Complete!" result.<br>
	 *  Note: Just because a group appears in yum grouplist, that does not mean the package members in the group are available for install.  There are also some rules regarding how packages are marked to indicate whether or not the group is considered installed (see man yum).  If necessary the method will try to group install with --skip-broken which means the group may not be fully installed at the end of this method. <br>
	 *  Learn more about groups at https://www.certdepot.net/rhel7-get-started-package-groups/
	 * @param group
	 * @return SSHCommandResult from the attempt to groupinstall
	 */
	public SSHCommandResult yumInstallGroup (String group) {
		boolean skipBroken=false;
		SSHCommandResult result = yumInstallGroup_(group, "--disableplugin=rhnplugin");	// --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		
		// Because we test composes of RHEL that include packages that are not yet released to production (and therefore not available in the SKUs we are testing with), we can end up with a yum group install error like this...
		//	--> Finished Dependency Resolution
		//	Error: Package: libreport-2.0.9-33.el6.x86_64 (rhel-6-hpc-node-rpms)
		//	           Requires: libreport-filesystem = 2.0.9-33.el6
		//	           Installed: libreport-filesystem-2.0.9-34.el6.x86_64 (@anaconda-RedHatEnterpriseLinux-201804071340.x86_64/6.10)
		//	               libreport-filesystem = 2.0.9-34.el6
		//	           Available: libreport-filesystem-2.0.9-24.el6.x86_64 (rhel-6-hpc-node-rpms)
		//	               libreport-filesystem = 2.0.9-24.el6
		//	           Available: libreport-filesystem-2.0.9-25.el6_7.x86_64 (rhel-6-hpc-node-rpms)
		//	               libreport-filesystem = 2.0.9-25.el6_7
		//	           Available: libreport-filesystem-2.0.9-32.el6.x86_64 (rhel-6-hpc-node-rpms)
		//	               libreport-filesystem = 2.0.9-32.el6
		//	           Available: libreport-filesystem-2.0.9-33.el6.x86_64 (rhel-6-hpc-node-rpms)
		//	               libreport-filesystem = 2.0.9-33.el6
		//	**********************************************************************
		//	yum can be configured to try to resolve such errors by temporarily enabling
		//	disabled repos and searching for missing dependencies.
		//	To enable this functionality please set 'notify_only=0' in /etc/yum/pluginconf.d/search-disabled-repos.conf
		//	**********************************************************************
		//
		//	 You could try using --skip-broken to work around the problem
		//	 You could try running: rpm -Va --nofiles --nodigest
		
		// let's try again with --skip-broken when this happens
		if (!result.getExitCode().equals(Integer.valueOf(0))) {
			String regex = "Error: Package: .+ Requires: .+ Installed: .+ \\(@anaconda.*\\).* You could try using --skip-broken to work around the problem";
			regex = "Error: Package: .+\\n\\s+Requires: .+\\n\\s+Installed: .+ \\(@anaconda.*\\)(\\n.*)+ You could try using --skip-broken to work around the problem";
			if (SubscriptionManagerCLITestScript.doesStringContainMatches(result.getStdout(),regex)) {
				skipBroken = true;
				log.warning("Attempting to group install '"+group+"' with option --skip-broken...");
				result = yumInstallGroup_(group, "--disableplugin=rhnplugin --skip-broken");				
			}
		}
		
		Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "ExitCode from attempt to yum groupinstall '"+group+"'.");
		Assert.assertContainsMatch(result.getStdout(), "^Complete!$", "Stdout from attempt to yum groupinstall '"+group+"'.");
		if (skipBroken) {
			log.warning("Skipping the assertion that group '"+group+"' is fully installed because we attempted to groupinstall with option --skip-broken");
		} else {
			Assert.assertTrue(!this.yumGroupList("Available", ""/*"--disablerepo=* --enablerepo="+repo*/).contains(group),"Yum group is NOT Available after calling '"+command+"'.");
		}
		return result;
	}
	/**
	 * Attempt to: yum -y groupinstall "group" WITHOUT asserting result
	 * @param group e.g. "Debugging Tools"
	 * @param options e.g. "--disableplugin=rhnplugin"
	 * @return SSHCommandResult from the attempt to groupinstall
	 *  WARNING: Just because a group appears in yum grouplist, that does not mean the package members in the group are available for install.  There are also some rules regarding how packages are marked to indicate whether or not the group is considered installed (see man yum)
	 *  Learn more about groups at https://www.certdepot.net/rhel7-get-started-package-groups/
	 */
	public SSHCommandResult yumInstallGroup_ (String group, String options) {
		String command = String.format("yum -y groupinstall \"%s\" %s",group, options);
		return this.sshCommandRunner.runCommandAndWait(command);
	}
	
	public SSHCommandResult yumRemoveGroup (String group) {
		String command = "yum -y groupremove \""+group+"\" --disableplugin=rhnplugin"; // --disableplugin=rhnplugin helps avoid: up2date_client.up2dateErrors.AbuseError
		SSHCommandResult result = RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 0, "^Complete!$",null);
		Assert.assertTrue(!this.yumGroupList("Installed", ""/*"--disablerepo=* --enablerepo="+repo*/).contains(group),"Yum group is NOT Installed after calling '"+command+"'.");
		return result;
	}
	
	/**
	 * @param option [headers|packages|metadata|dbcache|plugins|expire-cache|all]
	 * @return
	 */
	public SSHCommandResult yumClean (String option) {
		String command = "yum clean "+option;
		//command += " --disableplugin=rhnplugin"; // helps avoid: up2date_client.up2dateErrors.AbuseError
		command += " --enablerepo=*";	// helps on rhel7
		//return RemoteFileTasks.runCommandAndAssert(sshCommandRunner,command, 0, "^Cleaning",null);	// don't bother asserting results anymore since rhel7 exitCode is 1 when "There are no enabled repos."	// jsefler 1/26/2013
		return sshCommandRunner.runCommandAndWait(command);
	}
	
	public String getRedhatRelease() {
//		// verify the grinder hostname is a rhel 5 machine
//		log.info("Verifying prerequisite...  hostname '"+grinderHostname+"' is a Red Hat Enterprise Linux .* release 5 machine.");
//		Assert.assertEquals(sshCommandRunner.runCommandAndWait("cat /etc/redhat-release | grep -E \"^Red Hat Enterprise Linux .* release 5.*\"").getExitCode(),Integer.valueOf(0),"Grinder hostname must be RHEL 5.*");
		return sshCommandRunner.runCommandAndWait("cat /etc/redhat-release").getStdout();
	}
	
	/**
	 * @param key - e.g. "REGISTERED_TO_OTHER_WARNING"
	 * @return the branding string value for the key
	 */
	public String getBrandingString(String key) {

		// view /usr/share/rhsm/subscription_manager/branding/__init__.py and search for "self." to find branding message strings e.g. "REGISTERED_TO_OTHER_WARNING"
		return sshCommandRunner.runCommandAndWait("cd "+brandingDir+"; python -c \"import __init__ as sm;brand=sm.Branding(sm.get_branding());print brand."+key+"\"").getStdout();
	}
	
	
	/**
	 * Call rhnreg_ks without asserting any results
	 * @param rhnUsername - rhnreg_ks username
	 * @param rhnPassword - rhnreg_ks password
	 * @param rhnHostname
	 * @return SSHCommandResult containing stdout stderr and exitCode
	 */
	public SSHCommandResult registerToRhnClassic_(String rhnUsername, String rhnPassword, String rhnHostname) {
		String command;
		String profileName = "rhsm-automation."+hostname;
		
		// avoid creating a duplicate registration and consequently exhausting orphaned entitlements
		// delete all existing rhn registrations under this profileName
		deleteRhnSystemsRegisteredByName(rhnUsername, rhnPassword, rhnHostname, profileName);

		// register to RHN Classic
		// [root@jsefler-onprem-5server ~]# rhnreg_ks --serverUrl=https://xmlrpc.rhn.code.stage.redhat.com/XMLRPC --username=qa@redhat.com --password=CHANGE-ME --force --norhnsd --nohardware --nopackages --novirtinfo
		//	ERROR: refreshing remote package list for System Profile
		String serverUrl = rhnHostname+"/XMLRPC"; if (!rhnHostname.startsWith("http")) serverUrl = "https://xmlrpc."+serverUrl;
		command = String.format("rhnreg_ks --serverUrl=%s --username=%s --password=%s --profilename=%s --force --norhnsd --nohardware --nopackages --novirtinfo", serverUrl, rhnUsername, rhnPassword, profileName);
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201704051554:27.985 - FINE: ssh root@bkr-hv01-guest16.dsal.lab.eng.bos.redhat.com rhnreg_ks --serverUrl=https://rhsm-sat5.usersys.redhat.com/XMLRPC --username=rhsm-client --password=REDACTED --profilename=rhsm-automation.bkr-hv01-guest16.dsal.lab.eng.bos.redhat.com --force --norhnsd --nohardware --nopackages --novirtinfo
		//	201704051554:32.552 - FINE: Stdout: 
		//	201704051554:32.552 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/sbin/rhn_check", line 54, in <module>
		//	    from rhn.i18n import bstr, sstr
		//	ImportError: No module named i18n
		if (result.getStderr().contains("ImportError: No module named i18n")) {
			String bugId = "1439363"; // Bug 1439363 - ImportError: No module named i18n
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("The remainder of this test is blocked by bug "+bugId+".  There is no workaround.");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201704051554:27.985 - FINE: ssh root@bkr-hv01-guest16.dsal.lab.eng.bos.redhat.com rhnreg_ks --serverUrl=https://rhsm-sat5.usersys.redhat.com/XMLRPC --username=rhsm-client --password=REDACTED --profilename=rhsm-automation.bkr-hv01-guest16.dsal.lab.eng.bos.redhat.com --force --norhnsd --nohardware --nopackages --novirtinfo
		//	201704051554:32.552 - FINE: Stdout: 
		//	201704051554:32.552 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/sbin/rhn_check", line 54, in <module>
		//	    from rhn.tb import raise_with_tb
		//	ImportError: No module named tb
		if (result.getStderr().contains("ImportError: No module named tb")) {
			String bugId = "1439139"; // Bug 1439139 - [RHEL7.4] rhn_check - ImportError: No module named i18n 
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("The remainder of this test is blocked by bug "+bugId+".  There is no workaround.");
			}
		}
		// END OF WORKAROUND
		
		return result;
	}
	
	/**
	 * Call rhnreg_ks and assert the existence of a systemid file afterwards.
	 * @param rhnUsername - rhnreg_ks username
	 * @param rhnPassword - rhnreg_ks password
	 * @param rhnHostname
	 * @return the rhn system_id value from the contents of the systemid file
	 */
	public String registerToRhnClassic(String rhnUsername, String rhnPassword, String rhnHostname) {
		
		// register to RHN Classic
		SSHCommandResult result = registerToRhnClassic_(rhnUsername, rhnPassword, rhnHostname);
		
		// assert result
		Integer exitCode = result.getExitCode();
		String stdout = result.getStdout();
		String stderr = result.getStderr();
		String msg;
		
		msg = "ERROR: refreshing remote package list for System Profile";
		if (stdout.contains(msg)) {
			// ERROR: refreshing remote package list for System Profile
			log.warning("Ignoring stdout result: "+msg);
			stdout = stdout.replaceAll(msg, "");
		}
		
		msg = "forced skip_if_unavailable=True due to";
		if (stderr.contains(msg)) {
			// Repo content-label-72 forced skip_if_unavailable=True due to: /etc/pki/entitlement/2114809071147763952.pem
			String regex = "Repo .+ "+msg+": .+.pem";
			log.warning("Ignoring stderr results matching: "+regex);
			stderr = stderr.replaceAll(regex, "");
		}
		
		// this will occur on rhel7+ where there is no RHN Classic support.  See bugzilla 906875
		msg = "This system is not subscribed to any channels.\nRHN channel support will be disabled.";
		if (stderr.contains(msg)) {
			// This system is not subscribed to any channels.
			// RHN channel support will be disabled.
			log.warning("Ignoring stderr result: "+msg);
			stderr = stderr.replaceAll(msg, "");
		}
		
		// this will occur on rhel5 while bug 924919 is open, but does not really affect the success of rhnreg_ks.  See bugzilla 924919
		msg = "WARNING:rhsm-app.subscription_manager.isodate:dateutil module not found, trying pyxml";
		if (stderr.contains(msg)) {
			log.warning("Ignoring stderr result: "+msg);
			stderr = stderr.replaceAll(msg, "");
		}
		
		// this will occur on rhel7+ where Red Hat Network Classic is not supported.  See bugzilla 906875
		msg = "Red Hat Network Classic is not supported.";
		if (stderr.contains(msg)) {
			//	FINE: ssh root@jsefler-7.usersys.redhat.com rhnreg_ks --serverUrl=https://xmlrpc.rhn.code.stage.redhat.com/XMLRPC --username=qa@redhat.com --password=REDACTED --profilename=rhsm-automation.jsefler-7.usersys.redhat.com --force --norhnsd --nohardware --nopackages --novirtinfo
			//	FINE: Stdout: 
			//	FINE: Stderr: 
			//	An error has occurred:
			//
			//	Red Hat Network Classic is not supported.
			//	To register with Red Hat Subscription Management please run:
			//
			//	    subscription-manager register --auto-attach
			//
			//	Get more information at access.redhat.com/knowledge
			//	    
			//	See /var/log/up2date for more information
			//	FINE: ExitCode: 1
			if (Integer.valueOf(redhatReleaseX)>=7) {
				log.warning(msg);
				throw new SkipException("Skipping this test on RHEL '"+redhatReleaseX+"' which depends on RHN Classic registration.");
			}
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201609281555:15.362 - FINE: ssh root@jsefler-rhel7.usersys.redhat.com rhnreg_ks --serverUrl=https://rhsm-sat5.usersys.redhat.com/XMLRPC --username=rhsm-client --password=REDACTED --profilename=rhsm-automation.jsefler-rhel7.usersys.redhat.com --force --norhnsd --nohardware --nopackages --novirtinfo
		//	201609281555:16.284 - FINE: Stdout:  
		//	201609281555:16.285 - FINE: Stderr: 
		//	An error has occurred:
		//	<type 'exceptions.TypeError'>
		//	See /var/log/up2date for more information
		//	201609281555:16.285 - FINE: ExitCode: 1 
		if (stderr.contains("<type 'exceptions.TypeError'>")) {
			String bugId = "1380159"; // Bug 1380159 - <type 'exceptions.TypeError'>: 'str' object does not support item assignment
			boolean invokeWorkaroundWhileBugIsOpen = true;
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("The remainder of this test is blocked by bug "+bugId+".  There is no workaround.");
			}
		}
		// END OF WORKAROUND
		
		Assert.assertEquals(exitCode, new Integer(0),"Exitcode from attempt to register to RHN Classic.");
		Assert.assertEquals(stderr.trim(), "","Stderr from attempt to register to RHN Classic.");
		// TEMPORARY WORKAROUND
		boolean invokeWorkaroundWhileBugIsOpen = true;
		String bugId="1282961"; // Bug 1282961 - Plugin "search-disabled-repos" requires API 2.7. Supported API is 2.6.
		try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen && this.redhatReleaseX.equals("6") && this.isPackageVersion("subscription-manager", ">=", "1.15")) {
			Assert.assertEquals(stdout.replace("Plugin \"search-disabled-repos\" requires API 2.7. Supported API is 2.6.", "").trim(), "","Ignoring bug '"+bugId+"',Stdout from attempt to register to RHN Classic.");
		} else
		// END OF WORKAROUND
		Assert.assertEquals(stdout.trim(), "","Stdout from attempt to register to RHN Classic.");
		
		// assert this system is registered using RHN Classic
		Assert.assertTrue(isRhnSystemRegistered(),"This system is registered using RHN Classic.");
		
		// get the value of the systemid
		// [root@jsefler-onprem-5server rhn]# grep ID- /etc/sysconfig/rhn/systemid
		// <value><string>ID-1021538137</string></value>
		String command = String.format("grep ID- %s", rhnSystemIdFile);
		return sshCommandRunner.runCommandAndWait(command).getStdout().trim().replaceAll("\\<.*?\\>", "").replaceFirst("ID-", "");		// return 1021538137
	}
	
	/**
	 * Call rhn-channel --list to get the currently consumed RHN channels.
	 * @return
	 */
	public List<String> getCurrentRhnClassicChannels() {
		
		// [root@jsefler-onprem-5server rhn]# rhn-channel --list
		// rhel-x86_64-server-5
		// rhel-x86_64-server-supplementary-5
		// rhel-x86_64-server-supplementary-5-debuginfo
		String command = String.format("rhn-channel --list");
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		
		// assert result
		if (result.getExitCode()==1 && result.getStderr().trim().equals("This system is not associated with any channel.")) {
			//	[root@cloud-qe-7 ~]# rhn-channel --list
			//	This system is not associated with any channel.
			//	[root@cloud-qe-7 ~]# echo $?
			//	1
			log.warning(result.getStderr().trim());
		} else if (result.getExitCode()==1 && result.getStderr().trim().equals("Unable to locate SystemId file. Is this system registered?")) {
			//	[root@cloud-qe-7 ~]# rhn-channel --list
			//	Unable to locate SystemId file. Is this system registered?
			//	[root@cloud-qe-7 ~]# echo $?
			//	1
			log.warning(result.getStderr().trim());
		} else {
			Assert.assertEquals(result.getExitCode(), Integer.valueOf(0), "Exitcode from attempt to list currently consumed RHN Classic channels.");
			Assert.assertEquals(result.getStderr(), "", "Stderr from attempt to list currently consumed RHN Classic channels.");
		}
		
		// parse the rhnChannels from stdout 
		List<String> rhnChannels = new ArrayList<String>();
		if (result.getExitCode()==0) {	//if (!result.getStdout().trim().equals("")) {
			rhnChannels	= Arrays.asList(result.getStdout().trim().split("\\n"));
		}
		return rhnChannels;
	}
	
	public boolean isRhnSystemIdRegistered(String rhnUsername, String rhnPassword,String rhnHostname, String systemId) {
		String serverUrl = rhnHostname; if (!serverUrl.startsWith("http")) serverUrl="https://"+serverUrl;
		String command = String.format("rhn-is-registered.py --username=%s --password=%s --serverurl=%s  %s", rhnUsername, rhnPassword, serverUrl, systemId);
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		return Boolean.valueOf(result.getStdout().trim());
	}
	
	public boolean isRhnSystemRegistered() {
		Boolean rhnSystemIdFileExists = RemoteFileTasks.testExists(sshCommandRunner, rhnSystemIdFile);
		log.info("The existance of rhn system id file '"+rhnSystemIdFile+"' indicates this system is still registered using RHN Classic.");
		return rhnSystemIdFileExists;
	}
	
	public void deleteRhnSystemsRegisteredByName(String rhnUsername, String rhnPassword,String rhnHostname, String systemName) {
		
		String serverUrl = rhnHostname; if (!serverUrl.startsWith("http")) serverUrl="https://"+serverUrl;
		String command = String.format("rhn-delete-systems.py --username=%s --password=%s --serverurl=%s --delete-by-name %s", rhnUsername, rhnPassword, serverUrl, systemName);
		SSHCommandResult result = sshCommandRunner.runCommandAndWait(command);
		//return (result.getExitCode());
	}
	
	protected boolean poolsNoLongerAvailable(ArrayList<SubscriptionPool> beforeSubscription, ArrayList<SubscriptionPool> afterSubscription) {
		for(SubscriptionPool beforePool:beforeSubscription)
			if (afterSubscription.contains(beforePool))
				return false;
		return true;
	}
	
	/**
	 * @param sos TODO
	 * @param noSubscriptions TODO
	 * @param subscriptions TODO
	 * @param noproxy TODO
	 * @return the command line syntax for calling rhsm-debug system with these options
	 */
	public String rhsmDebugSystemCommand(String destination, Boolean noArchive, Boolean sos, Boolean noSubscriptions, Boolean subscriptions, String proxy, String proxyuser, String proxypassword, String noproxy) {

		// assemble the command
		String command = "rhsm-debug";					command += " system";
		if (destination!=null)							command += " --destination="+destination;
		if (noArchive!=null && noArchive)				command += " --no-archive";
		if (sos!=null && sos)							command += " --sos";
		if (noSubscriptions!=null && noSubscriptions)	command += " --no-subscriptions";
		if (subscriptions!=null && subscriptions)		command += " --subscriptions";
		if (proxy!=null)								command += " --proxy="+proxy;
		if (proxyuser!=null)							command += " --proxyuser="+proxyuser;
		if (proxypassword!=null)						command += " --proxypassword="+proxypassword;
		if (noproxy!=null)								command += " --noproxy="+noproxy;
		
		return command;
	}
	
	
	/**
	 * This command is very useful to run an rhsm cli command in a specific language.<p>
	 * It is also very useful to run an rhsm cli command in the native local (with lang=null)
	 * when the normal sshCommandRunner encounters:<br>
	 * Stderr: 'ascii' codec can't decode byte 0xe2 in position 55: ordinal not in range(128)
	 * @param localeVariable - either "LANG" or "LC_ALL" or something else.  run locale on a command line to see variables; passing null will default to LANG
	 * @param lang - "as_IN","bn_IN","de_DE", etc  See TranslationTests.supportedLangs
	 * @param rhsmCommand
	 * @return
	 */
	public SSHCommandResult runCommandWithLang(String localeVariable, String lang, String rhsmCommand){
		if (localeVariable==null) {
			localeVariable="LANG";
		}
		if (lang==null) {
			lang="";
		} else {
			if (!lang.toUpperCase().contains(".UTF")) lang=lang+".UTF-8";	// append ".UTF-8" when not already there
			lang=localeVariable+"="+lang;
		}
		String command = (lang+" "+rhsmCommand).trim();
		/* this workaround should no longer be needed after rhel70 fixes by ckozak similar to bugs 1052297 1048325 commit 6fe57f8e6c3c35ac7761b9fa5ac7a6014d69ce20 that employs #!/usr/bin/python -S    sys.setdefaultencoding('utf-8')    import site
		command = "PYTHONIOENCODING=ascii "+command;	// THIS WORKAROUND IS NEEDED AFTER master commit 056e69dc833919709bbf23d8a7b73a5345f77fdf RHEL6.4 commit 1bc25596afaf294cd217200c605737a43112a378 for bug 800323
		*/
		return sshCommandRunner.runCommandAndWait(command);
	}
	/**
	 * This command is very useful to run an rhsm cli command in a specific language.<p>
	 * It is also very useful to run an rhsm cli command in the native local (with lang=null)
	 * when the normal sshCommandRunner encounters:<br>
	 * Stderr: 'ascii' codec can't decode byte 0xe2 in position 55: ordinal not in range(128)
	 * @param lang - "as_IN","bn_IN","de_DE", etc  See TranslationTests.supportedLangs
	 * @param rhsmCommand
	 * @return
	 */
	public SSHCommandResult runCommandWithLang(String lang, String rhsmCommand){
		return (runCommandWithLang(null, lang, rhsmCommand));
	}
	public SSHCommandResult runCommandWithLangAndAssert(String lang, String rhsmCommand, Integer exitCode, String stdoutRegex, String stderrRegex){
		List<String> stdoutRegexs = null;
		if (stdoutRegex!=null) {
			stdoutRegexs = new ArrayList<String>();	stdoutRegexs.add(stdoutRegex);
		}
		List<String> stderrRegexs = null;
		if (stderrRegex!=null) {
			stderrRegexs = new ArrayList<String>();	stderrRegexs.add(stderrRegex);
		}
		return runCommandWithLangAndAssert(lang, rhsmCommand, exitCode, stdoutRegexs, stderrRegexs);
	}
	public SSHCommandResult runCommandWithLangAndAssert(String lang, String rhsmCommand, Integer exitCode, List<String> stdoutRegexs, List<String> stderrRegexs){
		// prepend the command with a well formated LANG
		if (lang==null) {
			lang="";
		} else {
			if (!lang.toUpperCase().contains(".UTF")) lang=lang+".UTF-8";	// append ".UTF-8" when not already there
			lang="LANG="+lang;
		}
		String command = lang+" "+rhsmCommand;
		/* this was an attempt to fix a problem that should have been solved on the jenkins node environment vars
		command = "LC_CTYPE= "+command;	// also unset LC_CTYPE
		*/
		/* this workaround should no longer be needed after rhel70 fixes by ckozak similar to bugs 1052297 1048325 commit 6fe57f8e6c3c35ac7761b9fa5ac7a6014d69ce20 that employs #!/usr/bin/python -S    sys.setdefaultencoding('utf-8')    import site
		command = "PYTHONIOENCODING=ascii "+command;	// THIS WORKAROUND IS NEEDED AFTER master commit 056e69dc833919709bbf23d8a7b73a5345f77fdf RHEL6.4 commit 1bc25596afaf294cd217200c605737a43112a378 for bug 800323
		*/
		
		// run the command and assert the expected results
		/* the problem with this original method call is that we could not intercept Runtime Error, hence the implementation that follows thi block includes a call to logRuntimeErrors()
		return RemoteFileTasks.runCommandAndAssert(sshCommandRunner, command, exitCode, stdoutRegexs, stderrRegexs);
		*/
		
		// run the command
		SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait(command);
		logRuntimeErrors(sshCommandResult);
		
		// assert the expected results
		if (exitCode!=null) {
			Assert.assertEquals(exitCode, sshCommandResult.getExitCode(),String.format("ExitCode from command '%s'.",command));
		}
		if (stdoutRegexs!=null) {
			for (String regex : stdoutRegexs) {
				Assert.assertContainsMatch(sshCommandResult.getStdout(),regex,"Stdout",String.format("Stdout from command '%s' contains matches to regex '%s',",command,regex));
			}
		}
		if (stderrRegexs!=null) {
			for (String regex : stderrRegexs) {
				Assert.assertContainsMatch(sshCommandResult.getStderr(),regex,"Stderr",String.format("Stderr from command '%s' contains matches to regex '%s',",command,regex));
			}
		}
		return sshCommandResult;
	}
	
	public void setLanguage(String lang){
		sshCommandRunner.runCommandAndWait("export LANG="+lang);
	}
	
	/**
	 * Bug 906550 - Any local-only certificates have been deleted.
	 * @param stdoutMsg
	 * @return
	 */
	public String workaroundForBug906550(String stdoutMsg) {
		// TEMPORARY WORKAROUND FOR BUG
		// Bug 906550 - Any local-only certificates have been deleted.
		// subscription-manager commit 7bb3751ad6f398b044efd095af61cd605d9831bf
		String bugId = "906550"; boolean invokeWorkaroundWhileBugIsOpen = true;
		// PERMANENT WORKAROUND FOR BUG
		// Bug 895447 - the count of subscriptions removed is zero,for the certs that have been imported
		// subscription-manager commit 46cbbe61713f5e9b43ff54793e2d1897d56191fd
		// subscription-manager commit 8e10e76fb5951e0b5d6c867c6c7209d8ec80dead
		//try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
		if (invokeWorkaroundWhileBugIsOpen) {
			// Any local-only certificates have been deleted.
			String subString = "Any local-only certificates have been deleted.";
			if (stdoutMsg.contains(subString)) {
			  log.info("Stripping substring '"+subString+"' from stdout while bug '"+bugId+"' is open.");
			  stdoutMsg = stdoutMsg.replace(subString, "").trim();
			}
			// 1 local certificate has been deleted.	// https://bugzilla.redhat.com/show_bug.cgi?id=895447#c8
			// 2 local certificates have been deleted.
			String subStringRegex = "(\\d+ local (certificate has|certificates have) been deleted\\.)";
			Pattern pattern = Pattern.compile(subStringRegex);
			Matcher matcher = pattern.matcher(stdoutMsg);
			while (matcher.find()) {
				log.info("Stripping substring '"+matcher.group()+"' from stdout while bug '"+bugId+"' is open.");
				stdoutMsg = stdoutMsg.replace(matcher.group(), "").trim();
			}
			// 3 subscriptions removed at the server.	// https://bugzilla.redhat.com/show_bug.cgi?id=895447#c8
			subStringRegex = "(\\d+ subscriptions removed at the server\\.)";
			pattern = Pattern.compile(subStringRegex);
			matcher = pattern.matcher(stdoutMsg);
			while (matcher.find()) {
				log.info("Stripping substring '"+matcher.group()+"' from stdout while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		return stdoutMsg;
	}
	
	/**
	 *  Bug 844455 - when consuming many entitlements, subscription-manager unsubscribe --all throws SSLTimeoutError: timed out
	 */
	protected void workaroundForBug844455() {
		
		if (false) { // DISABLE THIS WORKAROUND BECAUSE IT IS SLOW
		// TEMPORARY WORKAROUND FOR BUG
		List<File> entitlementFiles = getCurrentEntitlementCertFiles();
		int tooManyEntitlements = 30;
		if (entitlementFiles.size()>tooManyEntitlements) { 
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="844455";	// Bug 844455 - when consuming many entitlements, subscription-manager unsubscribe --all throws SSLTimeoutError: timed out
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround to avoid an SSLTimeoutError during an unregister or unsubscribe --all is to incrementally unsubscribe reducing the current entitlements to approximately "+tooManyEntitlements+".  Then resume the unregister or unsubscribe --all.");
				for (int i=entitlementFiles.size()-1; i>=tooManyEntitlements; i--) {
					unsubscribe_(null, getSerialNumberFromEntitlementCertFile(entitlementFiles.get(i)), null,null,null, null, null);
				}
			}
		}
		// END OF WORKAROUND
		}
		
		// TEMPORARY WORKAROUND FOR BUG
		int tooManyEntitlements = 30;
		List<File> entitlementFiles = getCurrentEntitlementCertFiles();
		if (entitlementFiles.size()>tooManyEntitlements) { 
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="844455";	// Bug 844455 - when consuming many entitlements, subscription-manager unsubscribe --all throws SSLTimeoutError: timed out
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround to avoid an SSLTimeoutError during an unregister or unsubscribe --all is to reduced the total consumed entitlements by unsubscribing from multiple serials until we are under "+tooManyEntitlements+" remaining.  Then resume the unregister or unsubscribe --all.");
				int avoidInfiniteLoopSize=entitlementFiles.size();
				do {
					List<BigInteger> serials = new ArrayList<BigInteger>();
					for(int e=0; e<tooManyEntitlements; e++) {
						//serials.add(getEntitlementCertFromEntitlementCertFile(entitlementFiles.get(e)).serialNumber);
						serials.add(getSerialNumberFromEntitlementCertFile(entitlementFiles.get(e)));
					}
					unsubscribe_(null, serials, null, null, null, null, null);
					entitlementFiles = getCurrentEntitlementCertFiles();
					if (avoidInfiniteLoopSize==entitlementFiles.size()) {break; /* because unsubscribe is failing */} else {avoidInfiniteLoopSize=entitlementFiles.size();}
				} while (entitlementFiles.size()>tooManyEntitlements);
			}
		}
		// END OF WORKAROUND
	}
	
	/**
	 * Bug 844455 - when consuming many entitlements, subscription-manager unsubscribe --all throws SSLTimeoutError: timed out
	 * @param jsonPools
	 * @return
	 */
	public JSONArray workaroundForBug844455(JSONArray jsonPools) {
		// TEMPORARY WORKAROUND FOR BUG
		int tooManyPools = 30;
		if (jsonPools.length()>tooManyPools) { 
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="844455";	// Bug 844455 - when consuming many entitlements, subscription-manager unsubscribe --all throws SSLTimeoutError: timed out
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround is to reducing the number of multiple pools approximately "+tooManyPools+".  Then resume the unregister or unsubscribe --all.");
				for (int i=jsonPools.length()-1; i>=30; i--) {
					jsonPools.remove(i);
				}
			}
		}
		// END OF WORKAROUND
		return jsonPools;
	}
	
	/**
	 * Bug 1040101 - consequence of an SSLTimeoutError when registering with activation keys
	 * @param jsonPools
	 * @return
	 */
	public JSONArray workaroundForBug1040101(JSONArray jsonPools) {
		// TEMPORARY WORKAROUND FOR BUG
		int tooManyPools = 30;
		if (jsonPools.length()>tooManyPools) { 
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1040101";	// Bug 1040101 - consequence of an SSLTimeoutError when registering with activation keys
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("The workaround is to reduce the number of multiple pools to approximately "+tooManyPools+".  Then resume testing with multiple pools.");
				for (int i=jsonPools.length()-1; i>=30; i--) {
					jsonPools.remove(i);
				}
			}
		}
		// END OF WORKAROUND
		return jsonPools;
	}
	
	/**
	 * Bug 876764 - String Updates: consumer -> unit
	 * @param type
	 * @return
	 */
	public boolean workaroundForBug876764(CandlepinType type) {
		// Bug 876764 - String Updates: consumer -> unit
		// This is a candlepin side bug that affects many messages sent back to the client.
		// Unfortunately these changes are present in candlepin master, but have not been deployed
		// to hosted candlepin.
		// For a short time the expected consumer/unit strings will be different depending on CandlepinType
		//return !type.equals(CandlepinType.standalone);
		return false;	// as of 9/4/2013, candlepin-0.8.25-1 has been deployed in stage which includes POST Bug 876764
	}
	
	
	/**
	 * Use this function to keep unmapped_guest_only pools from appearing in the list of available pools on a virtual system. <BR>
	 * This method fakes the job of virt-who by telling candlepin that this virtual system is actually a guest of itself (a trick for testing) <BR>
	 * Note: unmapped_guest_only pools was introduced in candlepin 0.9.42-1 commit ff5c1de80c4d2d9ca6370758ad77c8b8e0c71308 <BR>
	 * BEWARE: You should generally check if fact virt.is_guest is true before calling this method.
	 * @throws Exception
	 * @return JSONObject of the modified registered consumer (null if no guests were set)
	 */
	public JSONObject mapSystemAsAGuestOfItself() {
		// fake the job of virt-who by telling candlepin that this virtual system is actually a guest of itself (a trick for testing)
		//Let the caller decide		if (Boolean.valueOf(getFactValue("virt.is_guest"))) {
			String systemUuid = getCurrentConsumerId();
			if (systemUuid!=null) {	// is registered
				String virtUuid = getFactValue("virt.uuid");
				if (virtUuid==null) {	// can occur on s390x where virt.uuid is Unknown/null as demonstrated in Bug 815598 - [RFE] virt.uuid should not be "Unknown" in s390x when list fact 
					log.warning("Since the virt.uuid on this system is null (Unknown), create a fake virt.uuid and add it as a custom fact so we can include it in the consumer's list of guestIds thereby mapping this virt system as a guest of itself...");
					virtUuid = "fake-"+String.valueOf(System.currentTimeMillis());	// using a timestamp as a fake virt.uuid
					Map<String,String> factsMap = new HashMap<String,String>(); factsMap.put("virt.uuid",virtUuid);
					createFactsFileWithOverridingValues(factsMap);
				}
				try {
					String authenticator = this.currentlyRegisteredUsername!=null?this.currentlyRegisteredUsername:candlepinAdminUsername;
					String password = this.currentlyRegisteredPassword!=null?this.currentlyRegisteredPassword:candlepinAdminPassword;
					return CandlepinTasks.setGuestIdsForConsumer(authenticator,password, candlepinUrl, systemUuid,Arrays.asList(new String[]{"this-guest-is-self-hosted",virtUuid,"trick-for-testing"}));
				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail(e.getMessage());
				}
			}
		//}
		return null;
	}
	
	
	/**
	 * Check the SSHCommandResult for errors.  If an error is detected, log a warning with information to help troubleshoot it. 
	 * @param result
	 */
	public void logRuntimeErrors(SSHCommandResult result) {
		String issue;
		
		// TEMPORARY WORKAROUND FOR BUG	
		//	201704041152:20.587 - FINE: ssh root@hp-moonshot-03-c36.lab.eng.rdu.redhat.com subscription-manager identity
		//	201704041152:21.594 - FINE: Stdout: 
		//	201704041152:21.594 - FINE: Stderr: 
		//	This system is not yet registered. Try 'subscription-manager register --help' for more information.
		//	
		//	** COLLECTED WARNINGS **
		//	/sys/firmware/efi/systab: SMBIOS entry point missing
		//	No SMBIOS nor DMI entry point found, sorry.
		//	** END OF WARNINGS **
		//	
		//	201704041152:21.595 - FINE: ExitCode: 1
		issue = "** COLLECTED WARNINGS **";
		if (result.getStderr().contains(issue)) {
			String bugId = "1438869"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1438869 - No SMBIOS nor DMI entry point found, sorry.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201611111427:53.146 - FINE: ssh root@jsefler-rhel6server.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin
		//	201611111427:53.954 - FINE: Stdout: Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
		//	201611111427:53.954 - FINE: Stderr: 'module' object has no attribute 'PROXY_AUTHENTICATION_REQUIRED'
		//	201611111427:53.954 - FINE: ExitCode: 70
		issue = "'module' object has no attribute 'PROXY_AUTHENTICATION_REQUIRED'";
		if (result.getStderr().contains(issue)) {
			String bugId = "1394351"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1394351 - 'module' object has no attribute 'PROXY_AUTHENTICATION_REQUIRED'
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG	
		//	201611111443:59.584 - FINE: ssh root@jsefler-rhel6server.usersys.redhat.com subscription-manager register --username=testuser1 --password=password --org=admin
		//	201611111444:00.277 - FINE: Stdout: Registering to: jsefler-candlepin.usersys.redhat.com:8443/candlepin
		//	201611111444:00.277 - FINE: Stderr: global name 'socket' is not defined
		//	201611111444:00.277 - FINE: ExitCode: 70
		issue = "global name 'socket' is not defined";
		if (result.getStderr().contains(issue)) {
			String bugId = "1390688"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1390688 - global name 'socket' is not defined
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201503290125:44.627 - FINE: ssh root@jsefler-os6server.usersys.redhat.com subscription-manager unsubscribe --all
		//	201503290125:46.273 - FINE: Stdout: 
		//	3 subscriptions removed at the server.
		//	3 local certificates have been deleted.
		//	201503290125:46.274 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/dbus_interface.py", line 59, in emit_status
		//	    self.validity_iface.emit_status()
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 68, in __call__
		//	    return self._proxy_method(*args, **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 140, in __call__
		//	    **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/connection.py", line 630, in call_blocking
		//	    message, timeout)
		//	dbus.exceptions.DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
		//	201503290125:46.274 - FINE: ExitCode: 0
		issue = "Message did not receive a reply (timeout by message bus)";
		//	dbus.exceptions.DBusException: org.freedesktop.DBus.Error.NoReply: Did not receive a reply. Possible causes include: the remote application did not send a reply, the message bus security policy blocked the reply, the reply timeout expired, or the network connection was broken.
		issue = "dbus.exceptions.DBusException: org.freedesktop.DBus.Error.NoReply";
		if (result.getStderr().contains(issue)) {
			String bugId = "1207306"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1207306 - dbus.exceptions.DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		//	201503281401:54.767 - FINE: Stdout: Successfully attached a subscription for: Red Hat Enterprise Linux High Touch Beta
		//	201503281401:54.767 - FINE: Stderr: 
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/dbus_interface.py", line 59, in emit_status
		//	    self.validity_iface.emit_status()
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 68, in __call__
		//	    return self._proxy_method(*args, **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/proxies.py", line 140, in __call__
		//	    **keywords)
		//	  File "/usr/lib/python2.6/site-packages/dbus/connection.py", line 630, in call_blocking
		//	    message, timeout)
		//	dbus.exceptions.DBusException: org.freedesktop.DBus.Python.UnboundLocalError: Traceback (most recent call last):
		//	  File "/usr/lib/python2.6/site-packages/dbus/service.py", line 702, in _message_cb
		//	    retval = candidate_method(self, *args, **keywords)
		//	  File "/usr/libexec/rhsmd", line 202, in emit_status
		//	    self._dbus_properties = refresh_compliance_status(self._dbus_properties)
		//	  File "/usr/share/rhsm/subscription_manager/managerlib.py", line 920, in refresh_compliance_status
		//	    entitlements[label] = (name, state, message)
		//	UnboundLocalError: local variable 'state' referenced before assignment
		//	201503281401:54.767 - FINE: ExitCode: 0
		issue = "UnboundLocalError: local variable 'state' referenced before assignment";
		if (result.getStderr().contains(issue)) {
			String bugId = "1198369"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1198369 - UnboundLocalError: local variable 'state' referenced before assignment
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
			}
		}
		// END OF WORKAROUND
		
		//	ssh root@mgmt6.rhq.lab.eng.bos.redhat.com rhn-migrate-classic-to-rhsm.tcl '--force --servicelevel=STANDARD --destination-url=https://subscription.rhn.stage.redhat.com:443/subscription' admin nimda stage_auto_testuser redhat null null null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --force --servicelevel=STANDARD --destination-url=https://subscription.rhn.stage.redhat.com:443/subscription
		//	Legacy username: admin
		//	Legacy password:
		//	Destination username: stage_auto_testuser
		//	Destination password:
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-workstation-7
		//	rhel-x86_64-workstation-7-debuginfo
		//	rhel-x86_64-workstation-fastrack-7
		//	rhel-x86_64-workstation-fastrack-7-debuginfo
		//	rhel-x86_64-workstation-optional-7
		//	rhel-x86_64-workstation-optional-7-debuginfo
		//	rhel-x86_64-workstation-optional-fastrack-7
		//	rhel-x86_64-workstation-optional-fastrack-7-debuginfo
		//	rhel-x86_64-workstation-rh-common-7
		//	rhel-x86_64-workstation-rh-common-7-debuginfo
		//	rhel-x86_64-workstation-supplementary-7
		//	rhel-x86_64-workstation-supplementary-7-debuginfo
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-workstation-7
		//	rhel-x86_64-workstation-7-debuginfo
		//	rhel-x86_64-workstation-fastrack-7
		//	rhel-x86_64-workstation-fastrack-7-debuginfo
		//	rhel-x86_64-workstation-optional-7
		//	rhel-x86_64-workstation-optional-7-debuginfo
		//	rhel-x86_64-workstation-optional-fastrack-7
		//	rhel-x86_64-workstation-optional-fastrack-7-debuginfo
		//	rhel-x86_64-workstation-rh-common-7
		//	rhel-x86_64-workstation-rh-common-7-debuginfo
		//	rhel-x86_64-workstation-supplementary-7
		//	rhel-x86_64-workstation-supplementary-7-debuginfo
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//
		//	Attempting to register system to destination server...
		//	The system has been registered with ID: 2a236432-7d0f-490d-b2c4-10a6a16a6c01
		//	The proxy server received an invalid response from an upstream server
		//	System 'mgmt6.rhq.lab.eng.bos.redhat.com' successfully registered.
		//
		//	Stderr:
		//	ExitCode: 0	
		
		//	ssh root@cloud-qe-20.idmqe.lab.eng.bos.redhat.com rhn-migrate-classic-to-rhsm.tcl '--legacy-user=admin --legacy-password=nimda --destination-url=https://subscription.rhn.stage.redhat.com:443/subscription' null null stage_auto_testuser redhat null null null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --legacy-user=admin --legacy-password=nimda --destination-url=https://subscription.rhn.stage.redhat.com:443/subscription
		//	Destination username: stage_auto_testuser
		//	Destination password:
		//
		//	Retrieving existing legacy subscription information...
		//
		//	+-----------------------------------------------------+
		//	System is currently subscribed to these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-hpc-node-7
		//
		//	+-----------------------------------------------------+
		//	Installing product certificates for these legacy channels:
		//	+-----------------------------------------------------+
		//	rhel-x86_64-hpc-node-7
		//
		//	Product certificates installed successfully to /etc/pki/product.
		//
		//	Preparing to unregister system from legacy server...
		//	System successfully unregistered from legacy server.
		//
		//	Attempting to register system to destination server...
		//	The system has been registered with ID: ee64af7a-076c-479a-b378-2ad4439c58aa
		//	Runtime Error Lock wait timeout exceeded; try restarting transaction at com.mysql.jdbc.SQLError.createSQLException:1,078
		//	System 'cloud-qe-20.idmqe.lab.eng.bos.redhat.com' successfully registered.
		//
		//	Stderr:
		//	ExitCode: 0
		
		//	ssh root@jsefler-5server.usersys.redhat.com rhn-migrate-classic-to-rhsm.tcl --no-auto qa@redhat.com REDACTED testuser1 password admin null null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --no-auto
		//	Red Hat username: qa@redhat.com
		//	Red Hat password:
		//	Subscription Service username: testuser1
		//	Subscription Service password:
		//	Org: admin
		//	Unable to authenticate to RHN Classic. See /var/log/rhsm/rhsm.log for more details.
		//	Stderr:
		//	ExitCode: 1
		
		//	ssh root@jsefler-5server.usersys.redhat.com rhn-migrate-classic-to-rhsm.tcl --no-auto qa@redhat.com REDACTED testuser1 password admin null null
		//	Stdout:
		//	spawn rhn-migrate-classic-to-rhsm --no-auto
		//	Red Hat username: qa@redhat.com
		//	Red Hat password:
		//	Subscription Service username: testuser1
		//	Subscription Service password:
		//	Org: admin
		//	Problem encountered determining user roles in RHN Classic. Exiting.
		//	Stderr:
		//	ExitCode: 1
		
		// As shown above, rhn-migrate-classic-to-rhsm.tcl causes some problems since stderr gets stuffed into stdout and the exitCode may be zero
		// Let's tweak SSHCommandResult result for results that come from an expect script...
		if (result.getStdout().trim().startsWith("spawn")) {
			if (result.getStderr().trim().isEmpty()) {
				// create a new SSHCommandResult result with a fake exitCode and the actual stdout stuffed into stderr
				result = new SSHCommandResult(new Integer(-1), result.getStdout(), result.getStdout());
			}
		}
		
		
		// no reason to suspect an error when ExitCode is 0
		if (result.getExitCode()!=null) {
			if (result.getExitCode().equals(0)) {
				return;
			}
		}
		
		
		//	ssh root@ibm-p8-01-lp6.rhts.eng.bos.redhat.com subscription-manager subscribe --pool=8a99f9814931ea74014933dec2b60673
		//	Stdout:
		//	Stderr: Runtime Error Lock wait timeout exceeded; try restarting transaction at com.mysql.jdbc.SQLError.createSQLException:1,078
		//	ExitCode: 70
		
		//	ssh root@sachrhel7.usersys.redhat.com subscription-manager orgs --username=admin --password=changeme
		//	Stdout: 
		//	Stderr: undefined method `organizations' for nil:NilClass
		//	ExitCode: 255
		
		//	ssh root@cloud-qe-13.idm.lab.bos.redhat.com subscription-manager unregister
		//	Stdout: Unable to verify server's identity: timed out
		//	Stderr:
		//	ExitCode: 255
		
		//	ssh root@cloud-qe-9.idm.lab.bos.redhat.com subscription-manager facts --update
		//	Stdout:
		//	Stderr: Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.
		//	ExitCode: 255
		
		//	ssh root@yttrium.idm.lab.bos.redhat.com subscription-manager unregister
		//	Stdout: Remote server error. Please check the connection details, or see /var/log/rhsm/rhsm.log for more information.
		//	Stderr:
		//	ExitCode: 255
		//  ^^^^^^^^^^^^^ Indicative of a 502 Proxy Error
		
		//	ssh root@ibm-p740-01-lp2.rhts.eng.bos.redhat.com subscription-manager register --username=stage_auto_testuser --password=redhat --serverurl=subscription.rhn.stage.redhat.com/subscription --force
		//	Stdout:
		//	Stderr: Unable to reach the server at subscription.rhn.stage.redhat.com:443/subscription
		//	ExitCode: 69
		
		//	ssh root@intel-canoepass-12.lab.bos.redhat.com subscription-manager subscribe --pool=8a99f981498757d40149a5a9b04f4b00
		//	Stdout:
		//	Stderr: The proxy server received an invalid response from an upstream server
		//	ExitCode: 70
		
		//	ssh root@ibm-p8-03-lp2.rhts.eng.bos.redhat.com subscription-manager unsubscribe --all
		//	Stdout:
		//	Stderr: Request failed due to concurrent modification, please re-try.
		//	ExitCode: 70
		
		//	ssh root@ibm-p8-05-lp8.rhts.eng.bos.redhat.com subscription-manager subscribe --pool=8a99f9814aaae2d4014aac50279c01c8
		//	Stdout: The system is unable to complete the requested transaction
		//	Stderr:
		//	ExitCode: 1
		
		//	ssh root@cloud-qe-4.idmqe.lab.eng.bos.redhat.com subscription-manager repo-override --list
		//	Stdout:
		//	Stderr: 'NoneType' object is not iterable
		//	ExitCode: 70
		
		//	ssh root@ibm-x3550m3-09.lab.eng.brq.redhat.com subscription-manager release --list
		//	Stdout:
		//	Stderr: ''
		//	ExitCode: 70
		
		//	ssh root@ibm-z10-44.rhts.eng.bos.redhat.com subscription-manager facts --update
		//	Stdout: 
		//	Stderr: 
		//	ExitCode: 70
		
		//	ssh root@ibm-x3550m3-08.lab.eng.brq.redhat.com subscription-manager list --installed
		//	Stdout:
		//	Stderr:
		//	ExitCode: null
		
		//	ssh root@ibm-z10-77.rhts.eng.bos.redhat.com subscription-manager list --available
		//	Stdout:
		//	Stderr: Unable to serialize objects to JSON.
		//	ExitCode: 70
		
		//	ssh root@qe-blade-09.idmqe.lab.eng.bos.redhat.com subscription-manager register --username=stage_2013_data_center_test --password=redhat --org=7965071
		//	Stdout: Registering to: subscription.rhsm.stage.redhat.com:443/subscription
		//	Stderr: 'idCert'
		//	ExitCode: 70
		
		//	2014-04-08 16:41:56,930 [INFO] subscription-manager @managercli.py:299 - Server Versions: {'candlepin': 'Unknown', 'server-type': 'Red Hat Subscription Management'}
		//	2014-04-08 16:41:56,933 [DEBUG] subscription-manager @connection.py:418 - Loaded CA certificates from /etc/rhsm/ca/: candlepin-stage.pem, redhat-uep.pem
		//	2014-04-08 16:41:56,933 [DEBUG] subscription-manager @connection.py:450 - Making request: DELETE /subscription/consumers/892d9649-8079-43fe-ad04-2c3a83673f6e
		//	2014-04-08 16:42:07,444 [DEBUG] subscription-manager @connection.py:473 - Response: status=500
		//	2014-04-08 16:42:07,444 [ERROR] subscription-manager @connection.py:502 - Response: 500
		//	2014-04-08 16:42:07,444 [ERROR] subscription-manager @connection.py:503 - JSON parsing error: Expecting value: line 1 column 1 (char 0)
		//	2014-04-08 16:42:07,445 [ERROR] subscription-manager @managercli.py:156 - Unregister failed
		//	2014-04-08 16:42:07,445 [ERROR] subscription-manager @managercli.py:157 - Server error attempting a DELETE to /subscription/consumers/892d9649-8079-43fe-ad04-2c3a83673f6e returned status 500
		//	Traceback (most recent call last):
		//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1161, in _do_command
		//	    managerlib.unregister(self.cp, consumer)
		//	  File "/usr/share/rhsm/subscription_manager/managerlib.py", line 796, in unregister
		//	    uep.unregisterConsumer(consumer_uuid)
		//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 898, in unregisterConsumer
		//	    return self.conn.request_delete(method)
		//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 566, in request_delete
		//	    return self._request("DELETE", method, params)
		//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 482, in _request
		//	    self.validateResponse(result, request_type, handler)
		//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 530, in validateResponse
		//	    handler=handler)
		//	RemoteServerException: Server error attempting a DELETE to /subscription/consumers/892d9649-8079-43fe-ad04-2c3a83673f6e returned status 500
		
		if ((result.getStdout()+result.getStderr()).toLowerCase().contains("Runtime Error".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Error updating system data on the server".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("undefined method".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("The proxy server received an invalid response from an upstream server".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Problem encountered".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Remote server error".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Unable to verify server's identity".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Unable to reach the server".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Connection reset by peer".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Request failed due to concurrent modification".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("The system is unable to complete the requested transaction".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("object is not iterable".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("Unable to serialize objects to JSON.".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("timeout by message bus".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("timed out".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("'idCert'".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains(("See "+rhsmLogFile).toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().contains("''".toLowerCase()) ||
			(result.getStdout()+result.getStderr()).toLowerCase().trim().isEmpty() ||
			(result.getExitCode()==null) ||
			(result.getExitCode() > (Integer.valueOf(1)))) {	// some commands legitimately return 1
			// [root@jsefler-7 ~]# LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			String getTracebackCommand = "LINE_NUMBER=$(grep --line-number 'Making request:' "+rhsmLogFile+" | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n \"$LINE_NUMBER\" ]; then tail -n +$LINE_NUMBER "+rhsmLogFile+"; fi;";
			SSHCommandResult getTracebackCommandResult = sshCommandRunner.runCommandAndWaitWithoutLogging(getTracebackCommand);
			if (!getTracebackCommandResult.getStdout().isEmpty()) log.warning("Last request from "+rhsmLogFile+":\n"+getTracebackCommandResult.getStdout());
///*debugTestSSL*/ if ((result.getStdout()+result.getStderr()).toLowerCase().contains("Unable to verify server's identity".toLowerCase())) log.warning("Current RHSM Configuration:\n"); sshCommandRunner.runCommandAndWait("subscription-manager config --list");
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	201411121258:42.413 - FINE: ssh root@ibm-p8-kvm-04-guest-01.rhts.eng.bos.redhat.com subscription-manager register --username=stage_auto_testuser --password=**** --baseurl=myhost.example.com/ --force (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201411121258:53.631 - FINE: Stdout: 
			//	The system with UUID 44d314cd-23e4-43e3-a256-a74828d6377f has been unregistered
			//	The system has been registered with ID: 2017fa81-1b82-46da-84db-4b789118c6a9 
			//	Remote server error. Please check the connection details, or see /var/log/rhsm/rhsm.log for more information.
			//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411121258:53.632 - FINE: Stderr:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411121258:53.632 - FINE: ExitCode: 70 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411121258:53.632 - FINE: ssh root@ibm-p8-kvm-04-guest-01.rhts.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi; (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201411121258:53.837 - WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2014-11-12 12:58:53,331 [DEBUG] rhsmd @connection.py:466 - Making request: GET /subscription/consumers/2017fa81-1b82-46da-84db-4b789118c6a9/compliance
			//	2014-11-12 12:58:53,802 [DEBUG] rhsmd @connection.py:489 - Response: status=401
			//	2014-11-12 12:58:54,770 [DEBUG] subscription-manager @connection.py:489 - Response: status=502
			//	2014-11-12 12:58:54,770 [ERROR] subscription-manager @connection.py:518 - Response: 502
			//	2014-11-12 12:58:54,771 [ERROR] subscription-manager @connection.py:519 - JSON parsing error: No JSON object could be decoded
			//	2014-11-12 12:58:54,771 [ERROR] subscription-manager @managercli.py:157 - exception caught in subscription-manager
			//	2014-11-12 12:58:54,771 [ERROR] subscription-manager @managercli.py:158 - Server error attempting a GET to /subscription/ returned status 502
			//	Traceback (most recent call last):
			//	  File "/usr/sbin/subscription-manager", line 82, in <module>
			//	    sys.exit(abs(main() or 0))
			//	  File "/usr/sbin/subscription-manager", line 73, in main
			//	    return managercli.ManagerCLI().main()
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 2530, in main
			//	    return CLI.main(self)
			//	  File "/usr/share/rhsm/subscription_manager/cli.py", line 160, in main
			//	    return cmd.main()
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 478, in main
			//	    return_code = self._do_command()
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1092, in _do_command
			//	    profile_mgr.update_check(self.cp, consumer['uuid'], True)
			//	  File "/usr/share/rhsm/subscription_manager/cache.py", line 348, in update_check
			//	    if not uep.supports_resource(PACKAGES_RESOURCE):
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 716, in supports_resource
			//	    self._load_supported_resources()
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 703, in _load_supported_resources
			//	    resources_list = self.conn.request_get("/")
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 570, in request_get
			//	    return self._request("GET", method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 498, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 546, in validateResponse
			//	    handler=handler)
			//	RemoteServerException: Server error attempting a GET to /subscription/ returned status 502
			
			//	2014-11-12 19:10:09,319 [DEBUG] subscription-manager @connection.py:466 - Making request: GET /subscription/consumers/2031a746-d558-4dbe-9edb-4bded14b6a92/certificates/serials
			//	2014-11-12 19:10:09,820 [DEBUG] subscription-manager @connection.py:489 - Response: status=502
			//	2014-11-12 19:10:09,820 [ERROR] subscription-manager @connection.py:518 - Response: 502
			//	2014-11-12 19:10:09,820 [ERROR] subscription-manager @connection.py:519 - JSON parsing error: No JSON object could be decoded
			//	2014-11-12 19:10:09,821 [ERROR] subscription-manager @managercli.py:157 - Unable to attach: Server error attempting a GET to /subscription/consumers/2031a746-d558-4dbe-9edb-4bded14b6a92/certificates/serials returned status 502
			//	2014-11-12 19:10:09,821 [ERROR] subscription-manager @managercli.py:158 - Server error attempting a GET to /subscription/consumers/2031a746-d558-4dbe-9edb-4bded14b6a92/certificates/serials returned status 502
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1509, in _do_command
			//	    report = self.entcertlib.update()
			//	  File "/usr/share/rhsm/subscription_manager/certlib.py", line 31, in update
			//	    self.report = self.locker.run(self._do_update)
			//	  File "/usr/share/rhsm/subscription_manager/certlib.py", line 17, in run
			//	    return action()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 43, in _do_update
			//	    return action.perform()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 119, in perform
			//	    expected = self._get_expected_serials()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 254, in _get_expected_serials
			//	    exp = self.get_certificate_serials_list()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 234, in get_certificate_serials_list
			//	    reply = self.uep.getCertificateSerials(identity.uuid)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 965, in getCertificateSerials
			//	    return self.conn.request_get(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 570, in request_get
			//	    return self._request("GET", method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 498, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 546, in validateResponse
			//	    handler=handler)
			//	RemoteServerException: Server error attempting a GET to /subscription/consumers/2031a746-d558-4dbe-9edb-4bded14b6a92/certificates/serials returned status 502
			
			//	2014-11-26 10:03:18,366 [DEBUG] subscription-manager @connection.py:466 - Making request: GET /subscription/consumers/69dfafe4-04b4-44aa-8173-06c215632710
			//	2014-11-26 10:03:25,766 [DEBUG] subscription-manager @connection.py:489 - Response: status=502
			//	2014-11-26 10:03:25,767 [ERROR] subscription-manager @managercli.py:874 - The proxy server received an invalid response from an upstream server
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 868, in _do_command
			//	    self.show_service_level()
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 892, in show_service_level
			//	    consumer = self.cp.getConsumer(self.identity.uuid)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 869, in getConsumer
			//	    return self.conn.request_get(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 570, in request_get
			//	    return self._request("GET", method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 498, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 540, in validateResponse
			//	    raise RestlibException(response['status'], error_msg)
			//	RestlibException: The proxy server received an invalid response from an upstream server
			issue = "Response: status=502";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1105173"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1105173 - subscription-manager encounters frequent 502 responses from stage IT-Candlepin
				// duplicate of Bug 1113741 - RHEL 7 (and 6?): subscription-manager fails with "JSON parsing error: No JSON object could be decoded" error
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2014-11-16 05:22:14,215 [DEBUG] subscription-manager @connection.py:466 - Making request: POST /subscription/consumers/a21f1acf-ddd7-420a-8903-d75e9ba45e1f/entitlements?pool=8a99f981498757d40149a5a9b04f4b00
			//	2014-11-16 05:23:14,628 [ERROR] subscription-manager @managercli.py:157 - Unable to attach: timed out
			//	2014-11-16 05:23:14,628 [ERROR] subscription-manager @managercli.py:158 - timed out
			//	Traceback (most recent call last):
			//	File "/usr/share/rhsm/subscription_manager/managercli.py", line 1462, in _do_command
			//	ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 974, in bindByEntitlementPool
			//	return self.conn.request_post(method)
			//	File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 573, in request_post
			//	return self._request("POST", method, params)
			//	File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 480, in _request
			//	response = conn.getresponse()
			//	File "/usr/lib64/python2.7/httplib.py", line 1045, in getresponse
			//	response.begin()
			//	File "/usr/lib64/python2.7/httplib.py", line 409, in begin
			//	version, status, reason = self._read_status()
			//	File "/usr/lib64/python2.7/httplib.py", line 365, in _read_status
			//	line = self.fp.readline(_MAXLINE + 1)
			//	File "/usr/lib64/python2.7/socket.py", line 476, in readline
			//	data = self._sock.recv(self._rbufsize)
			//	File "/usr/lib64/python2.7/site-packages/M2Crypto/SSL/Connection.py", line 228, in read
			//	return self._read_bio(size)
			//	File "/usr/lib64/python2.7/site-packages/M2Crypto/SSL/Connection.py", line 213, in _read_bio
			//	return m2.ssl_read(self.ssl, size, self._timeout)
			//	SSLTimeoutError: timed out
			issue = "SSLTimeoutError: timed out";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1165239"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1165239 - subscription-manager encounters frequent SSLTimeoutErrors from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-07-20 16:59:05,912 [DEBUG] subscription-manager:31469 @connection.py:573 - Making request: PUT /subscription/consumers/a508bc4a-0986-4795-be1f-8a058a2b44a4/packages
			//	2016-07-20 16:59:36,992 [DEBUG] subscription-manager:31469 @connection.py:602 - Response: status=500
			//	2016-07-20 16:59:36,993 [ERROR] subscription-manager:31469 @connection.py:631 - Response: 500
			//	2016-07-20 16:59:36,993 [ERROR] subscription-manager:31469 @connection.py:632 - JSON parsing error: No JSON object could be decoded
			//	2016-07-20 16:59:36,993 [ERROR] subscription-manager:31469 @cache.py:166 - Error updating system data on the server
			//	2016-07-20 16:59:36,993 [ERROR] subscription-manager:31469 @cache.py:167 - Server error attempting a PUT to /subscription/consumers/a508bc4a-0986-4795-be1f-8a058a2b44a4/packages returned status 500
			//	Traceback (most recent call last):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cache.py", line 158, in update_check
			//	    self._sync_with_server(uep, consumer_uuid)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cache.py", line 417, in _sync_with_server
			//	    self.current_profile.collect())
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1055, in updatePackageProfile
			//	    ret = self.conn.request_put(method, pkg_dicts)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 703, in request_put
			//	    return self._request("PUT", method, params)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 611, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 667, in validateResponse
			//	    handler=handler)
			//	RemoteServerException: Server error attempting a PUT to /subscription/consumers/a508bc4a-0986-4795-be1f-8a058a2b44a4/packages returned status 500
			//	2016-07-20 16:59:36,995 [ERROR] subscription-manager:31469 @managercli.py:174 - exception caught in subscription-manager
			//	2016-07-20 16:59:36,995 [ERROR] subscription-manager:31469 @managercli.py:175 - Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.
			//	Traceback (most recent call last):
			//	  File "/usr/sbin/subscription-manager", line 81, in <module>
			//	    sys.exit(abs(main() or 0))
			//	  File "/usr/sbin/subscription-manager", line 72, in main
			//	    return managercli.ManagerCLI().main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 2732, in main
			//	    return CLI.main(self)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cli.py", line 160, in main
			//	    return cmd.main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 526, in main
			//	    return_code = self._do_command()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 1161, in _do_command
			//	    profile_mgr.update_check(self.cp, consumer['uuid'], True)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cache.py", line 405, in update_check
			//	    return CacheManager.update_check(self, uep, consumer_uuid, force)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cache.py", line 168, in update_check
			//	    raise Exception(_("Error updating system data on the server, see /var/log/rhsm/rhsm.log "
			//	Exception: Error updating system data on the server, see /var/log/rhsm/rhsm.log for more details.
			issue = "Error updating system data on the server";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1358508"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1358508 - Error updating system data on the server
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2014-11-18 13:31:35,122 [DEBUG] subscription-manager @connection.py:466 - Making request: DELETE /subscription/consumers/b7b49c97-f25a-43b7-9820-e7cc76bccbc3/entitlements
			//	2014-11-18 13:32:34,127 [DEBUG] subscription-manager @connection.py:489 - Response: status=500
			//	2014-11-18 13:32:34,127 [ERROR] subscription-manager @managercli.py:1625 - Runtime Error Lock wait timeout exceeded; try restarting transaction at com.mysql.jdbc.SQLError.createSQLException:1,078
			issue = "Runtime Error Lock wait timeout exceeded";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1084782"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1084782 - Runtime Error Lock wait timeout exceeded; try restarting transaction at com.mysql.jdbc.SQLError.createSQLException:1,078
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1165295"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1165295 - subscription-manager encounters frequent "Runtime Error Lock wait timeout exceeded" from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1161736"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1161736 - subscription-manager doesn't behave in a consistent way
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1231308"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1231308 - subscription-manager encounters frequent "Runtime Error Lock wait timeout exceeded"/"Unable to verify server's identity" from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1357117"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1357117 - subscription-manager encounters frequent "Runtime Error Lock wait timeout exceeded" from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2015-03-31 03:19:17,873 [DEBUG] rhsmd:22939 @connection.py:494 - Making request: GET /candlepin/consumers/589555ff-42bf-45e4-9799-1419bc945006/compliance
			//	2015-03-31 03:19:18,270 [DEBUG] rhsmd:22939 @connection.py:521 - Response: status=200, requestUuid=eabfdf97-edf7-44ac-91ee-815e82c350be
			//	2015-03-31 03:19:18,339 [DEBUG] rhsmd:22939 @cache.py:272 - Started thread to write cache: /var/lib/rhsm/cache/entitlement_status.json
			//	2015-03-31 03:19:19,130 [DEBUG] subscription-manager:22933 @connection.py:521 - Response: status=500
			//	2015-03-31 03:19:19,130 [ERROR] subscription-manager:22933 @managercli.py:161 - Unregister failed
			//	2015-03-31 03:19:19,130 [ERROR] subscription-manager:22933 @managercli.py:162 - Runtime Error could not execute statement at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse:2,102
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1240, in _do_command
			//	    managerlib.unregister(self.cp, self.identity.uuid)
			//	  File "/usr/share/rhsm/subscription_manager/managerlib.py", line 788, in unregister
			//	    uep.unregisterConsumer(consumer_uuid)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 990, in unregisterConsumer
			//	    return self.conn.request_delete(method)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 614, in request_delete
			//	    return self._request("DELETE", method, params)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 530, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 572, in validateResponse
			//	    raise RestlibException(response['status'], error_msg)
			//	RestlibException: Runtime Error could not execute statement at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse:2,102
			issue = "Runtime Error could not execute statement at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse:2,102";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1207721"; boolean invokeWorkaroundWhileBugIsOpen = true;	//	Bug 1207721 - Runtime Error could not execute statement at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse:2,102
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	201411291639:35.361 - FINE: ssh root@cloud-qe-22.idmqe.lab.eng.bos.redhat.com subscription-manager unsubscribe --serial=8305861300287544370 (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201411291639:41.734 - FINE: Stdout: 
			//	Serial numbers unsuccessfully removed at the server:
			//	   The proxy server received an invalid response from an upstream server
			//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411291639:41.735 - FINE: Stderr:  (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411291639:41.735 - FINE: ExitCode: 1 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201411291639:41.735 - FINE: ssh root@cloud-qe-22.idmqe.lab.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi; (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201411291639:41.924 - WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2014-11-29 16:39:42,297 [DEBUG] subscription-manager @connection.py:466 - Making request: GET /subscription/consumers/5de32c6a-7a46-4769-a69f-ea7c4f7a8db2/compliance
			//	2014-11-29 16:39:42,799 [DEBUG] subscription-manager @connection.py:489 - Response: status=200
			//	2014-11-29 16:39:42,799 [DEBUG] subscription-manager @cache.py:249 - Started thread to write cache: /var/lib/rhsm/cache/entitlement_status.json
			//	2014-11-29 16:39:42,800 [DEBUG] subscription-manager @cert_sorter.py:193 - valid entitled products: []
			//	2014-11-29 16:39:42,800 [DEBUG] subscription-manager @cert_sorter.py:194 - expired entitled products: []
			//	2014-11-29 16:39:42,800 [DEBUG] subscription-manager @cert_sorter.py:195 - partially entitled products: []
			//	2014-11-29 16:39:42,800 [DEBUG] subscription-manager @cert_sorter.py:196 - unentitled products: ['71']
			//	2014-11-29 16:39:42,800 [DEBUG] subscription-manager @cert_sorter.py:197 - future products: []
			//	2014-11-29 16:39:42,801 [DEBUG] subscription-manager @cert_sorter.py:198 - partial stacks: []
			//	2014-11-29 16:39:42,801 [DEBUG] subscription-manager @cert_sorter.py:199 - entitlements valid until: None
			//	2014-11-29 16:39:42,913 [INFO] rhsmd @rhsmd:273 - rhsmd started
			//	2014-11-29 16:39:42,915 [INFO] rhsmd @rhsmd:182 - D-Bus interface com.redhat.SubscriptionManager.EntitlementStatus.update_status called with status = 1
			//	2014-11-29 16:39:42,951 [DEBUG] rhsmd @identity.py:131 - Loading consumer info from identity certificates.
			//	2014-11-29 16:39:42,954 [INFO] rhsmd @rhsmd:149 - D-Bus signal com.redhat.SubscriptionManager.EntitlementStatus.entitlement_status_changed emitted
			//	2014-11-29 16:39:42,969 [DEBUG] subscription-manager @dbus_interface.py:60 - Failed to update rhsmd
			//	2014-11-29 16:39:42,969 [ERROR] subscription-manager @dbus_interface.py:61 - org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/dbus_interface.py", line 57, in _update
			//	    self.validity_iface.emit_status(ignore_reply=self.has_main_loop)
			//	  File "/usr/lib64/python2.7/site-packages/dbus/proxies.py", line 145, in __call__
			//	    **keywords)
			//	  File "/usr/lib64/python2.7/site-packages/dbus/connection.py", line 651, in call_blocking
			//	    message, timeout)
			//	DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			issue = "Message did not receive a reply (timeout by message bus)";
			issue = "DBusException: org.freedesktop.DBus.Error.NoReply";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1207306"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1207306 - dbus.exceptions.DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2017-03-30 15:32:21.794  FINE: ssh root@ibm-x3550m3-07.lab.eng.brq.redhat.com subscription-manager list --available
			//	2017-03-30 15:32:22.569  FINE: Stdout:
			//	2017-03-30 15:32:22.569  FINE: Stderr: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			//	2017-03-30 15:32:22.569  FINE: ExitCode: 70
			//	2017-03-30 15:32:22.570  FINE: ssh root@ibm-x3550m3-07.lab.eng.brq.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2017-03-30 15:32:23.124  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2017-03-30 21:32:16,929 [DEBUG] subscription-manager:12742:MainThread @connection.py:473 - Making request: GET /subscription/owners/7964055/pools?consumer=0d8975a8-8400-4e6a-a009-3f425f0ca843
			//	2017-03-30 21:32:18,436 [INFO] subscription-manager:12742:MainThread @connection.py:509 - Response: status=200, requestUuid=268b7e91-fed9-4f55-ac59-e200be523559, request="GET /subscription/owners/7964055/pools?consumer=0d8975a8-8400-4e6a-a009-3f425f0ca843"
			//	2017-03-30 21:32:18,441 [DEBUG] subscription-manager:12742:MainThread @managerlib.py:550 - Filtering 0 total pools
			//	2017-03-30 21:32:18,441 [DEBUG] subscription-manager:12742:MainThread @managerlib.py:556 -      Removed 0 incompatible pools
			//	2017-03-30 21:32:18,441 [DEBUG] subscription-manager:12742:MainThread @managerlib.py:589 -      13 pools to display, -13 filtered out
			//	2017-03-30 21:32:21,558 [DEBUG] subscription-manager:12802:MainThread @https.py:54 - Using standard libs to provide httplib and ssl
			//	2017-03-30 21:32:21,659 [DEBUG] subscription-manager:12802:MainThread @dbus_interface.py:35 - self.has_main_loop=False
			//	2017-03-30 21:32:21,721 [DEBUG] subscription-manager:12802:MainThread @ga_loader.py:89 - ga_loader GaImporterGtk3
			//	2017-03-30 21:32:21,729 [DEBUG] subscription-manager:12802:MainThread @plugins.py:569 - loaded plugin modules: [<module 'container_content' from '/usr/share/rhsm-plugins/container_content.pyc'>, <module 'ostree_content' from '/usr/share/rhsm-plugins/ostree_content.pyc'>]
			//	2017-03-30 21:32:21,729 [DEBUG] subscription-manager:12802:MainThread @plugins.py:570 - loaded plugins: {'container_content.ContainerContentPlugin': <container_content.ContainerContentPlugin object at 0x1221590>, 'ostree_content.OstreeContentPlugin': <ostree_content.OstreeContentPlugin object at 0x1221b50>}
			//	2017-03-30 21:32:21,729 [DEBUG] subscription-manager:12802:MainThread @identity.py:132 - Loading consumer info from identity certificates.
			//	2017-03-30 21:32:21,787 [INFO] subscription-manager:12802:MainThread @managercli.py:316 - X-Correlation-ID: 47a6ded7ad2b4611935850319cf8d19b
			//	2017-03-30 21:32:21,788 [INFO] subscription-manager:12802:MainThread @managercli.py:394 - Client Versions: {'python-rhsm': '1.19.2-1.el7', 'subscription-manager': '1.19.4-1.el7'}
			//	2017-03-30 21:32:21,789 [INFO] subscription-manager:12802:MainThread @connection.py:763 - Connection built: host=subscription.rhsm.stage.redhat.com port=443 handler=/subscription auth=identity_cert ca_dir=/etc/rhsm/ca/ insecure=False
			//	2017-03-30 21:32:21,789 [INFO] subscription-manager:12802:MainThread @connection.py:763 - Connection built: host=subscription.rhsm.stage.redhat.com port=443 handler=/subscription auth=none
			//	2017-03-30 21:32:21,790 [INFO] subscription-manager:12802:MainThread @managercli.py:369 - Consumer Identity name=ibm-x3550m3-07.lab.eng.brq.redhat.com uuid=0d8975a8-8400-4e6a-a009-3f425f0ca843
			//	2017-03-30 21:32:21,790 [DEBUG] subscription-manager:12802:MainThread @cache.py:157 - Checking current system info against cache: /var/lib/rhsm/facts/facts.json
			//	2017-03-30 21:32:21,915 [ERROR] subscription-manager:12802:MainThread @managercli.py:179 - exception caught in subscription-manager
			//	2017-03-30 21:32:21,915 [ERROR] subscription-manager:12802:MainThread @managercli.py:180 - org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			//	Traceback (most recent call last):
			//	  File "/usr/sbin/subscription-manager", line 89, in <module>
			//	    sys.exit(abs(main() or 0))
			//	  File "/usr/sbin/subscription-manager", line 80, in main
			//	    return managercli.ManagerCLI().main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 2792, in main
			//	    return CLI.main(self)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cli.py", line 160, in main
			//	    return cmd.main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 544, in main
			//	    return_code = self._do_command()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 2372, in _do_command
			//	    filter_string=self.options.filter_string)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 325, in get_available_entitlements
			//	    overlapping, uninstalled, text, filter_string)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 529, in get_filtered_pools_list
			//	    self.identity.uuid, active_on=active_on, filter_string=filter_string):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 283, in list_pools
			//	    require(FACTS).update_check(uep, consumer_uuid)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cache.py", line 158, in update_check
			//	    if self.has_changed() or force:
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/facts.py", line 69, in has_changed
			//	    self.facts = self.get_facts(True)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/facts.py", line 79, in get_facts
			//	    facts = facts_dbus_client.GetFacts()
			//	  File "/usr/lib/python2.7/site-packages/rhsmlib/dbus/facts/client.py", line 57, in GetFacts
			//	    return self.interface.GetFacts(*args, **kwargs)
			//	  File "/usr/lib64/python2.7/site-packages/dbus/proxies.py", line 70, in __call__
			//	    return self._proxy_method(*args, **keywords)
			//	  File "/usr/lib64/python2.7/site-packages/dbus/proxies.py", line 145, in __call__
			//	    **keywords)
			//	  File "/usr/lib64/python2.7/site-packages/dbus/connection.py", line 651, in call_blocking
			//	    message, timeout)
			//	DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
			issue = "Message did not receive a reply (timeout by message bus)";
			issue = "DBusException: org.freedesktop.DBus.Error.NoReply";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1438561"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1438561 - DBusException: org.freedesktop.DBus.Error.NoReply: Message did not receive a reply (timeout by message bus)
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2015-05-11 20:56:36,029 [DEBUG] subscription-manager:70001 @connection.py:494 - Making request: POST /subscription/consumers/4abab952-0b4b-4daa-bb34-5a0938c99672/entitlements?pool=8a99f9864d0ba396014d10a2642a1537
			//	2015-05-11 20:56:48,012 [DEBUG] subscription-manager:70001 @connection.py:521 - Response: status=500
			//	2015-05-11 20:56:48,012 [ERROR] subscription-manager:70001 @managercli.py:1496 - Runtime Error com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
			//	Traceback (most recent call last):
			//	File "/usr/share/rhsm/subscription_manager/managercli.py", line 1486, in _do_command
			//	ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 1017, in bindByEntitlementPool
			//	return self.conn.request_post(method)
			//	File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 605, in request_post
			//	return self._request("POST", method, params)
			//	File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 530, in _request
			//	self.validateResponse(result, request_type, handler)
			//	File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 572, in validateResponse
			//	raise RestlibException(response['status'], error_msg)
			//	RestlibException: Runtime Error com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
			issue = "Runtime Error com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock";
			if (getTracebackCommandResult.getStdout().contains(issue)) {
				String bugId = "1220830"; boolean invokeWorkaroundWhileBugIsOpen = true;	//	Bug 1220830 - subscription-manager encounters occasional Deadlock from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	201506121027:12.829 - FINE: ssh root@jsefler-os6.usersys.redhat.com subscription-manager subscribe --pool=8a99f98a4d730ed9014d96e90c9d275a --pool=8a99f9874d730eea014d96e682e1121e --pool=8a99f9874d730eea014d96e6849f124b --pool=8a99f9894d730ec6014d96e73e4064d9 --pool=8a99f9874d730eea014d96e687b4129e --pool=8a99f9874d730eea014d96e680ae11b9 --pool=8a99f9874d730eea014d96e6886312c7 --pool=8a99f9874d730eea014d96e68651126b --pool=8a99f9874d730eea014d96e67fd41189 --pool=8a99f9874d730eea014d96e682c71206 --pool=8a99f9874d730eea014d96e67fbb1174 --pool=8a99f9874d730eea014d96e6809811a2 --pool=8a99f9874d730eea014d96e683d1123a --pool=8a99f9874d730eea014d96e681b611eb --pool=8a99f9874d730eea014d96e6819e11d4 --pool=8a99f9874d730eea014d96e6890c12eb (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201506121036:51.780 - FINE: Stdout: 
			//	Successfully attached a subscription for: Red Hat JBoss Enterprise Application Platform with Management, 16 Core Standard, L3 Support Partner
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Workstation, Standard
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Server, Standard (Physical or Virtual Nodes)
			//	Successfully attached a subscription for: Red Hat Enterprise Linux for IBM POWER, Standard (4 sockets) (Up to 30 LPARs) with Smart Management
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Server, Premium (Physical or Virtual Nodes)
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Server for HPC Compute Node, Self-support (8 sockets) (Up to 1 guest)
			//	Successfully attached a subscription for: Red Hat Enterprise Linux High Touch Beta
			//	Successfully attached a subscription for: Red Hat Enterprise Linux for Virtual Datacenters, Premium
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Desktop, Self-support
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Workstation, Standard
			//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201506121036:51.796 - FINE: Stderr: Unable to verify server's identity: (104, 'Connection reset by peer')
			//	 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201506121036:51.798 - FINE: ExitCode: 70 (com.redhat.qe.tools.SSHCommandRunner.runCommandAndWait)
			//	201506121036:51.814 - FINE: ssh root@jsefler-os6.usersys.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi; (com.redhat.qe.tools.SSHCommandRunner.run)
			//	201506121036:51.868 - WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2015-06-12 10:36:02,337 [DEBUG] rhsmd:1998 @connection.py:494 - Making request: GET /subscription/consumers/e5c74163-3bdb-47c6-ba26-f4d392d8bd92/compliance
			//	2015-06-12 10:36:51,737 [ERROR] subscription-manager:1721 @managercli.py:160 - Unable to attach: (104, 'Connection reset by peer')
			//	2015-06-12 10:36:51,737 [ERROR] subscription-manager:1721 @managercli.py:161 - (104, 'Connection reset by peer')
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1486, in _do_command
			//	    ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 1017, in bindByEntitlementPool
			//	    return self.conn.request_post(method)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 605, in request_post
			//	    return self._request("POST", method, params)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 512, in _request
			//	    response = conn.getresponse()
			//	  File "/usr/lib64/python2.6/httplib.py", line 1012, in getresponse
			//	    response.begin()
			//	  File "/usr/lib64/python2.6/httplib.py", line 404, in begin
			//	    version, status, reason = self._read_status()
			//	  File "/usr/lib64/python2.6/httplib.py", line 360, in _read_status
			//	    line = self.fp.readline(_MAXLINE + 1)
			//	  File "/usr/lib64/python2.6/socket.py", line 479, in readline
			//	    data = self._sock.recv(self._rbufsize)
			//	  File "/usr/lib64/python2.6/site-packages/M2Crypto/SSL/Connection.py", line 228, in read
			//	    return self._read_bio(size)
			//	  File "/usr/lib64/python2.6/site-packages/M2Crypto/SSL/Connection.py", line 213, in _read_bio
			//	    return m2.ssl_read(self.ssl, size, self._timeout)
			//	SSLError: (104, 'Connection reset by peer')
			issue = "(104, 'Connection reset by peer')";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1231308"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1231308 - subscription-manager encounters frequent "Runtime Error Lock wait timeout exceeded"/"Unable to verify server's identity" from stage IT-Candlepin
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1302364"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1302364 - Unable to verify server's identity: (104, 'Connection reset by peer')
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2018-01-15 14:39:26,359 [DEBUG] subscription-manager:34555:MainThread  @connection.py:543 - Making request: DELETE  /subscription/consumers/c3b6eb11-da15-47ea-893d-7d3896af5952/entitlements
			//	2018-01-15  14:39:55,131 [ERROR] subscription-manager:34555:MainThread  @managercli.py:181 - Unable to perform remove due to the following  exception: [Errno 104] Connection reset by peer
			//	2018-01-15 14:39:55,131 [ERROR] subscription-manager:34555:MainThread @managercli.py:182 - [Errno 104] Connection reset by peer
			//	Traceback (most recent call last):
			//	  File "/usr/lib64/python2.7/site-packages/subscription_manager/managercli.py", line 1707, in _do_command
			//	    total = ent_service.remove_all_entitlements()
			//	  File "/usr/lib64/python2.7/site-packages/rhsmlib/services/entitlement.py", line 303, in remove_all_entitlements
			//	    response = self.cp.unbindAll(self.identity.uuid)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1273, in unbindAll
			//	    return self.conn.request_delete(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 702, in request_delete
			//	    return self._request("DELETE", method, params, headers=headers)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 716, in _request
			//	    info=info, headers=headers)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 573, in _request
			//	    response = conn.getresponse()
			//	  File "/usr/lib64/python2.7/httplib.py", line 1089, in getresponse
			//	    response.begin()
			//	  File "/usr/lib64/python2.7/httplib.py", line 444, in begin
			//	    version, status, reason = self._read_status()
			//	  File "/usr/lib64/python2.7/httplib.py", line 400, in _read_status
			//	    line = self.fp.readline(_MAXLINE + 1)
			//	  File "/usr/lib64/python2.7/socket.py", line 476, in readline
			//	    data = self._sock.recv(self._rbufsize)
			//	  File "/usr/lib64/python2.7/ssl.py", line 759, in recv
			//	    return self.read(buflen)
			//	  File "/usr/lib64/python2.7/ssl.py", line 653, in read
			//	    v = self._sslobj.read(len or 1024)
			//	error: [Errno 104] Connection reset by peer
			issue = "[Errno 104] Connection reset by peer";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1535150"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1535150 - [Errno 104] Connection reset by peer
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-01-27 16:24:22.520  FINE: ssh root@ibm-x3550m3-09.lab.eng.brq.redhat.com subscription-manager unsubscribe --all
			//	2016-01-27 16:24:50.438  FINE: Stdout: 1 subscription removed at the server.
			//
			//	2016-01-27 16:24:50.439  FINE: Stderr: Network error, unable to connect to server. Please see /var/log/rhsm/rhsm.log for more information.
			//
			//	2016-01-27 16:24:50.439  FINE: ExitCode: 70
			//	2016-01-27 16:24:50.439  FINE: ssh root@ibm-x3550m3-09.lab.eng.brq.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2016-01-27 16:24:50.896  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2016-01-27 22:24:33,609 [DEBUG] subscription-manager:23027 @connection.py:557 - Making request: GET /subscription/consumers/162ac9ed-d2aa-45d9-921a-ce3aeaae180d/certificates/serials
			//	2016-01-27 22:24:51,334 [ERROR] subscription-manager:23027 @entcertlib.py:121 - [Errno -3] Temporary failure in name resolution
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 119, in perform
			//	    expected = self._get_expected_serials()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 254, in _get_expected_serials
			//	    exp = self.get_certificate_serials_list()
			//	  File "/usr/share/rhsm/subscription_manager/entcertlib.py", line 234, in get_certificate_serials_list
			//	    reply = self.uep.getCertificateSerials(identity.uuid)
			//	  File "/usr/lib/python2.6/site-packages/rhsm/connection.py", line 1145, in getCertificateSerials
			//	    return self.conn.request_get(method)
			//	  File "/usr/lib/python2.6/site-packages/rhsm/connection.py", line 681, in request_get
			//	    return self._request("GET", method)
			//	  File "/usr/lib/python2.6/site-packages/rhsm/connection.py", line 571, in _request
			//	    conn.request(request_type, handler, body=body, headers=headers)
			//	  File "/usr/lib/python2.6/httplib.py", line 936, in request
			//	    self._send_request(method, url, body, headers)
			//	  File "/usr/lib/python2.6/httplib.py", line 973, in _send_request
			//	    self.endheaders()
			//	  File "/usr/lib/python2.6/httplib.py", line 930, in endheaders
			//	    self._send_output()
			//	  File "/usr/lib/python2.6/httplib.py", line 802, in _send_output
			//	    self.send(msg)
			//	  File "/usr/lib/python2.6/httplib.py", line 761, in send
			//	    self.connect()
			//	  File "/usr/lib/python2.6/site-packages/M2Crypto/httpslib.py", line 51, in connect
			//	    socket.getaddrinfo(self.host, self.port, 0, socket.SOCK_STREAM):
			//	gaierror: [Errno -3] Temporary failure in name resolution
			issue = "[Errno -3] Temporary failure in name resolution";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1302798"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1302798 - gaierror: [Errno -3] Temporary failure in name resolution
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-08-01 20:00:32.143  FINE: ssh root@ibm-z10-77.rhts.eng.bos.redhat.com subscription-manager list --available
			//	2016-08-01 20:00:51.651  FINE: Stdout:
			//	2016-08-01 20:00:51.651  FINE: Stderr: Unable to serialize objects to JSON.
			//
			//	2016-08-01 20:00:51.651  FINE: ExitCode: 70
			//	2016-08-01 20:00:51.651  FINE: ssh root@ibm-z10-77.rhts.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2016-08-01 20:00:51.815  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2016-08-01 20:00:58,186 [DEBUG] subscription-manager:30729 @connection.py:573 - Making request: GET /subscription/owners/7964055/pools?consumer=4b7a3fff-2b66-4a82-9741-fb4821e4b364
			//	2016-08-01 20:01:14,852 [DEBUG] subscription-manager:30729 @connection.py:602 - Response: status=500
			//	2016-08-01 20:01:14,852 [ERROR] subscription-manager:30729 @managercli.py:174 - exception caught in subscription-manager
			//	2016-08-01 20:01:14,852 [ERROR] subscription-manager:30729 @managercli.py:175 - Unable to serialize objects to JSON.
			//	Traceback (most recent call last):
			//	  File "/usr/sbin/subscription-manager", line 81, in <module>
			//	    sys.exit(abs(main() or 0))
			//	  File "/usr/sbin/subscription-manager", line 72, in main
			//	    return managercli.ManagerCLI().main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 2732, in main
			//	    return CLI.main(self)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/cli.py", line 160, in main
			//	    return cmd.main()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 526, in main
			//	    return_code = self._do_command()
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 2321, in _do_command
			//	    filter_string=self.options.filter_string)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 314, in get_available_entitlements
			//	    overlapping, uninstalled, text, filter_string)
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 519, in get_filtered_pools_list
			//	    self.identity.uuid, self.facts, active_on=active_on, filter_string=filter_string):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managerlib.py", line 278, in list_pools
			//	    active_on=active_on, owner=ownerid, filter_string=filter_string)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1260, in getPoolsList
			//	    results = self.conn.request_get(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 694, in request_get
			//	    return self._request("GET", method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 611, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 661, in validateResponse
			//	    raise RestlibException(response['status'], error_msg, response.get('headers'))
			//	RestlibException: Unable to serialize objects to JSON.
			issue = "Unable to serialize objects to JSON.";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1362535"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1362535 - Unable to serialize objects to JSON.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-08-12 00:33:35.448  FINE: ssh root@wolverine.idmqe.lab.eng.bos.redhat.com subscription-manager subscribe --pool=8a99f9815582f734015585f99da5513f --pool=8a99f9815582f734015585f99c47511c --pool=8a99f9815582f734015585f99add50f4 --pool=8a99f9815582f734015585f99a0950b9 --pool=8a99f9815582f734015585f99e9d519d --pool=8a99f9815582f734015585f9a7c952b9 --pool=8a99f9815582f734015585f9a654524a --pool=8a99f9815582f734015585f9989d5047 --pool=8a99f9815582f734015585f9a0c4521d --pool=8a99f9815582f734015585f99f4051c9 --pool=8a99f9815582f734015585f9995e5080 --pool=8a99f9815582f734015585f99fc851ee --pool=8a99f9815582f734015585f99b72510a
			//	2016-08-12 00:33:57.891  FINE: Stdout: 
			//	Successfully attached a subscription for: Red Hat Enterprise Linux for Virtual Datacenters, Premium
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Server, Standard (Physical or Virtual Nodes)
			//	Successfully attached a subscription for: Red Hat Enterprise Linux for IBM POWER, Standard (4 sockets) (Up to 30 LPARs) with Smart Management
			//	Successfully attached a subscription for: Red Hat Enterprise Linux Workstation, Standard
			//
			//	2016-08-12 00:33:57.891  FINE: Stderr: Runtime Error org.hibernate.exception.LockAcquisitionException: could not execute statement at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
			//
			//	2016-08-12 00:33:57.891  FINE: ExitCode: 70
			//	2016-08-12 00:33:57.892  FINE: ssh root@wolverine.idmqe.lab.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2016-08-12 00:33:58.129  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2016-08-12 00:33:50,844 [DEBUG] subscription-manager:2690:MainThread @connection.py:573 - Making request: POST /subscription/consumers/b8f5adea-bfea-44c4-a522-955f58a61a70/entitlements?pool=8a99f9815582f734015585f99e9d519d
			//	2016-08-12 00:33:51,919 [DEBUG] subscription-manager:2690:MainThread @connection.py:602 - Response: status=500
			//	2016-08-12 00:33:51,920 [ERROR] subscription-manager:2690:MainThread @managercli.py:1566 - Runtime Error org.hibernate.exception.LockAcquisitionException: could not execute statement at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
			//	Traceback (most recent call last):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 1556, in _do_command
			//	    ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1169, in bindByEntitlementPool
			//	    return self.conn.request_post(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 697, in request_post
			//	    return self._request("POST", method, params)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 611, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 661, in validateResponse
			//	    raise RestlibException(response['status'], error_msg, response.get('headers'))
			//	RestlibException: Runtime Error org.hibernate.exception.LockAcquisitionException: could not execute statement at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
			issue = "Runtime Error org.hibernate.exception.LockAcquisitionException: could not execute statement";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1366772"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1366772 - Runtime Error org.hibernate.exception.LockAcquisitionException: could not execute statement at sun.reflect.NativeConstructorAccessorImpl.newInstance0:-2
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-09-08 06:15:07.029  FINE: ssh root@hp-moonshot-03-c07.lab.eng.rdu.redhat.com subscription-manager subscribe --pool=8a99f9815582f734015585f9989d5047
			//	2016-09-08 06:16:08.858  FINE: Stdout: 
			//	2016-09-08 06:16:08.858  FINE: Stderr: Runtime Error could not obtain pessimistic lock at com.mysql.jdbc.SQLError.createSQLException:1,078
			//
			//	2016-09-08 06:16:08.859  FINE: ExitCode: 70
			//	2016-09-08 06:16:08.859  FINE: ssh root@hp-moonshot-03-c07.lab.eng.rdu.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2016-09-08 06:16:08.957  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2016-09-08 06:15:20,797 [DEBUG] subscription-manager:30381:MainThread @connection.py:573 - Making request: POST /subscription/consumers/25f1a0fb-59cf-48aa-a089-897a01282502/entitlements?pool=8a99f9815582f734015585f9989d5047
			//	2016-09-08 06:16:12,079 [DEBUG] subscription-manager:30381:MainThread @connection.py:602 - Response: status=500
			//	2016-09-08 06:16:12,080 [ERROR] subscription-manager:30381:MainThread @managercli.py:1570 - Runtime Error could not obtain pessimistic lock at com.mysql.jdbc.SQLError.createSQLException:1,078
			//	Traceback (most recent call last):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 1560, in _do_command
			//	    ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1169, in bindByEntitlementPool
			//	    return self.conn.request_post(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 697, in request_post
			//	    return self._request("POST", method, params)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 611, in _request
			//	    self.validateResponse(result, request_type, handler)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 661, in validateResponse
			//	    raise RestlibException(response['status'], error_msg, response.get('headers'))
			//	RestlibException: Runtime Error could not obtain pessimistic lock at com.mysql.jdbc.SQLError.createSQLException:1,078
			issue = "Runtime Error could not obtain pessimistic lock";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1374448"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1374448 - Runtime Error could not obtain pessimistic lock at com.mysql.jdbc.SQLError.createSQLException:1,078
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2016-09-08 06:14:00.215  FINE: ssh root@ibm-z10-13.rhts.eng.bos.redhat.com subscription-manager subscribe --pool=8a99f9815582f734015585f99973509a
			//	2016-09-08 06:16:53.512  FINE: Stdout: 
			//	2016-09-08 06:16:53.512  FINE: Stderr: ''
			//
			//	2016-09-08 06:16:53.512  FINE: ExitCode: 70
			//	2016-09-08 06:16:53.512  FINE: ssh root@ibm-z10-13.rhts.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2016-09-08 06:16:54.358  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2016-09-08 06:14:16,544 [DEBUG] subscription-manager:3495:MainThread @connection.py:573 - Making request: POST /subscription/consumers/ec27ba92-b7e5-4f97-8872-d9a3de796bcb/entitlements?pool=8a99f9815582f734015585f99973509a
			//	2016-09-08 06:16:51,800 [ERROR] subscription-manager:3495:MainThread @managercli.py:174 - Unable to attach: ''
			//	2016-09-08 06:16:51,800 [ERROR] subscription-manager:3495:MainThread @managercli.py:175 - ''
			//	Traceback (most recent call last):
			//	  File "/usr/lib/python2.7/site-packages/subscription_manager/managercli.py", line 1560, in _do_command
			//	    ents = self.cp.bindByEntitlementPool(self.identity.uuid, pool, self.options.quantity)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 1169, in bindByEntitlementPool
			//	    return self.conn.request_post(method)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 697, in request_post
			//	    return self._request("POST", method, params)
			//	  File "/usr/lib64/python2.7/site-packages/rhsm/connection.py", line 591, in _request
			//	    response = conn.getresponse()
			//	  File "/usr/lib64/python2.7/httplib.py", line 1089, in getresponse
			//	    response.begin()
			//	  File "/usr/lib64/python2.7/httplib.py", line 444, in begin
			//	    version, status, reason = self._read_status()
			//	  File "/usr/lib64/python2.7/httplib.py", line 408, in _read_status
			//	    raise BadStatusLine(line)
			//	BadStatusLine: ''
			issue = "BadStatusLine: ''";
			//	2017-02-24 22:10:28.427  FINE: ssh root@ivanova.idmqe.lab.eng.bos.redhat.com subscription-manager register --username=stage_auto_testuser1 --password=redhat --autosubscribe --servicelevel=stAndarD --force
			//	2017-02-24 22:13:10.090  FINE: Stdout:
			//	The system with UUID 032c087d-0c70-48a2-96b3-3db2233d7628 has been unregistered
			//	Registering to: subscription.rhsm.stage.redhat.com:443/subscription
			//
			//	2017-02-24 22:13:10.091  FINE: Stderr: Remote server error. Please check the connection details, or see /var/log/rhsm/rhsm.log for more information.
			//
			//	2017-02-24 22:13:10.091  FINE: ExitCode: 70
			//	2017-02-24 22:13:10.091  FINE: ssh root@ivanova.idmqe.lab.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2017-02-24 22:13:10.359  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2017-02-24 22:10:38,227 [DEBUG] subscription-manager:6475:MainThread @connection.py:490 - Making request: POST /subscription/consumers?owner=7964055
			//	2017-02-24 22:13:16,278 [ERROR] subscription-manager:6475:MainThread @managercli.py:177 - Error during registration:
			//	2017-02-24 22:13:16,279 [ERROR] subscription-manager:6475:MainThread @managercli.py:178 -
			//	Traceback (most recent call last):
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managercli.py", line 1149, in _do_command
			//	    content_tags=self.installed_mgr.tags)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 856, in registerConsumer
			//	    return self.conn.request_post(url, params)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 626, in request_post
			//	    return self._request("POST", method, params)
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/connection.py", line 512, in _request
			//	    response = conn.getresponse()
			//	  File "/usr/lib64/python2.6/site-packages/rhsm/m2cryptohttp.py", line 182, in getresponse
			//	    return self._connection.getresponse(*args, **kwargs)
			//	  File "/usr/lib64/python2.6/httplib.py", line 1049, in getresponse
			//	    response.begin()
			//	  File "/usr/lib64/python2.6/httplib.py", line 433, in begin
			//	    version, status, reason = self._read_status()
			//	  File "/usr/lib64/python2.6/httplib.py", line 397, in _read_status
			//	    raise BadStatusLine(line)
			//	BadStatusLine
			issue = "BadStatusLine";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1374460"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1374460 - sometimes stage candlepin does not return any error message; appears as a BadStatusLine: ''
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2017-02-27 00:06:57.786  FINE: ssh root@qe-blade-09.idmqe.lab.eng.bos.redhat.com subscription-manager register --username=stage_2013_data_center_test --password=redhat --org=7965071
			//	2017-02-27 00:07:00.089  FINE: Stdout: Registering to: subscription.rhsm.stage.redhat.com:443/subscription
			//
			//	2017-02-27 00:07:00.090  FINE: Stderr: 'idCert'
			//
			//	2017-02-27 00:07:00.090  FINE: ExitCode: 70
			//	2017-02-27 00:07:00.090  FINE: ssh root@qe-blade-09.idmqe.lab.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	2017-02-27 00:07:00.273  WARNING: Last request from /var/log/rhsm/rhsm.log:
			//	2017-02-27 00:07:06,243 [DEBUG] subscription-manager:11832:MainThread @connection.py:490 - Making request: POST /subscription/consumers?owner=7965071
			//	2017-02-27 00:07:07,042 [INFO] subscription-manager:11832:MainThread @connection.py:525 - Response: status=202, request="POST /subscription/consumers?owner=7965071"
			//	2017-02-27 00:07:07,044 [DEBUG] subscription-manager:11832:MainThread @cache.py:110 - Wrote cache: /var/lib/rhsm/cache/installed_products.json
			//	2017-02-27 00:07:07,044 [ERROR] subscription-manager:11832:MainThread @managercli.py:177 - exception caught in subscription-manager
			//	2017-02-27 00:07:07,044 [ERROR] subscription-manager:11832:MainThread @managercli.py:178 - 'idCert'
			//	Traceback (most recent call last):
			//	  File "/usr/sbin/subscription-manager", line 85, in <module>
			//	    sys.exit(abs(main() or 0))
			//	  File "/usr/sbin/subscription-manager", line 76, in main
			//	    return managercli.ManagerCLI().main()
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managercli.py", line 2768, in main
			//	    return CLI.main(self)
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/cli.py", line 160, in main
			//	    return cmd.main()
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managercli.py", line 537, in main
			//	    return_code = self._do_command()
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managercli.py", line 1159, in _do_command
			//	    consumer_info = self._persist_identity_cert(consumer)
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managercli.py", line 1225, in _persist_identity_cert
			//	    return managerlib.persist_consumer_cert(consumer)
			//	  File "/usr/lib/python2.6/site-packages/subscription_manager/managerlib.py", line 72, in persist_consumer_cert
			//	    consumer = identity.ConsumerIdentity(consumerinfo['idCert']['key'],
			//	KeyError: 'idCert'

			issue = "KeyError: 'idCert'";
			if (getTracebackCommandResult.getStdout().contains(issue) || result.getStderr().contains(issue)) {
				String bugId = "1393965"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1393965 - Fail to register a system to stage
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			//	ssh root@gizmo.idmqe.lab.eng.bos.redhat.com subscription-manager register --username=stage_auto_testuser1 --password=redhat --force
			//	Stdout:
			//	Unregistering from: subscription.rhsm.stage.redhat.com:443/subscription
			//	The system with UUID 66381640-b650-4f82-9889-b8e61102268e has been unregistered
			//	All local data removed
			//	Registering to: subscription.rhsm.stage.redhat.com:443/subscription
			//	The system has been registered with ID: cafe07a8-94e0-49e3-86d4-b3ed0492de23
			//	The registered system name is: gizmo.idmqe.lab.eng.bos.redhat.com
			//	Stderr:
			//	ExitCode: 70
			//	ssh root@gizmo.idmqe.lab.eng.bos.redhat.com LINE_NUMBER=$(grep --line-number 'Making request:' /var/log/rhsm/rhsm.log | tail --lines=1 | cut --delimiter=':' --field=1); if [ -n "$LINE_NUMBER" ]; then tail -n +$LINE_NUMBER /var/log/rhsm/rhsm.log; fi;
			//	Last request from /var/log/rhsm/rhsm.log:
			//	2018-01-25 20:42:30,494 [DEBUG] subscription-manager:5750:MainThread @connection.py:543 - Making request: PUT /subscription/consumers/cafe07a8-94e0-49e3-86d4-b3ed0492de23/packages
			//	2018-01-25 20:43:01,344 [INFO] subscription-manager:5750:MainThread @connection.py:586 - Response: status=500, request="PUT /subscription/consumers/cafe07a8-94e0-49e3-86d4-b3ed0492de23/packages"
			//	2018-01-25 20:43:01,346 [ERROR] subscription-manager:5750:MainThread @managercli.py:181 - exception caught in subscription-manager
			//	2018-01-25 20:43:01,347 [ERROR] subscription-manager:5750:MainThread @managercli.py:182 -
			//	Traceback (most recent call last):
			//	File "/usr/sbin/subscription-manager", line 96, in <module>
			//	sys.exit(abs(main() or 0))
			//	File "/usr/sbin/subscription-manager", line 87, in main
			//	return managercli.ManagerCLI().main()
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/managercli.py", line 2622, in main
			//	ret = CLI.main(self)
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/cli.py", line 181, in main
			//	return cmd.main()
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/managercli.py", line 496, in main
			//	return_code = self._do_command()
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/managercli.py", line 1148, in _do_command
			//	profile_mgr.update_check(self.cp, consumer['uuid'], True)
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/cache.py", line 417, in update_check
			//	return CacheManager.update_check(self, uep, consumer_uuid, force)
			//	File "/usr/lib64/python2.7/site-packages/subscription_manager/cache.py", line 178, in update_check
			//	raise re
			//	RestlibException		
			issue = "Response: status=500 from a PUT on /subscription/consumers/{UUID}/packages";
			if (SubscriptionManagerCLITestScript.doesStringContainMatches(getTracebackCommandResult.getStdout(),"request=\"PUT /subscription/consumers/[a-f,0-9,\\-]{36}/packages\"")) {
				String bugId = "1539115"; boolean invokeWorkaroundWhileBugIsOpen = true;	// Bug 1539115 - encountering frequent 500 responses from stage candlepin from a PUT on /subscription/consumers/{UUID}/packages
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					throw new SkipException("Encountered a '"+issue+"' from the server and could not complete this test while bug '"+bugId+"' is open.");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			//	2015-10-12 17:58:54,620 [DEBUG] subscription-manager:44349 @connection.py:523 - Making request: PUT /subscription/consumers/d8018dbc-7e66-4c0a-b322-9c28037fd8cf
			//	2015-10-12 17:58:55,094 [DEBUG] subscription-manager:44349 @connection.py:555 - Response: status=429
			//	2015-10-12 17:58:55,095 [ERROR] subscription-manager:44349 @managercli.py:1746 -
			//	Traceback (most recent call last):
			//	  File "/usr/share/rhsm/subscription_manager/managercli.py", line 1744, in _do_command
			//	    facts.update_check(self.cp, identity.uuid, force=True)
			//	  File "/usr/share/rhsm/subscription_manager/cache.py", line 148, in update_check
			//	    raise re
			//	RateLimitExceededException
			
			// Note: Candlepin-IT has introduced Throttling
		    // The number of requests within a 30 min periord cannot exceed 60 calls for a specific consumer.
			// Here are the throttled API rules from aedwards...
			//    #uri max_request lifetime_seconds methods
			//    "^/subscription/consumers/([^/]+)/?$" := "60 1800 GET POST PUT",
			//    "^/subscription/consumers/([^/]+)/entitlements?$" := "60 1800 GET POST",
			//    "^/subscription/consumers/([^/]+)/entitlements/dry-run?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/events?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/guests?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/host?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/release?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/compliance?$" := "60 1800 GET",
			//    "^/subscription/consumers/([^/]+)/certificates?$" := "60 1800 GET PUT",
			//    "^/subscription/consumers/([^/]+)/certificates/serials?$" := "60 1800 GET",
			//    "^/subscription/hypervisors?$" := "10 600 POST",
			// END OF WORKAROUND
		}
	}
	
	/**
	 * Compare the version of an installed package to a given version. For example...
	 * @param packageName - "subscription-manager"
	 * @param comparator - valid values: ">", "<", ">=", "<=", "=="  (for not equals functionality, simply ! the return value of "==")
	 * @param version - "1.10.14-7"
	 * @return true or false (null is returned when the packageName is not installed)
	 * 
	 * <br>Examples:
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >  1.10 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >= 1.10 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 == 1.10 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <= 1.10 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <  1.10 is false
	 * <br>
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >  1.10.14 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >= 1.10.14 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 == 1.10.14 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <= 1.10.14 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <  1.10.14 is false
	 * <br>
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >  1.10.14-1 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >= 1.10.14-1 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 == 1.10.14-1 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <= 1.10.14-1 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <  1.10.14-1 is false
	 * <br>
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >  1.10.14-7 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >= 1.10.14-7 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 == 1.10.14-7 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <= 1.10.14-7 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <  1.10.14-7 is false
	 * <br>
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >  1.10.14-7.5 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 >= 1.10.14-7.5 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 == 1.10.14-7.5 is false
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <= 1.10.14-7.5 is true
	 * <br>  subscription-manager-1.10.14-7.el7.x86_64 <  1.10.14-7.5 is true
	 */
	public Boolean isPackageVersion(String packageName, String comparator, String version) {
		
		if (!Arrays.asList(">", "<", ">=", "<=", "==").contains(comparator)) {
			log.warning("Do not know how to use comparator '"+comparator+"'.");
			return null;
		}
		
		// example package versions:
		//	libXau-1.0.8-2.1.el7.x86_64
		//	subscription-manager-1.10.14-7.git.0.798ba5c.el7.x86_64
		//	xz-libs-5.1.2-8alpha.el7.x86_64
		//	xorg-x11-drv-ati-7.2.0-9.20140113git3213df1.el7.x86_64
		//	device-mapper-event-1.02.84-10.el7.x86_64
		//	subscription-manager-migration-data-2.0.7-1.el7.noarch
		//	subscription-manager-migration-data-1.11.3.2-1.el5
		//	ntpdate-4.2.6p5-18.el7.x86_64
		
		// compatibility adjustment for python-rhsm* packages obsoleted by subscription-manager-rhsm* packages
		if (packageName.startsWith("python-rhsm") && isPackageVersion("subscription-manager",">=","1.20.3-1")) {	// commit f445b6486a962d12185a5afe69e768d0a605e175 Move python-rhsm build into subscription-manager
			log.fine("Adjusting query for isPackageVersion(\""+packageName+"\"...) to isPackageVersion(\""+packageName.replace("python-rhsm", "subscription-manager-rhsm")+"\"...) due to obsoleted package starting in version 1.20.3-1");
			packageName = packageName.replace("python-rhsm", "subscription-manager-rhsm");
		}
		
		// get the currently installed version of the packageName of the form subscription-manager-1.10.14-7.el7.x86_64
		if (this.installedPackageVersionMap.get(packageName)==null) {
			SSHCommandResult sshCommandResult = sshCommandRunner.runCommandAndWait("rpm --query "+packageName);
			if (sshCommandResult.getExitCode()==0) {
				this.installedPackageVersionMap.put(packageName,sshCommandResult.getStdout().trim());
			} else {
				log.warning("Package '"+packageName+"' is not installed.");
				return null;
			}
		}
		String installedPackageVersion = this.installedPackageVersionMap.get(packageName);
		
		// strip the packageName from installedPackageVersion to reveal the form 1.10.14-7.el7.x86_64
		installedPackageVersion = installedPackageVersion.replace(packageName+"-", "");
		
		return isVersion(installedPackageVersion, comparator, version);
	}
	public Map<String,String> installedPackageVersionMap = new HashMap<String,String>();	// contains key=python-rhsm, value=python-rhsm-0.98.9-1.el5
	
	/**
	 * @param version1
	 * @param comparator - valid values: ">", "<", ">=", "<=", "=="  (for not equals functionality, simply ! the return value of "==")
	 * @param version2
	 * @return
	 */
	public static Boolean isVersion(String version1, String comparator, String version2) {
		if (!Arrays.asList(">", "<", ">=", "<=", "==").contains(comparator)) {
			log.warning("Do not know how to use comparator '"+comparator+"'.");
			return null;
		}
		
		String s1 = version1;
		String s2 = version2;
		
		// convert 14-7 to 14.7
		s1 = s1.replace("-", ".");
		s2 = s2.replace("-", ".");
		
		// convert version strings (e.g. 1.10.14-7.el7.x86_64) into arrays of the form:  a1[] = 1, 10, 14, 7, el7, x86_64
		String[] a1 = s1.split("\\.");
		String[] a2 = s2.split("\\.");
		
		// individually start comparing numbers left to right
		int l1 = s1.split("\\.").length;
		int l2 = s2.split("\\.").length;
		for (int i=0; i<Math.min(l1,l2); i++) {
			
			// convert el7 to 0
			try {Float.valueOf(a1[i]);} catch (NumberFormatException e) {a1[i]="0";}
			try {Float.valueOf(a2[i]);} catch (NumberFormatException e) {a2[i]="0";}
			
			// convert to float
			float f1=Float.valueOf(a1[i]);
			float f2=Float.valueOf(a2[i]);
			
			// compare
			if (comparator.startsWith("<")) {
				if (f1 < f2) return true;
				if (f1 > f2) return false;
			}
			if (comparator.startsWith(">")) {
				if (f1 > f2) return true;
				if (f1 < f2) return false;
			}
			if (comparator.contains("=") && (i+1)>=Math.min(l1,l2)) {
				if (f1 == f2) return true;
			}
		}
		return false;
	}
}
