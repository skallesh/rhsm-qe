//comment
package sm.gui
class ui {
  String capitalize(s) { s[0].toUpperCase() + s[1..-1]}

  def same_name = { 
    Object[] element_list -> 
      def map = [:] //empty map
      element_list.collect { map.put(it, it.split("_").collect { capitalize(it) }.join(" ")) }
      map
  }

  def windows = [mainWindow: [id: "Subscription Manager",
                              elements: same_name('registration_settings',
                                                  'register_system',
                                                  'dd_subscription',
                                                  'view_my_system_facts',
                                                  'glossary',
                                                  'become_compliant')],
                 registerDialog: [id: "register_dialog",
                                  elements: [redhat_login: "account_login",
                                             password: "accound_password",
                                             system_name: "consumer-name",
                                             automatically_subscribe: "auto_bind",
                                             register: "register_button"]],
                 registrationSettingsDialog: [id: "register_token_dialog", 
                                              elements: [registration_token: "regtoken-entry"]],
                 errorDialog: "Error",
                 questionDialog: "Question",
                 factsDialog: "facts_dialog",
                 subscribeDialog: "dialog_add"]

  def element(String elem) {
    def window = windows.find { 
      it.value instanceof java.util.Map && 
      it.value.elements.get(elem) 
    }
    if (window) {
      [window.key, window.value.elements[elem]]
    }
    else null
  }
  
  def propertyMissing(String name) {
    //first try to get an element
	def elem = element(name)
    if (elem) elem[1]
    else {
		//if not found, maybe it's a window name
		def window = windows[name].id
		if (window) window 
		else throw new MissingPropertyException("No such property: $name for class ${this.class}")
       }
	}
  
  /* def define_elements() {
	  def allwin = windows.findAll { it.value instanceof java.util.Map }
	  def allelems = [:]
	  allwin.collect { it.value.elements.collect { allelems.put(it.key, element(it.key)) }}
  	  for (elem in allelems) {
			println("adding $elem")
			this.setProperty("$elem.key",elem.value)
		}
  }*/
}