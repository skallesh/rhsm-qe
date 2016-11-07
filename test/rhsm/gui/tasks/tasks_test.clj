(ns rhsm.gui.tasks.tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [mount.core :as mount]))


(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest register-with-creds-test
  (t/restart-app)
  (t/register-with-creds))

(deftest take-screenshot-test
  (t/take-screenshot "test"))

(deftest verify-or-take-screenshot-test
  (t/verify-or-take-screenshot false))
