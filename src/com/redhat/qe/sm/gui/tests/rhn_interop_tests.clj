(ns com.redhat.qe.sm.gui.tests.rhn-interop-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            [clojure.contrib.java-utils :as jutils]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(def systemid "/etc/sysconfig/rhn/systemid")
  
(defn systemid-exists? []
  (= 1 (->> (.getStdout (.runCommandAndWait @clientcmd (str "test -e " systemid " && echo 1 || echo 0"))) str .trim Integer/parseInt))
)

(defn kill-app []
  (.runCommandAndWait @clientcmd "killall -9 subscription-manager-gui")
  (tasks/ui waittillwindownotexist :main-window 30)
)
  
(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (if (tasks/ui exists? :main-window "")
    (kill-app))
  (.runCommandAndWait @clientcmd (str "touch " systemid))
)

(defn ^{Test {:groups ["interop"]}}
  check_warning [_]
  (tasks/start-app)
  (tasks/ui waittillwindowexist :warning-dialog 30)
  (verify (systemid-exists?))
  (verify (tasks/ui exists? :warning-dialog ""))
  (kill-app)
)

(defn ^{Test {:groups ["interop"]
              :dependsOnMethods ["check_warning"] }}
  check_warning_ok [_]
  (tasks/start-app)
  (tasks/ui waittillwindowexist :warning-dialog 30)
  (tasks/ui click :warn-ok)
  (verify (tasks/ui waittillwindownotexist :warning-dialog 30))
  (verify (tasks/ui exists? :main-window ""))
  (kill-app)
)

(defn ^{Test {:groups ["interop"]
              :dependsOnMethods ["check_warning"]}}
  check_warning_cancel [_]
  (tasks/start-app)
  (tasks/ui waittillwindowexist :warning-dialog 30)
  (tasks/ui click :warn-cancel)
  (verify (= 1 (tasks/ui waittillwindownotexist :main-window 30)))
  (kill-app)
)

(defn ^{Test {:groups ["interop"]
              :dependsOnMethods ["check_warning" "check_warning_ok" "check_warning_cancel"]}}
  check_no_warning [_]
  (.runCommandAndWait @clientcmd (str "rm -f " systemid))
  (verify (not (systemid-exists?)))
  (tasks/start-app)
  (verify (not (tasks/ui exists? :warning-dialog "")))
)

(gen-class-testng)

