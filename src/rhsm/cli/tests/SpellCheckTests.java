package rhsm.cli.tests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.Translation;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * @author jsefler
 *   
 **/
@Test(groups={"SpellCheckTests","Tier3Tests"})
public class SpellCheckTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************
	
	
	@Test(	description="check the subscription-manager msgid strings for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckAllMsgidsForSubscriptionManager_Test() throws IOException {
		File remoteFile = new File("/tmp/sm-modifiedMsgIdsForSubscriptionManager.txt");
		File localFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+remoteFile.getName()).replace("tmp/tmp", "tmp"));
		
		// adjust the msgids for known acceptable words that will fail the hunspell check
		Set<String> msgIds = new HashSet<String>();
		for (String msgId : translationMsgidSetForSubscriptionManager) {
			msgId = msgId.replace("Proxy _Username:", "Proxy Username:");
			msgId = msgId.replace("Configure Pro_xy", "Configure Proxy");
			msgId = msgId.replace("Proxy P_assword:", "Proxy Password:");
			msgId = msgId.replace("Repo_sitories", "Repositories");
			msgId = msgId.replace("R_edeem", "Redeem");
			msgId = msgId.replace("Au_thentication", "Authentication");
			msgId = msgId.replace("sla_", "service_level_agreement_");
			msgId = msgId.replace("_combobox", "_combination_box");
			msgId = msgId.replace("--servicelevel", "--service_level");
			msgId = msgId.replace("--ondate", "--on_date");
			msgId = msgId.replace("serverurl", "server_url");
			msgId = msgId.replace("baseurl", "base_url");
			msgId = msgId.replace("stdin", "standard in");
			msgId = msgId.replace("%prog", "%program");
			msgId = msgId.replace("Username", "User Name");
			msgId = msgId.replace("username", "user name");
			msgId = msgId.replace("Virt Only", "Virtualization Only");
			msgId = msgId.replace("cert's", "certificate's");
			msgId = msgId.replace("(Examples: en-us, de-de)", "(Examples: English, German)");
			msgId = msgId.replace("sos report", "save our ship report");
			msgId = msgId.replace("connectionStatusLabel", "connection Status Label");
			msgId = msgId.replace("autoheal_checkbox", "auto_heal_check_box");
			msgId = msgId.replace("checkbox", "check box");
			msgId = msgId.replace("Checkbox", "Check box");
			msgId = msgId.replace("Candlepin", "Candle pin");
			msgId = msgId.replace("Gpgcheck", "GNU privacy guard");
			msgId = msgId.replace("CLI", "Command Line Interface");
			msgId = msgId.replace("owner_treeview", "owner_tree_view");
			msgId = msgId.replace("progressbar", "progress_bar");
			msgId = msgId.replace("%(mappingfile)s", "mapping file");
			msgId = msgId.replace("mappingfile", "mapping file");
			msgId = msgId.replace("scrolledwindow", "scrolled_window");
			msgId = msgId.replace("prod2", "prod 2");	// msgid: prod 1, prod2, prod 3, prod 4, prod 5, prod 6, prod 7, prod 8
			msgId = msgId.replace("environment_treeview", "environment_tree_view");
			msgId = msgId.replace("rhsmcertd", "red hat subscription management certificate daemon");
			msgId = msgId.replace("up2date_client.config", "up to date client configuration");
			msgId = msgId.replace("up2date_client.rhnChannel", "up to date client red hat network channel");
			msgId = msgId.replace("up2date", "up to date");
			msgId = msgId.replace("firstboot", "first boot");
			msgId = msgId.replace("hostname", "host name");
			msgId = msgId.replace("redhat", "red hat");
			msgId = msgId.replace("\\tManifest", "Manifest");
			msgId = msgId.replace("jbappplatform", "java boss application platform");
			msgId = msgId.replace("Virtualization", "Virtual");
			msgId = msgId.replace("_vbox", "_vertical_box");
			msgId = msgId.replace("env_select", "environment_select");
			msgId = msgId.replace("login", "log-in");
			msgId = msgId.replace("Login", "Log-in");
			msgId = msgId.replace("Repo ", "Repository ");
			msgId = msgId.replace("repo ", "repository ");
			msgId = msgId.replace("repo-", "repository-");
			msgId = msgId.replace("repos ", "repositories ");
			msgId = msgId.replace("orgs", "organizations");
			msgId = msgId.replaceAll("repo$", "repository");
			msgId = msgId.replace("YYYY-MM-DD", "YEAR-MONTH-DAY");
			msgId = msgId.replace("privacy_statement.html", "privacy_statement.HTML");
			msgId = msgId.replace("rhn", "red hat network");
			msgId = msgId.replace("RHN", "Red Hat Network");
			msgId = msgId.replace("url", "uniform resource locator");
			msgId = msgId.replace("URL", "Uniform Resource Locator");
			msgId = msgId.replace("DER size", "binary size");
			msgId = msgId.replace("SSL", "Secure Sockets Layer");
			msgId = msgId.replace("UUID", "universally unique identifier");
			msgId = msgId.replace("SKU", "Stock Keeping Unit");
			msgId = msgId.replace("SLA", "Service Level Agreement");
			msgId = msgId.replace("DMI", "Desktop Management Interface");
			msgId = msgId.replace("UEP", "Unified Entitlement and Product");
			msgId = msgId.replace("GPG", "GNU privacy guard");
			msgId = msgId.replace("GNU", "G Not Unix");
			msgId = msgId.replace("GPLv2", "General Public License, version 2");
			msgId = msgId.replace("MERCHANTABILITY", "MERCHANTABLE");
			msgId = msgId.replace("{dateexample}", "");	// python variable used in msgid: Date entered is invalid. Date should be in YYYY-MM-DD format (example: {dateexample})
			msgId = msgId.replace("'subscription-manager register --help'", "subscription-manager register --help");
			msgId = msgId.replace("'Subscription Manager'", "Subscription Manager");
			msgId = msgId.replace("'release --list'", "release --list");
			msgId = msgId.replace("'red hat network-channel --remove --channel=<conflicting_channel>'","red hat network-channel --remove --channel=<conflicting_channel>");
			//msgId = msgId.replace("'%s'", "%s");	// already fixed by adjustment below
			msgId = msgId.replaceAll("'([^ ]+)'", "$1");	// remove surrounding single quotes from single words
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1188265
			if (msgId.contains("Susbscriptions")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1188265";	// Bug 1188265 - typo in subscription-manager-gui popup message
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+"Susbscriptions"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Susbscriptions", "Subscriptions");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1189880
			if (msgId.contains("unentitle")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189880";	// Bug 1189880 - Grammar issue, "unentitle" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"unentitle"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("unentitle", "unsubscribed");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1149098
			if (msgId.contains("unregister")||msgId.contains("Unregister")||msgId.contains("unregistration")||msgId.contains("reregister")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"unregister"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("unregister", "deregister");
					msgId = msgId.replace("Unregister", "Deregister");
					msgId = msgId.replace("unregistration", "Deregistration");
					msgId = msgId.replace("reregister", "register again");
					msgId = msgId.replace("deregister", "register");	// deregister is not recognized by hunspell -d en_US
					msgId = msgId.replace("Deregister", "Register");	// Deregister is not recognized by hunspell -d en_US
					msgId = msgId.replace("Deregistration", "Registration");	// Deregistration is not recognized by hunspell -d en_US
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1189937
			if (msgId.contains("Wildcard")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189937";	// Bug 1189937 - Grammar issue, "Wildcard" is not a word
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"Wildcard"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Wildcard", "Wild card");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("_Ok") || msgId.contains("Ok ")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189933";	// Bug 1189933 - Grammar issue, "Ok" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"Ok"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("_Ok", "OK");
					msgId = msgId.replace("Ok ", "OK ");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("startup")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189953";	// Bug 1189953 - Grammar issue, "startup" is not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"startup"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("startup", "start-up");
				}
			}
			// END OF WORKAROUND

			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("pre-configure")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189946";	// Bug 1189946 - Grammar issue, "pre-configure" versus "preconfigure".
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"pre-configure"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("pre-configure", "preconfigure");
				}
			}
			// END OF WORKAROUND
			msgId = msgId.replace("preconfigure", "prior configure");	// hunspell -d en_US does not recognize preconfigure
			
			msgIds.add(msgId);
		}
		// write the msgIds to a temporary file on the client
		writeSetOfStringsToFile(msgIds,localFile, System.getProperty("line.separator")+"--------"+System.getProperty("line.separator"));
		RemoteFileTasks.putFile(client.getConnection(), localFile.getPath(), remoteFile.getPath(), "0644");
		
		// run a hunspell check on the msgIds
		SSHCommandResult hunspellResult = client.runCommandAndWait("hunspell -l -d en_US "+remoteFile);
		Assert.assertEquals(hunspellResult.getExitCode(), Integer.valueOf(0),"ExitCode from running hunspell check on "+remoteFile);
		Assert.assertEquals(hunspellResult.getStderr(), "", "Stderr from running hunspell check on "+remoteFile);
		
		// report the hunspell check failures and the msgIds that the failed words are found in
		List<String> hunspellFailures = new ArrayList<String>();
		if (!hunspellResult.getStdout().trim().isEmpty()) hunspellFailures = Arrays.asList(hunspellResult.getStdout().trim().split("\n"));
		for (String hunspellFailure : hunspellFailures) {
			log.warning("'"+hunspellFailure+"' was identified by hunspell check as a potential misspelling.");
			for (String msgId : translationMsgidSetForSubscriptionManager) {
				if (msgId.contains(hunspellFailure)) {
					log.info("   '"+hunspellFailure+"' was found in msgid: "+msgId);
				}
			}
		}
		
		// assert that there were no unexpected hunspell check failures
		Assert.assertEquals(hunspellFailures.size(),0,"There are zero unexpected hunspell check failures in the msgids.");
		
		// How to demonstrate spellcheck failures...
		// Failure: "startup"
		//	[root@jsefler-os7 ~]# msgunfmt --no-wrap /usr/share/locale/as/LC_MESSAGES/rhsm.mo | grep "startup"
		//	msgid "launches the registration dialog on startup"
		
		// How to use hunspell check to find failures...
		//	[root@jsefler-os7 ~]# msgunfmt --no-wrap /usr/share/locale/ja/LC_MESSAGES/rhsm.mo | grep msgid | sed 's/msgid //' > /tmp/msgids.txt && hunspell -l -d en_US /tmp/msgids.txt
		//	[root@jsefler-os7 ~]# msgunfmt --no-wrap /usr/share/locale/ja/LC_MESSAGES/rhsm.mo | grep msgid | sed 's/msgid //' > /tmp/msgids.txt && hunspell -w -d en_US /tmp/msgids.txt
		//	[root@jsefler-os7 ~]# msgunfmt --no-wrap /usr/share/locale/ja/LC_MESSAGES/rhsm.mo | grep msgid | sed 's/msgid //' > /tmp/msgids.txt && hunspell -L -d en_US /tmp/msgids.txt
	}
	
	
	@Test(	description="check the candlepin msgid strings for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckAllMsgidsForCandlepin_Test() throws IOException {
		if (server==null) throw new SkipException("This test requires am ssh connection to the server to retrieve all the candlepin translated po files.");
		File remoteFile = new File("/tmp/sm-modifiedMsgIdsForCandlepin.txt");
		File localFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+remoteFile.getName()).replace("tmp/tmp", "tmp"));
		
		// adjust the msgids for known acceptable words that will fail the hunspell check
		Set<String> msgIds = new HashSet<String>();
		for (String msgId : translationMsgidSetForCandlepin) {
			msgId = msgId.replace("candlepin", "candle pin");
			msgId = msgId.replace("metadata", "data about data");
			msgId = msgId.replace("username", "user name");
			msgId = msgId.replace("Stackable", "Capable of being stacked");
			msgId = msgId.replace("Runtime", "Run time");
			msgId = msgId.replace("OAuth", "Owner Authentication");	// not sure if this is correct
			msgId = msgId.replace("hypervisor", "virtual machine monitor");
			msgId = msgId.replace("meta.json", "meta json");
			msgId = msgId.replace("consumer.json", "consumer json");
			msgId = msgId.replace("vCPUs", "virtual CPU");
			msgId = msgId.replace("UUID", "universally unique identifier");
			msgId = msgId.replace("JSON", "JavaScript Object Notation");
			msgId = msgId.replace("CDN", "Content Delivery Network");
			msgId = msgId.replace("GMT[+-]HH:?MM", "Regular Expression");	// "offsets specified in the form of \"GMT[+-]HH:?MM\"."
			msgId = msgId.replace("Multi-entitlement", "Multiple entitlement");
			msgId = msgId.replace("multi-entitlement", "multiple entitlement");
			msgId = msgId.replace("Multi-Entitleable", "Capability for Multiple Entitlements");
			msgId = msgId.replace("SKU", "Stock Keeping Unit");
			msgId = msgId.replace("''", "'");	// remove the escaping single quotes
			msgId = msgId.replaceAll("'([^ ]+)'", "$1");	// remove surrounding single quotes from single words
			
			// TEMPORARY WORKAROUND FOR BUG
			for (String word : Arrays.asList(new String[]{"ueber","indepent","databse","html","oauth","checkin.","json","ActivationKey","boolean","kbase","uuid","sku"})) {
				if (msgId.contains(word)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1190814";	// Bug 1190814 - typos in candlepin msgids
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
					if (invokeWorkaroundWhileBugIsOpen) {
						log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
						msgId = msgId.replace(word, "TYPO");
					}
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1149098
			if (msgId.contains("unregister") ||
				msgId.contains("Unregister") ||
				msgId.contains("unregistration") ||
				msgId.contains("reregister")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"unregister"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("unregister", "deregister");
					msgId = msgId.replace("Unregister", "Deregister");
					msgId = msgId.replace("unregistration", "Deregistration");
					msgId = msgId.replace("reregister", "register again");
					msgId = msgId.replace("deregister", "register");	// deregister is not recognized by hunspell -d en_US
					msgId = msgId.replace("Deregister", "Register");	// Deregister is not recognized by hunspell -d en_US
					msgId = msgId.replace("Deregistration", "Registration");	// Deregistration is not recognized by hunspell -d en_US
				}
			}
			// END OF WORKAROUND
			
			msgIds.add(msgId);
		}
		// write the msgIds to a temporary file on the client
		writeSetOfStringsToFile(msgIds,localFile, System.getProperty("line.separator")+"--------"+System.getProperty("line.separator"));
		RemoteFileTasks.putFile(server.getConnection(), localFile.getPath(), remoteFile.getPath(), "0644");
		
		// run a hunspell check on the msgIds
		SSHCommandResult hunspellResult = server.runCommandAndWait("hunspell -l -d en_US "+remoteFile);
		Assert.assertEquals(hunspellResult.getExitCode(), Integer.valueOf(0),"ExitCode from running hunspell check on "+remoteFile);
		Assert.assertEquals(hunspellResult.getStderr(), "", "Stderr from running hunspell check on "+remoteFile);
		
		// report the hunspell check failures and the msgIds that the failed words are found in
		List<String> hunspellFailures = new ArrayList<String>();
		if (!hunspellResult.getStdout().trim().isEmpty()) hunspellFailures = Arrays.asList(hunspellResult.getStdout().trim().split("\n"));
		for (String hunspellFailure : hunspellFailures) {
			log.warning("'"+hunspellFailure+"' was identified by hunspell check as a potential misspelling.");
			for (String msgId : translationMsgidSetForCandlepin) {
				if (msgId.contains(hunspellFailure)) {
					log.info("   '"+hunspellFailure+"' was found in msgid: "+msgId);
				}
			}
		}
		
		// assert that there were no unexpected hunspell check failures
		Assert.assertEquals(hunspellFailures.size(),0,"There are zero unexpected hunspell check failures in the msgids.");
	}

	
	@Test(	description="check the subscription-manager man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForSubscriptionManager_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = clienttasks.command;	// "subscription-manager";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		
		// TEMPORARY WORKAROUND FOR BUG
		if (doesStringContainMatches(modifiedManPage,"[^-]servicelevel")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1192094";	// Bug 1192094 - man page for subscription-manager references "servicelevel" command when it should say "service-level
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring unrecognized word '"+"servicelevel"+"' while bug '"+bugId+"' is open.");
				modifiedManPage = modifiedManPage.replaceAll("([^-])servicelevel", "$1service-level");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		if (modifiedManPage.contains("unregister") ||
			modifiedManPage.contains("Unregister") ||
			modifiedManPage.contains("UNREGISTER")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring unrecognized word '"+"unregister"+"' while bug '"+bugId+"' is open.");

				modifiedManPage = modifiedManPage.replaceAll("unregister", "deregister");
				modifiedManPage = modifiedManPage.replaceAll("Unregister", "Deregister");
				modifiedManPage = modifiedManPage.replaceAll("UNREGISTER", "DEREGISTER");
				
				modifiedManPage = modifiedManPage.replaceAll("deregister", "register");	// deregister is not recognized by hunspell -d en_US
				modifiedManPage = modifiedManPage.replaceAll("Deregister", "Register");	// Deregister is not recognized by hunspell -d en_US
				modifiedManPage = modifiedManPage.replaceAll("DEREGISTER", "REGISTER");	// DEREGISTERING is not recognized by hunspell -d en_US
				modifiedManPage = modifiedManPage.replaceAll("Deregistration", "Registration");	// Deregistration is not recognized by hunspell -d en_US

			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"wildcards","wildcard","suborganizations","expirations","reregistered","reregister","instaled"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192120";	// Bug 1192120 - typos and poor grammar in subscription-manager man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the subscription-manager-gui man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForSubscriptionManagerGui_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "subscription-manager-gui";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifiedManPage.replaceAll("(\\w+)‐\\n\\s+(\\w+)", "$1$2");	// unhyphenate all words at the ends of a line
		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzilla1192574comment2isNotFixed ");
		modifiedManPage = modifiedManPage.replaceAll("https://access.redhat.com/knowledge/docs/en-US/Red_Hat_Subscription_Management/1.0/html/Subscription_Management_Guide/index.html", " bugzilla1192574comment3isNotFixed ");
		modifiedManPage = modifyMisspellingsInManPage(modifiedManPage);
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"bugzilla1192574comment2isNotFixed","bugzilla1192574comment3isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192574";	// Bug 1192574 - typos and poor grammar in subscription-manager-gui man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the rhn-migrate-classic-to-rhsm man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhnMigrateClassicToRhsm_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhn-migrate-classic-to-rhsm";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		
		// TEMPORARY WORKAROUND FOR BUG
		//	[root@jsefler-os7 ~]# man -P cat rhn-migrate-classic-to-rhsm | head -1
		//	rhn-migrate-classic-to-rhsm(System Manager's Manrhn-migrate-classic-to-rhsm(8)
		if (modifiedManPage.contains("System Manager's Manr")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1192574";	// Bug 1192574 - typos and poor grammar in subscription-manager-gui man page
			// TODO Wrong bug 1192574
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring poor man page title while bug '"+bugId+"' is open.");
				modifiedManPage = modifiedManPage.replaceAll("System Manager's Manr", "System Manager's Manual r");
			}
		}
		// END OF WORKAROUND
		
		
		// TEMPORARY WORKAROUND FOR BUG
		if (modifiedManPage.contains("unregister") ||
			modifiedManPage.contains("Unregister") /*||modifiedManPage.contains("UNREGISTER")*/) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring unrecognized word '"+"unregister"+"' while bug '"+bugId+"' is open.");

				modifiedManPage = modifiedManPage.replaceAll("unregister", "deregister");
				modifiedManPage = modifiedManPage.replaceAll("Unregister", "Deregister");
				//modifiedManPage = modifiedManPage.replaceAll("UNREGISTER", "DEREGISTER");
				
				modifiedManPage = modifiedManPage.replaceAll("deregister", "register");	// deregister is not recognized by hunspell -d en_US
				modifiedManPage = modifiedManPage.replaceAll("Deregister", "Register");	// Deregister is not recognized by hunspell -d en_US
				//modifiedManPage = modifiedManPage.replaceAll("DEREGISTER", "REGISTER");	// DEREGISTERING is not recognized by hunspell -d en_US
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the install-num-migrate-to-rhsm man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForInstallNumMigrateToRhsm_Test() throws IOException {
		if (!clienttasks.redhatReleaseX.equals("5")) throw new SkipException("This test is applicable to RHEL5 only.");
		if (clienttasks.isPackageVersion("subscription-manager-migration", ">", "1.11.3-4") && clienttasks.redhatReleaseX.equals("5")) {
			throw new SkipException("Due to bug 1092754, the migration tool '"+rhsm.cli.tests.MigrationTests.installNumTool+"' has been removed from RHEL5.");
		}
		Assert.fail("This test has not been implemented for this version of subscription-manager");	// TODO
	}
	
	
	@Test(	description="check the rhsm.conf man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmConf_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhsm.conf";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		modifiedManPage = modifiedManPage.replaceAll("/old- licenses/", "/ bugzilla1192646comment4isNotFixed /");
		modifiedManPage = modifiedManPage.replaceAll(" 11/07/2014 ", " bugzilla1192646comment5isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"subscription-mananager","pulldown","bugzilla1192646comment4isNotFixed","bugzilla1192646comment5isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192646";	// Bug 1192646 - typos and poor grammar in rhsm.conf man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the rct man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRct_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rct";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzilla1193991comment3isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"certficate","filesystem","bugzilla1193991comment3isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1193991";	// Bug 1193991 - typos and poor grammar in rct man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the rhsmcertd man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmcertd_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhsmcertd";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzilla1194453comment3isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"certmgr.py","autoattachInterval","bugzilla1194453comment3isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1194453";	// Bug 1194453 - typos and poor grammar in rhsmcertd man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (XmlRpcException xre) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */}
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}
	
	
	@Test(	description="check the rhsm-icon man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmIcon_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhsm-icon";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
//TODO		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzillaXXXXcommentXisNotFixed ");
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");	
	}
	
	
	@Test(	description="check the rhsm-debug man page for misspelled words and typos",
			groups={"debugTest"},
			enabled=false) // TODO
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmDebug_Test() throws IOException {
		
	}
	
	
	// Candidates for an automated Test:
	
	
	
	
	// Configuration Methods ***********************************************************************
	@BeforeClass (groups="setup")
	public void buildTranslationFileMapsBeforeClass() {
		translationFileMapForSubscriptionManager = buildTranslationFileMapForSubscriptionManager();
		translationFileMapForCandlepin = buildTranslationFileMapForCandlepin();
	}
	Map<File,List<Translation>> translationFileMapForSubscriptionManager = null;
	Map<File,List<Translation>> translationFileMapForCandlepin = null;

	@BeforeClass (groups="setup",dependsOnMethods={"buildTranslationFileMapsBeforeClass"})
	public void buildTranslationMsgidSets() {
		if (clienttasks==null) return;
		
		// assemble a unique set of msgids (by taking the union of all the msgids from all of the translation files.)
		// TODO: My assumption that the union of all the msgids from the translation files completely matches
		//       the currently extracted message ids from the source code is probably incorrect.
		//       There could be extra msgids in the translation files that were left over from the last round
		//       of translations and are no longer applicable (should be excluded from this union algorithm).
		for (File translationFile : translationFileMapForSubscriptionManager.keySet()) {
			List<Translation> translationList = translationFileMapForSubscriptionManager.get(translationFile);
			for (Translation translation : translationList) {
				translationMsgidSetForSubscriptionManager.add(translation.msgid);
			}
		}
		
		for (File translationFile : translationFileMapForCandlepin.keySet()) {
			List<Translation> translationList = translationFileMapForCandlepin.get(translationFile);
			for (Translation translation : translationList) {
				translationMsgidSetForCandlepin.add(translation.msgid);
			}
		}
	}
	Set<String> translationMsgidSetForSubscriptionManager = new HashSet<String>(500);  // 500 is an estimated size
	Set<String> translationMsgidSetForCandlepin = new HashSet<String>(500);  // 500 is an estimated size

	
	// Protected Methods ***********************************************************************
	
	/**
	 * @param tool - name of the tool for which the man page was reported
	 * @param originalManPage - original stdout from typing man tool on the command line
	 * @param modifiedManPage - a modified copy of originalManPage for which acceptable substitutions have been made for known misspellings
	 * @return List of all the words that failed hunspell -l -d en_US 
	 * @throws IOException
	 */
	protected List<String> getSpellCheckFailuresForModifiedManPage(String tool, String originalManPage, String modifiedManPage) throws IOException {
		// write the modifiedManPage to a file on the client
		File remoteFile = new File("/tmp/sm-modifiedManPageFor-"+tool+".txt");
		File localFile = new File((getProperty("automation.dir", "/tmp")+"/tmp/"+remoteFile.getName()).replace("tmp/tmp", "tmp"));
		Writer fileWriter = new FileWriter(localFile);
		fileWriter.write(modifiedManPage);
		fileWriter.close();
		RemoteFileTasks.putFile(client.getConnection(), localFile.getPath(), remoteFile.getPath(), "0644");
		
		// run a hunspell check on the modifiedManPage
		SSHCommandResult hunspellResult = client.runCommandAndWait("hunspell -l -d en_US "+remoteFile);
		Assert.assertEquals(hunspellResult.getExitCode(), Integer.valueOf(0),"ExitCode from running hunspell check on "+remoteFile);
		Assert.assertEquals(hunspellResult.getStderr(), "", "Stderr from running hunspell check on "+remoteFile);
		
		// report the hunspell check failures and lines of the man page that contain the failed word
		List<String> hunspellFailures = new ArrayList<String>();
		List<String> manPageLines = Arrays.asList(originalManPage.split("\n"));
		if (!hunspellResult.getStdout().trim().isEmpty()) hunspellFailures = Arrays.asList(hunspellResult.getStdout().trim().split("\n"));
		for (String hunspellFailure : hunspellFailures) {
			log.warning("'"+hunspellFailure+"' was identified by hunspell check as a potential misspelling.");
			for (String manPageLine : manPageLines) {
				if (manPageLine.contains(hunspellFailure)) {
					log.info("   '"+hunspellFailure+"' was found in man page line: "+manPageLine);
				}
			}
		}
		
		return hunspellFailures;
	}
	
	/**
	 * @param originalManPage - original stdout result from typing man tool on the command line
	 * @return - return a modified copy of the originalManPage with acceptable substitutions for known strings that would otherwise fail the hunspell check
	 */
	protected String modifyMisspellingsInManPage(String originalManPage) {
		String modifiedManPage = originalManPage;
		
		// modifications to correct for man-page formatting
		modifiedManPage = modifiedManPage.replaceAll("(\\w+)‐\\n\\s+(\\w+)", "$1$2");	// unhyphenate all words at the ends of a line
		
		// modifications for hashed id strings
		modifiedManPage = modifiedManPage.replaceAll("[a-f,0-9,\\-]{36}", "UUID");		// consumer identity: eff9a4c9-3579-49e5-a52f-83f2db29ab52
		modifiedManPage = modifiedManPage.replaceAll("[a-f,0-9,]{32}", "POOLID");		// Pool Id: ff8080812bc382e3012bc3845da100d2	

		modifiedManPage = modifiedManPage.replaceAll("proxyuser", "proxy_user");
		modifiedManPage = modifiedManPage.replaceAll("proxypass", "proxy_pass");
		modifiedManPage = modifiedManPage.replaceAll("PROXYUSERNAME", "PROXY_USERNAME");
		modifiedManPage = modifiedManPage.replaceAll("PROXYPASSWORD", "PROXY_PASSWORD");
		modifiedManPage = modifiedManPage.replaceAll("CONSUMERTYPE", "CONSUMER_TYPE");
		modifiedManPage = modifiedManPage.replaceAll("CONSUMERID", "CONSUMER_ID");
		modifiedManPage = modifiedManPage.replaceAll("SERIALNUMBER", "SERIAL_NUMBER");
		modifiedManPage = modifiedManPage.replaceAll("POOLID", "POOL_ID");
		modifiedManPage = modifiedManPage.replaceAll("AUTOSUBSCRIBE", "AUTO-SUBSCRIBE");	// TODO: this may not be a acceptable, used in rhn-mgrate-classic-to-rhsm
		modifiedManPage = modifiedManPage.replaceAll("ENV([^I])", "ENVIRONMENT$1");
		modifiedManPage = modifiedManPage.replaceAll("by pressing <F1>", "by pressing <Function-1>");
		modifiedManPage = modifiedManPage.replaceAll("--servicelevel", "--service_level");
		modifiedManPage = modifiedManPage.replaceAll("--activationkey", "--activation_key");
		modifiedManPage = modifiedManPage.replaceAll("--serverurl", "--server_url");
		modifiedManPage = modifiedManPage.replaceAll("--ondate", "--on_date");
		modifiedManPage = modifiedManPage.replaceAll("--baseurl", "--base_url");
		modifiedManPage = modifiedManPage.replaceAll("--listslots", "--list_slots");
		modifiedManPage = modifiedManPage.replaceAll("--listhooks", "--list_hooks");
		modifiedManPage = modifiedManPage.replaceAll("--consumerid", "--consumer_identifier");
		modifiedManPage = modifiedManPage.replaceAll("--repo([^s])", "--repository$1");
		modifiedManPage = modifiedManPage.replaceAll("stdin", "standard-in");
		modifiedManPage = modifiedManPage.replaceAll("stdout", "standard-out");
		modifiedManPage = modifiedManPage.replaceAll("Multi-", "Multiple-");
		modifiedManPage = modifiedManPage.replaceAll("multi-", "multiple-");
		modifiedManPage = modifiedManPage.replaceAll("hypervisor", "virtual machine monitor");
		modifiedManPage = modifiedManPage.replaceAll("1234abcd", "");
		modifiedManPage = modifiedManPage.replaceAll("abcd1234", "");
		modifiedManPage = modifiedManPage.replaceAll("RH0103708", "Red-Hat-0103708");
		modifiedManPage = modifiedManPage.replaceAll("--environment=\"local dev\"", "--environment=\"local development\"");
		modifiedManPage = modifiedManPage.replaceAll("'jsmith.rhn.example.com'", "jsmith.rhn.example.com");
		modifiedManPage = modifiedManPage.replaceAll("jsmith", "John Smith");
		modifiedManPage = modifiedManPage.replaceAll("filename", "file_name");
		modifiedManPage = modifiedManPage.replaceAll("username", "user_name");
		modifiedManPage = modifiedManPage.replaceAll("username", "user_name");
		modifiedManPage = modifiedManPage.replaceAll("Username", "User_name");
		modifiedManPage = modifiedManPage.replaceAll("USERNAME", "USER_NAME");
		modifiedManPage = modifiedManPage.replaceAll("SYS0395", "SYSTEM SKU 395");
		modifiedManPage = modifiedManPage.replaceAll("baseurl", "base_url");
		modifiedManPage = modifiedManPage.replaceAll("productCertDir", "product-Certificate-Directory");
		modifiedManPage = modifiedManPage.replaceAll("entitlementCertDir", "entitlement-Certificate-Directory");
		modifiedManPage = modifiedManPage.replaceAll("consumerCertDir", "consumer-Certificate-Directory");
		modifiedManPage = modifiedManPage.replaceAll("pluginDir", "plugin-in-Directory");
		modifiedManPage = modifiedManPage.replaceAll("pluginConfDir", "plug-in-Configuration-Directory");
		modifiedManPage = modifiedManPage.replaceAll("certCheckInterval", "certificate-Check-Interval");
		modifiedManPage = modifiedManPage.replaceAll("autoAttachInterval", "auto-Attach-Interval");
		modifiedManPage = modifiedManPage.replaceAll("certFrequency", "certificate_Frequency");
		modifiedManPage = modifiedManPage.replaceAll("certInterval", "certificate_Interval");
		modifiedManPage = modifiedManPage.replaceAll("ca_cert_dir","certificate_authority_certificate_directory");
		modifiedManPage = modifiedManPage.replaceAll("repo_ca_cert","repository_certificate_authority_certificate");
		modifiedManPage = modifiedManPage.replaceAll("ssl_verify_depth","secure_socket_layer_verify_depth");
		modifiedManPage = modifiedManPage.replaceAll("Candlepin", "Candle-pin");
		modifiedManPage = modifiedManPage.replaceAll("candlepin", "candle-pin");
		modifiedManPage = modifiedManPage.replaceAll("/pki/","/public-key-infrastructure/");
		modifiedManPage = modifiedManPage.replaceAll("\\{\"fact1\": \"value1\",\"fact2\": \"value2\"\\}", "");
		modifiedManPage = modifiedManPage.replaceAll("cpu","computer_processing_unit");
		modifiedManPage = modifiedManPage.replaceAll("size: (\\d+)b","size: $1 bytes");
		modifiedManPage = modifiedManPage.replaceAll("mhz","mega_hertz");
		modifiedManPage = modifiedManPage.replaceAll("numa_node0","number_A_node_0");
		modifiedManPage = modifiedManPage.replaceAll("numa_node","number_A_node");
		modifiedManPage = modifiedManPage.replaceAll("virtualization_type","virtual_type");
		modifiedManPage = modifiedManPage.replaceAll("IP domain", "Internet Protocol domain");
		modifiedManPage = modifiedManPage.replaceAll("vCPUs", "virtual CPU");
		modifiedManPage = modifiedManPage.replaceAll("Service Type: L1-L3", "Service Type: Level-1 through Level-3");
		modifiedManPage = modifiedManPage.replaceAll("Subject DN of the certificate", "Subject Distinguished-Name of the certificate");
		modifiedManPage = modifiedManPage.replaceAll("CLI", "Command Line Interface");
		modifiedManPage = modifiedManPage.replaceAll("CN:", "Common Name:");
		modifiedManPage = modifiedManPage.replaceAll("CPU","Computer Processing Unit");
		modifiedManPage = modifiedManPage.replaceAll("Virt([^u])","Virtual$1");
		modifiedManPage = modifiedManPage.replaceAll("KVM","Kernel-based Virtual Machine");
		modifiedManPage = modifiedManPage.replaceAll("GenuineIntel","Genuine Intel");
		modifiedManPage = modifiedManPage.replaceAll("KICKSTART", "KICK-START");	// TODO: commonly used Red Hat word
		modifiedManPage = modifiedManPage.replaceAll("kickstart", "kick-start");	// TODO: commonly used Red Hat word
		modifiedManPage = modifiedManPage.replaceAll("Knowledgebase", "Knowledge-base");	// TODO: commonly used Red Hat word
		modifiedManPage = modifiedManPage.replaceAll("whitespace", "white-space");
		modifiedManPage = modifiedManPage.replaceAll("preselected", "already selected");
		modifiedManPage = modifiedManPage.replaceAll("cdn.redhat.com", "content_delivery_network.red_hat.com");
		modifiedManPage = modifiedManPage.replaceAll("rhsmcertd", "red hat subscription management certificate daemon");
		modifiedManPage = modifiedManPage.replaceAll("RHSMCERTD", "Red Hat Subscription Management Certificate Daemon");
		modifiedManPage = modifiedManPage.replaceAll("subscription-manager-gui", "subscription-manager-graphical-user-interface");
		modifiedManPage = modifiedManPage.replaceAll("firstboot", "first-boot");
		modifiedManPage = modifiedManPage.replaceAll("up2date", "up-to-date");
		modifiedManPage = modifiedManPage.replaceAll("virt-who", "virtual-who");
		modifiedManPage = modifiedManPage.replaceAll("rct( |\n)", "read-certificate$1");
		modifiedManPage = modifiedManPage.replaceAll("rct\\(8\\)", "read-certificate(8)");
		modifiedManPage = modifiedManPage.replaceAll("install-num-migrate", "install-number-migrate");
		modifiedManPage = modifiedManPage.replaceAll("systemid([^e])", "system-id$1");
		modifiedManPage = modifiedManPage.replaceAll("rhsm\\.conf([^i])", "rhsm.configuration$1");
		modifiedManPage = modifiedManPage.replaceAll("RHSM\\.CONF([^i])", "RHSM.CONFIGURATION$1");
		modifiedManPage = modifiedManPage.replaceAll("myserver.example.com", "my.server.example.com");
		modifiedManPage = modifiedManPage.replaceAll("cloudforms.example.com", "cloud.forms.example.com");
		modifiedManPage = modifiedManPage.replaceAll("newsubscription.example.com", "new.subscription.example.com");
		modifiedManPage = modifiedManPage.replaceAll("sam.example.com", "subscription-asset-management.example.com");
		modifiedManPage = modifiedManPage.replaceAll("--name=server1", "--name=server");
		modifiedManPage = modifiedManPage.replaceAll(".git.28.5cd97a5.fc20", "");
		modifiedManPage = modifiedManPage.replaceAll(".git.1.2f38ded.fc20", "");
		modifiedManPage = modifiedManPage.replaceAll("2012-09-14T14:55:29.280519", "2012-09-14 14:55:29.280519");
		modifiedManPage = modifiedManPage.replaceAll("gpl-2.0.txt", "General-Public-License-version-2-text");
		modifiedManPage = modifiedManPage.replaceAll("grep migr([^a])", "grep migrate$1");
		modifiedManPage = modifiedManPage.replaceAll("hostname", "host-name");
		modifiedManPage = modifiedManPage.replaceAll("HOSTNAME", "HOST-NAME");
		modifiedManPage = modifiedManPage.replaceAll("redhat", "red-hat");
		modifiedManPage = modifiedManPage.replaceAll("RedHat", "Red-Hat");
		modifiedManPage = modifiedManPage.replaceAll("CloudForms", "Cloud-Forms");
		modifiedManPage = modifiedManPage.replaceAll("x86_64", "x_86_64");
		modifiedManPage = modifiedManPage.replaceAll("x86", "x_86");
		modifiedManPage = modifiedManPage.replaceAll("ia64", "i_a_64");
		modifiedManPage = modifiedManPage.replaceAll("'east colo'", "east organization");
		modifiedManPage = modifiedManPage.replaceAll("login", "log-in");
		modifiedManPage = modifiedManPage.replaceAll("Login", "Log-in");
		modifiedManPage = modifiedManPage.replaceAll("Repo ", "Repository ");
		modifiedManPage = modifiedManPage.replaceAll("repo ", "repository ");
		modifiedManPage = modifiedManPage.replaceAll("repo-", "repository-");
		modifiedManPage = modifiedManPage.replaceAll("repos ", "repositories ");
		modifiedManPage = modifiedManPage.replaceAll("repos\n", "repositories\n");
		modifiedManPage = modifiedManPage.replaceAll("REPO_", "REPOSITORY_");
		modifiedManPage = modifiedManPage.replaceAll("REPOS OPTIONS", "REPOSITORIES OPTIONS");
		modifiedManPage = modifiedManPage.replaceAll("REPO-OVERRIDE", "REPOSITORY-OVERRIDE");
		modifiedManPage = modifiedManPage.replaceAll("CONFIG OPTIONS", "CONFIGURATION OPTIONS");
		modifiedManPage = modifiedManPage.replaceAll("config([^u])", "configuration$1");
		modifiedManPage = modifiedManPage.replaceAll("orgs", "organizations");
		modifiedManPage = modifiedManPage.replaceAll("ORGS", "ORGANIZATIONS");
		modifiedManPage = modifiedManPage.replaceAll("preconfigured", "configured in advance");
		modifiedManPage = modifiedManPage.replaceAll("ProductName:", "Product Name:");
		modifiedManPage = modifiedManPage.replaceAll("YYYY-MM-DD", "YEAR-MONTH-DAY");
		modifiedManPage = modifiedManPage.replaceAll("rhn", "red-hat-network");
		modifiedManPage = modifiedManPage.replaceAll("rhsm", "red-hat-subscription-management");
		modifiedManPage = modifiedManPage.replaceAll("rhel", "red-hat-enterprise-Linux");	// lowercase linux fails hunspell
		modifiedManPage = modifiedManPage.replaceAll("RHN", "Red Hat Network");
		modifiedManPage = modifiedManPage.replaceAll("RHSM", "Red Hat Subscription Management");
		modifiedManPage = modifiedManPage.replaceAll("RHEL", "Red Hat Enterprise Linux");
		modifiedManPage = modifiedManPage.replaceAll("-url", "-uniform-resource-locator");
		modifiedManPage = modifiedManPage.replaceAll("_url", "_uniform_resource_locator");
		modifiedManPage = modifiedManPage.replaceAll("URL", "Uniform Resource Locator");
		modifiedManPage = modifiedManPage.replaceAll("\\.pem", ".certificate");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("PEM file", "Privacy-enhanced mail certificate file");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("PEM certificate", "Privacy-enhanced mail certificate");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("DER +size", "binary size");
		modifiedManPage = modifiedManPage.replaceAll("DER +encoding", "binary encoding");
		modifiedManPage = modifiedManPage.replaceAll("UUID", "universally unique identifier");
		modifiedManPage = modifiedManPage.replaceAll("JSON", "JavaScript Object Notation");
		modifiedManPage = modifiedManPage.replaceAll("CDN", "Content Delivery Network");
		modifiedManPage = modifiedManPage.replaceAll("SKU", "Stock Keeping Unit");
		modifiedManPage = modifiedManPage.replaceAll("SLA", "Service Level Agreement");
		modifiedManPage = modifiedManPage.replaceAll("RPM", "Red Hat Package Manager");
		modifiedManPage = modifiedManPage.replaceAll("rpm", "red-hat-package-manager");
		modifiedManPage = modifiedManPage.replaceAll("SSL", "Secure Sockets Layer");
		modifiedManPage = modifiedManPage.replaceAll("GUI", "Graphical User Interface");
		modifiedManPage = modifiedManPage.replaceAll("UI", "User Interface");
		modifiedManPage = modifiedManPage.replaceAll("GPG", "GNU privacy guard");
		modifiedManPage = modifiedManPage.replaceAll("GNU", "G Not Unix");
		modifiedManPage = modifiedManPage.replaceAll("GPLv2", "General Public License, version 2");
		modifiedManPage = modifiedManPage.replaceAll("HTTPS", "Hypertext Transfer Protocol Secure");
		modifiedManPage = modifiedManPage.replaceAll("MERCHANTABILITY", "MERCHANTABLE");
		
		// modifications for people's name
		modifiedManPage = modifiedManPage.replaceAll("Pradeep(\n *| +)Kilambi", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Deon(\n *| +)Lackey", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Mark(\n *| +)Huth", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Paresh(\n *| +)Mutha", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Tasos(\n *| +)Papaioannou", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Bryan(\n *| +)Kearney", "Author");
		modifiedManPage = modifiedManPage.replaceAll("James(\n *| +)Bowes", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Jeff(\n *| +)Ortel", "Author");
		
		return modifiedManPage;
	}
	
	
	
	// Data Providers ***********************************************************************
	
	
	
}
