package sm.gui
import com.redhat.qe.sm.base.SubscriptionManagerBaseTestScript
import sm.gui.ui
import org.testng.annotations.BeforeSuite 
import org.testng.annotations.Test 

class testscript extends SubscriptionManagerBaseTestScript{

	def static ldtp = null
	def static UI = new ui()
	testscript() {
		if (ldtp == null) {
			
		}
	}
	
	@BeforeSuite
	def startLDTP(){
		ldtp= new ldtp("http://"  + clienthostname + ":4118/");
		String binary = System.getProperty("rhsm.gui.binary", "subscription-manager-gui");
		ldtp.launchapp(binary, []);
		ldtp.waittillguiexist(UI.mainWindow);
	}
	
	@Test(alwaysRun=true)
	def public void myfirstTest() {
		println("Oh boy!")
	}
}
