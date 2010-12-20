(ns sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
        [com.redhat.qe.handler :only (handle-type with-handlers recover-by)]
	 sm.gui.ldtp)
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations BeforeClass Test]))

(defn ^{BeforeClass {}}
  (with-handlers [(handle-type :already-registered [e]
                               (recover-by :unregister-first))]
    (sm.gui.tasks/register (@config :username) (@config :password))))

(defn ^{Test {:groups ["subscribe"]}} subscribe_all [_]
  (let [all-available (for [row (range (- (action getrowcount :all-subscriptions-view) 1))]
                         (action getcellvalue :all-subscriptions-view row 0))]
    (doall (for [subscription all-available]
       (with-handlers [(handle-type :wrong-consumer-type [e] (recover-by :log-warning))]
         (tasks/subscribe subscription))))))

(gen-class-testng)
