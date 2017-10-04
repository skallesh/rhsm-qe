(ns rhsm.cockpit.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.cockpit.tests.register-tests :as tests]))

;;
;; lein quickie rhsm.cockpit.tests.activation-key-test
;; lein test :only rhsm.cockpit.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      ;;(TestScript.)
                      (tests/startup nil)
                      (f)
                      (tests/cleanup nil)))

(deftest package_is_installed-test
  (let [[[run-command]] (tests/provide_run_command nil)]
    (tests/package_is_installed nil run-command)))

(deftest service_is_running-test
  (let [[[run-command]] (tests/provide_run_command nil)]
    (tests/service_is_running nil run-command)))

(deftest register-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register nil driver run-command locale lang)))

;; (deftest register-02-test
;;   (let [[driver run-command locale lang]
;;         (second (tests/webdriver_with_locale nil))]
;;     (tests/register nil driver run-command locale lang)))

;; (deftest register-03-test
;;   (let [[driver run-command locale lang]
;;         (nth (tests/webdriver_with_locale nil) 2)]
;;     (tests/register nil driver run-command locale lang)))

(deftest register-with-no-field-set-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_no_field_set nil driver run-command locale lang)))

(deftest register-with-empty-password-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_empty_password nil driver run-command locale lang)))

(deftest register-with-wrong-password-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_wrong_password nil driver run-command locale lang)))

(deftest register-with-empty-login-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_empty_login nil driver run-command locale lang)))

(deftest register-with-wrong-login-01-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/register_with_wrong_login nil driver run-command locale lang)))

(deftest unregister-test
  (let [[driver run-command locale lang]
        (first (tests/webdriver_with_locale nil))]
    (tests/unregister nil driver run-command locale lang)))
