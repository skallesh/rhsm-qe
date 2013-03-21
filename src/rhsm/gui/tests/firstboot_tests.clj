(ns rhsm.gui.tests.firstboot-tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            AfterClass
            BeforeClass
            BeforeGroups
            Test]
            [com.redhat.qe.auto.bugzilla BzChecker]))

(defn start_firstboot []
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  ;; RHEL5 has a different firstboot order than RHEL6
  (if (tasks/fbshowing? :firstboot-window "Firewall")
    (do
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/ui click :firstboot-forward)
      (tasks/sleep 3000) ;; FIXME find a better way than a hard wait...
      (verify (tasks/fbshowing? :register-now))))
  (tasks/ui click :register-now)
  (tasks/ui click :firstboot-forward)
  (assert ( = 1 (tasks/ui guiexist :firstboot-window "Choose Service"))))

(defn kill_firstboot []
  (.runCommand @clientcmd "killall -9 firstboot")
  (tasks/sleep 5000))

(defn zero-proxy-values []
  (tasks/set-conf-file-value "proxy_hostname" "")
  (tasks/set-conf-file-value "proxy_port" "")
  (tasks/set-conf-file-value "proxy_user" "")
  (tasks/set-conf-file-value "proxy_password" ""))

(defn reset_firstboot []
  (kill_firstboot)
  (.runCommand @clientcmd "subscription-manager clean")
  (zero-proxy-values)
  (start_firstboot))

(defn ^{BeforeClass {:groups ["setup"]}}
  firstboot_init [_]
  (verify (not (.isBugOpen (BzChecker/getInstance) "922806")))
  ;; new rhsm and classic have to be totally clean for this to run
  (.runCommand @clientcmd "subscription-manager clean")
  (let [sysidpath "/etc/sysconfig/rhn/systemid"]
    (.runCommand @clientcmd (str "[ -f " sysidpath " ] && rm " sysidpath ))))

(defn ^{AfterClass {:groups ["setup"]
                    :alwaysRun true}}
  firstboot_cleanup [_]
  (kill_firstboot)
  (.runCommand @clientcmd "subscription-manager clean")
  (zero-proxy-values))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_auth
  "Checks whether the proxy and authentication is enabled in rhsm-conf file" 
  [_]
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
    (tasks/verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_noauth
  "Checks whether the proxy is enabled and authentication is disabled in rhsm-conf file"
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (let [hostname (@config :noauth-proxy-hostname)
        port (@config :noauth-proxy-port)]
    (tasks/enableproxy hostname :port port :firstboot? true)
    (tasks/ui click :firstboot-forward)
    (tasks/checkforerror)
    (tasks/firstboot-register (@config :username) (@config :password))
    (tasks/verify-conf-proxies hostname port "" "")))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_disable_proxy
  "Checks whether the proxy and authentication is disabled in rhsm-conf file"
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/disableproxy true)
  (tasks/ui click :firstboot-forward)
  (tasks/checkforerror)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/verify-conf-proxies "" "" "" ""))

(defn firstboot_register_invalid_user
  "Register with invalid user credentials at firstboot"
  [user pass recovery]
  (reset_firstboot)
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

(defn ^{Test {:groups ["firstboot"
                       "blockedByBug-642660"
                       "blockedByBug-863572"]}}
  firstboot_check_back_button_state
  "Checks the state of back and forward button (whether disabled) when firstboot is regestering (when progress-bar is displayed) to to a server"                     
  [_]
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (tasks/firstboot-register (@config :username) (@config :password))
  (verify (= 1 (tasks/ui hasstate :firstboot-back "Sensitive")))
  (verify (= 1 (tasks/ui hasstate :firstboot-forward "Sensitive"))))

(defn ^{Test {:groups ["firstboot" "blockedByBug-872727"]
              :dependsOnMethods ["firstboot_check_back_button_state"]}}
  firstboot_check_back_button
  "Checks the functionality of the back button during firstboot"
  [_]
  (tasks/ui click :firstboot-back)
  (verify (tasks/ui showing? :register-rhsm))
  (let [output (.getStdout (.runCommandAndWait @clientcmd "subscription-manager identity"))]
    (verify (tasks/substring? "This system is not yet registered" output))))

;; https://tcms.engineering.redhat.com/case/72669/?from_plan=2806
(defn ^{Test {:groups ["firstboot" "blockedByBug-642660"]}}
  firstboot_skip_register
  "Checks whether firstboot skips register if subscription manger is already registered"
  [_]
  (kill_firstboot)
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (.runCommandAndWait @clientcmd "subscritption-manager clean")
  (.runCommandAndWait @clientcmd (str "subscription-manager register"
                                      " --username " (@config :username)
                                      " --password " (@config :password)
                                      " --org " (@config :owner-key)))
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  (verify (tasks/ui showing?
                    :firstboot-window
                    "Your system was registered for updates during installation.")))

;; https://tcms.engineering.redhat.com/case/72670/?from_plan=2806
(defn ^{Test {:groups ["firstboot"]}}
  firstboot_check_register_sm_unregistered
  "Checks whether firstboot navigates to register screen when subscription manager is unregistered"
  [_]
  (kill_firstboot)
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (reset_firstboot)
  (tasks/ui click :register-rhsm)
  (tasks/ui click :firstboot-forward)
  (verify (tasks/fbshowing? :firstboot-server-entry))
  (tasks/ui click :firstboot-forward)
  (verify (tasks/fbshowing? :firstboot-user)))

(data-driven firstboot_register_invalid_user {Test {:groups ["firstboot"]}}
  [^{Test {:groups ["blockedByBug-703491"]}}
   ["sdf" "sdf" :invalid-credentials]
   ["" "" :no-username]
   ["" "password" :no-username]
   ["sdf" "" :no-password]])

;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=700601

(gen-class-testng)
