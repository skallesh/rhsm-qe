(ns sm.gui.tests.compliance-assistant-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [sm.gui.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [sm.gui.tasks :as tasks]
             sm.gui.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))
  
(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (sm.gui.tasks/register (@config :username) (@config :password))))
    
(defn ^{Test {:groups ["compliance-assistant"]}}
  register_warning [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)) 
  (if-not (tasks/compliance?)
    (do (tasks/ui click :become-compliant)
        (tasks/ui waittillwindowexist :information-dialog 60)
        (verify (= 1 (tasks/ui guiexist :information-dialog "You must register*")))
        (tasks/ui click :info-ok))))
        
        
(gen-class-testng)
