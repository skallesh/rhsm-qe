(ns rhsm.api.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.api.tests.register_tests :as tests]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.dbus.tests.activation-key-test
;; lein test :only rhsm.dbus.tests.activation-key-test/register-using-activation-key-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (c/init)
                      (f)))

;; (deftest json-test
;;   (let [json-str (slurp "resources/content-of-register-response.json")]
;;     (println (subs json-str 0 256))
;;     (json/read-json json-str)
;;     )
;;   )

;; (deftest json-simple-test
;;   (let [json-str (slurp "resources/aa.json")]
;;     (println json-str)
;;     (json/read-json json-str)
;;     )
;;   )

(deftest register-methods-test
  (let [socket (tests/register_socket nil)]
    (tests/register_methods nil (-> socket first first))))

(deftest register-using-dbus-test
  (let [socket (tests/register_socket nil)]
    (tests/register_using_dbus nil (-> socket first first))))

(deftest dbus_register_reflects_identity_change-test
  (let [socket (tests/register_socket nil)]
    (tests/dbus_register_reflects_identity_change nil (-> socket first first))))

;; (deftest attach-subscriptions-even-wrong-org-was-used-before-test
;;   (tests/attach_subscriptions_even_wrong_org_was_used_before nil))
