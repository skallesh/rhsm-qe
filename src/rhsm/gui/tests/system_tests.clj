(ns rhsm.gui.tests.system-tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only [throw+
                                    try+]]
        rhsm.gui.tasks.tools
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
(def rhsm-log "/var/log/rhsm/rhsm.log")
(def tmpCAcertpath "/tmp/CA-certs/")
(def CAcertpath "/etc/rhsm/ca/")
(def unregStatus "Keep your system up to date by regestering.")

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
  (let [output (get-logging @clientcmd
                                  ldtpd-log
                                  "check_libglade_warnings"
                                  "libglade-WARNING"
                                  (tasks/start-app))]
    (verify (not (substring? "libglade-WARNING" output))))
  (tasks/kill-app))

(defn ^{Test {:groups ["system"
                       "blockedByBug-909823"]}}
  check_gtype_warnings
  "Asserts that the gtype WARNINGs are corrected."
  [_]
  (let [output (get-logging @clientcmd
                                  ldtpd-log
                                  "check_gtype_warnings"
                                  "gtype"
                                  (tasks/start-app))]
    (verify (not (substring? "gtype" output))))
  (tasks/kill-app))

(defn ^{Test {:groups ["system"
                       "blockedByBug-706384"]}}
  run_second_instance
  "Asserts that a second instance of rhsm-gui cannot be run."
  [_]
  (tasks/restart-app)
  (let [output (get-logging @clientcmd
                            ldtpd-log
                            "run_second_instance"
                            nil
                            (tasks/start-app)
                            (sleep 10000))]
    (verify (substring? "subscription-manager-gui is already running" output))
    (verify (not (substring? "Traceback" output)))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-747014"]}}
  check_help_button
  "Assertst that the help window opens."
  [_]
  (try
    (tasks/restart-app)
    (tasks/ui click :getting-started)
    (sleep 3000)
    (verify (bool (tasks/ui guiexist :help-dialog)))
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
                        (sleep 3000)
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
    (sleep 3000)
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
    (verify (not (bool (tasks/ui guiexist :main-window))))
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-833578"]}}
  check_online_documentation
  "Asserts that the online documentation opens."
  [_]
  (try
    (tasks/restart-app)
    (let [output (get-logging @clientcmd
                            ldtpd-log
                            "check_online_documentation"
                            nil
                            (tasks/ui click :online-documentation)
                            (tasks/ui waittillwindowexist :firefox-help-window 10))]
       (verify (bool (tasks/ui guiexist :firefox-help-window)))
       (verify (not (substring? "Traceback" output))))
    (finally    (tasks/ui closewindow :firefox-help-window)
                (tasks/restart-app))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-707041"]}}
  date_picker_traceback
  "Asserts that the date chooser does not throw a traceback."
  [_]
  (try
    (if-not (bool (tasks/ui guiexist :main-window)) (tasks/restart-app))
    (try+ (tasks/register-with-creds :re-register? false)
          (catch [:type :already-registered] _))
    (tasks/ui selecttab :all-available-subscriptions)
    (let [output (get-logging @clientcmd
                                    "/var/log/ldtpd/ldtpd.log"
                                    "date_picker_traceback"
                                    "Traceback"
                                    (tasks/ui click :calendar)
                                    (verify
                                     (bool (tasks/ui waittillwindowexist :date-selection-dialog 10)))
                                    (tasks/ui click :today))]
      (verify (clojure.string/blank? output)))
    (finally (if (bool (tasks/ui guiexist :date-selection-dialog))
               (tasks/ui closewindow :date-selection-dialog)))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-947485"]}}
  open_with_bad_hostname
  "Verifies that the gui can open with a bad hostname in /etc/rhsm/rhsm.conf."
  [_]
  (let [hostname (tasks/conf-file-value "hostname")]
    (try+
     (run-command "subscription-manager clean")
     (tasks/restart-app)
     (tasks/register-with-creds)
     (tasks/kill-app)
     (tasks/set-conf-file-value "hostname" "blahblahdoesnotexist.redhat.com")
     (tasks/start-app)
     (tasks/ui waittillwindowexist :main-window 20)
     (verify (bool (tasks/ui guiexist :main-window)))
     (finally
       (tasks/set-conf-file-value "hostname" hostname)))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-920091"]}}
  cli_unregister_check_traceback
  "Verifies whether it causes traceback when GUI is running and sub-man is unregistered through CLI"
  [_]
  (tasks/restart-app)
  (tasks/register-with-creds)
  (let [output (get-logging @clientcmd
                                  rhsm-log
                                  "check_traceback"
                                  nil
                                  (run-command "subscription-manager unregister"))]
    (verify (not (substring? "Traceback" output))))
  (tasks/kill-app))

(defn ^{Test {:groups ["system"
                         "blockedByBug-960465"]}}
    launch_gui_with_invalid_cert
    "Test to verify GUI can be launched with invalid certs"
    [_]
    (tasks/kill-app)
    (run-command "subscription-manager unregister")
    (try+
     (run-command (str "rmdir " tmpCAcertpath))
     (run-command (str "mkdir " tmpCAcertpath))
     (run-command (str "mv " CAcertpath "*.* " tmpCAcertpath))
     (verify (= 1 (tasks/start-app)))
     (finally
      (run-command (str "mv " tmpCAcertpath "*.* " CAcertpath))
      )))

(gen-class-testng)
