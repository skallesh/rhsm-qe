(ns rhsm.cockpit.tests.status-test
  (:require  [clojure.test :refer :all]
             [rhsm.cockpit.tests.status-tests :as tests]
             [rhsm.gui.tasks.test-config :as c]))

;;
;; lein quickie rhsm.cockpit.tests.activation-key-test
;; lein test :only rhsm.cockpit.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (tests/startup nil)
                      (f)
                      (tests/close_selenium_driver nil)
                      (tests/set_dbus_service_back nil)))

(deftest cockpit_should_inform_a_user_when_subscription_manager_is_not_installed-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/cockpit_should_inform_a_user_when_subscription_manager_is_not_installed
        nil driver run-command locale lang)))

(deftest cockpit-should-inform-a-user-for-all-locales-test
  (doseq [[driver run-command locale lang] (tests/webdriver_with_locale nil)]
    (tests/cockpit_should_inform_a_user_when_subscription_manager_is_not_installed
        nil driver run-command locale lang)))
