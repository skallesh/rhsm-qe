(ns sm.gui.tasks
  (:use [sm.gui.test-config :only (config)]
        [com.redhat.qe.handler :only (add-recoveries raise handle-type)]
        [com.redhat.qe.verify :only (verify)]
        sm.gui.ldtp)
  (:require sm.gui.ui)) ;;need to load ui even if we don't refer to it because of the extend-protocol in there.


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
     (action launchapp path [] 10)
     (action waittillwindowexist :main-window 30)))

(defn get-error-msg "Retrieves the error string from the RHSM error dialog."
  []
  (.trim (action getobjectproperty :error-msg "label")))
 
(defn clear-error-dialog []
  (action click :ok-error))

(defn checkforerror []
  (if (= 1 (action waittillwindowexist :error-dialog 3)) 
    (let [message (get-error-msg)
          type (matching-error message)]
      (clear-error-dialog)
      (raise {:type type 
              :msg message}))))

(defn register [username password & {:keys [system-name-input, autosubscribe]
				     :or {system-name-input nil, autosubscribe false}}]
  (action click :register-system)
  (action waittillguiexist :redhat-login)
  (action settextvalue :redhat-login username)
  (action settextvalue :password password)
  (when system-name-input
    (action settextvalue :system-name system-name-input))
  (add-recoveries {:cancel #(action click :register-cancel)}
    (action click :register)
    (checkforerror)))

(defn wait-for-progress-bar []
  (action waittillwindowexist :progress-dialog 1)
  (action waittillwindownotexist :progress-dialog 30))

(defn search [& {:keys [match-my-hardware?, overlap-with-existing-subscriptions?, provide-software-not-yet-installed?, contain-the-text, active-on]
		 :or {match-my-hardware? false
		      overlap-with-existing-subscriptions? false
		      provide-software-not-yet-installed? true
		      contain-the-text nil
		      active-on nil}}]
  (action selecttab :all-available-subscriptions)
  (let [setchecked (fn [needs-check?] (if needs-check? check uncheck))]
    (action (setchecked match-my-hardware?) :match-my-hardware)
    (action (setchecked overlap-with-existing-subscriptions?) :overlap-with-existing-subscriptions)
    (action (setchecked provide-software-not-yet-installed?) :provide-software-not-yet-installed)
    (if contain-the-text (do (action check :contain-the-text)
			     (action settextvalue :as-yet-unnamed-textbox))))
  (if active-on (comment "Procedure to set date goes here "))
  (action click :search)
  (wait-for-progress-bar))

(defn subscribe [s]
  (search)
  (action selectrow :available-subscription-table s)
  (action click :subscribe)
  (checkforerror)
  (action selectrowindex :contract-selection-table 0)  ;;pick first contract for now
  (action click :subscribe-contract-selection)
  (checkforerror)
  (wait-for-progress-bar))


(comment (defn get-all-facts []
   (action click :view-my-system-facts)
   (action waittillguiexist :facts-view)
   (let [table (element :facts-view)
	 rownums (range (action getrowcount :facts-view))
	 getcell (fn [row col] 
		   (action getcellvalue table row col))
	 facts (into {} (mapcat (fn [rowid] 
				  [(getcell rowid 0) (getcell rowid 1)])
				rownums))]
     (action click :close-facts)
     facts)))

(defn unregister []
  (action click :unregister-system)
  (action waittillwindowexist :question-dialog 5)
  (action click :yes)
  (checkforerror))


