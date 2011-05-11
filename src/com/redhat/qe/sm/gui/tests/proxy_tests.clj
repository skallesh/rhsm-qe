(ns com.redhat.qe.sm.gui.tests.proxy-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	      [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.str-utils :only (re-split)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks])
  (:import [org.testng.annotations Test BeforeClass]))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn register []
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))
    (verify (action exists? :unregister-system))))

(defn conf-file-value [k]
  (->> (.getStdout (.runCommandAndWait @clientcmd (str "grep " k " /etc/rhsm/rhsm.conf"))) (re-split #"=") last .trim))

(defn verify-conf-proxies [hostname port user password]
  (let [config-file-hostname  (conf-file-value "proxy_hostname")
        config-file-port      (conf-file-value "proxy_port")
        config-file-user      (conf-file-value "proxy_user")
        config-file-password  (conf-file-value "proxy_password")]
    (verify (= config-file-hostname hostname))
    (verify (= config-file-port port)) 
    (verify (= config-file-user user))
    (verify (= config-file-password password))))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_auth [_]
  (let [hostname  (@config :basicauth-proxy-hostname)
        port      (@config :basicauth-proxy-port)
        username  (@config :basicauth-proxy-username)
        password  (@config :basicauth-proxy-password)]
    (tasks/enableproxy-auth hostname port username password)
    (verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_noauth [_]
  (let [hostname  (@config :noauth-proxy-hostname)
        port      (@config :noauth-proxy-port)]
    (tasks/enableproxy-noauth hostname port)
    (verify-conf-proxies hostname port "" "")))
      
(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth" "enable_proxy_noauth"]}}
  disable_proxy [_]
  (tasks/disableproxy)
  (verify-conf-proxies "" "" "" ""))
      
(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth"]}}
  proxy_auth_connect [_]
  (enable_proxy_auth nil)
  (register)
  ;;TODO verify this also by connecting to the server and checking the proxy log
  )
    
(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_noauth"]}}
  proxy_noauth_connect [_]
  (enable_proxy_noauth nil)
  (register)
  ;;TODO verify this also by connecting to the server and checking the proxy log
  )

;;TODO add a disabled case to check for no connectivity through a proxy

(gen-class-testng)
