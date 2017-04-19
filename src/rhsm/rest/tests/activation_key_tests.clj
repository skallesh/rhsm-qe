(ns rhsm.rest.tests.activation-key-tests
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
            [rhsm.gui.tasks.rest :as rest]
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

(defn ^{Test {:groups ["activation-key"
                       "tier1"]}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  create_activation_key
  [ts]
  (let [activation-key
        (rest/post (format "%s/owners/%s/activation_keys" (ctasks/server-url) (@config :owner-key))
                   (@config :username)
                   (@config :password)
                   {:name (format "rhsm-rest-tests-%d" (System/currentTimeMillis))}) ]
    (let [activation-key-once-again
          (rest/get (format "%s/activation_keys/%s"
                            (ctasks/server-url)
                            (activation-key :id))
                    (@config :username)
                    (@config :password))]
      (doseq [key [:id :updated :products :name :created :autoAttach]]
        (verify (= (activation-key key)
                   (activation-key-once-again key))))
      (let [owner-in-activation-key (activation-key :owner)
            owner-in-activation-key-once-again (activation-key-once-again :owner)]
        (doseq [key [:id :key :displayName :href]]
          (verify (= (owner-in-activation-key key)
                     (owner-in-activation-key-once-again key))))))))

(defn ^{Test {:groups ["activation-key"
                       "tier1"]}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  list_activation_keys
  [ts]
  ;; #spy/d (rest/get (format "%s/activation_keys" (ctasks/server-url))
  ;;                  (@config :username)
  ;;                  (@config :password))
  )
