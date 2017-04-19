(ns rhsm.dbus.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.dbus.tests.register-tests :as tests]
             [rhsm.gui.tests.base :as base]
             ))

;;
;; lein quickie rhsm.dbus.tests.activation-key-test
;; lein test :only rhsm.dbus.tests.activation-key-test/register-using-activation-key-test
;;

;; ;; initialization of our testware
(use-fixtures :once (fn [f] (base/startup nil) (f)))

(deftest register-test
  (let [socket (tests/register_socket nil)]
    (tests/register nil (-> socket first first))))

;; (deftest attach-subscriptions-even-wrong-org-was-used-before-test
;;   (tests/attach_subscriptions_even_wrong_org_was_used_before nil))
