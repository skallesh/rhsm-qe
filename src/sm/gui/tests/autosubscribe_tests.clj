(ns sm.gui.tests.autosubscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [sm.gui.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [sm.gui.tasks :as tasks]
             sm.gui.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(defn warn-count []
  (if (= 1 (tasks/ui guiexist :main-window "You have*"))
    (let [countlabel (tasks/ui getobjectproperty :main-window "You have*" "label")]
      (Integer/parseInt (first (re-seq #"\w+" (.substring countlabel 9)))))
    0))

(defn compliance? []
  (= 1 (tasks/ui guiexist :main-window "All products are in compliance*")))
  
(defn ^{BeforeClass {}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn ^{Test {:groups ["autosubscribe"]}}
  register_autosubscribe [_]
    (let [beforesubs (warn-count)]
      (if (= 0 beforesubs)
        (verify (compliance?))
        (do 
          (tasks/register (@config :username) (@config :password) :autosubscribe true)
          (verify (<= (warn-count) beforesubs))
          ))))
   
(gen-class-testng)

