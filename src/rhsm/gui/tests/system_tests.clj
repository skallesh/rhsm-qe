(ns rhsm.gui.tests.system_tests
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
            AfterClass]
            org.testng.SkipException))

(def ldtpd-log "/var/log/ldtpd/ldtpd.log")
(def rhsm-log "/var/log/rhsm/rhsm.log")
(def tmpCAcertpath "/tmp/CA-certs/")
(def CAcertpath "/etc/rhsm/ca/")
(def unregStatus "Keep your system up to date by regestering.")
(def logtime "")
(def newtime "")

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
  (try
    (let [output (get-logging @clientcmd
                              ldtpd-log
                              "check_libglade_warnings"
                              "libglade-WARNING"
                              (tasks/start-app))]
     (verify (not (substring? "libglade-WARNING" output))))
   (finally (tasks/kill-app))))

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
    (tasks/ui click :online-documentation)
    (if (bool (tasks/ui waittillwindowexist "Error" 10))
      (do (tasks/ui click "Error" "OK")
          (tasks/ui waittillwindowexist "Preferred Applications" 10)
          (tasks/ui click "Preferred Applications" "Close")
          (throw (SkipException. (str "Firefox does not exist on this machine"))))
      (tasks/ui closewindow :firefox-help-window))
    (let [output (get-logging @clientcmd
                              ldtpd-log
                              "check_online_documentation"
                              nil
                              (tasks/ui click :online-documentation)
                              (tasks/ui waittillwindowexist :firefox-help-window 10))]
      (verify (bool (tasks/ui guiexist :firefox-help-window)))
      (verify (not (substring? "Traceback" output))))
    (finally    (if (tasks/ui guiexist :firefox-help-window) (tasks/ui closewindow :firefox-help-window))
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
     (when tmpCAcertpath (run-command (str "rm -rf " tmpCAcertpath)))
     (run-command (str "mkdir " tmpCAcertpath))
     (tasks/set-conf-file-value "ca_cert_dir" tmpCAcertpath)
     (verify (= 1 (tasks/start-app)))
     (finally
      (tasks/set-conf-file-value "ca_cert_dir" CAcertpath))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-923873"]}}
  check_status_when_unregistered
  "To verify that status in MyInstalledProducts icon color and product status are appropriately displayed when client is unregistered"
  [_]
  (tasks/kill-app)
  (run-command "subscription-manager unregister")
  (run-command "subscription-manager clean")
  (tasks/start-app)
  (verify (= 1 (tasks/ui guiexist :main-window "Keep your system*")))
  (tasks/do-to-all-rows-in
   :installed-view 2
   (fn [subscription]
     (try
       (verify (= (tasks/ui getcellvalue :installed-view subscription 2) "Unknown"))))))

(defn ^{Test {:groups ["system"
                        "blockedByBug-916666"]}}
  rhsmcertd_resart_check_timestamp
  "Checks whether the timestamp at which cert check was intiated is in sync with that displayed in facts window and help dialog"
  [_]
  (try
    (tasks/kill-app)
    (run-command "subscription-manager unregister")
    (run-command "subscription-manager clean")
    (tasks/start-app)
    (tasks/register-with-creds)
    (let
        [rhsmcertd-log "/var/log/rhsm/rhsmcertd.log"
         output (get-logging @clientcmd
                              rhsmcertd-log
                              "cert-check-timestamp"
                              "(Cert Check)"
                              (do
                                (run-command "service rhsmcertd stop")
                                (run-command "rhsmcertd -n")
                                (sleep 90000)))
          log-timestamp (re-find #"\d+:\d+:\d+" output)
          ;; The following steps add 4 hours as it is the default
          ;; interval in conf file. The following step is comversion of time
          ;; formats as the logs have 24hrs time format and in the GUI it
          ;; 12hrs time format. The last step adds a zero if the time
          ;; is less than 10hrs which makes sting comparison easier
          new-time (+ 4 (read-string (first (clojure.string/split log-timestamp #":")))) 
          hours (if (> new-time 12) (- new-time 12) new-time)
          compare-time (str (if ( < hours 10) (str "0" hours) hours)(re-find #":\d+:\d+" log-timestamp))]
      (def logtime log-timestamp)
      (def newtime compare-time))
    (tasks/ui click :about)
    (tasks/ui waittillwindowexist :about-dialog 10)
    (verify ( = newtime (re-find #"\d+:\d+:\d+" (tasks/ui gettextvalue :next-system-check))))
    (tasks/ui click :close-about-dialog)
    (tasks/ui click :view-system-facts)
    (tasks/ui waittillwindowexist :facts-dialog 10)
    (verify (= logtime (re-find #"\d+:\d+:\d+" (tasks/ui gettextvalue :update-time))))
    (tasks/ui click :close-facts)
    (finally
     (if-not (tasks/ui showing? :register-system) (tasks/unregister))
     (if (bool (tasks/ui guiexist :about-dialog)) (tasks/ui click :close-about-dialog))
     (if (bool (tasks/ui guiexist :facts-dialog)) (tasks/ui click :close-facts))
     (tasks/restart-app)
     ;; Worstcase scenario if service rhsmcertd is stopped we have to
     ;; turn it on as  rhsmcertd_stop_check_timestamp test depends on it
     (if-not (substring? "running" (:stdout (run-command "service rhsmcertd status")))
       (do
         (run-command "service rhsmcertd start")
         (sleep 150000))))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-916666"]
              :dependsOnMethods ["rhsmcertd_resart_check_timestamp"]}}
  rhsmcertd_stop_check_timestamp
  "Checks wheter the timestamp in about-dialog is Unknown when rhsmcertd is stopped"
  [_]
  (try
    (tasks/kill-app)
    (run-command "subscription-manager unregister")
    (run-command "subscription-manager clean")
    (tasks/start-app)
    (tasks/register-with-creds)
    (run-command "service rhsmcertd stop")
    (tasks/ui click :about)
    (tasks/ui waittillwindowexist :about-dialog 10)
    (verify ( = "Unknown" (re-find #"\d+:\d+:\d+" (tasks/ui gettextvalue :next-system-check))))
    (finally
     (if-not (tasks/ui showing? :register-system) (tasks/unregister))
     (if (bool (tasks/ui guiexist :about-dialog)) (tasks/ui click :close-about-dialog))
     (tasks/restart-app)
     ;; No sleep as we can continue without waiting for the service to
     ;; start as it does not affect the normal functioning of sub-man
     (if-not (substring? "running" (:stdout (run-command "service rhsmcertd status")))
       (run-command "service rhsmcertd start")))))

(defn ^{Test {:groups ["system"
                       "blockedByBug-984083"]}}
  check_for_break_charecters_in_popups
  "Test to ceck if there are any break characters in pop-ups"
  [_]
  (try
    (if (tasks/ui showing? :register-system)
      (tasks/register-with-creds))
    (tasks/ui click :unregister-system)
    (tasks/ui waittillwindowexist :question-dialog 30)
    (verify (not (substring? "</b>" (tasks/ui gettextvalue :question-dialog "*Are you sure*"))))
    (finally
     (if (tasks/ui guiexist :question-dialog)
       (do
         (tasks/ui click :yes)
         (tasks/checkforerror)))
     (if-not (tasks/ui showing? :register-system) (tasks/unregister))
     (tasks/restart-app))))

(gen-class-testng)
