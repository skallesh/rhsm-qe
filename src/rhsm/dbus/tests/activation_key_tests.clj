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
  [_]
  "provides a socket that is used by DBus RHSM Register service"
  (-> "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/RegisterServer com.redhat.RHSM1.RegisterServer Start"
      run-command
      :stdout
      (str/split #",guid=")
      first
      (str/split #"unix:abstract=")
      second))
(defn ^{Test {:groups ["activation-key"
                       "tier1"]
              :description "Given a system is unregistered
   and there is a simple activation key for my account
When I 
"
              :dataProvider "new-simple-activation-key"
              }
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}
        }
  register_using_activation_key
  [ts activation-key]
  (run-command "subscription-manager unregister")
  (let [socket (register-socket ts)]
    (-> (format "busctl --address=unix:abstract=%s call com.redhat.RHSM1 /com/redhat/RHSM1/Register com.redhat.RHSM1.Register RegisterWithActivationKeys 'sa(s)a{ss}' %s 1 %s 0" socket (@config :owner-key) (-> activation-key :name))
        run-command)))



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
