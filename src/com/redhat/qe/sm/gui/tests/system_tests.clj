(ns com.redhat.qe.sm.gui.tests.system-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.contrib.string :only (split
                                       split-lines
                                       trim
                                       replace-str
                                       substring?)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass
                                   BeforeGroups
                                   Test
                                   DataProvider
                                   AfterClass]))

(def ldtpd-log "/var/log/ldtpd/ldtpd.log")

(defn ^{BeforeClass {:groups ["setup"]}}
  clear_env [_]
  (tasks/kill-app))

(defn ^{AfterClass {:groups ["setup"]}}
  restart_env [_]
  (tasks/restart-app :unregister? true))

(defn ^{Test {:groups ["system"
                       "blockedByBug-656896"]}}
  check_libglade_warnings [_]
  (let [output (tasks/get-logging @clientcmd
                                  ldtpd-log
                                  "check_libglade_warnings"
                                  "libglade-WARNING"
                                  (tasks/start-app))]
    (verify (not (substring? "libglade-WARNING" output))))
  (tasks/kill-app))

(defn ^{Test {:groups ["system"
                       "blockedByBug-706384"]}}
  run_second_instance [_]
  (tasks/restart-app)
  (let [output (tasks/get-logging @clientcmd
                            ldtpd-log
                            "run_second_instance"
                            nil
                            (tasks/start-app)
                            (tasks/sleep 10000))]
    (verify (substring? "subscription-manager-gui is already running" output))
    (verify (not (substring? "Traceback" output)))))

;; TODO
;; https://bugzilla.redhat.com/show_bug.cgi?id=747014 < help button

(gen-class-testng)

