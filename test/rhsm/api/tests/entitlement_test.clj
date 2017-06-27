(ns rhsm.api.tests.entitlement-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.api.tests.entitlement_tests :as tests]
             [rhsm.gui.tasks.test-config :refer [config]]
             [org.httpkit.client :as http]
             [rhsm.api.rest :as rest]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.rest.tests.entitlement-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest entitlement-object-is-availabe-test
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.RHSM1
└─/com
  └─/com/redhat
    └─/com/redhat/RHSM1
      ├─/com/redhat/RHSM1/Config
      └─/com/redhat/RHSM1/RegisterServer
"
  (let [list-of-dbus-objects (->> "busctl tree com.redhat.RHSM1"
                             tools/run-command
                             :stdout
                             (re-seq #"(├─|└─)/com/redhat/RHSM1/([^ ]+)")
                             ;;  (["├─/com/redhat/RHSM1/Config\n" "├─" "Config\n"]
                             ;;   ["└─/com/redhat/RHSM1/RegisterServer\n" "└─" "RegisterServer\n"])
                             (map (fn [xs] (nth xs 2)))
                             (map clojure.string/trim)
                             (into #{}))]
    (is (clojure.set/subset? #{"Entitlement"} list-of-dbus-objects))))

(deftest entitlement-methods-inspection-test
  (let [methods-of-entitlement-object
        (->> "busctl introspect com.redhat.RHSM1 /com/redhat/RHSM1/Entitlement"
             tools/run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (is (clojure.set/subset? #{".GetStatus" ".GetPools"} methods-of-entitlement-object))))

(deftest GetStatus-entitlement-method
  (->> "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Entitlement com.redhat.RHSM1.Entitlement GetStatus"
       tools/run-command
       )
  )

