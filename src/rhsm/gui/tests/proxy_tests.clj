(ns rhsm.gui.tests.proxy-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [slingshot.slingshot :only (try+ throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks])
  (:import [org.testng.annotations Test BeforeClass AfterClass]))

(def auth-log "/var/log/squid/access.log")
(def noauth-log "/var/log/tinyproxy.log")

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (try+ (tasks/unregister)
        (catch [:type :not-registered] _)))

(defn register []
  (try+ (tasks/register (@config :username) (@config :password))
        (catch [:type :already-registered]
            {:keys [unregister-first]} (unregister-first)))
  (verify (not (tasks/ui showing? :register-system))))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_auth [_]
  (let [hostname  (@config :basicauth-proxy-hostname)
        port      (@config :basicauth-proxy-port)
        username  (@config :basicauth-proxy-username)
        password  (@config :basicauth-proxy-password)]
    (tasks/enableproxy hostname :port port :user username :pass password)
    (tasks/verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_noauth [_]
  (let [hostname  (@config :noauth-proxy-hostname)
        port      (@config :noauth-proxy-port)]
    (tasks/enableproxy hostname :port port)
    (tasks/verify-conf-proxies hostname port "" "")))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth" "enable_proxy_noauth"]}}
  disable_proxy [_]
  (tasks/disableproxy)
  (tasks/verify-conf-proxies "" "" "" ""))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth"]}}
  proxy_auth_connect [_]
  (enable_proxy_auth nil)
  (let [logoutput (tasks/get-logging @auth-proxyrunner
                                     auth-log
                                     "proxy-auth-connect"
                                     nil
                                     (register))]
    (verify (not  (clojure.string/blank? logoutput)))))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  proxy_noauth_connect [_]
  (enable_proxy_noauth nil)
  (let [logoutput (tasks/get-logging @noauth-proxyrunner
                                     noauth-log
                                     "proxy-noauth-connect"
                                     nil
                                     (register))]
    (verify (not  (clojure.string/blank? logoutput)))))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["disable_proxy"]}}
  disable_proxy_connect [_]
  (disable_proxy nil)
  ;; note: if this takes forever, blank out the proxy log file.
  (let [logoutput (tasks/get-logging @auth-proxyrunner
                                     auth-log
                                     "disabled-auth-connect"
                                     nil
                                     (register))]
    (verify (clojure.string/blank? logoutput)))
  (let [logoutput (tasks/get-logging @noauth-proxyrunner
                                     noauth-log
                                     "disabled-auth-connect"
                                     nil
                                     (register))]
    (verify (clojure.string/blank? logoutput))))

(defn test_proxy [expected-message]
  (tasks/ui click :configure-proxy)
  (tasks/ui click :test-connection)
  (try+
   (let [message (tasks/ui gettextvalue :connection-status)]
     (verify (= expected-message message)))
   (finally (tasks/ui click :close-proxy))))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth"]}}
  test_auth_proxy [_]
  (test_proxy "Proxy connection succeeded"))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  test_noauth_proxy [_]
  (test_proxy "Proxy connection succeeded"))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["disable_proxy"]}}
  test_disabled_proxy [_]
  (tasks/ui click :configure-proxy)
  (tasks/ui click :test-connection)
  (try+ (verify (not (some #(= "sensitive" %)
                           (tasks/ui getallstates :test-connection))))
        (verify (= "" (tasks/ui gettextvalue :connection-status)))
        (finally (tasks/ui click :close-proxy))))

(defn ^{Test {:groups ["proxy"]}}
  test_bad_proxy [_]
  (try+
   (tasks/enableproxy "doesnotexist.redhat.com")
   (test_proxy "Proxy connection failed")
   (finally (tasks/disableproxy))))

(defn ^{Test {:groups ["proxy"]}}
  bad_proxy [_]
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
     (if (= 1 (tasks/ui guiexist :register-dialog))
       (tasks/ui click :register-cancel))
     (disable_proxy nil))))

(defn ^{Test {:groups ["proxy" "blockedByBug-729688"]}}
  bad_proxy_facts [_]
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
     (if (= 1 (tasks/ui guiexist :facts-dialog))
       (tasks/ui click :close-facts))
     (disable_proxy nil))))

(defn ^{Test {:groups ["proxy" "blockedByBug-806993"]}}
  test_proxy_formatting [_]
  (try+
   (tasks/enableproxy "http://some.host.name:1337")
   (tasks/ui click :configure-proxy)
   (tasks/ui waittillwindowexist :proxy-config-dialog 60)
   (let [location (tasks/ui gettextvalue :proxy-location)]
     (verify (not (tasks/substring? "http://" location)))
     (verify (= 2 (count (clojure.string/split location #":")))))
   (finally
     (tasks/ui click :close-proxy)
     (disable_proxy nil))))

(defn ^{AfterClass {:groups ["setup"]
                    :alwaysRun true}}
  cleanup [_]
  (disable_proxy nil)
  (tasks/restart-app))

(gen-class-testng)
