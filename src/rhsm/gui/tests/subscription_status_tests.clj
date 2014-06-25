(ns rhsm.gui.tests.subscription_status_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           cli-tasks
                                           candlepin-runner)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.string :only (split)]
        rhsm.gui.tasks.tools
        clojure.pprint
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            rhsm.gui.tasks.ui
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations
            BeforeClass
            AfterClass
            Test
            DataProvider]
           org.testng.SkipException))

(def status-before-subscribe (atom {}))

(defn ^{BeforeClass {:groups ["subscription_status"
                               "tier1"]}}
  before_check_status_message
  [_]
  (try
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (tasks/restart-app :reregister? true)
    (catch Exception e
      (reset! (skip-groups :subscription_status) true)
      (throw e))))

(defn ^{Test {:groups ["subscription_status"
                       "tier1"
                       "blockedByBug-1012501"
                       "blockedByBug-1040119"]}}
  check_status_message_before_attaching
  "Asserts that status message displayed in main-window is right before subscriptions are attached"
  [_]
  (try
    (let
  	[installed-products (atom {})]
      (reset! installed-products (tasks/ui getrowcount :installed-view))
      (reset! status-before-subscribe
              (Integer. (re-find #"\d*" (tasks/ui gettextvalue :overall-status))))
      (verify (= @installed-products @status-before-subscribe)))))

(defn ^{Test {:groups ["subscription_status"
                       "tier1"
                       "blockedByBug-1012501"
                       "blockedByBug-1040119"]
              :dependsOnMethods ["check_status_message_before_attaching"]}}
  check_status_message_after_attaching
  "Asserts that status message displayed in main-window is right after attaching subscriptions"
  [_]
  (try
    (let
  	[subscribed-products (atom {})
         after-subscribe (atom {})]
      (tasks/search :match-installed? true)
      (dotimes [n 3]
        (tasks/subscribe (tasks/ui getcellvalue :all-subscriptions-view
                                   (rand-int (tasks/ui getrowcount :all-subscriptions-view)) 0)))
      (reset! subscribed-products (count (filter #(= "Subscribed" %)
                                                 (tasks/get-table-elements :installed-view 2))))
      (reset! after-subscribe (Integer. (re-find #"\d*"
                                                 (tasks/ui gettextvalue :overall-status))))
      (verify (= @after-subscribe (- @status-before-subscribe @subscribed-products))))))

(defn ^{Test {:groups ["subscription_status"
                       "tier1"
                       "blockedByBug-1012501"
                       "blockedByBug-1040119"]
              :dependsOnMethods ["check_status_message_after_attaching"]}}
  check_status_message_future_subscriptions
  "Asserts that status message displayed in main-window is right after attaching future
   subscriptions"
  [_]
  (try
    (let
  	[subscribed-products-date (atom {})
         after-date-products (atom {})
         present-date (do (tasks/ui selecttab :all-available-subscriptions)
                          (tasks/ui gettextvalue :date-entry))
         date-split (split present-date #"-")
         year (first date-split)
         month (second date-split)
         day (last date-split)
         new-year (+ (Integer. (re-find  #"\d+" year)) 1)]
      (tasks/ui enterstring :date-entry (str new-year "-" month "-" day))
      (tasks/search :match-installed? true)
      (dotimes [n 3]
        (tasks/subscribe (tasks/ui getcellvalue :all-subscriptions-view
                                   (rand-int (tasks/ui getrowcount :all-subscriptions-view)) 0)))
      (reset! subscribed-products-date (count (filter #(= "Subscribed" %)
                                                      (tasks/get-table-elements :installed-view 2))))
      (reset! after-date-products (Integer. (re-find #"\d*"
                                                     (tasks/ui gettextvalue :overall-status))))
      (verify (= @after-date-products (- @status-before-subscribe @subscribed-products-date))))))

(defn ^{Test {:groups ["subscription_status"
                       "tier1"
                       "blockedByBug-1012501"
                       "blockedByBug-1040119"]
              :dependsOnMethods ["check_status_message_future_subscriptions"]}}
  check_status_message_expired_subscriptions
  "Asserts that status message displayed in main-window is right after expiring
   attached subscriptions"
  [_]
  (if (nil?  @candlepin-runner)
    (throw (SkipException.
            (str "This test will not run on stage as a candelpin-runner cannot be created")))
    (do
      (try
        (let
            [subscribed-products-future (atom {})
             after-future-subscribe (atom {})]
          (run-command "date -s \"+1 year\"")
          (run-command "date -s \"+1 year\"" :runner @candlepin-runner)
          (tasks/restart-app)
          (reset! subscribed-products-future
                  (count (filter #(= "Subscribed" %)
                                 (tasks/get-table-elements :installed-view 2))))
          (reset! after-future-subscribe
                  (Integer. (re-find #"\d*"
                                     (tasks/ui gettextvalue :overall-status))))
          (verify (= @after-future-subscribe
                     (- @status-before-subscribe @subscribed-products-future))))
        (finally
          (run-command "date -s \"-1 year\"" :runner @candlepin-runner)
          (run-command "date -s \"-1 year\""))))))

(defn ^{AfterClass {:groups ["subscription_status"
                              "tier1"]
                     :alwaysRun true}}
  after_check_status_message
  [_]
  (let
      [time-cmd (str "systemctl stop ntpd.service;"
                   " ntpdate clock.redhat.com;"
                   " systemctl start ntpd.service")]
    (:stdout (run-command time-cmd))
    (:stdout (run-command time-cmd :runner @candlepin-runner))))

(gen-class-testng)
