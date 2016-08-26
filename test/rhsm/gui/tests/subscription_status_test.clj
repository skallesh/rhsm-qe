(ns rhsm.gui.tests.subscription-status-test
  (:use gnome.ldtp
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.subscription_status_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.ui :as ui]
             [clojure.core.match :refer [match]]
             [clojure.set :refer [subset?]]
             [rhsm.gui.tests.base :as base])
  (:import   org.testng.SkipException))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/before_check_status_message nil)
                      (f)
                      (tests/after_check_status_message nil)))

;; (require '[clojure.reflect :as cr]')
;; <stoner> (require '[clojure.pprint :as cpp]')
;; <stoner> (cpp/print-table (->> (cr/reflect {}) :members (sort-by :name)))

(deftest window-subscriptions-table-test
  (testing "Test that verifies that main window contains Subscriptions Table with proper label."
    (tasks/restart-app)
    (case (tools/get-release)
      "RHEL6" (is (subset? #{"ttblMySubscriptionsView"} (set (tasks/ui getobjectlist :main-window))))
      "RHEL7" (is (subset? #{"ttblMySubscriptionsView"} (set (tasks/ui getobjectlist :main-window))))
      "default")))

(deftest check_status_message_after_attaching-test
  (tests/check_status_message_before_attaching nil)
  (tests/check_status_message_after_attaching nil))

(deftest check_status_future_subscription-test
  (tests/check_status_message_before_attaching nil)
  (tests/check_status_message_after_attaching nil)
  (tests/check_status_message_future_temporary_subscriptions nil))
