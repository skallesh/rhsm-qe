(ns rhsm.dbus.tests.activation-key-tests
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
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            [rhsm.gui.tests.activation-key-tests :as atests]
            [rhsm.dbus.parser :as dbus]
            [clojure.string :as str])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            DataProvider]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(defn register-socket
  "provides a socket that is used by DBus RHSM Register service"
  [ts]
  "response:   s \"unix:abstract=/var/run/dbus-XP0szXbntD,guid=e234053b12b7e78b2c48cac758a60d17\""
  (-> (->> "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/RegisterServer com.redhat.RHSM1.RegisterServer Start"
           run-command
           :stdout
           (re-seq #"abstract=([^,]+)"))
      (nth 0)
      (nth 1)))

(defn ^{Test {:groups ["activation-key"
                       "tier2"]
              :description "Given a system is unregistered
   and there is a simple activation key for my account
When I 
"
              :dataProvider "new-simple-activation-key"}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  register_using_activation_key
  [ts activation-key]
  (run-command "subscription-manager unregister")
  (let [socket (register-socket ts)]
    (let [response (-> (format "busctl --address=unix:abstract=%s call com.redhat.RHSM1 /com/redhat/RHSM1/Register com.redhat.RHSM1.Register RegisterWithActivationKeys 'sa(s)a{ss}' %s 1 %s 0" socket (@config :owner-key) (-> activation-key :name))
                       run-command)]
      (verify (=  (:stderr response) ""))
      (let [[parsed-response rest-string] (-> response :stdout (.trim) dbus/parse)]
        (verify (->> rest-string (= "")))
        (verify (->> parsed-response keys (= ["content" "status" "headers"])))
        (verify (-> parsed-response (get "status") (= 200)))))))

(defn ^{DataProvider {:name "new-simple-activation-key"}}
  new_simple_activation_key
  "It provides simple activation key without any pool attached."
  [_]
  (let [response (ctasks/create-activation-key
                  (@config :username)
                  (@config :password)
                  (@config :owner-key)
                  (format "rhsm-dbus-tests-activation-key-%d" (System/currentTimeMillis)))]
    (to-array-2d [[response]])))

(gen-class-testng)
