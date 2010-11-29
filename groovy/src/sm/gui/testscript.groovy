package sm.gui
import com.redhat.qe.sm.base.SubscriptionManagerBaseTestScript
import sm.gui.ui
import sm.gui.ldtp
import org.testng.annotations.BeforeSuite 
import org.testng.annotations.Test 
//
class testscript extends SubscriptionManagerBaseTestScript{
	
	static { mixin ldtp }  
	//def static ldtp = null
	def static UI = new ui()
	
	@BeforeSuite
	def startLDTP(){
		connect("http://"  + clienthostname + ":4118/");
		String binary = System.getProperty("rhsm.gui.binary", "subscription-manager-gui");
		launchapp(binary, []);
		waittillguiexist(UI.mainWindow);
	}
	
	@Test(alwaysRun=true)
	def public void myfirstTest() {
		println("Oh boy!")
	}
}

