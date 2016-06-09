(ns rhsm.gui.tests.tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             )
  )

;; ;; initialization of our testware
;; (rhsm.runtestng/before-suite true)

;; (deftest register-with-creds-test
;;   (t/restart-app)
;;   (t/register-with-creds)
;;   )
