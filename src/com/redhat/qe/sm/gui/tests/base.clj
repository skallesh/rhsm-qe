(ns com.redhat.qe.sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[com.redhat.qe.sm.gui.tasks.tasks])
  (:require [com.redhat.qe.sm.gui.tasks.test-config :as config]
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations BeforeSuite AfterSuite]
           org.testng.SkipException))
  
(defn- restart-vnc []
  (.runCommandAndWait @config/clientcmd "service vncserver restart")
  ( . Thread (sleep 10000)))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (config/init)
  (let [arch (.arch @config/cli-tasks)]
    (if-not (some #(= arch %) '("i386" "i486" "i586" "i686" "x86_64"))
      (throw (SkipException. (str "Arch '" arch "' is not supported for GUI testing.")))))
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
