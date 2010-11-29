package sm.gui
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.net.URL

class ldtp {
	def url
	def client
	public ldtp(String url) {
		this.url = url 
		this.client = new XmlRpcClient()
		def config = new XmlRpcClientConfigImpl()
		config.setServerURL(new URL(url))
		this.client.setConfig(config)
	}
	 
	def methodMissing(String name, args) {
		//apply arglist if necessary
		return client.invokeMethod("execute", [name, args] )
	}
 
}
