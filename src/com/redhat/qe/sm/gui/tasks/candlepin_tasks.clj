(ns com.redhat.qe.sm.gui.tasks.candlepin-tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       cli-tasks
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [error.handler :only (add-recoveries raise)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.contrib.str-utils :only (re-split)]
        gnome.ldtp)
  (:require [clojure.contrib.logging :as log]
            com.redhat.qe.sm.gui.tasks.ui)
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [com.redhat.qe.sm.cli.tasks CandlepinTasks]
           [com.redhat.qe.sm.base SubscriptionManagerBaseTestScript]))

(defn server-url
  "Returns the server url as used by the automation."
  []
  (SubscriptionManagerBaseTestScript/sm_serverUrl))

(defn get-owners
  "Given a username and password, this function returns a list
  of owners associated with that user"
  [username password]
  (seq (CandlepinTasks/getOrgsKeyValueForUser username
                                              password
                                              (server-url)
                                              "displayName")))

(defn get-owner-display-name
  "Given a owner key (org key) this returns the owner's display name"
  [username password orgkey]
  (CandlepinTasks/getOrgDisplayNameForOrgKey  username
                                              password
                                              (server-url)
                                              orgkey))

(defn get-pool-id
  "Get the pool ID for a given subscription/contract pair."
  [username password orgkey subscription contract]
  (CandlepinTasks/getPoolIdFromProductNameAndContractNumber username
                                                            password
                                                            (server-url)
                                                            orgkey
                                                            subscription
                                                            contract))
(defn multi-entitlement?
  "Returns true if the subscription can be entitled to multiple times."
  [username password pool]
  (CandlepinTasks/isPoolProductMultiEntitlement username
                                                password
                                                (server-url)
                                                pool))


