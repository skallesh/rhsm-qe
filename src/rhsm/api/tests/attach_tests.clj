(ns rhsm.api.tests.attach_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           auth-proxyrunner
                                           noauth-proxyrunner)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.tools :as tools]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [rhsm.dbus.parser :as dbus]
            [rhsm.api.rest :as rest]
            [clojure.core.match :refer [match]]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            [rhsm.gui.tests.activation-key-tests :as atests]
            [clojure.string :as str])
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

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'tree' using busctl for com.redhat.RHSM1
Then the response contains 'Attach' node
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  attach_object_is_available
  [ts]
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.RHSM1
└─/com
  └─/com/redhat
    └─/com/redhat/RHSM1
      ├─/com/redhat/RHSM1/Config
      ├─/com/redhat/RHSM1/Entitlement
      ├─/com/redhat/RHSM1/Attach
      └─/com/redhat/RHSM1/RegisterServer
"
  (let [list-of-dbus-objects (->> "busctl tree com.redhat.RHSM1"
                                  run-command
                                  :stdout
                                  (re-seq #"(├─|└─)/com/redhat/RHSM1/([^ ]+)")
                                  (map (fn [xs] (nth xs 2)))
                                  (map clojure.string/trim)
                                  (into #{}))]
    (verify (clojure.set/subset? #{"Attach"} list-of-dbus-objects))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Attach
Then the methods list contains of methods 'AutoAttach', 'PoolAttach'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  attach_methods
  [ts]
  (let [methods-of-the-object
        (->> "busctl introspect com.redhat.RHSM1 /com/redhat/RHSM1/Attach"
             run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".AutoAttach" ".Introspect" ".PoolAttach"} methods-of-the-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a system is registered without any attached pool
When I call a DBus method 'PoolAttach' at com.redhat.RHSM1.Attach object
Then the response contains of list of json strings described attached pools
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  attach_pool_using_dbus
  [ts]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (->> "subscription-manager unregister" tools/run-command)
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      tools/run-command
      :stdout
      (.contains "Registering to:")
      verify)
  (let [list-of-available-pools (rest/list-of-available-pools
                                 (ctasks/server-url)
                                 (@config :owner-key)
                                 (@config :username)
                                 (@config :password))
        ids-of-available-pools (map :id list-of-available-pools)
        num-of-wanted-pools 2
        a-few-pool-ids (take num-of-wanted-pools ids-of-available-pools)]
    (let [{:keys [stdout stderr exitcode]}
          (->> (str "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Attach com.redhat.RHSM1.Attach PoolAttach"
                    " asia{sv} "
                    (format " %d " num-of-wanted-pools) (clojure.string/join " " a-few-pool-ids)
                    " 1 " ;;quantity
                    " 0 ")
               tools/run-command)]
      (verify (= stderr ""))
      (verify (= exitcode 0))
      (let [[response-data rest] (-> stdout dbus/parse)]
        (verify (= rest ""))
        (verify (= (type response-data) clojure.lang.PersistentVector))
        (verify (= (count response-data) num-of-wanted-pools))
        (let [entitlements (map (fn [x] (-> x (str/replace #"\\\"" "\"") json/read-str)) response-data)]
          (let [ids-from-response (->> entitlements
                                       (map (fn [ent] (-> ent first (get "pool") (get "id"))))
                                       set)]
            (verify (= ids-from-response (set a-few-pool-ids)))))))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a system is registered without any attached pool
When I call a DBus method 'AutoAttach' at com.redhat.RHSM1.Attach object
with the proper Service Level
Then the response contains of keys ['status','overall_status','reasons']
  and a value of 'status' is 1
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  autoattach_pool_using_dbus
  [ts]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (->> "subscription-manager unregister" tools/run-command)
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      tools/run-command
      :stdout
      (.contains "Registering to:")
      verify)
  (let [list-of-available-pools (rest/list-of-available-pools
                                 (ctasks/server-url)
                                 (@config :owner-key)
                                 (@config :username)
                                 (@config :password))
        ids-of-available-pools (map :id list-of-available-pools)
        a-few-pool-ids (take 2 ids-of-available-pools)]
    (let [{:keys [stdout stderr exitcode]} (->> (str "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Attach com.redhat.RHSM1.Attach AutoAttach"
                               " asia{sv} "
                               " 2 " (clojure.string/join " " a-few-pool-ids)
                               " 1 " ;;quantity
                               " 0 ")
                  tools/run-command)]
      (verify (= stderr ""))
      (verify (= exitcode 0))
      )
    )
  )


(gen-class-testng)
