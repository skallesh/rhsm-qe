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
  (tasks/ui click :firstboot-forward)
  (assert ( = 1 (tasks/ui guiexist :firstboot-window "Choose Server"))))

(defn ^{AfterClass {:groups ["setup"]}}
  kill_firstboot [_]
  (.runCommand @clientcmd "killall -9 firstboot")
  (tasks/sleep 5000))

(defn reset_firstboot []
  (kill_firstboot nil)
  (.runCommand @clientcmd "subscription-manager clean")
  (start_firstboot nil))
  
(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_auth [_]
  (reset_firstboot)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
  (let [hostname (@config :basicauth-proxy-hostname)
        port (@config :basicauth-proxy-port)
        username (@config :basicauth-proxy-username)
        password (@config :basicauth-proxy-password)]
    (tasks/enableproxy-auth hostname port username password true)
    (tasks/ui click :firstboot-forward)
    (tasks/checkforerror)
    (tasks/firstboot-register (@config :username) (@config :password))
    (tasks/verify-conf-proxies hostname port username password)))

(defn ^{Test {:groups ["firstboot"]}}
  firstboot_enable_proxy_noauth [_]
  (reset_firstboot)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
  (let [hostname (@config :noauth-proxy-hostname)
        port (@config :noauth-proxy-port)]
    (tasks/enableproxy-noauth hostname port true)
    (tasks/ui click :firstboot-forward)
    (tasks/checkforerror)
    (tasks/firstboot-register (@config :username) (@config :password))
    (tasks/verify-conf-proxies hostname port "" "")))
      
(defn ^{Test {:groups ["firstboot"]}}
  firstboot_disable_proxy [_]
  (reset_firstboot)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
  (tasks/disableproxy true)
  (tasks/ui click :firstboot-forward)
  (tasks/checkforerror)
  (tasks/firstboot-register (@config :username) (@config :password))
  (tasks/verify-conf-proxies "" "" "" ""))
      
      
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=700601

(gen-class-testng)
