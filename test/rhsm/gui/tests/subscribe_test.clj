(ns rhsm.gui.tests.subscribe-test
  (:use [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp
        rhsm.gui.tasks.tools)
  (:require  [clojure.test :refer :all]
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

(deftest all_subscriptions_are_sortable-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (or (and (= (Integer. major) 1) (= (Integer. minor) 17) (> (Integer. patch) 16))
            (and (= (Integer. major) 1) (> (Integer. minor) 18))
            (and (= (Integer. major) 1) (= (Integer. minor) 15))
            (> (Integer. major) 1))
      (tests/all_subscriptions_are_sortable nil)
      (is (thrown? AssertionError (tests/all_subscriptions_are_sortable nil))))))
