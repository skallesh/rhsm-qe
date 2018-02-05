(ns rhsm.api.tests.unregister-test
  (:require  [clojure.test :refer :all]
             [rhsm.api.tests.unregister_tests :as tests]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.dbus.tests.unregister-test
;; lein test :only rhsm.dbus.tests.unregister-test/unregister-methods-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (tests/setup nil)
                      (f)
                      (tests/set_config_values_to_original_ones nil)))

(deftest unregister-object-is-available-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/unregister_object_is_available nil run-command locale)))

(deftest unregister-methods-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/unregister_methods nil run-command locale)))

(deftest unregister-using-dbus-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/unregister_using_dbus nil run-command locale)))

(deftest unregister-twice-using-dbus-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/unregister_twice_using_dbus nil run-command locale)))

(deftest unregister-using-dbus-when-candlepin-is-down-and-proxy-is-used-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/unregister_using_dbus_when_candlepin_is_down_and_proxy_is_used nil run-command locale)))
