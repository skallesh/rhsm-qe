(ns rhsm.gui.tasks.test-config
  (:import [com.redhat.qe.auto.testng TestScript]
           [com.redhat.qe.tools SSHCommandRunner]
           [rhsm.cli.tasks SubscriptionManagerTasks]
           [rhsm.cli.tasks CandlepinTasks]
           [rhsm.gui.tasks SSLUtilities]))

(defprotocol Defaultable
  (default [this] "returns the default value if the key is not found")
  (mapkey [this] "returns the key to get the value"))

(defrecord DefaultMapKey[key default]
  Defaultable
  (default [this] (:default this))
  (mapkey [this] (:key this)))

(extend-protocol Defaultable
  java.lang.String
  (default [this] nil)
  (mapkey [this] this))

(defn property-map [map]
  (zipmap (keys map)
          (for [v (vals map)]
            (let [val (System/getProperty (mapkey v) (default v))]
              (if (= "" val) nil val)))))

(defn get-properties []
  (let [m (property-map {
                         :basicauth-proxy-hostname "sm.basicauthproxy.hostname"
                         :basicauth-proxy-password "sm.basicauthproxy.password"
                         :basicauth-proxy-port "sm.basicauthproxy.port"
                         :basicauth-proxy-username "sm.basicauthproxy.username"
                         :binary-path (DefaultMapKey. "sm.gui.binary" "subscription-manager-gui")
                         :client-hostname "sm.client1.hostname"
                         :server-hostname "sm.server.hostname"
                         :server-port "sm.server.port"
                         :server-prefix "sm.server.prefix"
                         :firstboot-binary-path (DefaultMapKey. "sm.firstboot.binary" "firstboot")
                         :ldtpd-source-url (DefaultMapKey. "sm.ldtpd.sourceUrl" nil)
                         :noauth-proxy-hostname "sm.noauthproxy.hostname"
                         :noauth-proxy-port "sm.noauthproxy.port"
                         :owner-key "sm.client1.org"
                         :password "sm.client1.password"
                         :password1 "sm.client2.password"
                         :ssh-key-passphrase "sm.sshkey.passphrase"
                         :ssh-key-private (DefaultMapKey. "sm.sshkey.private" ".ssh/id_auto_dsa")
                         :ssh-timeout "sm.ssh.emergencyTimeoutMS"
                         :ssh-user (DefaultMapKey. "sm.ssh.user" "root")
                         :username "sm.client1.username"
                         :username1 "sm.client2.username"
                         })]
       (merge m (property-map
                 {:ldtp-url (DefaultMapKey. "sm.ldtp.url"
                              (str "http://" (m :client-hostname) ":4118"))}))))
(def config (atom {}))
(def clientcmd (atom nil))
(def cli-tasks (atom nil))
(def candlepin-tasks (atom nil))
(def auth-proxyrunner (atom nil))
(def noauth-proxyrunner (atom nil))
(def candlepin-runner (atom nil))

(defn init []
  (TestScript.) ;;sets up logging, reads properties
  (swap! config merge (get-properties))
  ;; client command runner to run ssh commands on the rhsm client box
  (reset! clientcmd (SSHCommandRunner. (@config :client-hostname)
                                       (@config :ssh-user)
                                       (@config :ssh-key-private)
                                       (@config :ssh-key-passphrase)
                                       nil))
  (when (@config :ssh-timeout)
    (.setEmergencyTimeout @clientcmd (Long/valueOf (@config :ssh-timeout))))
  ;; client command runner to run file and other tasks on rhsm client box
  (reset! cli-tasks (SubscriptionManagerTasks. @clientcmd))
  (.initializeFieldsFromConfigFile @cli-tasks)
  ;; command runner to run ssh commands on the squid proxy server (with auth)
  (reset! auth-proxyrunner (SSHCommandRunner. (@config :basicauth-proxy-hostname)
                                              (@config :ssh-user)
                                              (@config :ssh-key-private)
                                              (@config :ssh-key-passphrase)
                                              nil))
  (when (@config :ssh-timeout)
    (.setEmergencyTimeout @auth-proxyrunner (Long/valueOf (@config :ssh-timeout))))
  ;; command runner to run ssh commands on the proxy server (no authentication)
  (reset! noauth-proxyrunner (SSHCommandRunner. (@config :noauth-proxy-hostname)
                                                (@config :ssh-user)
                                                (@config :ssh-key-private)
                                                (@config :ssh-key-passphrase)
                                                nil))
  (when (@config :ssh-timeout)
    (.setEmergencyTimeout @noauth-proxyrunner (Long/valueOf (@config :ssh-timeout))))
  ;; command runner to run ssh commands on the candlepin server
  (comment
  (reset! candlepin-runner (SSHCommandRunner. (@config :server-hostname)
                                              (@config :ssh-user)
                                              (@config :ssh-key-private)
                                              (@config :ssh-key-passphrase)
                                              nil))
  (when (@config :ssh-timeout)
    (.setEmergencyTimeout @candlepin-runner (Long/valueOf (@config :ssh-timeout)))))
  ;; instantiate CandlepinTasks
  (reset! candlepin-tasks (CandlepinTasks.))
  ;; turn off SSL Checking so rest API works
  (SSLUtilities/trustAllHostnames)
  (SSLUtilities/trustAllHttpsCertificates))
