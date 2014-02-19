(ns rhsm.gui.tests.firstboot_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            AfterClass
            BeforeClass
            BeforeGroups
            Test]
            org.testng.SkipException
            [com.redhat.qe.auto.bugzilla BzChecker]))

(comment

(defn start_firstboot []
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (verify (tasks/fbshowing? :register-now))
  (tasks/ui click :register-now)
  (tasks/ui click :firstboot-forward)
  (assert ( bool (tasks/ui guiexist :firstboot-window window-name))))

(defn kill_firstboot []
  (run-command "killall -9 firstboot")
  (sleep 5000))

(defn zero-proxy-values []
  (tasks/set-conf-file-value "proxy_hostname" "")
  (tasks/set-conf-file-value "proxy_port" "")
  (tasks/set-conf-file-value "proxy_user" "")
  (tasks/set-conf-file-value "proxy_password" ""))

(defn reset_firstboot []
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values)
  (start_firstboot))

(defn ^{BeforeClass {:groups ["setup"]}} 
  firstboot_init [_]
  (try
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (skip-if-bz-open "922806")
    (skip-if-bz-open "1016643" (= "RHEL7" (get-release)))
    ;; new rhsm and classic have to be totally clean for this to run
    (run-command "subscription-manager clean")
    (let [sysidpath "/etc/sysconfig/rhn/systemid"]
      (run-command (str "[ -f " sysidpath " ] && rm " sysidpath )))
    (catch Exception e
      (reset! (skip-groups :firstboot) true)
      (throw e))))

(defn ^{AfterClass {:groups ["setup"]
                    :alwaysRun true}}
  firstboot_cleanup [_]
  (assert-valid-testing-arch)
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values))

)
