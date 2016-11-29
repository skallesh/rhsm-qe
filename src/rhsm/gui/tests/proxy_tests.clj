(ns rhsm.gui.tests.proxy_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           auth-proxyrunner
                                           noauth-proxyrunner)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups]))

(def rhsm-log "/var/log/rhsm/rhsm.log")
(def ldtpd-log "/var/log/ldtpd/ldtpd.log")
(def proxy-success "Proxy connection succeeded")

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (try+ (skip-if-bz-open "1142918")
        (if (not (bool (tasks/ui guiexist :main-window)))
          (tasks/start-app))
        (tasks/unregister)
        (catch [:type :not-registered] _)
        (catch Exception e
          (reset! (skip-groups :proxy) true)
          (throw e))))

(defn register []
  (try+ (tasks/register (@config :username) (@config :password))
        (catch [:type :already-registered]
               {:keys [unregister-first]} (unregister-first)))
  (verify (not (tasks/ui showing? :register-system))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-1250348"]
              :priority (int 100)}}
  enable_proxy_auth
  "Asserts that the rhsm.conf file is correctly set after setting a proxy with auth."
  [_]
  (let [hostname  (@config :basicauth-proxy-hostname)
        port      (@config :basicauth-proxy-port)
        username  (@config :basicauth-proxy-username)
        password  (@config :basicauth-proxy-password)]
    (tasks/enableproxy hostname :port port :user username :pass password)
    (tasks/verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-1250348"]
              :priority (int 101)}}
  enable_proxy_noauth
  "Asserts that the rhsm.conf file is correctly set after setting a proxy without auth."
  [_]
  (let [hostname  (@config :noauth-proxy-hostname)
        port      (@config :noauth-proxy-port)]
    (tasks/enableproxy hostname :port port)
    (tasks/verify-conf-proxies hostname port "" "")))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["enable_proxy_auth"
                                 "enable_proxy_noauth"]}}
  disable_proxy
  "Asserts that the rhsm.conf file is correctly set after diabling proxies."
  [_]
  (tasks/disableproxy)
  (tasks/verify-conf-proxies "" "" "" ""))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["enable_proxy_auth"]}}
  proxy_auth_connect
  "Asserts that rhsm can connect after setting a proxy with auth."
  [_]
  (enable_proxy_auth nil)
  (let [logoutput (get-logging @auth-proxyrunner
                               (@config :basicauth-proxy-log)
                               "proxy-auth-connect"
                               nil
                               (register))]
    (verify (not  (clojure.string/blank? logoutput)))))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  proxy_noauth_connect
  "Asserts that rhsm can connect after setting a proxy without auth."
  [_]
  (enable_proxy_noauth nil)
  (let [logoutput (get-logging @noauth-proxyrunner
                               (@config :noauth-proxy-log)
                               "proxy-noauth-connect"
                               nil
                               (register))]
    (verify (not  (clojure.string/blank? logoutput)))))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["disable_proxy"]}}
  disable_proxy_connect
  "Asserts that a proxy is not used after clearing proxy settings."
  [_]
  (disable_proxy nil)
  ;; note: if this takes forever, blank out the proxy log file.
  (let [logoutput (get-logging @auth-proxyrunner
                               (@config :basicauth-proxy-log)
                               "disabled-auth-connect"
                               nil
                               (register))]
    (verify (clojure.string/blank? logoutput)))
  (let [logoutput (get-logging @noauth-proxyrunner
                               (@config :noauth-proxy-log)
                               "disabled-noauth-connect"
                               nil
                               (register))]
    (verify (clojure.string/blank? logoutput))))

(defn test_proxy [expected-message]
  (tasks/ui click :configure-proxy)
  (try
    (if (bool (tasks/ui hasstate :test-connection "SENSITIVE"))
      (do
        (tasks/ui click :test-connection)
        (sleep 2000)
        (let [message (tasks/ui gettextvalue :connection-status)]
          (verify (= expected-message message))))
      (do
        (verify (not (some #(= "sensitive" %)
                           (tasks/ui getallstates :test-connection))))
        (verify (= "" (tasks/ui gettextvalue :connection-status)))))
    (finally (tasks/ui click :close-proxy))))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["enable_proxy_auth"]}}
  test_auth_proxy
  "Tests the 'test connection' button when using a proxy with auth."
  [_]
  (enable_proxy_auth nil)
  (test_proxy "Proxy connection succeeded")
  (tasks/disableproxy))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  test_noauth_proxy
  "Tests the 'test connection' button when using a proxy without auth."
  [_]
  (enable_proxy_noauth nil)
  (test_proxy "Proxy connection succeeded")
  (tasks/disableproxy))

(defn ^{Test {:groups ["proxy"
                       "tier2"]
              :dependsOnMethods ["disable_proxy"]}}
  test_disabled_proxy
  "Test that the 'test connection' button is disabled when proxy settings are cleared."
  [_]
  (try
    (disable_proxy nil)
    (test_proxy "")
    (finally
      (if (bool (tasks/ui guiexist :proxy-config-dialog))
        (tasks/ui click :close-proxy)))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-927340"
                       "blockedByBug-1371632"]
              :dependsOnMethods ["disable_proxy"]}}
  test_proxy_with_blank_proxy
  "Test whether 'Test Connection' returns appropriate message when 'Location Proxy' is empty"
  [_]
  (try
    (disable_proxy nil)
    (tasks/enableproxy "" :close? false)
    (test_proxy "Proxy connection failed")
    (finally (tasks/disableproxy))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-927340"
                       "blockedByBug-1371632"]
              :dependsOnMethods ["disable_proxy"]}}
  test_proxy_with_blank_credentials
  "Test whether 'Test Connection' returns appropriate message when User and Password fields are empty"
  [_]
  (try
    (disable_proxy nil)
    (tasks/enableproxy "" :close? false :auth? true :user "" :pass "")
    (test_proxy "Proxy connection failed")
    (finally (tasks/disableproxy))))

(defn ^{Test {:groups ["proxy"
                       "tier2"]}}
  test_bad_proxy
  "Tests the 'test connection' button when using a non-existant proxy."
  [_]
  (try+
   (tasks/enableproxy "doesnotexist.redhat.com")
   (test_proxy "Proxy connection failed")
   (finally
     (tasks/close-error-dialog)
     (tasks/disableproxy))))

(defn ^{Test {:groups ["proxy"
                       "tier2"]}}
  bad_proxy
  "Tests error message when using a non-existant proxy."
  [_]
  (try+ (tasks/unregister)
        (catch [:type :not-registered] _))
  (try
    (let [hostname  "blahblah"
          port      "666"]
      (tasks/enableproxy hostname :port port)
      (tasks/verify-conf-proxies hostname port "" ""))
    (let [thrown-error (try+ (register)
                             (catch Object e (:type e)))]
      (verify (= thrown-error :network-error)))
    (finally
      (if (bool (tasks/ui guiexist :register-dialog))
        (tasks/ui click :register-close))
      (tasks/close-error-dialog)
      (disable_proxy nil))))

(defn ^{Test {:groups ["proxy"
                       "tier3"
                       "blockedByBug-729688"]}}
  bad_proxy_facts
  "Tests facts-update through a bad proxy."
  [_]
  (disable_proxy nil)
  (register)
  (try
    (let [hostname  "blahblah"
          port      "666"]
      (tasks/enableproxy hostname :port port)
      (tasks/verify-conf-proxies hostname port "" ""))
    (let [thrown-error (try+ (tasks/ui click :view-system-facts)
                             (tasks/ui waittillguiexist :facts-view)
                             (tasks/ui click :update-facts)
                             (tasks/checkforerror)
                             (catch Object e
                               (:type e)))]
      (verify (= thrown-error :error-updating)))
    (finally
      (if (bool (tasks/ui guiexist :facts-dialog))
        (tasks/ui click :close-facts))
      (tasks/close-error-dialog)
      (disable_proxy nil))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-806993"]}}
  test_proxy_formatting
  "Tests the auto-formatting feature of the proxy location field."
  [_]
  (try+
   (tasks/enableproxy "http://some.host.name:1337")
   (tasks/ui click :configure-proxy)
   (tasks/ui waittillwindowexist :proxy-config-dialog 60)
   (let [location (tasks/ui gettextvalue :proxy-location)]
     (verify (not (substring? "http://" location)))
     (verify (= 2 (count (clojure.string/split location #":")))))
   (finally
     (tasks/ui click :close-proxy)
     (disable_proxy nil))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-1323276"]
              :description "Given a system is subscribed without proxy.
When I click on 'Configure Proxy'
 and I click on 'I would like to connect via an HTTP Proxy'
 and I click on 'Use Authentication with HTTP Proxy'
Then I should see nothing in a field 'Proxy Location'."}}
  no_litter_in_location_when_using_proxy
  [_]
  (tasks/restart-app :force-kill? true)
  (tasks/disableproxy)
  (register)
  (tasks/ui click :configure-proxy)
  (tasks/ui waittillwindowexist :proxy-config-dialog 60)
  (verify (->> :proxy-location (tasks/ui gettextvalue) clojure.string/blank?))
  (tasks/ui check :proxy-checkbox)
  (verify (->> :proxy-location (tasks/ui gettextvalue) clojure.string/blank?))
  (tasks/ui check :authentication-checkbox)
  (verify (->> :proxy-location (tasks/ui gettextvalue) clojure.string/blank?)))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-1371632"]
              :description "Given a system is unregistered
    and I run subscription-manager-gui
    and I click on 'System' -> 'Configure proxy'
When I click on 'I would like to connect via an HTTP Proxy'
 and I click on 'Use Authentication with HTTP Proxy'
Then I should see a button 'Test connection' being disabled."}}
  test_connection_button_is_blocked_before_all_fields_are_set
  [_]
  (tasks/restart-app :force-kill? true)
  (tasks/disableproxy)
  (try+ (tasks/unregister)
        (catch [:type :not-registered] _))
  (tasks/ui click :configure-proxy)
  (tasks/ui waittillwindowexist :proxy-config-dialog 60)
  (tasks/ui check :proxy-checkbox)
  (tasks/ui check :authentication-checkbox)
  (verify (not (tasks/ui hasstate :test-connection "enabled"))))

(defn ^{Test {:groups ["proxy"
                       "tier2"
                       "blockedByBug-1395684"]
              :description "Given a system is unregistered
    and I run subscription-manager-gui
    and I click on 'Register' button
    and I click on 'I would like to connect via an HTTP Proxy'
When I click on 'Use Authentication with HTTP Proxy'
 and I leave fields 'user' and 'password' blank
 and I click on the button 'Save'
 and I click on the button 'Next' in the 'System Registration' dialog
Then I should see the message 'Proxy connection failed, please check your settings.'
 and no traceback should appear in the log file."}}
  error_dialog_when_registering_via_proxy
  [_]
  (tasks/restart-app :force-kill? true)
  (tasks/disableproxy)
  (try+ (tasks/unregister)
        (catch [:type :not-registered] _))
  (tasks/ui click :register-system)
  (tasks/ui click :configure-proxy)
  (tasks/ui waittillwindowexist :proxy-config-dialog 60)
  (tasks/ui check :proxy-checkbox)
  (tasks/ui uncheck :authentication-checkbox)
  (tasks/ui settextvalue :proxy-location (str (@config :basicauth-proxy-hostname)
                                              (when (@config :basicauth-proxy-port)
                                                (str ":" (@config :basicauth-proxy-port)))))
  (tasks/ui click :close-proxy)
  (tasks/ui waittillguinotexist :proxy-config-dialog 60)
  (verify (-> (get-logging @clientcmd
                           rhsm-log
                           "error_dialog_when_registering_via_proxy"
                           nil
                           (tasks/ui click :register))
              (.contains "Traceback (most recent call last)")
              not))
  (let [thrown-error (try+ (tasks/checkforerror) (catch Object e (:type e)))]
    (verify (= thrown-error :proxy-connection-failed))))

(defn ^{Test {:groups ["proxy"
                       "tier3"
                       "blockedByBug-920551"]}}
  test_invalid_proxy_restart
  "Test to check whether traceback is thrown when an invalid proxy is configured and sub-man is restarted"
  [_]
  (setup nil)
  (disable_proxy nil)
  (tasks/enableproxy "doesnotexist.redhat.com")
  (let [output (get-logging @clientcmd
                            ldtpd-log ; Changed to
                                      ; ldtpd-log from rhsm-log
                                      ; based on comments on 920551
                            "check_for_traceback"
                            nil
                            (tasks/restart-app))]
    (verify (not (substring? "Traceback" output))))
  (tasks/ui guiexist :main-window)
  (disable_proxy nil))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  cleanup [_]
  (skip-if-bz-open "1142918")
  (assert-valid-testing-arch)
  (disable_proxy nil)
  (tasks/restart-app))

(gen-class-testng)
