(ns rhsm.api.tests.config-test
  (:require  [clojure.test :refer :all]
             [rhsm.api.tests.config_tests :as tests]
             [rhsm.gui.tasks.test-config :as c]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.dbus.tests.activation-key-test
;; lein test :only rhsm.dbus.tests.activation-key-test/register-using-activation-key-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (tests/startup nil)
                      (f)
                      (tests/set_inotify_to_1 nil)))

(deftest config-object-is-availabe-test
  (tests/config_object_is_available nil))

(deftest config-methods-inspection-test
  (tests/config_methods nil))

(deftest dbus_get_actual_value_when_property_is_changed-test
  (tests/dbus_get_actual_value_when_property_is_changed nil))

(deftest dbus_get_actual_value_even_inotify_is_zero-test
  (tests/dbus_get_actual_value_even_inotify_is_zero nil))

;; (deftest config-getAll-test
;;   (tests/getAll_using_dbus nil))

;; (deftest config-get-property-test
;;   (tests/get_property_using_dbus nil))

;; (deftest config-get-section-test
;;   (tests/get_section_using_dbus nil))

;; (deftest config-set-property-test
;;   (tests/set_property_using_dbus nil))
