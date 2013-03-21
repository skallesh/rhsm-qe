(ns rhsm.gui.tests.system-tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only [throw+
                                    try+]]
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [clojure.tools.logging :as log]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            BeforeGroups
            Test
            DataProvider
            AfterClass]))

(def ldtpd-log "/var/log/ldtpd/ldtpd.log")

(defn ^{BeforeClass {:groups ["setup"]}}
  clear_env [_]
  (tasks/kill-app))

(defn ^{AfterClass {:groups ["setup"]
                    :alwaysRun true}}
  restart_env [_]
  (tasks/restart-app :unregister? true))

(defn ^{Test {:groups ["system"
                       "blockedByBug-656896"]}}
  check_libglade_warnings
  "Asserts that the libglade-WARNINGs are corrected."
  [_]
  (let [output (tasks/get-logging @clientcmd
                                  ldtpd-log
                                  "check_libglade_warnings"
                                  "libglade-WARNING"
                                  (tasks/start-app))]
    (verify (not (tasks/substring? "libglade-WARNING" output))))
  (tasks/kill-app))

(defn ^{Test {:groups ["system"
                       "blockedByBug-706384"]}}
  run_second_instance
  "Asserts that a second instance of rhsm-gui cannot be run."
  [_]
  (tasks/restart-app)
  (let [output (tasks/get-logging @clientcmd
                            ldtpd-log
                            "run_second_instance"
                            nil
                            (tasks/start-app)
                            (tasks/sleep 10000))]
    (verify (tasks/substring? "subscription-manager-gui is already running" output))
    (verify (not (tasks/substring? "Traceback" output)))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-747014"]}}
  check_help_button
  "Assertst that the help window opens."
  [_]
  (try
    (tasks/restart-app)
    (tasks/ui click :getting-started)
    (tasks/sleep 3000)
    (verify (= 1 (tasks/ui guiexist :help-dialog)))
    (tasks/ui closewindow :help-dialog)
    (finally (tasks/restart-app))))

(defn check_escape_window
  "Asserts that windows correctly render after exiting them with a shortcut."
  [window shortcut]
  (tasks/restart-app :unregister? true)
  (let [exec-shortcut (fn [s] (tasks/ui generatekeyevent s))
        count-objects (fn [w] (count (tasks/ui getobjectlist w)))
        beforecount (do (exec-shortcut shortcut)
                        ;sleeps are necessary because window doesn't instantly render
                        (tasks/ui waittillwindowexist window 10)
                        (tasks/sleep 3000)
                        (count-objects window))
        ;this has to be here due to weird issues in RHEL5
        ; where the objectlist was getting cached
        ; creating a traceback dumps the cache and this works for a quick fix
        fuckcache (fn [] (try+ (tasks/ui getchild "blah")
                              (catch Exception e "")))]
    (fuckcache)
    (log/info (str "Items: " beforecount))
    (fuckcache)
    (exec-shortcut "<ESC>")
    (exec-shortcut shortcut)
    (tasks/ui waittillwindowexist window 10)
    (tasks/sleep 3000)
    (let [newcount (count-objects window)]
      (verify (= beforecount newcount)))))

(data-driven
 check_escape_window {Test {:groups ["system"
                                     "blockedByBug-862099"]}}
 [[:register-dialog "<CTRL>r"]
  [:import-dialog "<CTRL>i"]])


(defn ^{Test {:groups ["system"
                       "blockedByBug-785203"]}}
  check_close_button
  "Checks that the close menu item works."
  [_]
  (tasks/restart-app)
  (try
    (tasks/ui selectmenuitem :main-window "System")
    (tasks/ui selectmenuitem :main-window "Quit")
    (tasks/ui waittillwindownotexist :main-window 5)
    (verify (= 0 (tasks/ui guiexist :main-window)))
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-833578"]}}
  check_online_documentation
  "Asserts that the online documentation opens."
  [_]
  (try
    (tasks/restart-app)
    (let [output (tasks/get-logging @clientcmd
                            ldtpd-log
                            "check_online_documentation"
                            nil
                            (tasks/ui click :online-documentation)
                            (tasks/ui waittillguiexist :firefox-help-window 10))]
       (verify (= 1 (tasks/ui guiexist :firefox-help-window)))
       (verify (not (tasks/substring? "Traceback" output))))
    (finally    (tasks/ui closewindow :firefox-help-window)
                (tasks/restart-app))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-707041"]}}
  date_picker_traceback
  "Asserts that the date chooser does not throw a traceback."
  [_]
  (try
    (if-not (= 1 (tasks/ui guiexist :main-window)) (tasks/restart-app))
    (try+ (tasks/register-with-creds :re-register? false)
          (catch [:type :already-registered] _))
    (tasks/ui selecttab :all-available-subscriptions)
    (let [output (tasks/get-logging @clientcmd
                                    "/var/log/ldtpd/ldtpd.log"
                                    "date_picker_traceback"
                                    "Traceback"
                                    (tasks/ui click :calendar)
                                    (verify
                                     (= 1 (tasks/ui waittillwindowexist :date-selection-dialog 10)))
                                    (tasks/ui click :today))]
      (verify (clojure.string/blank? output)))
    (finally (if (= 1 (tasks/ui guiexist :date-selection-dialog))
               (tasks/ui closewindow :date-selection-dialog)))))

(gen-class-testng)
