(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[sm.gui.test-config :only (config)]
	[test-clj.testng :only (gen-class-testng)]
	[sm.gui.ui :only (action)])
  (:require [sm.gui.ldtp :as ldtp] 
            [sm.gui.ui :as ui]
            [clojure.contrib.error-kit :as handler]))



;;tasks
 (handler/deferror *error-dialog* [] [s]
   "Indicates an error dialog has appeared in the application."
    {:msg (str "Error dialog is present with message: " s)
     :unhandled (handler/throw-msg RuntimeException)})
 
(defn clear-error-dialog []
  (ldtp/click (element :ok-error)))

(defn checkforerror []
  (if (= 1 (ldtp/waittillguiexist (element :error-dialog) 3)) 
    (handler/raise *error-dialog* "")))

(defn start-app
  ([path]
     (ldtp/launchapp path)))

(defnk register [username password :system-name-input nil :autosubscribe false ]
  (action ldtp/click :register-system)
  (action ldtp/waittillguiexist :redhat-login)
  (action ldtp/settextvalue :redhat-login username)
  (action ldtp/settextvalue :password password)
  (when system-name-input
    (action ldtp/settextvalue :system-name system-name-input))
  ; (ldtp/setchecked (element :automatically-subscribe) autosubscribe) 
  (action ldtp/click :register)
  (handler/with-handler
    (checkforerror)
    (handler/handle *error-dialog* [s] (clear-error-dialog))))
  
(defn get-all-facts []
  (ldtp/click (element :system-facts))
  (ldtp/waittillguiexist (element :facts-dialog))
  (let [table (element :facts-table)
        rownums (range (ldtp/getrowcount table))
        getcell (fn [row col] 
                  (ldtp/getcellvalue table row col))
        facts (into {} (mapcat (fn [rowid] 
                                 [(getcell rowid 0) (getcell rowid 1)])
                               rownums))]
    (ldtp/click (element :close-facts))
    facts))

(defn ^{:test {:configuration :beforeSuite}}
  startup [_]
  (ldtp/set-url (config :ldtp-url))
  (start-app (config :binary-path)))

(defn ^{:test {} } faketest [_]
  (println "w00t"))

(gen-class-testng)

(comment (action click :register-button )


	 )