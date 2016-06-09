(ns rhsm.app.sm.ui-test
  (:require [rhsm.app.sm.ui :as ui]
            [clojure.test :refer :all]
            [mount.core :as mount]
            [rhsm.gui.tasks.tools :as tt]
            )
  )

(use-fixtures :once (fn [f]
                      (println "fixtures once starting")
                      (mount/start)
                      (f)
                      ))

(deftest window-system-registration-is-available-test
  (testing "Test such that a window 'System Registration' is named as register_dialog for RHEL7"
    (case (tt/get-release)
      "RHEL7" (is (= "register_dialog" (-> ui/windows :register-dialog :id)))
      "RHEL6" (is (= "System Registration" (-> ui/windows :register-dialog :id)))
      "default"
      )
    )
  )
