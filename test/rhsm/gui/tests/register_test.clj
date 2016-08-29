(ns rhsm.gui.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as tests]
             [rhsm.gui.tests.import_tests :as itests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tests.testbase :as testbase]))

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (itests/create_certs nil)
                      (println "------ tests/setup ------")
                      (tests/setup nil)
                      (println "------ test ------")
                      (f)))

(deftest a-part-of-suite-01-test
  (tests/register_bad_credentials ["sdf" "sdf"] :invalid-credentials))

(deftest a-part-of-suite-02-test
  (tests/register_bad_credentials ["test user" "password"] :invalid-credentials))

(deftest a-part-of-suite-03-test
  (tests/register_bad_credentials ["test user" ""] :no-password))

(deftest a-part-of-suite-04-test
  (tests/register_bad_credentials ["  " "  "] :no-username))

(deftest a-part-of-suite-05-test
  (tests/register_bad_credentials ["" ""] :no-username))

(deftest a-part-of-suite-06-test
  (tests/register_bad_credentials ["" "password"] :no-username))

(deftest a-part-of-suite-07-test
  (tests/register_bad_credentials ["sdf" ""] :no-password))

(deftest a-part-of-suite-08-test
  (tests/register_bad_credentials nil [(@c/config :username) (@c/config :password) :system-name-input ""] :no-system-name))

(deftest simple-register-test
  (tests/simple_register nil "testuser1" "password" nil))

(deftest simple-register-test
  (tests/simple_register nil "testuser2" "password" "Admin Owner"))

(deftest simple-register-test
  (tests/simple_register nil "testuser1" "password" "Admin Owner"))

(deftest simple-register-test
  (tests/simple_register nil "testuser1" "password" "Snow White"))

(deftest check_traceback_unregister-test
  (tests/check_traceback_unregister nil))
