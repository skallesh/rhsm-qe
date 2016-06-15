(ns rhsm.gui.tasks.candlepin-tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [mount.core :as mount]
             )
  )

(use-fixtures :once (fn [f] (base/startup nil)(f)))

;; initialization of our testware
(deftest register-with-creds-test
  (is (contains? #{"Snow White" "Admin Owner"} (ct/get-owner-display-name (:username @c/config) (:password @c/config) (:owner-key @c/config))))
  )
