(ns com.redhat.qe.sm.gui.tasks.tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd cli-tasks)]
        [error.handler :only (add-recoveries raise)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.contrib.str-utils :only (re-split)]
        gnome.ldtp)
  (:require [clojure.contrib.logging :as log]
            com.redhat.qe.sm.gui.tasks.ui)) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.


(def ui gnome.ldtp/action) ;;alias action in ldtp to ui here

(defn sleep [ms] (. Thread (sleep ms)))

(def is-boolean?
  (fn [expn]
    (or
      (= expn 'true)
      (= expn 'false))))

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {:invalid-credentials #"Invalid Credentials|Invalid username or password.*"
                   :no-username #"You must enter a login"
                   :no-password #"You must enter a password"
                   :wrong-consumer-type #"Consumers of this type are not allowed"
                   })

(defn matching-error "Returns a keyword of known error, if the message matches any of them."
  [message]
  (let [matches-message? (fn [key] (let [re (known-errors key)]
                                    (if (re-find re message) key false)))]
    (or (some matches-message? (keys known-errors))
	:sm-error)))

(defn connect
  ([url] (set-url url))
  ([] (connect (@config :ldtp-url))))

(defn start-app
  ([]
     (start-app (@config :binary-path)))
  ([path]
     (ui launchapp path [] 10)
     (ui waittillwindowexist :main-window 30)))
     
(defn start-firstboot []
  (let [path (@config :firstboot-binary-path)]
    (ui launchapp path [] 10)
    (ui waittillwindowexist :firstboot-window 30)))

(defn get-error-msg "Retrieves the error string from the RHSM error dialog."
  []
  (.trim (ui getobjectproperty :error-msg "label")))
 
(defn clear-error-dialog []
  (ui click :ok-error))

(defn checkforerror []
  (add-recoveries
   {:log-warning (fn [e] (log/warn
                         (format "Got error %s, message was: '%s'"
                                 (name (:type e)) (:msg e))))}
   (if (= 1 (ui waittillwindowexist :error-dialog 3)) 
     (let [message (get-error-msg)
           type (matching-error message)]
       (clear-error-dialog)
       (raise {:type type 
               :msg message})))))
               
(defn set-conf-file-value [field value]
  (.updateConfFileParameter @cli-tasks (.rhsmConfFile @cli-tasks) field value))
  
(defn conf-file-value [k]
  (.getConfFileParameter @cli-tasks (.rhsmConfFile @cli-tasks) k))

(defn unregister []
  (if (ui showing? :register-system)
    (raise {:type :not-registered
            :msg "Tried to unregister when already unregistered."}))
  (ui click :unregister-system)
  (ui waittillwindowexist :question-dialog 30)
  (ui click :yes)
  (checkforerror))

(defn register [username password & {:keys [system-name-input, autosubscribe]
				     :or {system-name-input nil, autosubscribe false}}]
  (if (ui showing? :unregister-system)
    (raise {:type :already-registered
            :username username
            :password password
            :unregister-first (fn [e] (unregister)
                                (register (:username e) (:password e)))}))
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
    (checkforerror)))

(defn firstboot-register [username password & {:keys [system-name-input, autosubscribe]
                          :or {system-name-input nil, autosubscribe false}}]
  (assert  (= 1 (ui guiexist :firstboot-window "Entitlement Platform Registration")))
  (ui settextvalue :firstboot-user username)
  (ui settextvalue :firstboot-pass password)
  (when system-name-input
    (ui settextvalue :firstboot-system-name system-name-input))
  (if autosubscribe
    (ui check :firstboot-autosubscribe)
    (ui uncheck :firstboot-autosubscribe))
    (ui click :firstboot-forward)
    (checkforerror))
  

(defn wait-for-progress-bar []
  (ui waittillwindowexist :progress-dialog 1)
  (ui waittillwindownotexist :progress-dialog 30))

(defn search ([match-system?, do-not-overlap?, match-installed?, contain-text, active-on] 
  (ui selecttab :all-available-subscriptions)
  (ui check :more-search-options)
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

(defn subscribe [s]
  (ui selecttab :all-available-subscriptions)
  (if-not (ui rowexist? :all-subscriptions-view s)
    (raise {:type :subscription-not-available
            :name s
            :msg (str "Not found in 'All Available Subscriptions':" s)}))
  (ui selectrow :all-subscriptions-view s)
  (ui click :subscribe)
  (checkforerror)
  (ui waittillwindowexist :contract-selection-dialog 5)
  (if (= 1 (ui guiexist :contract-selection-dialog))
    (do (ui selectrowindex :contract-selection-table 0)  ;;pick first contract for now
        (ui click :subscribe-contract-selection)))
  (checkforerror)
  (wait-for-progress-bar))

(defn unsubscribe [s]
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

(defn warn-count []
  (if (= 1 (ui guiexist :main-window "You have*"))
    (let [countlabel (ui getobjectproperty :main-window "You have*" "label")]
      (Integer/parseInt (first (re-seq #"\w+" (.substring countlabel 9)))))
    0))

(defn compliance? []
  (= 1 (ui guiexist :main-window "Product entitlement certificates valid through*")))  

(defn first-date-of-noncomply []
  (if (= 1 (ui guiexist :subscription-assistant-dialog))
    (let [datelabel (ui getobjectproperty :subscription-assistant-dialog "*first date*" "label")]
      (.substring datelabel 0 10))))
  
(defn assistant-subscribe [s]
  (if-not (ui rowexist? :assistant-subscription-view s)
    (raise {:type :subscription-not-available
            :name s
            :msg (str "Not found in 'Subscription Assistant Subscriptions':" s)}))
  (ui selectrow :assistant-subscription-view s)
  (ui click :assistant-subscribe)
  (checkforerror)
  (wait-for-progress-bar))  

(defn get-table-elements [view col]
  (for [row (range (action getrowcount view))]
    (ui getcellvalue view row col))) 
  
(defn do-to-all-rows-in [view col f]
  (let [item-list (get-table-elements view col)]
    (doseq [item item-list]
      (f item))))

(defn verify-conf-proxies [hostname port user password]
  (let [config-file-hostname  (conf-file-value "proxy_hostname")
        config-file-port      (conf-file-value "proxy_port")
        config-file-user      (conf-file-value "proxy_user")
        config-file-password  (conf-file-value "proxy_password")]
    (verify (= config-file-hostname hostname))
    (verify (= config-file-port port)) 
    (verify (= config-file-user user))
    (verify (= config-file-password password))))

(comment 
(defn get-all-facts []
   (ui click :view-system-facts)
   (ui waittillguiexist :facts-view)
   (let [table (element :facts-view)
	 rownums (range (ui getrowcount :facts-view))
	 getcell (fn [row col] 
		   (ui getcellvalue table row col))
	 facts (into {} (mapcat (fn [rowid] 
				  [(getcell rowid 0) (getcell rowid 1)])
				rownums))]
     (ui click :close-facts)
     facts))
)




