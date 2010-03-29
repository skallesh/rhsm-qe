package com.redhat.qe.sm.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SCPTools;
import com.redhat.qe.tools.SSHCommandRunner;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.sm.tasks.Subscription;
import com.redhat.qe.tools.SSHCommandRunner;

public class Setup extends TestScript{
	String clientHostname				= System.getProperty("rhsm.client.hostname");
	String serverHostname				= System.getProperty("rhsm.server.hostname");
	String username						= System.getProperty("rhsm.client.username");
	String password						= System.getProperty("rhsm.client.password");
	String certFrequency				= System.getProperty("rhsm.client.certfrequency");
	String rpmLocation					= System.getProperty("rhsm.rpm");
	String serverPort 					= System.getProperty("rhsm.server.port");
	String serverBaseUrl				= System.getProperty("rhsm.server.baseurl");
	String clientsshKeyPrivate			= System.getProperty("rhsm.sshkey.private",".ssh/id_auto_dsa");
	String clientsshKeyPath				= System.getProperty("automation.dir")+"/"+clientsshKeyPrivate;
	String clientsshUser				= System.getProperty("rhsm.ssh.user","root");
	String clientsshkeyPassphrase		= System.getProperty("rhsm.sshkey.passphrase","");
	
	String defaultConfigFile			= "/etc/rhsm/rhsm.conf";
	String rhsmcertdLogFile				= "/var/log/rhsm/rhsmcertd.log";
	
	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	ArrayList<Subscription> availSubscriptions = new ArrayList<Subscription>();
	ArrayList<Subscription> consumedSubscriptions = new ArrayList<Subscription>();
	
	public static SSHCommandRunner sshCommandRunner = null;

	public void refreshSubscriptions(){
		availSubscriptions.clear();
		consumedSubscriptions.clear();
		
		log.info("Refreshing subscription information...");
		String[] availSubs = SSHCommandRunner.executeViaSSHWithReturn(clientHostname, "root",
				RHSM_LOC + "list --available")[0].split("\\n");
		
		//if extraneous output comes out over stdout, figure out where the useful output begins
		int outputBegin = 2;
		for(int i=0;i<availSubs.length;i++){
			if(availSubs[i].contains("productId"))
				break;
			outputBegin++;
		}
		
		for(int i=outputBegin;i<availSubs.length;i++)
			try {
				availSubscriptions.add(new Subscription(availSubs[i].trim()));
			} catch (ParseException e) {
				e.printStackTrace();
				log.warning("Unparseable subscription line: "+ availSubs[i]);
			}
		
		String[] consumedSubs = SSHCommandRunner.executeViaSSHWithReturn(clientHostname, "root",
				RHSM_LOC + "list --consumed")[0].split("\\n");
		
		//if extraneous output comes out over stdout, figure out where the useful output begins
		outputBegin = 2;
		for(int i=0;i<consumedSubs.length;i++){
			if(consumedSubs[i].contains("productId"))
				break;
			outputBegin++;
		}
		
		for(int i=outputBegin;i<consumedSubs.length;i++)
			try {
				consumedSubscriptions.add(new Subscription(consumedSubs[i].trim()));
			} catch (ParseException e) {
				log.warning("Unparseable subscription line: "+ availSubs[i]);
			}
	}
	
	public ArrayList<Subscription> getNonSubscribedSubscriptions(){
		ArrayList<Subscription> nsSubs = new ArrayList<Subscription>();
		for(Subscription s:availSubscriptions)
			if (!consumedSubscriptions.contains(s))
				nsSubs.add(s);
		return nsSubs;
	}
	
	public void installLatestSMRPM(){
		log.info("Retrieving latest subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rm -f ~/subscription-manager.rpm");
		sshCommandRunner.runCommandAndWait("wget -O ~/subscription-manager.rpm --no-check-certificate "+rpmLocation);
		
		log.info("Installing newest subscription-manager RPM...");
		sshCommandRunner.runCommandAndWait("rpm -e subscription-manager");
		sshCommandRunner.runCommandAndWait("rpm --force --nodeps -Uvh ~/subscription-manager.rpm");
	}
	
	public void updateSMConfigFile(String hostname, String port){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^hostname=.*$", 
						"hostname="+hostname),
						0,
						"Updated rhsm config hostname to point to:" + hostname
				);
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^port=.*$", 
						"port="+port),
						0,
						"Updated rhsm config port to point to:" + port
				);
	}
	
	public void changeCertFrequency(String frequency){
		Assert.assertEquals(
				RemoteFileTasks.searchReplaceFile(sshCommandRunner, 
						defaultConfigFile, 
						"^certFrequency=.*$", 
						"certFrequency="+frequency),
						0,
						"Updated rhsmd cert refresh frequency to "+frequency+" minutes"
				);
		sshCommandRunner.runCommandAndWait("mv "+rhsmcertdLogFile+" "+rhsmcertdLogFile+".bak");
		sshCommandRunner.runCommandAndWait("service rhsmcertd restart");
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"started: interval = "+frequency),
				0,
				"interval reported as "+frequency+" in "+rhsmcertdLogFile);
	}
	
	public void registerToCandlepin(String username, String password){
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"register --username="+username+" --password="+password);
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/key.pem").intValue(),
						0,
						"/etc/pki/consumer/key.pem is present after register");
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/cert.pem").intValue(),
						0,
						"/etc/pki/consumer/cert.pem is present after register");
		/*Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/cert.uuid").intValue(),
						0,
						"/etc/pki/consumer/cert.uuid is present after register");*/
	}
	
	public void subscribeToSubscription(Subscription sub){
		log.info("Subscribing to entitlement with productID:"+ sub.productId);
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"subscribe --product="+sub.productId);
		this.refreshSubscriptions();
		Assert.assertTrue(consumedSubscriptions.contains(sub), "Successfully subscribed to entitlement with productID:"+ sub.productId);
	}
	
	public void unsubscribeFromSubscription(Subscription sub){
		log.info("Subscribing to entitlement with productID:"+ sub.productId);
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"unsubscribe --product="+sub.productId);
		this.refreshSubscriptions();
		Assert.assertFalse(consumedSubscriptions.contains(sub), "Successfully unsubscribed from entitlement with productID:"+ sub.productId);
	}
	
	public void cleanOutAllCerts(){
		log.info("Cleaning out certs from /etc/pki/consumer, /etc/pki/entitlement/, and /etc/pki/product/");
		sshCommandRunner.runCommandAndWait("rm -f /etc/pki/consumer/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/entitlement/*");
		sshCommandRunner.runCommandAndWait("rm -rf /etc/pki/product/*");
	}
	
	public void sleep(long i) {
		log.info("Sleeping for "+i+" milliseconds...");
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			log.info("Sleep interrupted!");
		}
	}

	
	@BeforeSuite(groups={"sm_setup"},description="subscription manager set up",alwaysRun=true)
	public void setupSM() throws ParseException, IOException{
		sshCommandRunner = new SSHCommandRunner(clientHostname, 
				clientsshUser, clientsshKeyPath, clientsshkeyPassphrase, null);
		this.installLatestSMRPM();
		this.cleanOutAllCerts();
		this.updateSMConfigFile(serverHostname, serverPort);
		this.changeCertFrequency(certFrequency);
	}
}
