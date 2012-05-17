(ns com.redhat.qe.sm.gui.tasks.candlepin-tasks
  (:use [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd)]
        [clojure.string :only (trim)])
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
  ([owner consumerid & {:keys [all?]
                        :or {all? false}}]
     (rest/get (str (server-url)
                    "/owners/" owner
                    "/pools?consumer=" consumerid
                    (if all? "&listall=true"))
               (@config :username)
               (@config :password)))
  ([all?]
     (list-available
      (get-consumer-owner-key)
      (get-consumer-id)
      :all? all?))
  ([]
     (list-available
      (get-consumer-owner-key)
      (get-consumer-id))))


(defn build-product-map
  [& {:keys [all?]
      :or {all? false}}]
  (let [everything (if all?
                     (list-available true)
                     (list-available))
        productlist (atom {})]
    (doseq [s everything]
      (doseq [p (:providedProducts s)]
        (if (nil? (@productlist (:productName p)))
          (reset! productlist
                  (assoc @productlist
                    (:productName p)
                    [(:productName s)]))
          (reset! productlist
                  (assoc @productlist
                    (:productName p)
                    (into (@productlist (:productName p)) [(:productName s)]))))))
    @productlist))


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
  (if orgkey
    (CandlepinTasks/getOrgDisplayNameForOrgKey  username
                                                password
                                                (server-url)
                                                orgkey)))

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


