package com.redhat.qe.sm.gui.locators;

import com.redhat.qe.ldtpclient.Element;

public class UI {

	public static String mainWindow = "dialog_updates";
	public Element closeButton = new Element (mainWindow, "button_close");
	public Element addButton = new Element (mainWindow, "button_add1");
	public Element registrationButton = new Element (mainWindow, "account_settings");
	public Element updateButton = new Element (mainWindow, "button_update1");
	public Element unsubscribeButton = new Element (mainWindow, "button_unsubscribe1");

}
