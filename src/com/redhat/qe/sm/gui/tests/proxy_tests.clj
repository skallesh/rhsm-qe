(ns com.redhat.qe.sm.gui.tests.proxy-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks])
  (:import [org.testng.annotations Test BeforeClass AfterClass]))

(def auth-log "/var/log/squid/access.log")
(def noauth-log "/var/log/tinyproxy.log")

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn register []
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))
    (verify (action exists? :unregister-system))))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_auth [_]
  (let [hostname  (@config :basicauth-proxy-hostname)
        port      (@config :basicauth-proxy-port)
        username  (@config :basicauth-proxy-username)
        password  (@config :basicauth-proxy-password)]
    (tasks/enableproxy-auth hostname port username password)
    (tasks/verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_noauth [_]
  (let [hostname  (@config :noauth-proxy-hostname)
        port      (@config :noauth-proxy-port)]
    (tasks/enableproxy-noauth hostname port)
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
                                     register)]
    (verify (not  (clojure.string/blank? logoutput)))))
    
(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  proxy_noauth_connect [_]
  (enable_proxy_noauth nil)
  (let [logoutput (tasks/get-logging @noauth-proxyrunner 
                                     noauth-log
                                     "proxy-noauth-connect"
                                     nil
                                     register)]
    (verify (not  (clojure.string/blank? logoutput)))))

(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["disable_proxy"]}}
  disable_proxy_connect [_]
  (disable_proxy nil)
  (let [logoutput (tasks/get-logging @auth-proxyrunner 
                                     auth-log
                                     "disabled-auth-connect"
                                     nil
                                     register)]
    (verify (clojure.string/blank? logoutput)))
  (let [logoutput (tasks/get-logging @noauth-proxyrunner 
                                     noauth-log
                                     "disabled-auth-connect"
                                     nil
                                     register)]
    (verify (clojure.string/blank? logoutput))))

(defn ^{Test {:groups ["proxy"]}}
  bad_proxy [_]
  (let [hostname  "blahblah"
        port      "666"]
    (tasks/enableproxy-noauth hostname port)
    (tasks/verify-conf-proxies hostname port "" ""))
  (let [thrown-error (with-handlers [(handle :network-error [e]
                                             (:type e))]
                       (register))]
    (verify (= thrown-error :network-error)))
  (disable_proxy nil))

(defn ^{Test {:groups ["proxy" "blockedByBug-729688"]}}
  bad_proxy_facts [_]
  (disable_proxy nil)
  (register)
  (let [hostname  "blahblah"
        port      "666"]
    (tasks/enableproxy-noauth hostname port)
    (tasks/verify-conf-proxies hostname port "" ""))
  (let [thrown-error (with-handlers [(handle :error-updating [e]
                                             (:type e))]
                       (tasks/ui click :view-system-facts)
                       (tasks/ui waittillguiexist :facts-view)
                       (tasks/ui click :update-facts)
                       (tasks/checkforerror))]
    (verify (= thrown-error :error-updating)))
  (tasks/ui click :close-facts)
  (disable_proxy nil))

(defn ^{AfterClass {:groups ["setup"]}}
  cleanup [_]
  (disable_proxy nil))

(gen-class-testng)
