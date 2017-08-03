(ns rhsm.api.tests.attach-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.api.tests.attach_tests :as tests]
             [rhsm.gui.tasks.test-config :refer [config]]
             [org.httpkit.client :as http]
             [rhsm.api.rest :as rest]
             [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.rest.tests.entitlement-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest attach-object-is-availabe-test
  (tests/attach_object_is_available nil))

(deftest attach-methods-inspection-test
  (tests/attach_methods nil))

(deftest Attach-method-PoolAttach-test
  (tests/attach_pool_using_dbus nil))
