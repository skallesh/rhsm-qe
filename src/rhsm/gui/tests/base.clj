(ns rhsm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.tasks]
        rhsm.gui.tasks.tools)
  (:require [rhsm.gui.tasks.test-config :as config]
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations BeforeSuite
            AfterSuite]
           org.testng.SkipException))

(defn restart-vnc
  "function that restarts the vnc server"
  []
  (if (= "RHEL7" (get-release))
    (do (run-command "systemctl restart vncserver@:2.service"))
    (do
      (run-command "service vncserver stop")
      ( . Thread (sleep 5000))
      (run-command "rm -f /tmp/.X2-lock; rm -f /tmp/.X11-unix/X2")
      (run-command "service vncserver start")))
  ( . Thread (sleep 10000)))

(defn run-and-assert
  "Wrapper around run-command that throws a SkipException if the command fails"
  [command]
  (let [result (run-command command)]
    (if (not (bash-bool (:exitcode result)))
      (throw (SkipException. (str "Command '" command "' failed! Skipping suite."))))))

(defn update-ldtpd
  "This function updates ldtpd on the client"
  [url]
  (when url
    (if (= "RHEL5" (get-release))
      (let [path "/root/bin/ldtpd"]
        (run-and-assert (str "wget " url " -O " path))
        (run-and-assert (str "chmod +x " path))))))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (config/init)
  (let [arch (.arch @config/cli-tasks)]
    (if-not (some #(= arch %) '("i386" "i486" "i586" "i686" "x86_64"))
      (throw (SkipException. (str "Arch '" arch "' is not supported for GUI testing.")))))
  (update-ldtpd (:ldtpd-source-url @config/config))
  (restart-vnc)
  (connect)
  (start-app))

(defn ^{AfterSuite {:groups ["setup"]}}
  killGUI [_]
  (kill-app)
  (log/info "Contents of ldtpd.log:")
  (log/info (:stdout
             (run-command
              "cat /var/log/ldtpd/ldtpd.log"))))

(gen-class-testng)
