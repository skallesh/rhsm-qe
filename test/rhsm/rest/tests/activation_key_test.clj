(ns rhsm.rest.tests.activation-key-test
  (:require  [clojure.test :refer :all]
             [rhsm.rest.tests.activation-key-tests :as tests]
             [rhsm.gui.tests.base :as base]))

;;
;; lein quickie rhsm.rest.tests.activation-key-test
;; lein test :only rhsm.rest.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f] (base/startup nil) (f)))

(deftest create-activation-key-test
  (tests/create_activation_key nil))

(deftest list-activation-keys-test
  (tests/list_activation_keys nil))

