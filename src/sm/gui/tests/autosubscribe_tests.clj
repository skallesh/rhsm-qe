(ns sm.gui.tests.autosubscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [sm.gui.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [sm.gui.tasks :as tasks]
             sm.gui.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

  
(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn ^{Test {:groups ["autosubscribe"]}}
  register_autosubscribe [_]
    (let [beforesubs (tasks/warn-count)]
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register (@config :username) (@config :password) :autosubscribe true)
          (verify (<= (tasks/warn-count) beforesubs))
          ))))
   
(gen-class-testng)

