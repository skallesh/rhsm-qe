(ns rhsm.api.tests.attach-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.api.tests.attach_tests :as tests]
             [rhsm.gui.tasks.test-config :as c]
             [org.httpkit.client :as http]
             [rhsm.api.rest :as rest]
             [clojure.data.json :as json])
  (:import  rhsm.base.SubscriptionManagerCLITestScript))

;;
;; lein quickie rhsm.rest.tests.entitlement-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (tests/startup nil)
                      (f)))

(deftest attach-object-is-availabe-01-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/attach_object_is_available nil run-command locale)))

(deftest attach-object-is-availabe-02-test
  (let [[run-command locale] (second (tests/client_with_english_locales nil))]
    (tests/attach_object_is_available nil run-command locale)))

(deftest attach-methods-inspection-01-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/attach_methods nil run-command locale)))

(deftest attach-methods-inspection-02-test
  (let [[run-command locale] (second (tests/client_with_english_locales nil))]
    (tests/attach_methods nil run-command locale)))

(deftest PoolAttach-01-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/attach_pool_using_dbus nil run-command locale)))

(deftest PoolAttach-reflects-identity-change-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/dbus_attach_pool_reflects_identity_change_even_inotify_is_zero nil run-command locale)))

(deftest AutoAttach-reflects-identity-change-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/dbus_autoattach_pool_reflects_identity_change_even_inotify_is_zero nil run-command locale)))

(deftest PoolAttach-02-test
  (let [[run-command locale] (second (tests/client_with_english_locales nil))]
    (tests/attach_pool_using_dbus nil run-command locale)))

(deftest AutoAttach-01-test
  (let [[run-command locale] (first (tests/client_with_english_locales nil))]
    (tests/autoattach_pool_using_dbus nil run-command locale)))

(deftest AutoAttach-02-test
  (let [[run-command locale] (second (tests/client_with_english_locales nil))]
    (tests/autoattach_pool_using_dbus nil run-command locale)))
