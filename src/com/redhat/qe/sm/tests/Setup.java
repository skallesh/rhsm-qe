package com.redhat.qe.sm.tests;

import java.text.ParseException;
import java.util.ArrayList;

import org.testng.annotations.Test;

import com.redhat.qe.auto.testng.TestScript;
import com.redhat.qe.sm.tasks.Subscription;
import com.redhat.qe.tools.SSHCommandRunner;

public class Setup extends TestScript{
	String client = System.getProperty("rhsm.client.hostname");
	String server = System.getProperty("rhsm.server.hostname");
	String serverPort = System.getProperty("rhsm.server.port");
	public static final String RHSM_LOC = "/usr/sbin/subscription-manager-cli ";
	
	ArrayList<Subscription> availSubscriptions = new ArrayList<Subscription>();
	ArrayList<Subscription> consumedSubscriptions = new ArrayList<Subscription>();
	
	public static SSHCommandRunner sshCommandRunner = null; //TODO added this for ssalevan - you need to initialize it somewhere - jweiss

	public void refreshSubscriptions() throws ParseException{
		availSubscriptions.clear();
		consumedSubscriptions.clear();
		
		String[] availSubs = SSHCommandRunner.executeViaSSHWithReturn(client, "root",
				RHSM_LOC + "list --available")[0].split("\\n");
		for(int i=2;i<availSubs.length;i++)
			availSubscriptions.add(new Subscription(availSubs[i].trim()));
	
		String[] consumedSubs = SSHCommandRunner.executeViaSSHWithReturn(client, "root",
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
	
	@Test(groups={"sm_setup"},description="subscription manager set up",alwaysRun=false)
	public void setupSM() throws ParseException{
		this.refreshSubscriptions();
		
	}
}
