(ns rhsm.gui.tests.activation-key-tests
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
            [rhsm.gui.tests.base :as base])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            DataProvider]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(defn ^{Test {:groups ["activation-key"
                       "tier1"]
              :description "Given a system is inregistered
   and an activation key with the right key exists
When I try to register with an automation key
   and I use the right Organization Name
   and I click on 'register'
Then the right subscriptions should be attached automatically."}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  register_using_activation_key
  [_ activation-key]
  (try+ (tasks/unregister) (catch [:type :not-registered] _))
  (tasks/restart-app :force-kill? true)
  (tasks/ui click :register-system)
  (tasks/ui waittillguiexist :register-dialog)
  (tasks/ui settextvalue :register-server (ctasks/server-path))
  (tasks/ui check :activation-key-checkbox)
  (tasks/ui click :default-server)
  (tasks/ui settextvalue :organization (@config :owner-key))
  (tasks/ui settextvalue :activation-key  (-> activation-key :name))
  (tasks/ui click :register)
  (tasks/ui waittillguiexist :register-dialog)
  )


(defn ^{Test {:groups ["activation-key"
                       "tier2"
                       "blockedByBug-1376014"]
              :description "Given a system is inregistered
When I try to register with an automation key
   and I use wrong org
   and I click a button 'Back'
   and I register with the right username and password
Then the right subscriptions should be attached automatically."}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]
                        }
        }
  attach_subscriptions_even_wrong_org_was_used_before
  [_]
  (try+ (tasks/unregister) (catch [:type :not-registered] _))
  (let [activation-key (ctasks/create-activation-key (@config :username)
                                                     (@config :password)
                                                    )])
  (tasks/restart-app :force-kill? true)
  (tasks/ui click :register-system)
  (tasks/ui waittillguiexist :register-dialog)
  (tasks/ui settextvalue :register-server (ctasks/server-path))
  (tasks/ui check :activation-key-checkbox)
  (tasks/ui click :default-server)
  (tasks/ui click :register)
  (tasks/ui waittillguiexist :register-dialog)
  )

(defn ^{DataProvider {:name "new-simple-activation-key"}}
  new_simple_activation_key
  "It provides simple activation key without any pool attached."
  [_]
  (let [response (ctasks/create-activation-key
                  (@config :username)
                  (@config :password)
                  (@config :owner-key)
                  (format "rhsm-gui-tests-activation-key-%d" (System/currentTimeMillis)))]
    ;;(ctasks/list-activation-keys (@config :username) (@config :password))
    (to-array-2d [[response]])))

(gen-class-testng)
