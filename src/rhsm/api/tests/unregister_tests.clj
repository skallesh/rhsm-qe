(ns rhsm.api.tests.unregister_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           auth-proxyrunner
                                           noauth-proxyrunner)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)])
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.tools :as tools]
            [clojure.string :as s]
            [clojure.data.json :as json]
            [clojure.core.match :refer [match]]
            [rhsm.dbus.parser :as dbus])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            DataProvider]
           org.testng.SkipException
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(def original-candlepin-hostname (atom ""))
(def original-proxy-hostname (atom ""))
(def original-proxy-port (atom ""))
(def original-proxy-user (atom ""))
(def original-proxy-password (atom ""))
(def original-no-proxy (atom ""))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup
  [_]
  "installs scripts usable for playing with a file /etc/rhsm/rhsm.conf"
  (tools/run-command "mkdir -p ~/bin")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/get-config-value.py > get-config-value.py")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/set-config-value.py > set-config-value.py")
  (tools/run-command "chmod 755 ~/bin/get-config-value.py")
  (tools/run-command "chmod 755 ~/bin/set-config-value.py")
  (letfn [(read-value [name] (-> (format "~/bin/get-config-value.py /etc/rhsm/rhsm.conf server %s" name)
                                 tools/run-command
                                 :stdout
                                 clojure.string/trim))]
    (reset! original-candlepin-hostname (read-value "hostname"))
    (reset! original-proxy-hostname (read-value "proxy_hostname"))
    (reset! original-proxy-port (read-value "proxy_port"))
    (reset! original-proxy-user (read-value "proxy_user"))
    (reset! original-proxy-password (read-value "proxy_password"))
    (reset! original-no-proxy (read-value "no_proxy"))))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  set_config_values_to_original_ones
  [ts]
  "set config values back to original ones"
  (log/info "set config values back to original ones.")
  (letfn [(set-value [name value] (tools/run-command (format "~/bin/set-config-value.py /etc/rhsm/rhsm.conf server %s '%s'" name value)))]
    (set-value "hostname" @original-candlepin-hostname)
    (set-value "proxy_hostname" @original-proxy-hostname)
    (set-value "proxy_port" @original-proxy-port)
    (set-value "proxy_user" @original-proxy-user)
    (set-value "proxy_password" @original-proxy-password)
    (set-value "no_proxy" @original-no-proxy))
  (-> "systemctl restart rhsm.service" tools/run-command))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :description "Given a dbus service rhsm is active
When I call 'tree' using busctl for com.redhat.RHSM1
Then the response contains of 'Ungregister' node
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_object_is_available
  [ts run-command locale]
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.RHSM1
└─/com
  └─/com/redhat
    └─/com/redhat/RHSM1
      ├─/com/redhat/RHSM1/Config
      ├─/com/redhat/RHSM1/Entitlement
      ├─/com/redhat/RHSM1/Unregister
      └─/com/redhat/RHSM1/RegisterServer
"
  (let [list-of-dbus-objects (->> "busctl tree com.redhat.RHSM1"
                                  run-command
                                  :stdout
                                  (re-seq #"(├─|└─)/com/redhat/RHSM1/([^ ]+)")
                                  ;;  (["├─/com/redhat/RHSM1/Config\n" "├─" "Config\n"]
                                  ;;   ["└─/com/redhat/RHSM1/RegisterServer\n" "└─" "RegisterServer\n"])
                                  (map (fn [xs] (nth xs 2)))
                                  (map clojure.string/trim)
                                  (into #{}))]
    (verify (clojure.set/subset? #{"Entitlement"} list-of-dbus-objects))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :dependsOnMethods ["unregister_object_is_available"]
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Unregister
Then the methods list contains of methods 'Unregister', 'Introspect'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_methods
  [ts run-command locale]
  (let [methods-of-the-object
        (->> (format "busctl --system introspect com.redhat.RHSM1 /com/redhat/RHSM1/Unregister")
             run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".Unregister" ".Introspect"} methods-of-the-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :dependsOnMethods ["unregister_methods"]
              :description "Given a system is registered
When I call 'Unregister' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Unregister
Then the system is unregistered
   and 'subscription-manager status' returns 'Overall Status: Unknown'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_using_dbus
  [ts run-command locale]
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:''"))]
    (verify (.contains stdout "method return time="))
    (verify (s/blank? stderr))
    (verify (= 0 exitcode)))
  (let [{:keys [stdout stderr exitcode]} (run-command "subscription-manager status")]
    (verify (.contains stdout "Overall Status: Unknown"))
    (verify (s/blank? stderr))
    (verify (= 1 exitcode))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :dependsOnMethods ["unregister_methods"]
              :description "Given a system is registered
When I call 'Unregister' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Unregister
   and I call the same method again
Then the response tells that it is necessary to register a consumer first
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_twice_using_dbus
  [ts run-command locale]
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:'en'"))]
    (verify (.contains stdout "method return time="))
    (verify (s/blank? stderr))
    (verify (= 0 exitcode)))
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:'en'"))]
    (verify (s/blank? stdout))
    (verify (.contains stderr "This object requires the consumer to be registered before it can be used."))
    (verify (= 1 exitcode))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier2"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered
When I call 'Unregister' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Unregister
   and I call the same method again
Then the response tells that it is necessary to register a consumer first
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_twice_using_dbus
  [ts run-command locale]
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:'en'"))]
    (verify (.contains stdout "method return time="))
    (verify (s/blank? stderr))
    (verify (= 0 exitcode)))
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:'en'"))]
    (verify (s/blank? stdout))
    (verify (.contains stderr "This object requires the consumer to be registered before it can be used."))
    (verify (= 1 exitcode))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier2"
                       "blockedByBug-1516439"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered
   and proxy is used
   and a candlepin server is down
When I call 'Unregister' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Unregister
Then the response tells that it is necessary to register a consumer first
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  unregister_using_dbus_when_candlepin_is_down_and_proxy_is_used
  [ts run-command locale]
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (letfn [(set-value [name value] (-> (format "~/bin/set-config-value.py /etc/rhsm/rhsm.conf server %s '%s'" name value)
                                      run-command))]
    (set-value "hostname" (str @original-candlepin-hostname ".test"))
    (set-value "proxy_hostname" (@config :noauth-proxy-hostname))
    (set-value "proxy_port"     (@config :noauth-proxy-port))
    (set-value "proxy_user" "")
    (set-value "proxy_password" ""))
  (-> "systemctl restart rhsm.service" run-command)
  (let [{:keys [stdout stderr exitcode]} (run-command
                                          (str "dbus-send --system --print-reply --dest='com.redhat.RHSM1'"
                                               " /com/redhat/RHSM1/Unregister "
                                               " com.redhat.RHSM1.Unregister.Unregister "
                                               " dict:string:string:'','' "
                                               " string:'en'"))]
    ;; set config values back
    (set_config_values_to_original_ones nil)
    (verify (.contains stderr "Tunnel connection failed: 500 Unable to connect"))
    (verify (s/blank? stdout))
    (verify (= 1 exitcode))))

(defn run-command
  "Runs a given command on the client using SSHCommandRunner()."
  [runner command]
  (let [result (.runCommandAndWait runner command)
        out (.getStdout result)
        err (.getStderr result)
        exit (.getExitCode result)]
    {:stdout out
     :stderr err
     :exitcode exit}))

(defn ^{DataProvider {:name "client-with-english-locales"}}
  client_with_english_locales
  "It provides a run-command for a locally run command. And locales dict to solve problems with locales."
  [_]
  (-> [[(partial run-command @clientcmd) "en_US.UTF-8"]]
      to-array-2d))

(gen-class-testng)
