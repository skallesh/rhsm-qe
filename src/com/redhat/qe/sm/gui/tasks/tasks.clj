(ns com.redhat.qe.sm.gui.tasks.tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       cli-tasks
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [slingshot.slingshot :only [throw+ try+]]
        [com.redhat.qe.verify :only (verify)]
        [clojure.string :only (split
                               split-lines)]
        matchure
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [com.redhat.qe.sm.gui.tasks.candlepin-tasks :as ctasks]
            com.redhat.qe.sm.gui.tasks.ui) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [com.redhat.qe.sm.cli.tasks CandlepinTasks]
           [com.redhat.qe.sm.base SubscriptionManagerBaseTestScript]))


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

(defn #^String substring?
  "True if s contains the substring."
  [substring #^String s]
  (.contains s substring))

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {:invalid-credentials #"Invalid Credentials|Invalid username or password.*"
                   :no-username #"You must enter a login"
                   :no-password #"You must enter a password"
                   :wrong-consumer-type #"Consumers of this type are not allowed"
                   :network-error #"Network error, unable to connect to server.*"
                   :error-updating #"Error updating system data*"
                   :date-error #"Invalid date format. Please re-enter a valid date*"
                   :invalid-cert #"is not a valid certificate file. Please upload a valid certificate*"
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
     (start-app (@config :binary-path) :main-window))
  ([path]
     (ui launchapp path [] 10))
  ([path window]
     (ui launchapp path [] 10)
     (ui waittillwindowexist window 30)
     (sleep 1000)
     (ui maximizewindow window)))

(defn kill-app
  "Kills the subscription-manager-gui"
  []
  (.runCommandAndWait @clientcmd "killall -9 subscription-manager-gui")
  (ui waittillwindownotexist :main-window 30))

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
     (if (= 1 (ui waittillwindowexist :error-dialog wait)) 
       (let [message (get-error-msg)
             type (matching-error message)]
         (clear-error-dialog)
         (throw+ {:type type 
                  :msg message
                  :log-warning (fn []
                                 (log/warn
                                  (format "Got error '%s', message was: '%s'"
                                          (name type) message)))}))))
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
    (throw+ {:type :not-registered
            :msg "Tried to unregister when already unregistered."}))
  (ui click :unregister-system)
  (ui waittillwindowexist :question-dialog 30)
  (ui click :yes)
  (checkforerror))

(defn register
  "Registers subscription manager by clicking the 'Register System' button in the gui."
  [username password & {:keys [system-name-input
                               skip-autosubscribe
                               owner
                               sla
                               auto-select-sla]
                        :or {system-name-input nil
                             skip-autosubscribe true
                             owner nil
                             sla nil
                             auto-select-sla true}}]
  (if (ui showing? :unregister-system)
    (throw+ {:type :already-registered
             :username username
             :password password
             :system-name-input system-name-input
             :skip-autosubscribe skip-autosubscribe
             :owner owner
             :unregister-first (fn []
                                 (unregister)
                                 (register username
                                           password
                                           :system-name-input system-name-input
                                           :skip-autosubscribe skip-autosubscribe
                                           :owner owner))}))
  (ui click :register-system)
  (ui waittillguiexist :redhat-login)
  (ui settextvalue :redhat-login username)
  (ui settextvalue :password password)
  (when system-name-input
    (ui settextvalue :system-name system-name-input))
  (if skip-autosubscribe 
   (ui check :skip-autobind)
   (ui uncheck :skip-autobind))  
  (try+ 
   (ui click :register)
   (checkforerror 10)
   (if (= 1 (ui guiexist :register-dialog))
     (do
       ;; handle owner selection
       (if (= 1 (ui waittillshowing :owners 30))
         (do
           (when owner (do 
                         (if-not (ui rowexist? :owner-view owner)
                           (throw+ {:type :owner-not-available
                                    :name owner
                                    :msg (str "Not found in 'Owner Selection':" owner)})
                           (ui selectrow :owner-view owner))))    
           (ui click :register)
           (checkforerror 10)))
       (ui waittillnotshowing :registering 1800)  ;; 30 minutes
       ;; handle sla selection
       (if (= 1 (ui guiexist :subscribe-system-dialog))
         (do
           (if auto-select-sla
             (do 
               (if (= 1 (ui guiexist :sla-forward)) ;; sla selection is presented
                 (do (when sla (ui click :subscribe-system-dialog sla))
                     (ui click :sla-forward)))
               (ui click :sla-subscribe))
             ;; else leave sla dialog open
             (when sla (ui click :subscribe-system-dialog sla)))))))
   (checkforerror)
   (catch Object e
     (if (substring? "No service levels" (:msg e))
       (throw+ (assoc e :type :no-sla-available))
       (throw+ (into e {:cancel (fn [] (ui click :register-cancel))})))))
  (sleep 10000))

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
  [username password & {:keys [system-name-input, autosubscribe?, org]
                          :or {system-name-input nil, autosubscribe? false, org nil}}]
  (assert  (or (fbshowing? :firstboot-user)
               (= 1 (ui guiexist :firstboot-window "Entitlement Platform Registration"))))
  (ui settextvalue :firstboot-user username)
  (ui settextvalue :firstboot-pass password)
  (when system-name-input
    (ui settextvalue :firstboot-system-name system-name-input))
  (if autosubscribe?
    (ui check :firstboot-autosubscribe)
    (ui uncheck :firstboot-autosubscribe))
  (ui click :firstboot-forward)
  (checkforerror)
  (if (ui showing? :firstboot-window "Organization Selection")
    (do 
      (if org (ui selectrow :firstboot-owner-table org))
      (ui click :firstboot-forward)))
  (checkforerror))
  

(defn wait-for-progress-bar
  "Waits for a progress bar to finish."
  []
  (ui waittillwindowexist :progress-dialog 1)
  (ui waittillwindownotexist :progress-dialog 30)
  (checkforerror))

;; TODO: write this with better airities
(defn search
  "Performs a subscription search within subscription-manager-gui."
  ([match-system?, do-not-overlap?, match-installed?, contain-text, active-on] 
  (ui selecttab :all-available-subscriptions)
  (ui click :more-search-options)
  (let [setchecked (fn [needs-check?] (if needs-check? check uncheck))]
    (ui (setchecked match-system?) :match-system)
    (ui (setchecked do-not-overlap?) :do-not-overlap)
    (ui (setchecked match-installed?) :match-installed))
  (ui click :more-search-options)
  (if active-on ;BZ#675777
    (ui settextvalue :date-entry active-on))
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

(defn is-item?
  "Determines if an item in a table is a dropdown or an item."
  [table rownum]
  (try (let [value (ui getcellvalue table rownum 2)]
         (if (= 0 value)
           (try (let [value (ui getcellvalue table rownum 3)]
                  (if (= 0 value)
                    false
                    true))
                (catch Exception e true))
           true))
       (catch Exception e false)))

(defn skip-dropdown
  "Skips dropdown items in a table."
  [table item]
  (if-not (ui rowexist? table item)
    (throw+ {:type :item-not-available
             :name item
             :msg (str "Not found in " table ": " item)}))
  (let [row (ui gettablerowindex table item)]
    (if (is-item? table row)
      (do
        (ui selectrow table item)
        row)
      (do
        (if (and (is-item? table (+ 1 row))
                 (= item (ui getcellvalue table (+ 1 row) 0)))
          (do (ui selectrowindex table (+ 1 row))
              (+ 1 row))
          (throw+ {:type :invalid-item
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
    (throw+ {:type :contract-selection-not-available
             :name s
             :msg (str s " does not have multiple contracts.")})))

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
    (throw+ {:type :not-subscribed
             :name s
             :msg (str "Not found in 'My Subscriptions': " s)}))
  (ui selectrow :my-subscriptions-view s)
  (ui click :unsubscribe)
  (ui waittillwindowexist :question-dialog 60)
  (ui click :yes)
  (checkforerror))

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
           (try
             (ui settextvalue :proxy-location (str proxy ":" port))
             (catch Exception e
               (if (substring? "not implemented" (.getMessage e))
                 (do (sleep 2000)
                     (ui generatekeyevent (str proxy ":" port)))
                 (throw e))))
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
           (try
             (ui settextvalue :proxy-location (str proxy ":" port))
             (catch Exception e
               ;; yay rhel5
               (if (substring? "not implemented" (.getMessage e))
                 (do (sleep 2000)
                     (ui generatekeyevent (str proxy ":" port)))
                 (throw e))))
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
  (or (= 1 (ui guiexist :main-window "Product entitlement certificates valid*"))
      (= 1 (ui guiexist :main-window "No product certificates installed*"))))  

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
            item-filter (fn [item] (is-item? view item))
            rownums (filter item-filter (range (ui getrowcount view)))
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

(comment
  ;old and busted
  (defn get-logging
    
    [runner logfile name grep f]
    (let [marker (str (System/currentTimeMillis) " " name)]
      (RemoteFileTasks/markFile runner logfile marker)
      (f)
      (RemoteFileTasks/getTailFromMarkedFile runner logfile marker grep))))

(defmacro get-logging [runner logfile name grep & forms]
  "Runs given forms and returns changes to a log file.
  runner: an instance of a SSHCommandRunner
  log: string containing which log file to look at
  name: string used to mark the log file
  grep: what to grep the results for"
  `(let [marker# (str (System/currentTimeMillis) " " ~name)]
     (RemoteFileTasks/markFile ~runner ~logfile marker#)
     (do ~@forms)
     (RemoteFileTasks/getTailFromMarkedFile ~runner ~logfile marker# ~grep)))

(defn register-with-creds
  "Registers user with credentials found in automation.properties"
  [& {:keys [re-register?]
    :or {re-register? true}}]
  (let [ownername (ctasks/get-owner-display-name (@config :username)
                                                 (@config :password)
                                                 (@config :owner-key))]
    (if re-register?
      ;re-register with handlers
      (try+
       (register (@config :username)
                 (@config :password)
                 :owner ownername)
       (catch
           [:type :already-registered]
           {:keys [unregister-first]}
         (unregister-first)))
      ;else just attempt a register
      (register (@config :username)
                (@config :password)
                :owner ownername))))

(defn restart-app
  "Restarts subscription-manager-gui"
  [& {:keys [unregister?
             reregister?]
      :or {unregister? false
           reregister? false}}]
  (kill-app)
  (if (or unregister? reregister?)
    (.runCommandAndWait @clientcmd "subscription-manager unregister"))
  (start-app)
  (if reregister?
    (register-with-creds)))

(defn get-all-facts
  "Creates and returns a map of all system facts as read in via the rhsm-gui"
  []
  (ui click :view-system-facts)
  (ui waittillguiexist :facts-view)
  (sleep 5000)
  (let [groups (get-table-elements :facts-view 0)
        getcell (fn [row col] 
                   (ui getcellvalue :facts-view row col))
        is-data? (fn [rownum]
                   (try (let [value (getcell rownum 1)
                              field (getcell rownum 0)]
                          (if-not (= value 0)
                            true
                            (if-not (= nil (some #{\.} (seq field)))
                              true
                              false)))
                        (catch Exception e false)))
        _ (doseq [item groups] (do (ui selectrow :facts-view item)
                                   (sleep 500)
                                   (ui doubleclickrow :facts-view item)
                                   (sleep 500)))
        rownums (filter is-data? (range (ui getrowcount :facts-view)))
        facts (into {} (map (fn [rowid] 
                              [(getcell rowid 0) (getcell rowid 1)])
                               rownums))]
    (ui click :close-facts)
    facts))

(defn write-facts
  "Writes overriding facts file into /etc/rhsm/facts/
    @facts      : facts to be written. Either a json formatted string or
                   a clojure datastructure to be converted to a json string
    @filename   : file name of the facts override file
    @path       : path of the filename
    @overwrite? : boolean to overwrite the file
    @update?    : boolean to update facts with candlepin"  
  [facts & {:keys [filename path overwrite? update?]
            :or {filename "override.facts"
                 path "/etc/rhsm/facts/"
                 overwrite? true
                 update? true}}]
  (let [redirect (if overwrite? ">" ">>")
        contents (if (string? facts) facts (json/json-str facts))
        command (str "echo '" contents "' " redirect " " path filename)]
    (.RunCommandAndWait @clientcmd command)
    (if update?
      (.runCommandAndWait @clientcmd "subscription-manager facts --update"))))


(comment
  (defn build-installed-product-map
    []
    (let [pemfiles (split-lines (.getStdout (.runcommandAndWait @clientcmd "ls /etc/pki/product/")))
          productmap (atom {})
          dir "/etc/pki/product/"]
      (doseq [pem pemfile]))))


