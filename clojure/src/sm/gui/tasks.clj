(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[sm.gui.test-config :only (config)])
  (:require [sm.gui.ldtp :as ldtp] 
            [sm.gui.ui :as ui]
            [clojure.contrib.error-kit :as handler]))

(def element (ui/element-getter ui/windows))

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
     (ldtp/launchapp path [] 5 1))
  ([]
     (start-app (config :binary-path))))

(defnk register [username password :system-name-input nil :autosubscribe false ]
  (ldtp/click (element :registration))
  (ldtp/waittillguiexist (element :redhat-login))
  (ldtp/settextvalue (element :redhat-login) username)
  (ldtp/settextvalue (element :password) password)
  (when system-name-input
    (ldtp/settextvalue (element :system-name) system-name-input))
  ; (ldtp/setchecked (element :automatically-subscribe) autosubscribe) 
  (ldtp/click (element :register))
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