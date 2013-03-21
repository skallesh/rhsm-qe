(ns rhsm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.tasks])
  (:require [rhsm.gui.tasks.test-config :as config]
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations BeforeSuite
            AfterSuite]
           org.testng.SkipException))

(defn restart-vnc
  "function that restarts the vnc server"
  []
  (if (= :rhel7 (get-release))
    (do (.runCommandAndWait @config/clientcmd "systemctl restart vncserver@:2.service"))
    (do
      (.runCommandAndWait @config/clientcmd "service vncserver stop")
      ( . Thread (sleep 5000))
      (.runCommandAndWait @config/clientcmd "rm -f /tmp/.X2-lock; rm -f /tmp/.X11-unix/X2")
      (.runCommandAndWait @config/clientcmd "service vncserver start")))
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
  (kill-app)
  (log/info "Contents of ldtpd.log:")
  (log/info (.getStdout
             (.runCommandAndWait
              @config/clientcmd
              "cat /var/log/ldtpd/ldtpd.log"))))

(gen-class-testng)
