(ns rhsm.gui.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             )
  )

;; ;; initialization of our testware
;; (rhsm.runtestng/before-suite true)
(rtests/setup nil)

(deftest simple-register-test
  (testing "Simple Register Tests"
    (t/restart-app)
    (rtests/simple_register nil (:username @c/config) (:password @c/config)
                            (ct/get-owner-display-name (:username @c/config) (:password @c/config) (:owner-key @c/config)))
    )
  )

(deftest auto_to_register_button-test
  (t/restart-app)
  (rtests/check_auto_to_register_button nil)
  )

(deftest register_multi_click-test
  (t/restart-app)
  (rtests/register_multi_click nil)
  )
