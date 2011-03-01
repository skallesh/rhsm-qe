(ns com.redhat.qe.sm.gui.tasks.test-config
  (:import [com.redhat.qe.auto.testng TestScript]
	         [com.redhat.qe.tools SSHCommandRunner]
           [com.redhat.qe.sm.cli.tasks SubscriptionManagerTasks]))

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
      (for [v (vals map) ]
        (System/getProperty (mapkey v) (default v)))))

(defn get-properties []
     (let [m (property-map {:binary-path (DefaultMapKey. "sm.gui.binary" "subscription-manager-gui")
			    :client-hostname "sm.client1.hostname"
			    :username "sm.client1.username"
			    :password "sm.client1.password"
          :ssh-user (DefaultMapKey. "sm.ssh.user" "root")
          :ssh-key-private (DefaultMapKey."sm.sshkey.private" ".ssh/id_auto_dsa")
			    :ssh-key-passphrase "sm.sshkey.passphrase"
          :basicauth-proxy-hostname "sm.basicauthproxy.hostname"
          :basicauth-proxy-port "sm.basicauthproxy.port"
          :basicauth-proxy-username "sm.basicauthproxy.username"
          :basicauth-proxy-password "sm.basicauthproxy.password"
          :noauth-proxy-hostname "sm.noauthproxy.hostname"
          :noauth-proxy-port "sm.noauthproxy.port"})]
       (merge m (property-map
		 {:ldtp-url (DefaultMapKey. "sm.ldtp.url"
			      (str "http://" (m :client-hostname) ":4118"))}))))
(def config (atom {}))
(def clientcmd (atom nil))
(def cli-tasks (atom nil))
(defn init []
  (TestScript.) ;;sets up logging, reads properties
  (swap! config merge (get-properties))
  (reset! clientcmd (SSHCommandRunner. (@config :client-hostname)
				       (@config :ssh-user)
				       (@config :ssh-key-private)
				       (@config :ssh-key-passphrase)
				       nil))
  (reset! cli-tasks (SubscriptionManagerTasks. @clientcmd)))
