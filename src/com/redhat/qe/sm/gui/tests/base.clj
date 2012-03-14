(ns com.redhat.qe.sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[com.redhat.qe.sm.gui.tasks.tasks])
  (:require [com.redhat.qe.sm.gui.tasks.test-config :as config]
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations BeforeSuite AfterSuite]))
  
(defn- restart-vnc []
  (.runCommandAndWait @config/clientcmd "service vncserver restart")
  ( . Thread (sleep 10000)))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (config/init)
  (restart-vnc)
  (connect)
  (start-app))

(defn ^{AfterSuite {:groups ["setup"]}}
  killGUI [_]
  (.runCommand @config/clientcmd "killall -9 subscription-manager-gui")
  (log/info "Contents of ldtpd.log:")
  (log/info (.getStdout
             (.runCommandAndWait
              @config/clientcmd
              "cat /var/log/ldtpd/ldtpd.log"))))

(gen-class-testng)
