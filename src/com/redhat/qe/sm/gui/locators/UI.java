package com.redhat.qe.sm.gui.locators;

import com.redhat.qe.ldtpclient.Element;

public class UI {

	public static String mainWindowName = "dialog_updates";
	
	public static Element mainWindow = new Element (mainWindowName, "");
	public static Element closeButton = new Element (mainWindowName, "button_close");
	public static Element addButton = new Element (mainWindowName, "button_add1");
	public static Element registrationButton = new Element (mainWindowName, "account_settings");
	public static Element updateButton = new Element (mainWindowName, "button_update1");
	public static Element unsubscribeButton = new Element (mainWindowName, "button_unsubscribe1");
	
	
	public static String registerDialogName = "register_dialog";

	public static Element useridTextbox = new Element (registerDialogName, "account_login");
	public static Element passwordTextbox = new Element (registerDialogName, "account_password");
	public static Element autoregisterCheckbox = new Element (registerDialogName, "auto_bind");
	public static Element registerButton = new Element (registerDialogName, "Register");
	

}
