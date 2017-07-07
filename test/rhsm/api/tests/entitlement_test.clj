(ns rhsm.api.tests.entitlement-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.api.tests.entitlement_tests :as tests]
             [rhsm.gui.tasks.test-config :refer [config]]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.rest.tests.entitlement-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest entitlement-object-is-available-test
  (tests/entitlement_object_is_available nil))

(deftest entitlement-methods-inspection-test
  (tests/entitlement_methods nil))

(deftest entitlement_status_of_invalid_subscription_test
  (tests/entitlement_status_of_invalid_subscription_using_dbus nil))

(deftest entitlement_status_of_invalid_subscription_test
  (tests/get_consumed_pools_using_dbus nil))
