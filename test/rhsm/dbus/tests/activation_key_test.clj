(ns rhsm.dbus.tests.activation-key-test
  (:require  [clojure.test :refer :all]
             [rhsm.dbus.tests.activation-key-tests :as tests]
             [rhsm.gui.tests.base :as base]
             ))

;;
;; lein quickie rhsm.dbus.tests.activation-key-test
;; lein test :only rhsm.dbus.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f] (base/startup nil) (f)))

(deftest register-using-activation-key-test
  (let [activation-key (tests/new_simple_activation_key nil)]
    (tests/register_using_activation_key nil (-> activation-key first first))))

;; (deftest attach-subscriptions-even-wrong-org-was-used-before-test
;;   (tests/attach_subscriptions_even_wrong_org_was_used_before nil))
