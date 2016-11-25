(ns rhsm.gui.tests.base-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             ))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest open-connection-to-candlepin-test
  (base/open-connection-to-candlepin))
