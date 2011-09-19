(ns com.redhat.qe.sm.gui.tasks.tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       cli-tasks
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [error.handler :only (add-recoveries raise)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.contrib.str-utils :only (re-split)]
        gnome.ldtp)
  (:require [clojure.contrib.logging :as log]
            com.redhat.qe.sm.gui.tasks.ui) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [com.redhat.qe.sm.cli.tasks CandlepinTasks]))


(def ui gnome.ldtp/action) ;;alias action in ldtp to ui here

(defn sleep
  "Sleeps for a given ammount of miliseconds."
  [ms]
  (. Thread (sleep ms)))

(def is-boolean?
  (fn [expn]
    (or
      (= expn 'true)
      (= expn 'false))))

(defmacro loop-timeout [timeout bindings & forms]
  `(let [starttime# (System/currentTimeMillis)]
     (loop ~bindings
       (if  (> (- (System/currentTimeMillis) starttime#) ~timeout)
	 (throw (RuntimeException. (str "Hit timeout of " ~timeout "ms.")))
	 (do ~@forms)))))

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {:invalid-credentials #"Invalid Credentials|Invalid username or password.*"
                   :no-username #"You must enter a login"
                   :no-password #"You must enter a password"
                   :wrong-consumer-type #"Consumers of this type are not allowed"
                   :network-error #"Network error, unable to connect to server.*"
                   :error-updating #"Error updating system data*"
                   })

(defn matching-error
  "Returns a keyword of known error, if the message matches any of them."
  [message]
  (let [matches-message? (fn [key] (let [re (known-errors key)]
                                    (if (re-find re message) key false)))]
    (or (some matches-message? (keys known-errors))
	:sm-error)))

(defn connect
  ([url] (set-url url))
  ([] (connect (@config :ldtp-url))))

(defn start-app
  "starts the subscription-manager-gui
  @path: lauch the application at [path]"
  ([]
     (start-app (@config :binary-path)))
  ([path]
     (ui launchapp path [] 10)
     (ui waittillwindowexist :main-window 30)))

(defn kill-app
  "Kills the subscription-manager-gui"
  []
  (.runCommandAndWait @clientcmd "killall -9 subscription-manager-gui")
  (ui waittillwindownotexist :main-window 30))

(defn restart-app
  "Restarts subscription-manager-gui"
  []
  (kill-app)
  (start-app))

(defn start-firstboot
  "Convenience function that calls start-app with the firstboot path."
  []
  ;(if (= "NO" (.getConfFileParameter @cli-tasks "/etc/sysconfig/firstboot" "RUN_FIRSTBOOT"))
  ;  (.updateConfFileParameter @cli-tasks (.rhsmConfFile @cli-tasks) "RUN_FIRSTBOOT" "YES"))
  (let [path (@config :firstboot-binary-path)]
    (ui launchapp path [] 10)
    (ui waittillwindowexist :firstboot-window 30)))

(defn get-error-msg
  "Retrieves the error string from the RHSM error dialog."
  []
  (.trim (ui getobjectproperty :error-msg "label")))
 
(defn clear-error-dialog
  "Clears an error dialog by clicking OK."
  []
  (ui click :ok-error))

(defn checkforerror
  "Checks for the error dialog for 3 seconds and logs the error message.
  Allows for recovery of the error message.
  @wait: specify the time to wait for the error dialog."
  ([wait]
      (add-recoveries
       {:log-warning (fn [e] (log/warn
                             (format "Got error %s, message was: '%s'"
                                     (name (:type e)) (:msg e))))}
       (if (= 1 (ui waittillwindowexist :error-dialog wait)) 
         (let [message (get-error-msg)
               type (matching-error message)]
           (clear-error-dialog)
           (raise {:type type 
                   :msg message})))))
  ([] (checkforerror 3)))
               
(defn set-conf-file-value
  "Edits /etc/rhsm/rhsm.conf to set values within it.
  field: the field to change.
  value: the value to set the field to."
  [field value]
  (.updateConfFileParameter @cli-tasks (.rhsmConfFile @cli-tasks) field value))
  
(defn conf-file-value
  "Grabs the value of a field in /etc/rhsm/rhsm.conf."
  [k]
  (.getConfFileParameter @cli-tasks (.rhsmConfFile @cli-tasks) k))

(defn unregister
  "Unregisters subscripton manager by clicking the 'unregister' button."
  []
  (if (ui showing? :register-system)
    (raise {:type :not-registered
            :msg "Tried to unregister when already unregistered."}))
  (ui click :unregister-system)
  (ui waittillwindowexist :question-dialog 30)
  (ui click :yes)
  (checkforerror))

(defn register
  "Registers subscription manager by clicking the 'Register System' button in the gui."
  [username password & {:keys [system-name-input, autosubscribe, owner]
                        :or {system-name-input nil, autosubscribe false, owner nil}}]
  (if (ui showing? :unregister-system)
    (raise {:type :already-registered
          :username username
          :password password
          :name system-name-input
          :auto autosubscribe
          :ownername owner
          :unregister-first (fn [e] (unregister)
                              (register (:username e)
                                        (:password e)
                                        :system-name-input (:name e)
                                        :autosubscribe (:auto e)
                                        :owner (:ownername e)))}))
  (ui click :register-system)
  (ui waittillguiexist :redhat-login)
  (ui settextvalue :redhat-login username)
  (ui settextvalue :password password)
  (when system-name-input
    (ui settextvalue :system-name system-name-input))
  (if autosubscribe 
   (ui check :automatically-subscribe)
   (ui uncheck :automatically-subscribe))  
  (add-recoveries {:cancel (fn [e] (ui click :register-cancel))}
   (ui click :register)
   (checkforerror 10)
   (if (= 1 (ui guiexist :register-dialog)) 
     (if (= 1 (ui waittillshowing :owners 30))
       (do
         (when owner (do 
                       (if-not (ui rowexist? :owners owner)
                         (raise {:type :owner-not-available
                                 :name owner
                                 :msg (str "Not found in 'Owner Selection':" owner)}))
                       (ui selectrow :owners owner)))
         (ui click :register)
         (sleep 5))))              
    (checkforerror)))

(defn fbshowing?
  "Utility to see if a GUI object in firstboot on a RHEL5 system is showing."
  ([item]
     ;; since all items exist at all times in firstboot,
     ;;  we must poll the states and see if 'SHOWING' is among them
     ;; "SHOWING" == 24  on RHEL5
     (= 24 (some #{24} (seq (ui getallstates item)))))
  ([window_name component_name]
     (if (some #(re-find (re-pattern (str ".*" component_name ".*")) %)
               (seq (ui getobjectlist window_name)))
    (= 24 (some #{24} (seq (ui getallstates window_name component_name))))
    false)))

(defn firstboot-register
  "Subscribes subscription-manager from within firstboot."
  [username password & {:keys [system-name-input, autosubscribe]
                          :or {system-name-input nil, autosubscribe false}}]
  (assert  (or (fbshowing? :firstboot-user)
               (= 1 (ui guiexist :firstboot-window "Entitlement Platform Registration"))))
  (ui settextvalue :firstboot-user username)
  (ui settextvalue :firstboot-pass password)
  (when system-name-input
    (ui settextvalue :firstboot-system-name system-name-input))
  (if autosubscribe
    (ui check :firstboot-autosubscribe)
    (ui uncheck :firstboot-autosubscribe))
    (ui click :firstboot-forward)
    (checkforerror))
  

(defn wait-for-progress-bar
  "Waits for a progress bar to finish."
  []
  (ui waittillwindowexist :progress-dialog 1)
  (ui waittillwindownotexist :progress-dialog 30)
  (checkforerror))

(defn search
  "Performs a subscription search within subscription-manager-gui."
  ([match-system?, do-not-overlap?, match-installed?, contain-text, active-on] 
  (ui selecttab :all-available-subscriptions)
  (ui click :more-search-options)
  (let [setchecked (fn [needs-check?] (if needs-check? check uncheck))]
    (ui (setchecked match-system?) :match-system)
    (ui (setchecked do-not-overlap?) :do-not-overlap)
    (ui (setchecked match-installed?) :match-installed))
  (if active-on (comment "Procedure to set date goes here, BZ#675777 "))
  (if contain-text
    (ui settextvalue :contains-the-text contain-text)
    (ui settextvalue :contains-the-text ""))
  (ui click :search)
  (wait-for-progress-bar)) 
  ([{:keys [match-system?, do-not-overlap?, match-installed?, contain-text, active-on]
     :or {match-system? true
          do-not-overlap? true
          match-installed? false
          contain-text nil
          active-on nil}}]
     (search match-system?, do-not-overlap?, match-installed?, contain-text, active-on))
  ([] (search {})))

(defn skip-dropdown
  "Skips dropdown items in a table."
  [table item]
  (if-not (ui rowexist? table item)
    (raise {:type :item-not-available
            :name item
            :msg (str "Not found in " table ": " item)}))
  (let [row (ui gettablerowindex table item)
        is-item? (fn [rownum]
                  (try (ui getcellvalue table rownum 2) true
                       (catch Exception e false)))]
    (if (is-item? row)
      (ui selectrow table item)
      (do
        (if (and (is-item? (+ 1 row))
                 (= item (ui getcellvalue table (+ 1 row) 0)))
          (ui selectrowindex table (+ 1 row))
          (raise {:type :invalid-item
                  :name item
                  :msg (str "Invalid item:" item)}))))))

(defn open-contract-selection
  "Opens the contract selection dialog for a given subscription." 
  [s]
  (ui selecttab :all-available-subscriptions)
  (skip-dropdown :all-subscriptions-view s)
  (ui click :subscribe)
  (checkforerror)
  (if-not (= 1 (ui waittillwindowexist :contract-selection-dialog 5))
    (raise {:type :contract-selection-not-available
            :name s
            :msg (str s " Does not have multiple contracts.")})))

(defn subscribe
  "Subscribes to a given subscription, s."
  [s] 
  (ui selecttab :all-available-subscriptions)
  (skip-dropdown :all-subscriptions-view s)
  (ui click :subscribe)
  (checkforerror)
  (ui waittillwindowexist :contract-selection-dialog 5)
  (if (= 1 (ui guiexist :contract-selection-dialog))
    (do (ui selectrowindex :contract-selection-table 0)  ;;pick first contract for now
        (ui click :subscribe-contract-selection)))
  (checkforerror)
  (wait-for-progress-bar))
  

(defn unsubscribe
  "Unsubscribes from a given subscription, s"
  [s]
  (ui selecttab :my-subscriptions)
  (sleep 5000)
  (if-not (ui rowexist? :my-subscriptions-view s)
    (raise {:type :not-subscribed
            :name s
            :msg (str "Not found in 'My Subscriptions': " s)}))
  (ui selectrow :my-subscriptions-view s)
  (ui click :unsubscribe)
  (ui waittillwindowexist :question-dialog 60)
  (ui click :yes)
  (checkforerror) )

(defn enableproxy-auth
  "Configures a proxy that uses authentication through subscription-manager-gui."
  ([proxy port user pass firstboot]
     (assert (is-boolean? firstboot))
     (if firstboot 
       (do (ui click :firstboot-proxy-config)
           (ui waittillwindowexist :firstboot-proxy-dialog 60)
           (ui check :firstboot-proxy-checkbox)
           (ui settextvalue :firstboot-proxy-location (str proxy ":" port))
           (ui check :firstboot-auth-checkbox)
           (ui settextvalue :firstboot-proxy-user user)
           (ui settextvalue :firstboot-proxy-pass pass)
           (ui click :firstboot-proxy-close)
           (checkforerror))
       (do (ui selecttab :my-installed-software)
           (ui click :proxy-configuration)
           (ui waittillwindowexist :proxy-config-dialog 60)
           (ui check :proxy-checkbox)
           (ui settextvalue :proxy-location (str proxy ":" port))
           (ui check :authentication-checkbox)
           (ui settextvalue :username-text user)
           (ui settextvalue :password-text pass)
           (ui click :close-proxy)
           (checkforerror))))
  ([proxy port user pass] (enableproxy-auth proxy port user pass false)))

(defn enableproxy-noauth
  "Configures a proxy that does not use authentication through subscription-manager-gui."
  ([proxy port firstboot]
     (assert (is-boolean? firstboot))
     (if firstboot
       (do (ui click :firstboot-proxy-config)
           (ui waittillwindowexist :firstboot-proxy-dialog 60)
           (ui check :firstboot-proxy-checkbox)
           (ui settextvalue :firstboot-proxy-location (str proxy ":" port))
           (ui uncheck :firstboot-auth-checkbox)
           (ui click :firstboot-proxy-close)
           (checkforerror))
       (do (ui selecttab :my-installed-software)
           (ui click :proxy-configuration)
           (ui waittillwindowexist :proxy-config-dialog 60)
           (ui check :proxy-checkbox)
           (ui settextvalue :proxy-location (str proxy ":" port))
           (ui uncheck :authentication-checkbox)
           (ui click :close-proxy)
           (checkforerror))))
  ([proxy port] (enableproxy-noauth proxy port false)))
  
(defn disableproxy
  "Disables any proxy settings through subscription-manager-gui."
  ([firstboot]
     (assert (is-boolean? firstboot))
     (if firstboot
       (do (ui click :firstboot-proxy-config)
           (ui waittillwindowexist :firstboot-proxy-dialog 60)
           (ui uncheck :firstboot-proxy-checkbox)
           (ui uncheck :firstboot-auth-checkbox)
           (ui click :firstboot-proxy-close)
           (checkforerror))
       (do (ui selecttab :my-installed-software)
           (ui click :proxy-configuration)
           (ui waittillwindowexist :proxy-config-dialog 60)
           (ui uncheck :proxy-checkbox)
           (ui uncheck :authentication-checkbox)
           (ui click :close-proxy)
           (checkforerror))))
  ([] (disableproxy false)))

(defn warn-count
  "Grabs the number of products that do not have a valid subscription tied to them
  as reported by the GUI."
  []
  (if (= 1 (ui guiexist :main-window "You have*"))
    (let [countlabel (ui getobjectproperty :main-window "You have*" "label")]
      (Integer/parseInt (first (re-seq #"\w+" (.substring countlabel 9)))))
    0))

(defn compliance?
  "Returns true if the GUI reports that all products have a valid subscription."
  []
  (= 1 (ui guiexist :main-window "Product entitlement certificates valid*")))  

(defn first-date-of-noncomply
  "Pulls the first date of noncompliance from the subscription assistant dialog."
  []
  (if (= 1 (ui guiexist :subscription-assistant-dialog))
    (let [datelabel (ui getobjectproperty :subscription-assistant-dialog "*first date*" "label")]
      (.substring datelabel 0 10))))
  
(defn assistant-subscribe
  "Subscribes to a given subscription from within the subscription assistant."
  [s]
  (skip-dropdown :assistant-subscription-view s)
  (ui click :assistant-subscribe)
  (checkforerror)
  (wait-for-progress-bar))  

(defn get-table-elements
  "Returns a vector containing all elements in a given table and column."
  [view col & {:keys [skip-dropdowns?]
               :or {skip-dropdowns? false}}]
  (if-not skip-dropdowns?
    (do
      (for [row (range (action getrowcount view))]
        (ui getcellvalue view row col)))
    (do
      (let [allitems (get-table-elements view 0)
            is-item? (fn [rownum]
                      (try (ui getcellvalue view rownum 2) true
                           (catch Exception e false)))
            rownums (filter is-item? (range (ui getrowcount view)))
            items (map (fn [rowid]
                         (ui getcellvalue view rowid 0))
                       rownums)]
        items)))) 
  
(defn do-to-all-rows-in
  "Perferms a given function on all elements in a given table and column."
  [view col f & {:keys [skip-dropdowns?]
                 :or {skip-dropdowns? false}}]
  (let [item-list (get-table-elements view col :skip-dropdowns? skip-dropdowns?)]
    (doseq [item item-list]
      (f item))))

(defn verify-conf-proxies
  "Utility function to help verify that the proxy values in
  /etc/rhsm/rhsm.conf match what is expected."
  [hostname port user password]
  (let [config-file-hostname  (conf-file-value "proxy_hostname")
        config-file-port      (conf-file-value "proxy_port")
        config-file-user      (conf-file-value "proxy_user")
        config-file-password  (conf-file-value "proxy_password")]
    (verify (= config-file-hostname hostname))
    (verify (= config-file-port port)) 
    (verify (= config-file-user user))
    (verify (= config-file-password password))))

(defn get-logging
  "Runs a given function f and returns changes to a log file.
  runner: an instance of a SSHCommandRunner
  log: string containing which log file to look at
  name: string used to mark the log file
  grep: what to grep the results for"
  [runner log name grep f]
  (let [marker (str (System/currentTimeMillis) " " name)]
    (RemoteFileTasks/markFile runner log marker)
    (f)
    (RemoteFileTasks/getTailFromMarkedFile runner log marker grep)))

(defn get-owners
  "Given a username and password, this function returns a list
  of owners associated with that user"
  [username password]
  (let [server (conf-file-value "hostname")
        port (conf-file-value "port")
        prefix (conf-file-value "prefix")]
    (seq (CandlepinTasks/getOrgsKeyValueForUser server
                                                port
                                                prefix
                                                username
                                                password
                                                "displayName"))))

(defn get-owner-display-name
  "Given a owner key (org key) this returns the owner's display name"
  [username password orgkey]
  (let [server (conf-file-value "hostname")
        port (conf-file-value "port")
        prefix (conf-file-value "prefix")]
    (CandlepinTasks/getOrgDisplayNameForOrgKey server
                                               port
                                               prefix
                                               username
                                               password
                                               orgkey)))

(defn get-pool-id
  "Get the pool ID for a given subscription/contract pair."
  [username password orgkey subscription contract]
  (let [server (conf-file-value "hostname")
        port (conf-file-value "port")
        prefix (conf-file-value "prefix")]
    (CandlepinTasks/getPoolIdFromProductNameAndContractNumber server
                                                              port
                                                              prefix
                                                              username
                                                              password
                                                              orgkey
                                                              subscription
                                                              contract)))
(defn multi-entitlement?
  "Returns true if the subscription can be entitled to multiple times."
  [username password pool]
  (let [server (conf-file-value "hostname")
        port (conf-file-value "port")
        prefix (conf-file-value "prefix")]
    (CandlepinTasks/isPoolProductMultiEntitlement server
                                                  port
                                                  prefix
                                                  username
                                                  password
                                                  pool)))

(defn get-all-facts []
  (ui click :view-system-facts)
  (ui waittillguiexist :facts-view)
  (let [groups (get-table-elements :facts-view 0)
        is-data? (fn [rownum]
                   (try (ui getcellvalue :facts-view rownum 1) true
                        (catch Exception e false)))
        _ (doseq [item groups] (do (ui doubleclickrow :facts-view item)
                                   (sleep 500)))
        rownums (filter is-data? (range (ui getrowcount :facts-view)))
        getcell (fn [row col] 
                   (ui getcellvalue :facts-view row col))
        facts (into {} (map (fn [rowid] 
                              [(getcell rowid 0) (getcell rowid 1)])
                               rownums))]
    (ui click :close-facts)
    facts))





