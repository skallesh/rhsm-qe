(ns rhsm.cockpit.tests.activation-key-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.cockpit.tests.activation-key-tests :as tests])
  (:import [rhsm.base SubscriptionManagerCLITestScript]))

;;
;; lein quickie rhsm.cockpit.tests.activation-key-test
;; lein test :only rhsm.cockpit.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (.. (new SubscriptionManagerCLITestScript) setupBeforeSuite)
                      (c/init)
                      (tests/startup nil)
                      (f)
                      (tests/cleanup nil)
                      (tests/delete_all_actually_created_activation_keys nil)))

(deftest register-01-test
  (let [[driver run-command activation-key locale lang]
        (first (tests/webdriver_with_activation_key_english_locale nil))]
    (tests/register_with_activation_key nil driver run-command activation-key locale lang)))

