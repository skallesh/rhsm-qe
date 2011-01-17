(ns sm.gui.tasks
  (:use [sm.gui.test-config :only (config)]
        [com.redhat.qe.handler :only (add-recoveries raise *error*)]
        [com.redhat.qe.verify :only (verify)]
        sm.gui.ldtp)
  (:require [clojure.contrib.logging :as log]
            sm.gui.ui)) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.


(def ui sm.gui.ldtp/action) ;;alias action in ldtp to ui here

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {:invalid-credentials #"Invalid username"
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

(defn connect []
  (set-url (@config :ldtp-url)))

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
   {:log-warning #(log/warn (format "Got error %s, message was: '%s'" (name (:type *error*)) (:msg *error*)))}
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
            :unregister-first (fn [] (unregister)
                                (register (:username *error*) (:password *error*)))}))
  (ui click :register-system)
  (ui waittillguiexist :redhat-login)
  (ui settextvalue :redhat-login username)
  (ui settextvalue :password password)
  (when system-name-input
    (ui settextvalue :system-name system-name-input))
  (add-recoveries {:cancel #(ui click :register-cancel)}
    (ui click :register)
    (checkforerror)))

(defn wait-for-progress-bar []
  (ui waittillwindowexist :progress-dialog 1)
  (ui waittillwindownotexist :progress-dialog 30))

(defn search [& {:keys [match-hardware?, overlap?, not-installed?, contain-text, active-on]
		 :or {match-hardware? false
		      overlap? false
		      not-installed? true
		      contain-text nil
		      active-on nil}}]
  (ui selecttab :all-available-subscriptions)
  (let [setchecked (fn [needs-check?] (if needs-check? check uncheck))]
    (ui (setchecked match-hardware?) :match-hardware-checkbox)
    (ui (setchecked overlap?) :overlap-checkbox)
    (ui (setchecked not-installed?) :not-installed-checkbox)
    (if contain-text (do (ui check :contain-text-checkbox)
			     (ui settextvalue :as-yet-unnamed-textbox))))
  (if active-on (comment "Procedure to set date goes here "))
  (ui click :search-button)
  (wait-for-progress-bar))

(defn subscribe [s]
  (ui selecttab :all-available-subscriptions)
  (ui selectrow :all-subscriptions-view s)
  (ui click :subscribe)
  (checkforerror)
  (ui selectrowindex :contract-selection-table 0)  ;;pick first contract for now
  (ui click :subscribe-contract-selection)
  (checkforerror)
  (wait-for-progress-bar))

(defn unsubscribe [s]
  (ui selecttab :my-subscriptions)
  (ui selectrow :my-subscriptions-view s)
  (ui click :unsubscribe)
  (checkforerror))

(comment (defn get-all-facts []
   (ui click :view-my-system-facts)
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




