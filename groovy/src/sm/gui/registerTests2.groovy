package sm.gui

import org.testng.annotations.Test;

@Mixin(registerTests)
class registerTests2 {

	public registerTests2() {
		version = "2.4"
	}
	
	@Test
	def public void mytest(){
		println(regularMethod())
		println("My first test passed boyyyyy on ${version}")
	}

}
