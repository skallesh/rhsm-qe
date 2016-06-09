(ns rhsm.gui.tests.candlepin-tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [mount.core :as mount]
             )
  )

;; initialization of our testware
(rhsm.runtestng/before-suite true)


;; (deftest register-with-creds-test
;;   (is (contains? #{"Snow White" "Admin Owner"} (ct/get-owner-display-name (:username @c/config) (:password @c/config) (:owner-key @c/config))))
;;   )
