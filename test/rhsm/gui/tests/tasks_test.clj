(ns rhsm.gui.tests.tasks-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.app.sm.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [mount.core :as mount]
             )
  )

;; ;; initialization of our testware
;; (rhsm.runtestng/before-suite true)

(use-fixtures :once (fn [f] (println "fixtures once starting") (mount/start) (f)))

(deftest window-system-registration-is-available-test
  (testing "Test such that a window 'System Registration' is named as register_dialog for RHEL7"
    (println ui/windows)
    (case (tt/get-release)
      "RHEL7" (is (= "register_dialog" (-> ui/windows :register-dialog :id)))
      "RHEL6" (is (= "System Registration" (-> ui/windows :register-dialog :id)))
      "default"
      )
    )
  )

;; (deftest register-with-creds-test
;;   (t/restart-app)
;;   (t/register-with-creds)
;;   )
