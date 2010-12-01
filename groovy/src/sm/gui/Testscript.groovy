package sm.gui

import org.testng.annotations.BeforeSuite 
import org.testng.annotations.Test 
//

import com.redhat.qe.sm.base.SubscriptionManagerBaseTestScript;

@Mixin(ldtp)
class Testscript extends SubscriptionManagerBaseTestScript{
	
	def version = "2.3"
	public TestScript(){
		
	}
	//def static ldtp = null
	def static UI = new ui()
	
	@BeforeSuite
	def startLDTP(){
		connect("http://"  + clienthostname + ":4118/");
		String binary = System.getProperty("sm.gui.binary", "subscription-manager-gui");
		launchapp(binary, []);
		waittillguiexist(UI.mainWindow);
	}
	
	@Test(alwaysRun=true)
	def public static void myfirstTest() {
		println("Oh boy!")
	}
}

