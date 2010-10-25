(ns sm.gui.ui
  (:import com.redhat.qe.ldtpclient.Element
   [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))



(def windows {:mainWindow  {:id "manage_subscriptions_dialog"
                            :elements {:close-main "button_close"
                                       :add "add_button"
                                       :registration "account_settings"}}
              :registerDialog {:id "register_dialog"
                               :elements {:redhat-login "account_login"
                                          :password "accound_password"
                                          :system-name "consumer-name"
                                          :automatically-subscribe "auto_bind"
                                          :register "register_button"}}
              :registrationSettingsDialog {:id "register_token_dialog"
                                           :elements {:registration-token "regtoken-entry"}}
              :errorDialog "Error"
              :questionDialog "Question"
              :factsDialog "facts_dialog"
              :subscribeDialog "dialog_add"})


(defn element [name-kw]
  (let [window (first (filter #(get-in % [:elements name-kw]) (vals windows)))
        elem (get-in window [:elements name-kw])]
    (if elem [(:id window) elem]
      (throw (RuntimeException. (format "%s not found in ui mapping." name-kw))))))


(defmacro define-ldtp-call [name args]
  `(defn ~@(symbol name) ~@(map symbol args)))

; ~@(concat '(.execute *xmlrpc*) ~@(map symbol args))

(defn create-xmlrpc-client [url specfile]
  (let [config (XmlRpcClientConfigImpl.)
        client (XmlRpcClient.)]
    (. config setServerURL (java.net.URL. url))
    (. client setConfig config)
    (def ^{:private true} *xmlrpc* client)
    (let [methods (with-in-str (slurp specfile) (read))]
      (doall 
        (map (fn [method] (define-ldtp-call (first method) (second method))) 
             methods)))))

