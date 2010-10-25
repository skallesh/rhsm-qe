(ns sm.gui.ui
  (:import com.redhat.qe.ldtpclient.Element
   [org.apache.xmlrpc.client XmlRpcClient XmlRpcClientConfigImpl]))

(defn javacall "call a method on a java object, given an arbitrary number of args" 
  [obj name & args]
  (clojure.lang.Reflector/invokeInstanceMethod obj (str name)
					       (if args (to-array args) clojure.lang.RT/EMPTY_ARRAY)))

;;A map of keywords to the GNOME ui data
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


(defn element "Given a keyword, retrieve the window id and element id for it, 
and return those 2 items in a vector" 
  [name-kw]
  (let [window (first (filter #(get-in % [:elements name-kw]) (vals windows)))
        elem (get-in window [:elements name-kw])]
    (if elem [(:id window) elem]
      (throw (RuntimeException. (format "%s not found in ui mapping." name-kw))))))


(defn define-ldtp-call2 [name args client]
  (intern *ns* (symbol name) 
    (fn [& args] 
      (apply javacall client "execute" name args))))

(defn create-xmlrpc-client [url specfile]
  (let [config (XmlRpcClientConfigImpl.)
        client (XmlRpcClient.)]
    (.setServerURL config (java.net.URL. url))
    (.setConfig client config)
    (let [methods (with-in-str (slurp specfile) (read))]
      (doall 
        (map (fn [method] (define-ldtp-call2 (first method) (second method) client)) 
             methods)))
    client))

