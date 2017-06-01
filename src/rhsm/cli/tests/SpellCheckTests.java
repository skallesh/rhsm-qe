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

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import rhsm.base.SubscriptionManagerCLITestScript;
import rhsm.data.Translation;

import com.redhat.qe.Assert;
import com.redhat.qe.auto.bugzilla.BugzillaAPIException;
import com.redhat.qe.auto.bugzilla.BzChecker;
import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SSHCommandResult;

import com.github.redhatqe.polarize.metadata.DefTypes.Project;
import com.github.redhatqe.polarize.metadata.TestDefinition;

/**
 * @author jsefler
 * 
 * Good sources for acceptable spellings...
 * 	http://www.oxforddictionaries.com/
 * 	http://dictionary.reference.com/
 *   
 *   To interactively test a spelling...
 *   [root@jsefler-7 ~]# hunspell -d en_US -a
 *   @(#) International Ispell Version 3.2.06 (but really Hunspell 1.3.2)
 *   couldn't
 *   *            <==== indicates one word spelled correctly
 *   
 *   could not
 *   *
 *   *            <==== indicates two words spelled correctly
 *   
 *   couldnt
 *   & couldnt 2 0: couldn't, could     <==== indicates misspelling and shows suggestions
 *   
 *   ^C
 *   [root@jsefler-7 ~]# 
 *   
 **/
@Test(groups={"SpellCheckTests","Tier3Tests"})
public class SpellCheckTests extends SubscriptionManagerCLITestScript {
	
	
	// Test Methods ***********************************************************************

	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21777", "RHEL7-51594"})
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
			msgId = msgId.replace("Virt-who", "Virtualization-who");
			msgId = msgId.replace("Virt Only", "Virtualization Only");
			msgId = msgId.replace("Virt Limit", "Virtualization Limit");
			msgId = msgId.replace("Virtualization", "Virtual");
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
			msgId = msgId.replace("Hostname", "Host name");
			msgId = msgId.replace("redhat", "red hat");
			msgId = msgId.replace("\\tManifest", "Manifest");
			msgId = msgId.replace("jbappplatform", "java boss application platform");
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
			msgId = msgId.replace("RHEL", "Red Hat Enterprise Linux");
			msgId = msgId.replace("url", "uniform resource location");	// locator is not recognized by hunspell-1.2.8-16.el6
			msgId = msgId.replace("URL", "Uniform Resource Location");	// Locator is not recognized by hunspell-1.2.8-16.el6
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
			msgId = msgId.replace("'subscription-manager plugins'", "subscription-manager plugins");
			msgId = msgId.replace("'subscription-manager register --help'", "subscription-manager register --help");
			msgId = msgId.replace("'Subscription Manager'", "Subscription Manager");
			msgId = msgId.replace("'release --list'", "release --list");
			msgId = msgId.replace("'red hat network-channel --remove --channel=<conflicting_channel>'","red hat network-channel --remove --channel=<conflicting_channel>");
			msgId = msgId.replace("/kb/","/knowledge-base/");	//Knowledge Base Article: https://access.redhat.com/kb/docs/DOC-45563
			msgId = msgId.replace("Doesn't","Does not");	// not recognized by hunspell-1.2.8-16.el6
			msgId = msgId.replace("doesn't","does not");
			msgId = msgId.replace("Couldn't","Could not");
			msgId = msgId.replace("couldn't","could not");
			msgId = msgId.replace("Shouldn't","Should not");
			msgId = msgId.replace("shouldn't","should not");
			msgId = msgId.replace("consumerid", "consumer_id");
			msgId = msgId.replace("consumer_uuid", "consumer_universally_unique_identifier");
			msgId = msgId.replace("&#x2022;","bullet");
			//msgId = msgId.replace("'%s'", "%s");	// already fixed by adjustment below
			msgId = msgId.replaceAll("'([^ ]+)'", "$1");	// remove surrounding single quotes from single words
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1188265
			if (msgId.contains("Susbscriptions")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1188265";	// Bug 1188265 - typo in subscription-manager-gui popup message
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"unentitle"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("unentitle", "unsubscribed");
				}
				
				if (clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 599f217a4cf06248720fa0a30bd08b0b4ecc0f18
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+"unentitle"+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					msgId = msgId.replace("unentitle", "unsubscribed");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1149098
			if (msgId.contains("unregister")||msgId.contains("Unregister")||msgId.contains("unregistration")||msgId.contains("reregister")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
				else {
					log.info("Bug '"+bugId+"' was CLOSED WONTFIX.  Tolerating 'unregister'.");
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"Wildcard"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Wildcard", "Wild card");
				}
				else {
					log.info("Bug '"+bugId+"' was CLOSED WONTFIX.  Tolerating 'Wildcard'.");
					msgId = msgId.replace("Wildcard", "Wild card");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("_Ok") || msgId.contains("Ok ")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189933";	// Bug 1189933 - Grammar issue, "Ok" not a word.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"startup"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("startup", "start-up");
				}
				else {
					log.info("Bug '"+bugId+"' was CLOSED WONTFIX.  Tolerating 'startup'.");
					msgId = msgId.replace("startup", "start-up");
				}
			}
			// END OF WORKAROUND

			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("pre-configure")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1189946";	// Bug 1189946 - Grammar issue, "pre-configure" versus "preconfigure".
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"pre-configure"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("pre-configure", "preconfigure");
				}
				
				if (clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 0162a16a4dde7c54985ec27fd1515f1b664d829c
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring unrecognized word '"+"pre-configure"+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					msgId = msgId.replace("pre-configure", "preconfigure");
				}
			}
			// END OF WORKAROUND
			msgId = msgId.replace("preconfigure", "prior configure");	// hunspell -d en_US does not recognize preconfigure
			
			// TEMPORARY WORKAROUND FOR BUG
			if (msgId.contains("plugin")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1200507";	// Bug 1200507 - Grammar issue, "plugin" should be hyphenated.
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"plugin"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("plugin", "plug-in");
				}
				else {
					log.info("Bug '"+bugId+"' was CLOSED WONTFIX.  Tolerating 'plugin'.");
					msgId = msgId.replace("plugin", "plug-in");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1223852
			if (msgId.contains("Deletedfd")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1223852";	// Bug 1223852 - repolib report has 'deletedfd' typo in string catalog
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+"Deletedfd"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Deletedfd", "Deleted");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1233379
			if (msgId.contains("systemid")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1233379";	// Bug 1233379 - Grammar issue, "systemid" is not a word
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				String word = "systemid";
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring unrecognized word '"+word+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("systemid", "system id");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1188265
			if (msgId.contains("Editition")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1317613";	// Bug 1317613 - typo in src/subscription_manager/gui/data/ui/selectsla.ui
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+"Awesome Developer Editition"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Awesome Developer Editition", "Awesome Developer Edition");
				}
			}
			// END OF WORKAROUND
			
			// TEMPORARY WORKAROUND FOR BUG: https://bugzilla.redhat.com/show_bug.cgi?id=1372779
			if (msgId.contains("Proxy connnection")) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1372779";	// Bug 1372779 - another typo in "Proxy connnection failed, please check your settings." 
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+"Proxy connnection"+"' while bug '"+bugId+"' is open.");
					msgId = msgId.replace("Proxy connnection", "Proxy connection");
				}
			}
			// END OF WORKAROUND
			
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21499", "RHEL7-51593"})
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
			msgId = msgId.replace("json", "JavaScript object notation");
			msgId = msgId.replace("JSON", "JavaScript Object Notation");
			msgId = msgId.replace("ActivationKey", "Activation Key");
			msgId = msgId.replace("CDN", "Content Delivery Network");
			msgId = msgId.replace("GMT[+-]HH:?MM", "Regular Expression");	// "offsets specified in the form of \"GMT[+-]HH:?MM\"."
			msgId = msgId.replace("Multi-entitlement", "Multiple entitlement");
			msgId = msgId.replace("multi-entitlement", "multiple entitlement");
			msgId = msgId.replace("Multi-Entitleable", "Capability for Multiple Entitlements");
			msgId = msgId.replace("SKU", "Stock Keeping Unit");
			msgId = msgId.replace("unmapped", "not mapped");	// unmapped fails on hunspell-1.3.3-3.fc20.x86_64
			msgId = msgId.replace("Unmapped", "Not mapped");	// unmapped fails on hunspell-1.3.3-3.fc20.x86_64
			msgId = msgId.replace("uber", "supreme example");	// unmapped fails on hunspell-1.3.3-3.fc20.x86_64
			msgId = msgId.replace("orgs", "organizations");
			msgId = msgId.replace("''", "'");	// remove the escaping single quotes
			msgId = msgId.replaceAll("'([^ ]+)'", "$1");	// remove surrounding single quotes from single words
			
			// TEMPORARY WORKAROUND FOR BUG
			for (String word : Arrays.asList(new String[]{"ueber","indepent","databse","html","oauth","checkin.","json","ActivationKey","boolean","kbase","uuid","sku"})) {
				if (msgId.contains(word)) {
					boolean invokeWorkaroundWhileBugIsOpen = true;
					String bugId="1190814";	// Bug 1190814 - typos in candlepin msgids
					try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
				else {
					log.info("Bug '"+bugId+"' was CLOSED WONTFIX.  Tolerating 'unregister'.");
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21785", "RHEL7-51602"})
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
		modifiedManPage = modifyMisspellingsInManPage(modifiedManPage);
		modifiedManPage = modifiedManPage.replaceAll("PLUGIN OPTIONS", "PLUG-IN OPTIONS");	// TODO: should not fail on "PLUGIN OPTIONS" since it is a valid module
		modifiedManPage = modifiedManPage.replaceAll("The plugins command", "The plug-ins command");	// TODO: should not fail on "PLUGIN OPTIONS" since it is a valid module
		modifiedManPage = modifiedManPage.replaceAll("plugins", " bugzilla1192120comment13isNotFixed ");
		modifiedManPage = modifiedManPage.replaceAll("plugin", " bugzilla1192120comment13isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		if (doesStringContainMatches(modifiedManPage,"[^-]servicelevel")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1192094";	// Bug 1192094 - man page for subscription-manager references "servicelevel" command when it should say "service-level
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			String word="servicelevel";
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring unrecognized word '"+word+"' while bug '"+bugId+"' is open.");
				modifiedManPage = modifiedManPage.replaceAll("([^-])"+word, "$1service-level");
			}
			if (clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 961aa8d43ef6e18ef9cde2e740be8462101bb4c6
				log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
				SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
				log.warning("Ignoring unrecognized word '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
				modifiedManPage = modifiedManPage.replace(word, "TYPO");
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		if (modifiedManPage.contains("unregister") ||
			modifiedManPage.contains("Unregister") ||
			modifiedManPage.contains("UNREGISTER")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1149098";	// Bug 1149098 - Grammar issue, "unregister" not a word.
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
		for (String word : Arrays.asList(new String[]{"wildcards","wildcard","suborganizations","expirations","reregistered","reregister","instaled","equilivent","bugzilla1192120comment13isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192120";	// Bug 1192120 - typos and poor grammar in subscription-manager man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				
				if (word.equals("suborganizations") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 063747b1b1d83fe89eff91fc5fb96f57d95eb5d5
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("expirations") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 063747b1b1d83fe89eff91fc5fb96f57d95eb5d5
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("reregistered") /*&& clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1" FailedQA)*/) {	// commit 063747b1b1d83fe89eff91fc5fb96f57d95eb5d5
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("instaled") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 063747b1b1d83fe89eff91fc5fb96f57d95eb5d5
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("equilivent") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 063747b1b1d83fe89eff91fc5fb96f57d95eb5d5
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// TEMPORARY WORKAROUND FOR BUG
		if (modifiedManPage.contains("Specifes")) {
			boolean invokeWorkaroundWhileBugIsOpen = true;
			String bugId="1440319";	// Bug 1440319 - typo in the word "Specifes" in subscription-manager man page
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
			if (invokeWorkaroundWhileBugIsOpen) {
				log.warning("Ignoring unrecognized word '"+"Specifes"+"' while bug '"+bugId+"' is open.");
				modifiedManPage = modifiedManPage.replaceAll("Specifes", "Specifies");
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			, testCaseID = {"RHEL6-21784", "RHEL7-51601"})
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
		modifiedManPage = modifiedManPage.replaceAll("(\\w+)(?:‐|-)\\n\\s+(\\w+)", "$1$2");	// unhyphenate all words at the ends of a line
		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzilla1192574comment2isNotFixed ");
		modifiedManPage = modifiedManPage.replaceAll("https://access.redhat.com/knowledge/docs/en-US/Red_Hat_Subscription_Management/1.0/html/Subscription_Management_Guide/index.html", " bugzilla1192574comment3isNotFixed ");
		modifiedManPage = modifyMisspellingsInManPage(modifiedManPage);
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"bugzilla1192574comment2isNotFixed","bugzilla1192574comment3isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192574";	// Bug 1192574 - typos and poor grammar in subscription-manager-gui man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				
				if (word.equals("bugzilla1192574comment3isNotFixed") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 589bf7debe8702d147f3a69f61f34c44ab47ef63
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21784", "RHEL7-51601"})
	@Test(	description="check the rhn-migrate-classic-to-rhsm man page for misspelled words and typos",
			groups={/*blockedByBug-1390712*/},
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
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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
			try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21780", "RHEL7-51597"})
	@Test(	description="check the rhsm.conf man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmConf_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhsm.conf";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		modifiedManPage = modifiedManPage.replaceAll("/old- licenses/", "/ bugzilla1192646comment4isNotFixed /");
		modifiedManPage = modifiedManPage.replaceAll(" 11/07/2014 ", " bugzilla1192646comment5isNotFixed ");
		modifiedManPage = modifiedManPage.replaceAll("subscription manager plugins", " bugzilla1192646comment7isNotFixed ");
		modifiedManPage = modifiedManPage.replaceAll("plugin configuration", " bugzilla1192646comment7isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"subscription-mananager","pulldown","bugzilla1192646comment4isNotFixed","bugzilla1192646comment5isNotFixed","bugzilla1192646comment7isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1192646";	// Bug 1192646 - typos and poor grammar in rhsm.conf man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				
				if (word.equals("subscription-mananager") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 5157378c714de78fbb0ca9f5b47b567ac44efa84
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("pulldown") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 5157378c714de78fbb0ca9f5b47b567ac44efa84
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("bugzilla1192646comment4isNotFixed") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 5157378c714de78fbb0ca9f5b47b567ac44efa84
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("bugzilla1192646comment5isNotFixed") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 5157378c714de78fbb0ca9f5b47b567ac44efa84
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21778", "RHEL7-51595"})
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				
				if (word.equals("certficate") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit a7a358714b66faf1ba2031f2e9918e1078756efa
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21783", "RHEL7-51600"})
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
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				
				if (word.equals("certmgr.py") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit cf092e1f5f51a60f983b2feacc400d97250ff406
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("autoattachInterval") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit cf092e1f5f51a60f983b2feacc400d97250ff406
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
	}


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21782", "RHEL7-51599"})
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


	@TestDefinition( projectID = {Project.RHEL6, Project.RedHatEnterpriseLinux7}
			       , testCaseID = {"RHEL6-21781", "RHEL7-51598"})
	@Test(	description="check the rhsm-debug man page for misspelled words and typos",
			groups={},
			enabled=true)
	//@ImplementsNitrateTest(caseId=)
	public void SpellCheckManPageForRhsmDebug_Test() throws IOException {
		if (clienttasks==null) throw new SkipException("A client connection is needed for this test.");
		String tool = "rhsm-debug";
		SSHCommandResult manPageResult = client.runCommandAndWait("man "+tool);
		Assert.assertEquals(manPageResult.getExitCode(),Integer.valueOf(0), "ExitCode from man page for '"+tool+"'.");
		
		// modify the contents of manPageResult for acceptable word spellings
		String modifiedManPage = manPageResult.getStdout();
		modifiedManPage = modifyMisspellingsInManPage(manPageResult.getStdout());
		modifiedManPage = modifiedManPage.replaceAll("System Manager's Manual", " bugzilla1194468comment2isNotFixed ");
		
		// TEMPORARY WORKAROUND FOR BUG
		for (String word : Arrays.asList(new String[]{"intead","bugzilla1194468comment2isNotFixed"})) {
			if (modifiedManPage.contains(word)) {
				boolean invokeWorkaroundWhileBugIsOpen = true;
				String bugId="1194468";	// Bug 1194468 - typos and poor grammar in rhsm-debug man page
				try {if (invokeWorkaroundWhileBugIsOpen&&BzChecker.getInstance().isBugOpen(bugId)) {log.fine("Invoking workaround for "+BzChecker.getInstance().getBugState(bugId).toString()+" Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);} else {invokeWorkaroundWhileBugIsOpen=false;}} catch (BugzillaAPIException be) {/* ignore exception */} catch (RuntimeException re) {/* ignore exception */} 
				if (invokeWorkaroundWhileBugIsOpen) {
					log.warning("Ignoring known misspelling of '"+word+"' while bug '"+bugId+"' is open.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
				if (word.equals("intead") && clienttasks.isPackageVersion("subscription-manager", "<", "1.15.1-1")) {	// commit 23e0c319ae3ed9ff2dc684b46fc1c2bf4e05f840
					log.fine("Invoking workaround for Bugzilla "+bugId+".  (https://bugzilla.redhat.com/show_bug.cgi?id="+bugId+")");
					SubscriptionManagerCLITestScript.addInvokedWorkaround(bugId);
					log.warning("Ignoring known misspelling of '"+word+"' which is fixed in newer release subscription-manager-1.15.1-1.");
					modifiedManPage = modifiedManPage.replace(word, "TYPO");
				}
			}
		}
		// END OF WORKAROUND
		
		// assert that there were no unexpected hunspell check failures in the modified man page
		Assert.assertEquals(getSpellCheckFailuresForModifiedManPage(tool,manPageResult.getStdout(),modifiedManPage).size(),0,"There are zero unexpected hunspell check failures in the man page for '"+tool+"'.");
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
			int occurances=0;
			for (String manPageLine : manPageLines) if (manPageLine.contains(hunspellFailure)) occurances++;
			if (occurances>20) {	// avoid running out of java memory
				log.info("   '"+hunspellFailure+"' was found in too many lines to list");
				continue;
			}
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
		modifiedManPage = modifiedManPage.replaceAll(".", "");	// needed on rhel6 to fix highlighted .SH words like NNAAMMEE
		modifiedManPage = modifiedManPage.replaceAll("subscription-manager-gui\\(8\\) Subscription Managementsubscription-manager-gui\\(8\\)","subscription-manager-gui(8)   Subscription Management   subscription-manager-gui(8)");
		modifiedManPage = modifiedManPage.replaceAll("rhn-migrate-classic-to-rhsm\\(System Manager's Manrhn-migrate-classic-to-rhsm\\(8\\)","rhn-migrate-classic-to-rhsm(8)   System Manager's Manual   rhn-migrate-classic-to-rhsm(8)");

		// join acceptable hyphenated words (e.g. third-party) that have been wrapped across two lines (e.g. third-
		//      party) 
		modifiedManPage = modifiedManPage.replaceAll("(auto)(?:‐|-)\\n\\s+(attach)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(rhn)(?:‐|-)\\n\\s+(migrate)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(subscription)(?:‐|-)\\n\\s+(manager)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(third)(?:‐|-)\\n\\s+(party)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(on)(?:‐|-)\\n\\s+(premise)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(destination)(?:‐|-)\\n\\s+(url)", "$1-$2");
		modifiedManPage = modifiedManPage.replaceAll("(rhncfg)(?:‐|-)\\n\\s+(actions)", "$1-$2");	// package rhncfg-actions is provided by rhncfg
		
		// unhyphenate all words that the man tool wrapped at the end of a line (e.g. oper-
	    //      ating)
		modifiedManPage = modifiedManPage.replaceAll("(\\w+)(?:‐|-)\\n\\s+(\\w+)", "$1$2");
		
		// modifications for hashed id strings
		modifiedManPage = modifiedManPage.replaceAll("[a-f,0-9,\\-]{36}", "UUID");		// consumer identity: eff9a4c9-3579-49e5-a52f-83f2db29ab52
		modifiedManPage = modifiedManPage.replaceAll("[a-f,0-9,]{32}", "POOLID");		// Pool Id: ff8080812bc382e3012bc3845da100d2	

		modifiedManPage = modifiedManPage.replaceAll("proxyuser", "proxy_user");
		modifiedManPage = modifiedManPage.replaceAll("proxypass", "proxy_pass");
		modifiedManPage = modifiedManPage.replaceAll("noproxy", "no_proxy");
		modifiedManPage = modifiedManPage.replaceAll("PROXYUSERNAME", "PROXY_USERNAME");
		modifiedManPage = modifiedManPage.replaceAll("PROXYPASSWORD", "PROXY_PASSWORD");
		modifiedManPage = modifiedManPage.replaceAll("NOPROXY", "NO_PROXY");
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
		modifiedManPage = modifiedManPage.replaceAll("--legacyuser", "--legacy-user");
		modifiedManPage = modifiedManPage.replaceAll("--legacypassword", "--legacy-password");
		modifiedManPage = modifiedManPage.replaceAll("--consumerid", "--consumer_identifier");
		modifiedManPage = modifiedManPage.replaceAll("--sos", "--save-our-souls");
		modifiedManPage = modifiedManPage.replaceAll("--repo([^s])", "--repository$1");
		modifiedManPage = modifiedManPage.replaceAll("stdin", "standard-in");
		modifiedManPage = modifiedManPage.replaceAll("stdout", "standard-out");
		modifiedManPage = modifiedManPage.replaceAll("stdout", "standard-out");
		modifiedManPage = modifiedManPage.replaceAll("autoattachInterval", "auto-attach-Interval");
		modifiedManPage = modifiedManPage.replaceAll("isn't", "is not");	// fails on hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("isn’t", "is not");	// fails on hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("aren't", "are not");	// fails on hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("aren’t", "are not");	// fails on hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("gzipped", "GNU compressed");	// man rhsm-debug
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
		modifiedManPage = modifiedManPage.replaceAll("pluginDir", "plug-in-Directory");
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
		modifiedManPage = modifiedManPage.replaceAll("index.html", "index.HTML");
		modifiedManPage = modifiedManPage.replaceAll("rhsmcertd-worker.py", "red-hat-subscription-management-certificate-daemon-worker_python");
		modifiedManPage = modifiedManPage.replaceAll("rhsmcertd", "red hat subscription management certificate daemon");
		modifiedManPage = modifiedManPage.replaceAll("RHSMCERTD", "Red Hat Subscription Management Certificate Daemon");
		modifiedManPage = modifiedManPage.replaceAll("subscription-manager-gui", "subscription-manager-graphical-user-interface");
		modifiedManPage = modifiedManPage.replaceAll("firstboot", "first-boot");
		modifiedManPage = modifiedManPage.replaceAll("sosreport", "save-our-soul-report");	// sos - A set of tools to gather troubleshooting information from a system
		modifiedManPage = modifiedManPage.replaceAll("spacewalk-abrt", "spacewalk-abort");	// spacewalk-abrt - rhn-check plug-in for collecting information about crashes handled by ABRT.
		modifiedManPage = modifiedManPage.replaceAll("spacewalk-oscap", "spacewalk-open-scan-plugin");	// spacewalk-oscap is a plug-in for rhn-check. With this plugin, user is able to run OpenSCAP scan from Spacewalk or Red Hat Satellite server.
		modifiedManPage = modifiedManPage.replaceAll("osad", "open-source-architecture-daemon");	// Open Source Architecture Daemon - This package effectively replaces the behavior of rhnsd/rhn_check that only poll the Spacewalk Server from time to time.
		modifiedManPage = modifiedManPage.replaceAll("rhncfg-actions", "red-hat-network-configuration-actions");	// package provided by rhncfg
		modifiedManPage = modifiedManPage.replaceAll("rhncfg-client", "red-hat-network-configuration-actions");	// package provided by rhncfg
		modifiedManPage = modifiedManPage.replaceAll("rhncfg", "red-hat-network-configuration");	// Spacewalk Configuration Client Libraries
		modifiedManPage = modifiedManPage.replaceAll("rhnsd", "red-hat-network-servers-daemon");	// Red Hat Network query daemon - The Red Hat Update Agent that automatically queries the Red Hat Network servers and determines which packages need to be updated on your machine, and runs any actions.
		modifiedManPage = modifiedManPage.replaceAll("rhnpush", "red-hat-network-push");	// Package uploader for the Spacewalk or Red Hat Satellite Server
		modifiedManPage = modifiedManPage.replaceAll("up2date", "up-to-date");
		modifiedManPage = modifiedManPage.replaceAll("virt-who", "virtual-who");
		modifiedManPage = modifiedManPage.replaceAll("rct( |\n)", "read-certificate$1");
		modifiedManPage = modifiedManPage.replaceAll("rct\\(8\\)", "read-certificate(8)");
		modifiedManPage = modifiedManPage.replaceAll("RCT\\(8\\)", "READ-CERTIFICATE(8)");
		modifiedManPage = modifiedManPage.replaceAll("install-num-migrate", "install-number-migrate");
		modifiedManPage = modifiedManPage.replaceAll("systemid([^e])", "system-id$1");
		modifiedManPage = modifiedManPage.replaceAll("rhsm\\.conf([^i])", "rhsm.configuration$1");
		modifiedManPage = modifiedManPage.replaceAll("RHSM\\.CONF([^i])", "RHSM.CONFIGURATION$1");
		modifiedManPage = modifiedManPage.replaceAll("subscription_manager\\.managercli", "subscription_manager.manager_command_line_interface");
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
		modifiedManPage = modifiedManPage.replaceAll("’east colo’", "east organization");
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
		modifiedManPage = modifiedManPage.replaceAll("SUBMODULE", "SUB_MODULE");
		modifiedManPage = modifiedManPage.replaceAll("submodule", "sub-module");
		modifiedManPage = modifiedManPage.replaceAll("config([^u])", "configuration$1");
		modifiedManPage = modifiedManPage.replaceAll("orgs", "organizations");
		modifiedManPage = modifiedManPage.replaceAll("ORGS", "ORGANIZATIONS");
		modifiedManPage = modifiedManPage.replaceAll("preconfigured", "configured in advance");
		modifiedManPage = modifiedManPage.replaceAll("ProductName:", "Product Name:");
		modifiedManPage = modifiedManPage.replaceAll("YYYY-MM-DD", "YEAR-MONTH-DAY");
		modifiedManPage = modifiedManPage.replaceAll("rhn", "red-hat-network");
		modifiedManPage = modifiedManPage.replaceAll("rhsmcertd", "rhsm certificate daemon");
		modifiedManPage = modifiedManPage.replaceAll("rhsmd", "rhsm daemon");
		modifiedManPage = modifiedManPage.replaceAll("rhsm", "red-hat-subscription-management");
		modifiedManPage = modifiedManPage.replaceAll("rhel", "red-hat-enterprise-Linux");	// lowercase linux fails hunspell
		modifiedManPage = modifiedManPage.replaceAll("RHN", "Red Hat Network");
		modifiedManPage = modifiedManPage.replaceAll("RHSM", "Red Hat Subscription Management");
		modifiedManPage = modifiedManPage.replaceAll("RHEL", "Red Hat Enterprise Linux");
		modifiedManPage = modifiedManPage.replaceAll("-url", "-uniform-resource-location");	// locator is not recognized by hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("_url", "_uniform_resource_location");	// locator is not recognized by hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("URL", "Uniform Resource Location");	// Locator is not recognized by hunspell-1.2.8-16.el6
		modifiedManPage = modifiedManPage.replaceAll("\\.pem", ".certificate");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("PEM file", "Privacy-enhanced mail certificate file");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("PEM certificate", "Privacy-enhanced mail certificate");	// Base64-encoded X.509 certificate
		modifiedManPage = modifiedManPage.replaceAll("DER\\s+size", "binary size");
		modifiedManPage = modifiedManPage.replaceAll("DER\\s+encoding", "binary encoding");
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
		modifiedManPage = modifiedManPage.replaceAll("'\\*'", "\"splat\"");	// comes from rhsm.conf no_proxy description: '*' is a special value
		
		// filesystem was judged WONTFIX https://bugzilla.redhat.com/show_bug.cgi?id=1193991#c5
		modifiedManPage = modifiedManPage.replaceAll("filesystem", "file system");
		
		// unregister was judged WONTFIX https://bugzilla.redhat.com/show_bug.cgi?id=1149098#c10
		modifiedManPage = modifiedManPage.replaceAll("unregister", "not register");	// deregister is not recognized by hunspell -d en_US
		modifiedManPage = modifiedManPage.replaceAll("Unregister", "Not register");	// Deregister is not recognized by hunspell -d en_US
		modifiedManPage = modifiedManPage.replaceAll("UNREGISTER", "NOT REGISTER");	// DEREGISTER is not recognized by hunspell -d en_US
		
		// plugin was judged WONTFIX https://bugzilla.redhat.com/show_bug.cgi?id=1200507
		modifiedManPage = modifiedManPage.replaceAll("plugins", "plug-ins");
		modifiedManPage = modifiedManPage.replaceAll("plugin", "plug-in");
		
		// plugin was judged WONTFIX https://bugzilla.redhat.com/show_bug.cgi?id=1192646#c8
		modifiedManPage = modifiedManPage.replaceAll("subscription manager plugins", "subscription manager plug-ins");
		modifiedManPage = modifiedManPage.replaceAll("plugin configuration", "plug-in configuration");
		
		// wildcard was judged WONTFIX https://bugzilla.redhat.com/show_bug.cgi?id=1189937
		modifiedManPage = modifiedManPage.replaceAll("wildcard", "wild-card");
		
		// modifications for people's name
		modifiedManPage = modifiedManPage.replaceAll("Pradeep(\n *| +)Kilambi", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Deon(\n *| +)Lackey", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Mark(\n *| +)Huth", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Paresh(\n *| +)Mutha", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Tasos(\n *| +)Papaioannou", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Bryan(\n *| +)Kearney", "Author");
		modifiedManPage = modifiedManPage.replaceAll("James(\n *| +)Bowes", "Author");
		modifiedManPage = modifiedManPage.replaceAll("Jeff(\n *| +)Ortel", "Author");
		modifiedManPage = modifiedManPage.replaceAll("William(\n *| +)Poteat", "Author");
		
		return modifiedManPage;
	}
	
	
	
	// Data Providers ***********************************************************************
	
	
	
}
