(ns rhsm.cockpit.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.cockpit.tests.register-tests :as tests]))

;;
;; lein quickie rhsm.cockpit.tests.activation-key-test
;; lein test :only rhsm.cockpit.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (tests/startup nil)
                      (f)))

(deftest register-test
  (let [[[driver run-command]] (tests/webdriver nil)]
    (tests/register nil driver run-command)))
