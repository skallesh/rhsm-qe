package com.redhat.qe.sm.tests;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.redhat.qe.tools.RemoteFileTasks;
import com.redhat.qe.tools.SCPTools;
import com.redhat.qe.tools.SSHCommandRunner;

import com.redhat.qe.auto.testopia.Assert;
import com.redhat.qe.jon.base.CommandLineTestScript;
import com.redhat.qe.sm.tasks.Subscription;
import com.redhat.qe.tools.SSHCommandRunner;

public class Setup extends CommandLineTestScript{
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
	
	public static SSHCommandRunner sshCommandRunner = null; //TODO added this for ssalevan - you need to initialize it somewhere - jweiss

	public void refreshSubscriptions() throws ParseException{
		availSubscriptions.clear();
		consumedSubscriptions.clear();
		
		String[] availSubs = SSHCommandRunner.executeViaSSHWithReturn(clientHostname, "root",
				RHSM_LOC + "list --available")[0].split("\\n");
		for(int i=2;i<availSubs.length;i++)
			availSubscriptions.add(new Subscription(availSubs[i].trim()));
	
		String[] consumedSubs = SSHCommandRunner.executeViaSSHWithReturn(clientHostname, "root",
				RHSM_LOC + "list --consumed")[0].split("\\n");
		for(int i=2;i<availSubs.length;i++)
			consumedSubscriptions.add(new Subscription(consumedSubs[i].trim()));
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
		sshCommandRunner.runCommandAndWait("rpm --force -Uvh ~/subscription-manager.rpm");
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
						frequency),
						0,
						"Updated rhsmd cert refresh frequency to "+frequency+" minutes"
				);
		sshCommandRunner.runCommandAndWait("mv "+rhsmcertdLogFile+" "+rhsmcertdLogFile+".bak");
		sshCommandRunner.runCommandAndWait("service rhsmd restart");
		Assert.assertEquals(RemoteFileTasks.grepFile(sshCommandRunner,
				rhsmcertdLogFile,
				"^started: interval = "+frequency),
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
		Assert.assertEquals(
				sshCommandRunner.runCommandAndWait(
						"stat /etc/pki/consumer/cert.uuid").intValue(),
						0,
						"/etc/pki/consumer/cert.uuid is present after register");
	}
	
	public void subscribeToSubscription(Subscription sub) throws ParseException{
		log.info("Subscribing to entitlement with productID:"+ sub.productId);
		sshCommandRunner.runCommandAndWait(RHSM_LOC +
				"subscribe --product="+sub.productId);
		this.refreshSubscriptions();
		Assert.assertTrue(consumedSubscriptions.contains(sub));
	}
	
	@BeforeSuite(groups={"sm_setup"},description="subscription manager set up",alwaysRun=true)
	public void setupSM() throws ParseException, IOException{
		sshCommandRunner = new SSHCommandRunner(clientHostname, 
				clientsshUser, clientsshKeyPath, clientsshkeyPassphrase, null);
		this.installLatestSMRPM();
		this.updateSMConfigFile(serverHostname, serverPort);
		this.changeCertFrequency(certFrequency);
	}
}
