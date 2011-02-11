(ns sm.gui.tasks
  (:use [sm.gui.test-config :only (config)]
        [error.handler :only (add-recoveries raise)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp)
  (:require [clojure.contrib.logging :as log]
            sm.gui.ui)) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.


(def ui gnome.ldtp/action) ;;alias action in ldtp to ui here

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

(defn unregister []
  (if (ui showing? :register-system)
    (raise {:type :not-registered
            :msg "Tried to unregister when already unregistered."}))
  (ui click :unregister-system)
  (ui waittillwindowexist :question-dialog 5)
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
  (if-not (ui rowexist? :my-subscriptions-view s)
    (raise {:type :not-subscribed
            :name s
            :msg (str "Not found in 'My Subscriptions': " s)}))
  (ui selectrow :my-subscriptions-view s)
  (ui click :unsubscribe)
  (ui waittillwindowexist :question-dialog 60)
  (ui click :yes)
  (checkforerror ) )

(defn enableproxy-auth [proxy port user pass]
  (ui selecttab :my-installed-software)
  (ui click :proxy-configuration)
  (ui waittillwindowexist :proxy-config-dialog 60)
  (ui check :proxy-checkbox)
  (ui settextvalue :proxy-location (str proxy ":" port))
  (ui check :authentication-checkbox)
  (ui settextvalue :username-text user)
  (ui settextvalue :password-text pass)
  (ui click :close-proxy)
  (checkforerror) )

(defn enableproxy-noauth [proxy port]
  (ui selecttab :my-installed-software)
  (ui click :proxy-configuration)
  (ui waittillwindowexist :proxy-config-dialog 60)
  (ui check :proxy-checkbox)
  (ui settextvalue :proxy-location (str proxy ":" port))
  (ui uncheck :authentication-checkbox)
  (ui click :close-proxy)
  (checkforerror) )
  
(defn disableproxy []
  (ui selecttab :my-installed-software)
  (ui click :proxy-configuration)
  (ui waittillwindowexist :proxy-config-dialog 60)
  (ui uncheck :proxy-checkbox)
  (ui uncheck :authentication-checkbox)
  (ui click :close-proxy)
  (checkforerror) )

(comment (defn get-all-facts []
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
     facts)))




