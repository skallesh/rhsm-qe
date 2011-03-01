(ns sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.tasks.tasks])
  (:require [sm.gui.tasks.test-config :as config])
  (:import [org.testng.annotations BeforeSuite AfterSuite]))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (config/init)
  (connect)
  (start-app))

(defn ^{AfterSuite {:groups ["setup"]}}
  killGUI [_]
  (.runCommand @config/clientcmd "killall -9 subscription-manager-gui"))

(gen-class-testng)
