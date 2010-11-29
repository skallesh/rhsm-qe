def capitalize(s) { s[0].toUpperCase() + s[1..-1]}

def same_name = { Object[] element_list -> 
		map = [:] //empty map
		element_list.collect { map.put(it, split("_").collect { capitalize(it) }.join(" ")) }
	}

def windows = [mainWindow:  [id: "manage_subscriptions_dialog",
		elements: [close_main: "button_close",
			add: "add_button",
			registration: "account_settings"]],
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

def new_windows = [mainWindow: [id: "Subscription Manager",
		elements: same_name('registration_settings',
		'register_system',
		'dd-subscription',
		'view-my-system-facts',
		'glossary',
		'become-compliant')],
	registerDialog: windows.register_dialog]


def loop_w_timeout(timeout, cond, body) {
	start = System.currentTimeMillis()
	while (cond.call()) {
		if (System.currentTimeMillis() - start > timeout) throw new RuntimeException("Timed out!")
		body.call()
	}
}

