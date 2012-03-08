(ns com.redhat.qe.sm.gui.tests.firstboot-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+ throw+)]
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
  (assert ( = 1 (tasks/ui guiexist :firstboot-window "Choose Server"))))

(defn ^{AfterClass {:groups ["setup"]}}
  kill_firstboot [_]
  (.runCommand @clientcmd "killall -9 firstboot")
  (tasks/sleep 5000))

(defn zero-proxy-values []
  (tasks/set-conf-file-value "proxy_hostname" "")
  (tasks/set-conf-file-value "proxy_port" "")
  (tasks/set-conf-file-value "proxy_user" "")
  (tasks/set-conf-file-value "proxy_password" ""))

(defn reset_firstboot []
  (kill_firstboot nil)
  (.runCommand @clientcmd "subscription-manager clean")
  (zero-proxy-values)
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

(defn firstboot_register_invalid_user [user pass recovery]
  (reset_firstboot)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
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
     (verify  (or (tasks/fbshowing? :firstboot-user)
                  (= 1 (tasks/ui guiexist :firstboot-window "Entitlement Platform Registration")))))))

(defn ^{Test {:groups ["firstboot" "blockedByBug-642660"]}}
  firstboot_check_back_button [_]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (reset_firstboot)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
  (tasks/ui click :firstboot-forward)
  (tasks/firstboot-register (@config :username) (@config :password))
  (verify (= 0 (tasks/ui hasstate :firstboot-back "Sensitive"))))

(defn ^{Test {:groups ["firstboot" "blockedByBug-642660"]}}
  firstboot_skip_register [_]
  (kill_firstboot nil)
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (.runCommandAndWait @clientcmd (str "subscription-manager register"
                                      " --username " (@config :username)
                                      " --password " (@config :password)
                                      " --org " (@config :owner-key)))
  (start_firstboot nil)
  (tasks/ui click :register-rhn)
  (tasks/ui uncheck :rhn-classic-mode)
  (tasks/ui click :firstboot-forward)
  (verify (tasks/ui showing? :firstboot-window "My Installed Software")))

(data-driven firstboot_register_invalid_user {Test {:groups ["firstboot"]}}
  [["sdf" "sdf" :invalid-credentials]
   ["" "" :no-username]
   ["" "password" :no-username]
   ["sdf" "" :no-password]])

(data-driven firstboot_register_invalid_user {Test {:groups ["firstboot" "blockedByBug-703491"]}}
  [["badusername" "badpassword" :invalid-credentials]])


;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=742416
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=700601
;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=705170

(gen-class-testng)

