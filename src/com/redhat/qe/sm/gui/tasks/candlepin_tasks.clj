(ns com.redhat.qe.sm.gui.tasks.candlepin-tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       cli-tasks
                                                       auth-proxyrunner
                                                       noauth-proxyrunner)]
        [error.handler :only (add-recoveries raise)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.contrib.string :only (trim)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.rest :as rest])
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [com.redhat.qe.sm.cli.tasks CandlepinTasks]
           [com.redhat.qe.sm.base SubscriptionManagerBaseTestScript]))

(defn server-url
  "Returns the server url as used by the automation."
  []
  (SubscriptionManagerBaseTestScript/sm_serverUrl))

; Not a candlepin task, but sticking this here.
(defn get-consumer-id
  "Returns the consumer id if registered."
  []
  (let [identity 
        (trim
         (.getStdout
          (.runCommandAndWait
           @clientcmd
           "subscription-manager identity | grep identity | cut -f 2 -d :")))]
    (if (= identity "")
      nil
      identity)))

(defn get-consumer-owner-key
  "Looks up the consumer's owner by consumer-id."
  ([consumerid]
     (:key (rest/get
            (str (server-url) "/consumers/" consumerid "/owner")
            (@config :username)
            (@config :password))))
  ([]
     (get-consumer-owner-key (get-consumer-id))))

(defn list-available
  "Gets a list of all available pools for a given owner and consumer."
  ([owner consumerid]
     (rest/get (str (server-url) "/owners/" owner "/pools?consumer=" consumerid)
               (@config :username)
               (@config :password)))
  ([]
     (list-available (get-consumer-owner-key) (get-consumer-id))))

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


