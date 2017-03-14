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
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
            [clojure.core.match :refer [match]]
            rhsm.gui.tasks.ui
            [clojure.java.io :as io])
  (:import [org.testng.annotations
            AfterClass
            BeforeClass
            BeforeGroups
            Test]
           [com.redhat.qe.auto.bugzilla BzChecker]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(def window-name "Choose Service")

(defn start_firstboot []
  (if (= "RHEL7" (get-release))
    (do (tasks/restart-app)
        (if-not (tasks/ui showing? :register-system)
          (tasks/unregister))
        (tasks/start-firstboot)
        (verify (tasks/fbshowing? :firstboot-window
                                  "Subscription Management Registration"))
        ;; (tasks/ui click :register-now)    unable to access this radio button
        (tasks/ui click :firstboot-forward))
    (do
      (tasks/start-firstboot)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :license-yes)
      (tasks/ui click :firstboot-forward)
      ;; RHEL5 has a different firstboot order than RHEL6
      (if (and (= "RHEL5" (get-release))
               (tasks/fbshowing? :firewall))
        (do
          (tasks/ui click :firstboot-forward)
          (tasks/ui click :firstboot-forward)
          (tasks/ui click :firstboot-forward)
          (tasks/ui click :firstboot-forward)
          (sleep 3000) ;; FIXME find a better way than a hard wait...
          (verify (tasks/fbshowing? :software-updates))))
      (tasks/ui click :register-now)
      (tasks/ui click :firstboot-forward)
      (assert (bool (tasks/ui guiexist :firstboot-window window-name))))))

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
  (if-not (tasks/ui showing? :register-system)
    (do (tasks/unsubscribe_all)
        (tasks/unregister)))
  (run-command "subscription-manager clean")
  (zero-proxy-values)
  (start_firstboot))

(defn skip-by-rhel-release [{:keys [family variant version]}]
  "It raises a skipException in some cases. The main purpose of this function is to ensure
  that firstboot related tests are skipped for newer versions of RHEL. They are obsolete in such case."
  (let [[_ major minor] (re-find #"(\d)\.(\d)" version)]
    (match [major minor]
      ["7" (a :guard #(>= (Integer. %) 2))] (throw (SkipException. "Firsboot only applies to RHEL < 7.2"))
      ["8" _]   (throw (SkipException. "Firsboot only applies to RHEL < 7.2"))
      ["5" "7"] (throw (SkipException. "Skipping firstboot tests on RHEL 5.7 as the tool is not updated"))
      :else [major minor])))

(defn ^{BeforeClass {:groups ["setup"]}}
  firstboot_init [_]
  (try
    (let [[rhel-version-major rhel-version-minor] (skip-by-rhel-release (get-release :true))]
      (skip-if-bz-open "922806")
      (skip-if-bz-open "1016643" (= rhel-version-major "7"))
      (when (= rhel-version-major "7") (base/startup nil)))
    (assert (= 0 (-> (run-command (format "which %s" (@config :firstboot-binary-path))) :exitcode))
            "No firstboot binary found")
    ;; new rhsm and classic have to be totally clean for this to run
    (run-command "subscription-manager clean")
    (let [sysidpath "/etc/sysconfig/rhn/systemid"]
      (run-command (str "[ -f " sysidpath " ] && rm " sysidpath)))
    (catch Exception e
      (reset! (skip-groups :firstboot) true)
      (throw (SkipException. (str "some problem in setup for firstboot_tests. Was: " (.toString e)))))))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  firstboot_cleanup [_]
  (assert-valid-testing-arch)
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values))

(defn ^{Test           {:groups   ["firstboot"
                                   "tier2"
                                   "tier1" "acceptance"
                                   "blockedByBug-1020542"
                                   "blockedByBug-1020672"
                                   "blockedByBug-1031176"
                                   "blockedByBug-1153634"
                                   "blockedByBug-1157404"
                                   "blockedByBug-1159936"]
                        :priority (int 100)}
        TestDefinition {:projectID  [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-32864"]}}
  firstboot_rhel7_register
  "since firstboot in RHEL7 is minimalistic and discrepancies wont be fixed, this
   test performs a happy-path test to assert general working of firstboot screens"
  [_]
  (if (= "RHEL7" (get-release))
    (do
      (reset_firstboot)
      (verify (tasks/fbshowing? :firstboot-window "proxy_button"))
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/ui click :firstboot-forward)
      (sleep 2000)
      (if (bool (tasks/ui guiexist :firstboot-window))
        (do (tasks/ui click :firstboot-forward)
            (sleep 2000)))
      (verify (not (bool (tasks/ui guiexist :firstboot-window))))
      (sleep 3000)
      (verify (not (tasks/ui showing? :register-system))))
    (throw (SkipException.
            (str "This is not RHEL7 !!!
                  Skipping firstboot_rhel7_register test.")))))

(defn ^{Test {:groups ["firstboot"
                       "tier2"]
              :dependsOnMethods ["firstboot_rhel7_register"]}}
  firstboot_rhel7_proxy_auth
  "firstboot register with proxy auth test for RHEL7"
  [_]
  (if (= "RHEL7" (get-release))
    (do
      (try
        (reset_firstboot)
        (verify (tasks/fbshowing? :firstboot-window "proxy_button"))
        (tasks/ui click :firstboot-window "proxy_button")
        (tasks/ui waittillguiexist :firstboot-proxy-dialog)
        (verify (bool (tasks/ui guiexist :firstboot-proxy-dialog)))
        (let [hostname (@config :basicauth-proxy-hostname)
              port (@config :basicauth-proxy-port)
              username (@config :basicauth-proxy-username)
              password (@config :basicauth-proxy-password)]
          (tasks/enableproxy hostname :port port :user username :pass password :firstboot? true))
        (tasks/firstboot-register (@config :username) (@config :password))
        (tasks/ui click :firstboot-forward)
        (sleep 2000)
        (verify (not (bool (tasks/ui guiexist :firstboot-window))))
        (verify (not (tasks/ui showing? :register-system)))
        (finally
          (reset_firstboot)
          (tasks/disableproxy true)
          (kill_firstboot))))
    (throw (SkipException.
            (str "This is not RHEL7 !!!
                  Skipping firstboot_rhel7_proxy_auth test.")))))

(defn ^{Test {:groups ["firstboot"
                       "tier2"]
              :dependsOnMethods ["firstboot_rhel7_proxy_auth"]}}
  firstboot_rhel7_proxy_noauth
  "firstboot register with proxy noauth test for RHEL7"
  [_]
  (if (= "RHEL7" (get-release))
    (do
      (try
        (reset_firstboot)
        (verify (tasks/fbshowing? :firstboot-window "proxy_button"))
        (tasks/ui click :firstboot-window "proxy_button")
        (tasks/ui waittillguiexist :firstboot-proxy-dialog)
        (verify (bool (tasks/ui guiexist :firstboot-proxy-dialog)))
        (let [hostname (@config :noauth-proxy-hostname)
              port (@config :noauth-proxy-port)]
          (tasks/enableproxy hostname :port port :firstboot? true))
        (tasks/firstboot-register (@config :username) (@config :password))
        (tasks/ui click :firstboot-forward)
        (sleep 2000)
        (verify (not (bool (tasks/ui guiexist :firstboot-window))))
        (verify (not (tasks/ui showing? :register-system)))
        (finally
          (reset_firstboot)
          (tasks/disableproxy true)
          (kill_firstboot))))
    (throw (SkipException.
            (str "This is not RHEL7 !!!
                  Skipping firstboot_rhel7_proxy_noauth test.")))))

(defn ^{Test           {:groups   ["firstboot"
                                   "tier3"
                                   "blockedByBug-973269"
                                   "blockedByBug-988411"
                                   "blockedByBug-1199211"]
                        :priority (int 200)}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36996"]}}
  firstboot_enable_proxy_auth
  "Checks whether the proxy and authentication is enabled in rhsm-conf file"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (try
    (reset_firstboot)
    (tasks/ui click :register-rhsm)
    (let [hostname (@config :basicauth-proxy-hostname)
          port (@config :basicauth-proxy-port)
          username (@config :basicauth-proxy-username)
          password (@config :basicauth-proxy-password)]
      (tasks/enableproxy hostname :port port :user username :pass password :firstboot? true)
      (tasks/ui click :firstboot-forward)
      (tasks/checkforerror)
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/verify-conf-proxies hostname port username password))
    (finally
      (reset_firstboot)
      (tasks/disableproxy true)
      (kill_firstboot))))

(defn ^{Test           {:groups           ["firstboot"
                                           "tier3"
                                           "blockedByBug-973269"
                                           "blockedByBug-988411"]
                        :dependsOnMethods ["firstboot_enable_proxy_auth"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36370"]}}
  firstboot_enable_proxy_noauth
  "Checks whether the proxy is enabled and authentication is disabled in rhsm-conf file"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (if (= "RHEL5" (get-release))
    (throw (SkipException.
            (str "Skipping 'firstboot_enable_proxy_noauth' test on RHEL5 "
                 "as password fiel in proxy window is not accessible"))))
  (try
    (reset_firstboot)
    (tasks/ui click :register-rhsm)
    (let [hostname (@config :noauth-proxy-hostname)
          port (@config :noauth-proxy-port)]
      (tasks/enableproxy hostname :port port :firstboot? true)
      (tasks/ui click :firstboot-forward)
      (tasks/checkforerror)
      (tasks/firstboot-register (@config :username) (@config :password))
      (tasks/verify-conf-proxies hostname port "" ""))
    (finally
      (reset_firstboot)
      (tasks/disableproxy true)
      (kill_firstboot))))

(defn ^{Test {:groups ["firstboot"
                       "tier3"]
              :dependsOnMethods ["firstboot_enable_proxy_noauth"]}}
  firstboot_disable_proxy
  "Checks whether the proxy and authentication is disabled in rhsm-conf file"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/disableproxy true)
  (tasks/ui click :firstboot-forward)
  (tasks/checkforerror)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/verify-conf-proxies "" "" "" "")
  (kill_firstboot))

(defn firstboot_register_invalid_user
  "Register with invalid user credentials at firstboot"
  [user pass recovery]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (skip-if-bz-open "1199211")
  (reset_firstboot)
  (tasks/disableproxy true)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (let [test-fn (fn [username password expected-error-type]
                  (try+
                   (tasks/firstboot-register username password)
                   (catch [:type expected-error-type]
                          {:keys [type]}
                     type)))]
    (let [thrown-error (apply test-fn [user pass recovery])
          expected-error recovery]
      (verify (= thrown-error expected-error))
      ;; https://bugzilla.redhat.com/show_bug.cgi?id=703491
      (verify (tasks/fbshowing? :firstboot-user)))))

(defn ^{Test           {:groups ["firstboot"
                                 "tier3"
                                 "blockedByBug-642660"
                                 "blockedByBug-863572"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36995"]}}
  firstboot_check_back_button_state
  "Checks the state of back and forward button (whether disabled) when firstboot is
   regestering (when progress-bar is displayed) to to a server. This check is performed
   only in RHEL5 for RHEL6 and above this would be a part of firstboot-register"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (if (= "RHEL5" (get-release))
    (do
      (tasks/firstboot-register (@config :username) (@config :password))
      (verify (not (bool (tasks/ui hasstate :firstboot-back "Sensitive"))))
      (verify (not (bool (tasks/ui hasstate :firstboot-forward "Sensitive")))))
    (tasks/firstboot-register (@config :username) (@config :password) :back-button? true)))

(defn ^{Test           {:groups           ["firstboot"
                                           "tier3"
                                           "blockedByBug-872727"
                                           "blockedByBug-973317"]
                        :dependsOnMethods ["firstboot_check_back_button_state"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36995"]}}
  firstboot_check_back_button
  "Checks the functionality of the back button during firstboot"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/ui click :firstboot-back)
  (verify (tasks/fbshowing? :firstboot-window window-name))
  (let [output (:stdout (run-command "subscription-manager identity"))]
    ;; Functionality of back-button is limited to RHEL5.
    ;; In RHEL6 back-button behavior is different
    (verify (substring? "system identity: " output))))

;; https://tcms.engineering.redhat.com/case/72669/?from_plan=2806
(defn ^{Test           {:groups ["firstboot"
                                 "tier2"
                                 "blockedByBug-642660"
                                 "blockedByBug-1142495"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36506"]}}
  firstboot_skip_register
  "Checks whether firstboot skips register if subscription manger is already registered"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (kill_firstboot)
  (run-command "subscription-manager unregister")
  (run-command "subscritption-manager clean")
  (run-command (str "subscription-manager register"
                    " --username " (@config :username)
                    " --password " (@config :password)
                    " --org " (@config :owner-key)))
  (start_firstboot)
  (if (tasks/fbshowing? :firstboot-window "Set Up Software Updates")
    (do
      (tasks/ui click :firstboot-forward)
      (verify (tasks/fbshowing? :firstboot-window "Create User")))))

;; https://tcms.engineering.redhat.com/case/72670/?from_plan=2806
(defn ^{Test           {:groups           ["firstboot"
                                           "tier2"]
                        :dependsOnMethods ["firstboot_skip_register"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36507"]}}
  firstboot_check_register_sm_unregistered
  "Checks whether firstboot navigates to register screen when subscription manager is unregistered"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (run-command "subscription-manager unregister")
  (kill_firstboot)
  (run-command "subscription-manager clean")
  (zero-proxy-values)
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  (if (and (= "RHEL5" (get-release))
           (tasks/fbshowing? :firstboot-window "Firewall"))
    (do
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (sleep 3000) ;; FIXME find a better way than a hard wait...
      (verify (tasks/fbshowing? :software-updates))))
  (tasks/ui click :register-now)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (verify (bool (tasks/ui guiexist :firstboot-window
                          "Subscription Management Registration"))))

(defn ^{Test           {:groups ["firstboot"
                                 "tier2"
                                 "blockedByBug-973317"]}
        TestDefinition {:projectID  [`DefTypes$Project/RHEL6]
                        :testCaseID ["RHEL6-36504"]}}
  firstboot_back_button_after_register
  "Verifies that on clicking backbutton after registering from Create User
   menu should navigte to Choose Service menu"
  [_]
  (if (= "RHEL7" (get-release))
    (throw (SkipException.
            (str "Skipping firstboot tests on RHEL7 as it's no longer supported !!!!"))))
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/ui click :firstboot-forward)
  (verify (tasks/fbshowing? :firstboot-window "Create User"))
  (tasks/ui click :firstboot-back)
  (verify (not (tasks/fbshowing? :firstboot-window window-name))))

(data-driven firstboot_register_invalid_user {Test {:groups ["firstboot"
                                                             "tier2"]}}
             [^{Test {:groups ["blockedByBug-703491"]}}
              (if-not (assert-skip :firstboot)
                (do
                  ["sdf" "sdf" :invalid-credentials]
                  ["" "" :no-username]
                  ["" "password" :no-username]
                  ["sdf" "" :no-password])
                (to-array-2d []))])

;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=700601

(gen-class-testng)
