(ns rhsm.gui.tests.subscribe-test
  (:use [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp
        rhsm.gui.tasks.tools)
  (:require  [clojure.test :refer :all]
             [clojure.core.match :refer [match]]
             [rhsm.gui.tests.subscribe_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/register nil)
                      (f)))

(deftest multi-contract-test
  (tests/get_multi_contract_subscriptions nil :debug true))

(deftest check_contract_selection_dates-test
  (let [subscriptions (tests/get_multi_contract_subscriptions nil :debug true)]
    (tests/check_contract_selection_dates nil #spy/d (-> subscriptions first first))))

(deftest check_multiplier_logic-test
  (let [subscriptions #spy/d (tests/get_subscriptions nil)]
    (tests/check_multiplier_logic nil (-> subscriptions first))))

(deftest unsubscribe_each-test
  (let [subscriptions (tests/get_subscriptions nil)]
    (for [subscription subscriptions]
      (tests/subscribe_each nil subscription))
    (for [subscription subscriptions]
      (tests/unsubscribe_each nil subscription))))

(deftest check_contracts_and_virt_type-test
  (let [subscriptions #spy/d (tests/get_multi_contract_subscriptions nil)]
    (tests/check_contracts_and_virt_type nil (-> subscriptions first first))))

(deftest check_contracts_and_virt_type-test
  (let [subscriptions #spy/d (tests/get_subscriptions nil)]
    (for [subscription subscriptions]
      (tests/check_contracts_and_virt_type nil subscription))))

(deftest all_subscriptions_table_is_sortable-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (match (vec (for [v [major minor patch]] (Integer. v)))
               [1 15 _] true
               [1 17 (_ :guard #(> % 16))] true
               [1 18 _] true
               [(_ :guard #(> % 1)) _ _] true
               :else  false)
      (tests/all_subscriptions_table_is_sortable  nil :all-available-subscriptions-subscription-header 0)
      (is (thrown? AssertionError (tests/all_subscriptions_table_is_sortable
                                   nil
                                   :all-available-subscriptions-subscription-header
                                   0))))))


(deftest my_subscriptions_table_is_sortable-01-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (match (vec (for [v [major minor patch]] (Integer. v)))
               [1 15 _] true
               [1 17 (_ :guard #(> % 16))] true
               [1 18 _] true
               [(_ :guard #(> % 1)) _ _] true
               :else  false)
      (tests/my_subscriptions_table_is_sortable  nil :my-subscriptions-subscription-header 0)
      (is (thrown? AssertionError (tests/my_subscriptions_table_is_sortable
                                   nil
                                   :my-subscriptions-subscription-header
                                   0))))))

(deftest my_subscriptions_table_is_sortable-02-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (match (vec (for [v [major minor patch]] (Integer. v)))
                      [1 15 _] true
                      [1 17 (_ :guard #(> % 16))] true
                      [1 18 _] true
                      [(_ :guard #(> % 1)) _ _] true
                      :else  false)
      (tests/my_subscriptions_table_is_sortable  nil :my-subscriptions-enddate-header 2)
      (is (thrown? AssertionError (tests/my_subscriptions_table_is_sortable
                                   nil
                                   :my-subscriptions-enddate-header
                                   2))))))

(deftest my_subscriptions_table_is_sortable-03-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (match (vec (for [v [major minor patch]] (Integer. v)))
                      [1 15 _] true
                      [1 17 (_ :guard #(> % 16))] true
                      [1 18 _] true
                      [(_ :guard #(> % 1)) _ _] true
                      :else  false)
      (tests/my_subscriptions_table_is_sortable  nil :my-subscriptions-quantity-header 3)
      (is (thrown? AssertionError (tests/my_subscriptions_table_is_sortable
                                   nil
                                   :my-subscriptions-quantity-header
                                   3))))))

(deftest changes_when_consumer_is_purged-test
  (tests/changes_when_consumer_is_purged nil))
