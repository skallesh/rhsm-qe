(ns rhsm.api.tests.subscription-manager-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.api.tests.subscription_manager_tests :as tests]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.rest.tests.entitlement-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (tests/startup nil)
                      (tests/install_monitor_systemd_service nil)
                      (f)))

(deftest subscription-manager-object-is-available-test
  (tests/subscription_manager_object_is_available nil))

(deftest entitlement-methods-inspection-test
  (tests/subscription_manager_methods nil))

(deftest entitlement-signals-inspection-test
  (tests/subscription_manager_signals nil))

(deftest check_status_using_dbus-test
  (tests/check_status_using_dbus nil))

(deftest dbus_entitlement_status_changed_signal_is_emited-test
  (tests/dbus_entitlement_status_changed_signal_is_emited nil))
