(ns com.redhat.qe.sm.gui.tests.firstboot-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations AfterClass BeforeClass BeforeGroups Test]))
  
(defn ^{BeforeClass {:groups ["setup"]}}
  start_firstboot [_]
  (tasks/start-firstboot)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :license-yes)
  (tasks/ui click :firstboot-forward)
  (tasks/ui click :register-now)
  (tasks/ui click :firstboot-forward))

(defn reset_firstboot []
  (kill_firstboot nil)
  (.runCommand @clientcmd "subscription-manager clean")
  (start_firstboot nil))
  
(defn conf-file-value [k]
  (->> (.getStdout (.runCommandAndWait @clientcmd (str "grep " k " /etc/rhsm/rhsm.conf"))) (re-split #"=") last .trim))  
  
(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_auth [_]
  (let [hostname (@config :basicauth-proxy-hostname)
        port (@config :basicauth-proxy-port)
        username (@config :basicauth-proxy-username)
        password (@config :basicauth-proxy-password)]
    (tasks/enableproxy-auth hostname port username password true)
    (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      (verify (= config-file-hostname hostname))
      (verify (= config-file-port port))
      (verify (= config-file-user username))
      (verify (= config-file-password password)) )))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_noauth [_]
  (let [hostname (@config :noauth-proxy-hostname)
        port (@config :noauth-proxy-port)]
    (tasks/enableproxy-noauth hostname port true)
    (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      (verify (= config-file-hostname hostname))
      (verify (= config-file-port port)) 
      (verify (= config-file-user ""))
      (verify (= config-file-password "")) )))
      
(defn ^{Test {:groups ["firstboot"]}}
  firstboot_disable_proxy [_]
  (tasks/disableproxy true)
  (let [config-file-hostname (conf-file-value "proxy_hostname")
          config-file-port (conf-file-value "proxy_port")
          config-file-user (conf-file-value "proxy_user")
          config-file-password (conf-file-value "proxy_password")]
      (verify (= config-file-hostname ""))
      (verify (= config-file-port ""))
      (verify (= config-file-user ""))
      (verify (= config-file-password "")) ))

(defn ^{AfterClass {:groups ["setup"]}}
  kill_firstboot [_]
  (.runCommand @clientcmd "killall -9 firstboot")
  (tasks/sleep 5000))

(gen-class-testng)
