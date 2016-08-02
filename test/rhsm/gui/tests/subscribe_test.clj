(ns rhsm.gui.tests.subscribe-test
  (:use [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp
        rhsm.gui.tasks.tools)
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.subscribe_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]

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
