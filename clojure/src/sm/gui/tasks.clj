(ns sm.gui.tasks
  (:use [sm.gui.test-config :only (config)]
	[sm.gui.ui :only (action element)]
	sm.gui.ldtp)
  (:require [clojure.contrib.error-kit :as errkit]
	    [sm.gui.errors :as errors]))


(defn get-error-msg "Retrieves the error string from the RHSM error dialog."
  []
  (action getobjectproperty :error-dialog "lbl[A-Za-z]*" "label"))

 
(defn clear-error-dialog []
  (action click :ok-error))

(defn waittillwindow [windowid seconds exist?]
  (apply (if exist? waittillguiexist waittillguinotexist) (conj (element windowid) "" seconds)))

(defn waittillwindowexist [windowid seconds]
  (waittillwindow windowid seconds true))

(defn waittillwindownotexist [windowid seconds]
  (waittillwindow windowid seconds false))

(defn checkforerror []
  (if (= 1 (waittillwindowexist :error-dialog 3)) 
    (let [message (get-error-msg)]
      (clear-error-dialog)
      (throw (RuntimeException. message)))))

(defn start-app
  ([]
     (start-app (config :binary-path)))
  ([path]
     (action launchapp path [] 10)
     (waittillwindowexist :mainWindow 30)))

(declare err-handler)
(defn handle-error
  "Checks for an error dialog.  The caller can optionally pass in a
map of keywords to functions.  The keys are names of ways to recover
from an error, the functions actually perform the recovery.  If some
caller way down the stack wants to handle the error, he can bind
'err-handler' to a function that takes a keyword of a known error
type, and returns a map with keys :handled (true or false, if he
handled the error), and :recovery (keyword name of a recovery to
choose.  He can leave this off and just perform his own recovery as
well)."
  ([] (handle-error {}))
  ([recoveries]
     (try (checkforerror)
	  (catch RuntimeException e
	    (if (bound? (var err-handler))
	      (errors/handle e err-handler recoveries)
	      (throw e))))))

(defn select-tab [tab]
  (let [locator (element tab)
	args [selecttab (first locator) "ptl0" (second locator)]]
    (apply action args)))

(defn register [username password & {:keys [system-name-input, autosubscribe]
				     :or {system-name-input "", autosubscribe false}}]
  (action click :register-system)
  (action waittillguiexist :redhat-login)
  (action settextvalue :redhat-login username)
  (action settextvalue :password password)
  (when system-name-input
    (action settextvalue :system-name system-name-input))
  (action click :register)
  (handle-error {:cancel #(action click :register-cancel)}))

(defn search [& {:keys [match-my-hardware, overlap-with-existing-subscriptions, provide-software-not-yet-installed, contain-the-text, active-on]
		 :or {match-my-hardware false
		      overlap-with-existing-subscriptions false
		      provide-software-not-yet-installed true
		      contain-the-text nil
		      active-on nil}}]
  (select-tab :all-available-subscriptions)
  (let [checkuncheck (fn [check?] (if check? check uncheck))]
    (action (checkuncheck match-my-hardware) :match-my-hardware)
    (action (checkuncheck overlap-with-existing-subscriptions) :overlap-with-existing-subscriptions )
    (action (checkuncheck provide-software-not-yet-installed) :provide-software-not-yet-installed)
    (if contain-the-text (do (action check :contain-the-text)
			     (action settextvalue :as-yet-unnamed-textbox))))
  (if active-on (comment "Procedure to set date goes here "))
  (action click :search)
  (waittillwindowexist :progress-dialog 1)
  (waittillwindownotexist :progress-dialog 30))

(defn get-all-facts []
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
    facts))

(defn unregister []
  (action click :unregister-system)
  (action waittillguiexist :question-dialog)
  (action click :yes))

(defn connect []
  (set-url (config :ldtp-url)))


