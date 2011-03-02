(ns com.redhat.qe.sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	      [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))))

(defn ^{Test {:groups ["subscribe"]}}
  subscribe_all [_]
  (tasks/search {})
  (tasks/do-to-all-rows-in :all-subscriptions-view 0
                  (fn [subscription]
                    (with-handlers [(ignore :subscription-not-available)
                                    (handle :wrong-consumer-type [e]
                                                 (recover e :log-warning))]
                      (tasks/subscribe subscription)))))

(defn ^{Test {:groups ["subscribe"]
              :dependsOnMethods [ "subscribe_all"]}}
  unsubscribe_all [_]
  (tasks/ui selecttab :my-subscriptions)
  (tasks/do-to-all-rows-in :my-subscriptions-view 0
                     (fn [subscription] (with-handlers [(ignore :not-subscribed)]
                                         (tasks/unsubscribe subscription)
                                         (verify (= (tasks/ui rowexist? :my-subscriptions-view subscription) false))))))

(gen-class-testng)
