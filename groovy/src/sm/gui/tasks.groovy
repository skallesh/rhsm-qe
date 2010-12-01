package sm.gui
import java.util.Map;
import sm.gui.ui
import sm.gui.ldtp

@Mixin(ldtp)
class tasks {
	
	def register(String username, String password, String systemName= null, boolean autoSubscribe=false) {
		/*[systemName: clienthostname, autoSubscribe:false, *:options].with {
			println("username: $username, ")
			}*/
		click(ui.register)
	}
	
	/*def withRequiredArgs(Map args, List<String> reqargs, Closure c) {
		for (reqarg in reqargs){
			if (! args.get(reqarg)) throw new IllegalArgumentException("Method called without required Map argument $reqarg.")
		}
		c.call(args)
	}*/
}
