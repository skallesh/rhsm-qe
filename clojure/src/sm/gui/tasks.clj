(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[sm.gui.test-config :only (config)]
	[test-clj.testng :only (gen-class-testng)]
	[sm.gui.ui :only (action element)]
	sm.gui.ldtp)
  (:require [clojure.contrib.error-kit :as handler]))



;;tasks
 (handler/deferror *error-dialog* [] [s]
   "Indicates an error dialog has appeared in the application."
    {:msg (str "Error dialog is present with message: " s)
     :unhandled (handler/throw-msg RuntimeException)})
 
(defn clear-error-dialog []
  (action click :ok-error))

(defn checkforerror []
  (if (= 1 (action waittillguiexist :error-dialog 3)) 
    (handler/raise *error-dialog* "")))

(defn start-app
  ([path]
     (action launchapp path)))

(defnk register [username password :system-name-input nil :autosubscribe false ]
  (action click :register-system)
  (action waittillguiexist :redhat-login)
  (action settextvalue :redhat-login username)
  (action settextvalue :password password)
  (when system-name-input
    (action settextvalue :system-name system-name-input))
  ; (setchecked (element :automatically-subscribe) autosubscribe) 
  (action click :register)
  (handler/with-handler
    (checkforerror)
    (handler/handle *error-dialog* [s] (clear-error-dialog))))
  
(defn get-all-facts []
  (action click :system-facts)
  (action waittillguiexist :facts-dialog)
  (let [table (element :facts-table)
        rownums (range (action getrowcount table))
        getcell (fn [row col] 
                  (action getcellvalue table row col))
        facts (into {} (mapcat (fn [rowid] 
                                 [(getcell rowid 0) (getcell rowid 1)])
                               rownums))]
    (action click :close-facts)
    facts))

(defn ^{:test {:configuration :beforeSuite}}
  startup [_]
  (set-url (config :ldtp-url))
  (start-app (config :binary-path)))

(defn ^{:test {} } faketest [_]
  (println "w00t"))

(gen-class-testng)

