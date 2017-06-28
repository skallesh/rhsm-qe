(ns rhsm.api.tests.entitlement_tests
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

(def http-options {:timeout 3000
                   :insecure? true
                   :accept :json
                   :content-type :json
                   :keepalive 30000})

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'tree' using busctl for com.redhat.RHSM1
Then the response contains 'Entitlement' node
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  entitlement_object_is_available
  [ts]
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.RHSM1
└─/com
  └─/com/redhat
    └─/com/redhat/RHSM1
      ├─/com/redhat/RHSM1/Config
      ├─/com/redhat/RHSM1/Entitlement
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
                       "tier1"]
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Entitlement
Then the methods list contains of methods 'GetStatus', 'GetPools'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  entitlement_methods
  [ts]
  (let [methods-of-entitlement-object
        (->> "busctl introspect com.redhat.RHSM1 /com/redhat/RHSM1/Entitlement"
             run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".GetStatus" ".GetPools"} methods-of-entitlement-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :description "Given a system is registered
and subscription-manager tells a status is 'Invalid'
When I call 'GetStatus' using busctl and com.redhat.RHSM1.Entitlement object
Then the response contains of keys ['status','overall_status','reasons']
  and a value of 'status' is 1
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  entitlement_status_of_invalid_subscription_using_dbus
  [ts]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command (format "subscription-manager register  --username %s --password %s --org=%s" (@config :username) (@config :password) (@config :owner-key)))
  (let [response (-> "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Entitlement com.redhat.RHSM1.Entitlement GetStatus"
                     run-command)]
    (verify (= (:exitcode response) 0))
    (verify (=  (:stderr response) ""))
    (let [[parsed-response rest-of-the-string] (-> response :stdout (.trim) dbus/parse)
          keys-of-the-parsed-response (-> parsed-response keys set)
          status-of-the-response (-> parsed-response (get "status"))
          overall-status (-> parsed-response (get "overall_status"))]
      (verify (= rest-of-the-string ""))
      (verify (= keys-of-the-parsed-response #{"status" "overall_status" "reasons"}))
      (verify (= status-of-the-response 1))
      (verify (= overall-status "Invalid")))))

(gen-class-testng)
