(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[clojure.contrib.error-kit :only (with-handler handle do-not-handle)]
	[sm.gui.test-config :only (config)]
	[sm.gui.ui :only (action element)]
	sm.gui.ldtp)
  (:require [clojure.contrib.error-kit :as errkit]
	    [sm.gui.errors :as errors]))


(defn get-error-msg "Retrieves the error string from the RHSM error dialog."
  []
  (action getobjectproperty :error-dialog "lbl[A-Za-z]*" "label"))

 
(defn clear-error-dialog []
  (action click :ok-error))

(defn waittillwindowexist [windowid seconds]
  (apply waittillguiexist (conj (element windowid) "" seconds)))

(defn checkforerror []
  (if (= 1 (waittillwindowexist :error-dialog 3)) 
    (let [message (get-error-msg)]
      (clear-error-dialog)
      (errors/raise-matching-error message))))

(defn start-app
  ([]
     (start-app (config :binary-path)))
  ([path]
     (action launchapp path [] 10)))

(defnk register [username password :system-name-input nil :autosubscribe false ]
  (action click :register-system)
  (action waittillguiexist :redhat-login)
  (action settextvalue :redhat-login username)
  (action settextvalue :password password)
  (when system-name-input
    (action settextvalue :system-name system-name-input))

					; (setchecked (element :automatically-subscribe) autosubscribe) 
  (action click :register)
 
  (with-handler (checkforerror)
    (handle errors/*sm-error* [s]
		    (action click :register-cancel)
		    (do-not-handle))))
  
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


