(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[sm.gui.test-config :only (config)]
	[test-clj.testng :only (gen-class-testng)]
	[sm.gui.ui :only (action element)]
	sm.gui.ldtp)
  (:require [clojure.contrib.error-kit :as handler]))


(defn get-error-msg []
  (action getobjectproperty :error-dialog "lbl[A-Za-z]*" "label"))

(handler/deferror *error-dialog* [] [s]
  "Indicates an error dialog has appeared in the application."
  {:msg (str "Error dialog is present with message: " s)
   :unhandled (handler/throw-msg RuntimeException)})
 
(defn clear-error-dialog []
  (action click :ok-error))

(defn waittillwindowexist [windowid seconds]
  (apply waittillguiexist (conj (element windowid) "" seconds)))

(defn checkforerror []
  (if (= 1 (waittillwindowexist :error-dialog 3)) 
    (handler/raise *error-dialog* (get-error-msg))))

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
  (comment (handler/with-handler
      (checkforerror)
      (handler/handle *error-dialog* [s] (clear-error-dialog)))))
  
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

(defn ^{:test {:configuration :beforeSuite}}
  startup [_]
  (connect)
  (start-app (config :binary-path)))

(gen-class-testng)

