(ns rhsm.gui.tasks.tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as  tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.tools.logging :as log]
             [clojure.string :as s]
             [clojure.core.match :as match]
             [mount.core :as mount]
             [clojure.java.io :as io])
  (:import [com.redhat.qe.tools RemoteFileTasks]))

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

(deftest screenshot-on-exception-01-test
  (is (thrown? Exception (tasks/screenshot-on-exception
                          :default-name
                          (sl/throw+ "error in common way")))))

(deftest screenshot-on-exception-02-test
  (is (thrown? Exception (tasks/screenshot-on-exception
                          "suffix-of-screenshot"
                          (sl/throw+ "error in common way")))))
