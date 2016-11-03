(ns rhsm.gui.tasks.tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as  tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
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
  (tasks/restart-app)
  (tasks/register-with-creds))

(deftest take-screenshot-test
  (tasks/take-screenshot "test"))

(deftest verify-or-take-screenshot-test
  (tasks/verify-or-take-screenshot false))

(deftest screenshot-on-exception-test
  (tasks/screenshot-on-exception
   :default-name
   (println "some message")
   (throw+ "error in common way")))
