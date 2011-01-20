(ns sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
        [error.handler :only (handle-type with-handlers expect recover-by)]
	 sm.gui.ldtp)
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(defn- do-to-all-rows-in [view f]
  (let [subscription-list (for [row (range (action getrowcount view))]
                            (action getcellvalue view row 0))]
    (doseq [item subscription-list]
      (f item))))

(defn ^{BeforeClass {}}
  register [_]
  (with-handlers [(handle-type :already-registered [e]
                               (recover-by :unregister-first))]
    (sm.gui.tasks/register (@config :username) (@config :password))))

(defn ^{Test {:groups ["subscribe"]}}
  subscribe_all [_]
  (tasks/search)
  (do-to-all-rows-in :all-subscriptions-view
                  (fn [subscription]
                    (with-handlers [(expect :subscription-not-available)
                                    (handle-type :wrong-consumer-type [e]
                                                 (recover-by :log-warning))]
                      (tasks/subscribe subscription)))))

(defn ^{Test {:groups ["subscribe"]
              :dependsOnMethods [ "subscribe_all"]}}
  unsubsubscribe_all [_]
  (action selecttab :my-subscriptions)
  (do-to-all-rows-in :my-subscriptions-view
                     (fn [subscription] (with-handlers [(ignore-type :not-subscribed)]
                                         (tasks/unsubscribe subscription)))))

(gen-class-testng)
