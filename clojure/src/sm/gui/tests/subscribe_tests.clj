(ns sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
        [com.redhat.qe.handler :only (handle-type with-handlers recover-by)]
	 sm.gui.ldtp)
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations BeforeClass Test]))

(defn- do-to-all-rows-in [view f]
  (let [subscription-list (for [row (range (- (action getrowcount view) 1))]
                            (action getcellvalue view row 0))]
    (doall (map f subscription-list))))

(defn ^{BeforeClass {}}
  register [_]
  (with-handlers [(handle-type :already-registered [e]
                               (recover-by :unregister-first))]
    (sm.gui.tasks/register (@config :username) (@config :password))))

(defn ^{Test {:groups ["subscribe"]}}
  subscribe_all [_]
  (do-to-all-rows-in :all-subscriptions-view
                  (fn [subscription]
                    (with-handlers [(handle-type :wrong-consumer-type [e]
                                                 (recover-by :log-warning))]
                      (tasks/subscribe subscription)))))

(defn ^{Test {:dependsOnTests "subscribe_all"}}
  unsubsubscribe_all [_]
  (do-to-all-rows-in :my-subscriptions-view (fn [subscription] (tasks/unsubscribe subscription))))

(gen-class-testng)
