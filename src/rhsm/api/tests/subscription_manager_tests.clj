(ns rhsm.api.tests.subscription_manager_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config
         :only (config
                clientcmd
                auth-proxyrunner
                noauth-proxyrunner)
         :as c]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.tools :as tools]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.core.match :refer [match]]
            [rhsm.dbus.parser :as dbus]
            [clojure.java.io :as io])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            BeforeSuite
            AfterSuite
            DataProvider]
           org.testng.SkipException
           [com.redhat.qe.tools RemoteFileTasks]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup
  [_]
  "installs scripts usable for playing with a file /etc/rhsm/rhsm.conf"
  (tools/run-command "mkdir -p ~/bin")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/get-config-value.py > get-config-value.py")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/set-config-value.py > set-config-value.py")
  (tools/run-command "chmod 755 ~/bin/get-config-value.py")
  (tools/run-command "chmod 755 ~/bin/set-config-value.py"))

(defn ^{BeforeSuite {:groups ["setup"]}}
  install_monitor_systemd_service [_]
  "installs a service that can monitor a DBus signal of EntitlementStatus object"
  (tools/run-command "mkdir -p ~/bin")
  (RemoteFileTasks/createFile  (.getConnection @c/clientcmd)
                               "/root/bin/monitor-entitlement-status-changes.sh"
                               (slurp (io/file "resources/bin/monitor-entitlement-status-changes.sh"))
                               "0755")
  (RemoteFileTasks/createFile  (.getConnection @c/clientcmd)
                               "/etc/systemd/system/rhsm-monitor-entitlement-status-changed-signal.service"
                               (slurp (io/file "resources/etc/systemd/system/dbus-entitlement-status-changes-monitor.service"))
                               "0644")
  (tools/run-command "systemctl daemon-reload"))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  set_inotify_to_1
  [ts]
  "set a variable inotify back to 1 in /etc/rhsm/rhsm.conf"
  (log/info "set a variable inotify to 1 in /etc/rhsm/rhsm.conf")
  (tools/run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 1")
  (tools/run-command "systemctl stop rhsm.service")
  (tools/run-command "systemctl start rhsm.service"))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'tree' using busctl for com.redhat.SubscriptionManager
Then the response contains 'EntitlementStatus' node
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-112591"]}}
  subscription_manager_object_is_available
  [ts]
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.SubscriptionManager
└─/EntitlementStatus
"
  (let [list-of-dbus-objects (->> "busctl tree com.redhat.SubscriptionManager"
                                  tools/run-command
                                  :stdout
                                  (re-seq #"(├─|└─)/([^ ]+)")
                                  (map (fn [xs] (nth xs 2)))
                                  (map clojure.string/trim)
                                  (into #{}))]
    (verify (clojure.set/subset? #{"EntitlementStatus"} list-of-dbus-objects))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.SubscriptionManager
   and an interface /com/redhat/SubscriptionManager/EntitlementStatus
Then the methods list contains of methods 'GetStatus'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-112591"]}}
  subscription_manager_methods
  [ts]
  (let [methods-of-the-object
        (->> "busctl introspect com.redhat.SubscriptionManager --no-legend /EntitlementStatus"
             tools/run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".check_status" ".update_status" ".Introspect"} methods-of-the-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.SubscriptionManager
   and an interface /com/redhat/SubscriptionManager/EntitlementStatus
Then the methods list contains of methods 'GetStatus'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-112591"]}}
  subscription_manager_signals
  [ts]
  (let [signals-of-the-object
        (->> "busctl introspect com.redhat.SubscriptionManager --no-legend /EntitlementStatus"
             tools/run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]signal[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".entitlement_status_changed"} signals-of-the-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a system is unregistered
When I call a DBus method 'check_status' at com.redhat.SubscriptionManager.EntitlementStatus object
Then the response contains of a number the same as 'subscription-manager status' returns as 'exitcode'.
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-112591"]}}
  check_status_using_dbus
  [ts]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (-> "subscription-manager unregister" tools/run-command)
  (let [{:keys [stdout stderr exitcode]}
        (->> (str "busctl call com.redhat.SubscriptionManager /EntitlementStatus com.redhat.SubscriptionManager.EntitlementStatus check_status")
             tools/run-command)]
    (verify (= stderr ""))
    (verify (= exitcode 0))
    (let [[response-data rest] (-> stdout dbus/parse)]
      (clojure.test/is (= rest ""))
      (clojure.test/is (= (type response-data) Integer))
      (let [{:keys [stdout stderr exitcode]} (-> "subscription-manager status"
                                                 tools/run-command)]
        (verify (= 5 response-data)) ;; the system was unregistered
        ))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a system is unregistered
When I register the system
Then a DBus signal 'EntitlementStatus/entitlement_status_changed' is emited.
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-112591"]}}
  dbus_entitlement_status_changed_signal_is_emited
  [ts]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))

  (tools/run-command "subscription-manager unregister")
  ;; start a monitor service
  (tools/run-command "systemctl start rhsm-monitor-entitlement-status-changed-signal.service")
  (tools/run-command "systemctl status rhsm-monitor-entitlement-status-changed-signal.service")
  (let [start-datetime (-> "date +'%F %T'" tools/run-command  :stdout  str/trim)]
    (let [output (->> (format "subscription-manager register --username=%s --password=%s --org=%s"
                              (@config :username) (@config :password) (@config :owner-key))
                      tools/run-command
                      :stdout) ]
      (verify (.contains output "Registering to:")))
    (let [monitor-log-lines (->> (str "journalctl  -u rhsm-monitor-entitlement-status-changed-signal.service"
                                      " --since '" start-datetime "'"
                                      " --no-pager ")
                                 tools/run-command
                                 :stdout
                                 str/split-lines
                                 (filter (partial re-find #"\ssig\s"))
                                 (filter (partial re-find #"entitlement_status_changed")))
          num-of-log-lines (count monitor-log-lines)]
      (verify (= 1 num-of-log-lines)))
    (tools/run-command "subscription-manager unregister")
    (let [monitor-log-lines (->> (str "journalctl  -u rhsm-monitor-entitlement-status-changed-signal.service"
                                      " --since '" start-datetime "'"
                                      " --no-pager ")
                                 tools/run-command
                                 :stdout
                                 str/split-lines
                                 (filter (partial re-find #"\ssig\s"))
                                 (filter (partial re-find #"entitlement_status_changed")))
          num-of-log-lines (count monitor-log-lines)]
      (verify (= 2 num-of-log-lines)))))

(gen-class-testng)
