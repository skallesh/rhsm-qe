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

(defn conf-file-value [k]
  (->> (.getStdout (.runCommandAndWait @clientcmd (str "grep " k " /etc/rhsm/rhsm.conf"))) (re-split #"=") last .trim))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_auth [_]
  (let [hostname (@config :basicauth-proxy-hostname)
        port (@config :basicauth-proxy-port)
        username (@config :basicauth-proxy-username)
        password (@config :basicauth-proxy-password)]
    (tasks/enableproxy-auth hostname port username password)
    ;;(println "finished ui")
    (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      ;;(println "finished ssh")
      (verify (= config-file-hostname hostname))
      (verify (= config-file-port port))
      (verify (= config-file-user username))
      (verify (= config-file-password password))
      ;;(println "finished vierify")
      )))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy_noauth [_]
  (let [hostname (@config :noauth-proxy-hostname)
        port (@config :noauth-proxy-port)]
    (tasks/enableproxy-noauth hostname port)
    (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      (verify (= config-file-hostname hostname))
      (verify (= config-file-port port)) 
      (verify (= config-file-user ""))
      (verify (= config-file-password "")) )))
      
(defn ^{Test {:groups ["proxy"]
              :dependsOnMethods ["enable_proxy_auth" "enable_proxy_noauth"]}}
  disable_proxy [_]
  (tasks/disableproxy)
  (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      (verify (= config-file-hostname ""))
      (verify (= config-file-port ""))
      (verify (= config-file-user ""))
      (verify (= config-file-password "")) ))
      
(gen-class-testng)
