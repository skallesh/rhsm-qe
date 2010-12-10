(ns sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.tasks])
  (:require [sm.gui.test-config :as config])
  (:import [org.testng.annotations BeforeSuite AfterSuite]))

(defn ^{BeforeSuite {}}
  startup [_]
  (config/init)
  (connect)
  (start-app))

(defn ^{AfterSuite {}}
  killGUI [_]
  (.runCommand @config/clientcmd "killall -9 subscription-manager-gui"))

(gen-class-testng)
