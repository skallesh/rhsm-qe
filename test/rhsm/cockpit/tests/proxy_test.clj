(ns rhsm.cockpit.tests.proxy-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.cockpit.tests.proxy-tests :as tests])
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
                      (tests/cleanup nil)))

(deftest auth-proxy-works-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/auth_proxy_works nil driver run-command locale lang)))

(deftest register-with-auth-proxy-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_auth_proxy nil driver run-command locale lang)))

(deftest noauth-proxy-works-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/noauth_proxy_works nil driver run-command locale lang)))

(deftest register-with-noauth-proxy-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_noauth_proxy nil driver run-command locale lang)))
