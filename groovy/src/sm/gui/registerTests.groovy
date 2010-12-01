package sm.gui
import org.testng.annotations.Test 
import sm.gui.Testscript

class Blah {
	
}

@Mixin(Testscript)
class registerTests {
	
	/*public registerTests(){
		base = new Testscript()
	}*/
	
	def regularMethod(){
		"what regularMethod returns!"
	}
	
	@Test
	def public void mytest(){
		println("My first test passed boyyyyy on ${version}")
	}
}

