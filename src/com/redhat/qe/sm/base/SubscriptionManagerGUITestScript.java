package com.redhat.qe.sm.base;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.redhat.qe.ldtpclient.LDTPClient;
import com.redhat.qe.sm.gui.locators.UI;


public class SubscriptionManagerGUITestScript extends SubscriptionManagerBaseTestScript {

	UI ui = new UI();
	LDTPClient ldtp;
	
	@BeforeSuite
	public void startLDTP(){
		ldtp  = new LDTPClient("http://"  + clienthostname + ":8001/");
		ldtp.init();
		ldtp.launchApp("subscription-manager-gui", new String[] {});
		ldtp.waitTilGuiExist(UI.mainWindow);
	}
	
	//test test, really belongs elsewhere, but here now for convenience - jweiss
	@Test
	public void register(){
		ldtp.click(UI.registrationButton);
		ldtp.setTextValue(UI.useridTextbox, clientusername);
		ldtp.setTextValue(UI.passwordTextbox, clientpassword);
		ldtp.click(UI.registerButton);
	}
}
