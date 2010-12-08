(ns sm.gui.tasks
  (:use [clojure.contrib.def :only (defnk)]
	[sm.gui.test-config :only (config)]
	[test-clj.testng :only (gen-class-testng)]
	[sm.gui.ui :only (action element)]
	sm.gui.ldtp)
  (:require [clojure.contrib.error-kit :as handler]))

;;parent error
(handler/deferror *sm-error* [] "Indicates an error dialog has appeared in the application." [s]
  {:msg (str "Error dialog is present with message: " s)
   :unhandled (handler/throw-msg RuntimeException)})

;; A mapping of RHSM error messages to regexs that will match that error.
(def known-errors {'*invalid-login* #"Invalid username"})

(defmacro define-all-errors
  "Expands to create an error type for each of the pre-defined known RHSM
error types."
  []
 `(do  ~@(for [name (keys known-errors)]
     `(handler/deferror ~name [*sm-error*] [s#]
	{:msg (str "Error " ~name s#)
	 :unhandled (handler/throw-msg RuntimeException)}))))

(define-all-errors)

(defn raise "Calls error-kit's raise with an arbitrary or calculated error type."
  [err msg]
  (handler/raise err msg))

(defn get-error-msg "Retrieves the error string from the RHSM error dialog."
  []
  (action getobjectproperty :error-dialog "lbl[A-Za-z]*" "label"))

 
(defn clear-error-dialog []
  (action click :ok-error))

(defn waittillwindowexist [windowid seconds]
  (apply waittillguiexist (conj (element windowid) "" seconds)))

(defn checkforerror []
  (if (= 1 (waittillwindowexist :error-dialog 3)) 
    (let [message (get-error-msg)
	  matches-message? (fn [key] (let [re (known-errors key)]
				       (if (re-find re message) key false)))
	  name (or (some matches-message? (keys known-errors))
		   '*sm-error*)
	  err (ns-resolve 'sm.gui.tasks name) ]
      (raise err message))))

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
  (checkforerror)
  (comment (handler/with-handler
     (checkforerror)
     (handler/handle *sm-error* [s] (clear-error-dialog)))))
  
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

