(ns rhsm.gui.tests.autosubscribe-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools
        [rhsm.gui.tasks.test-config :only (config clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.autosubscribe_tests :as atests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (atests/setup nil) (f)))

(deftest simple_autosubscribe-test
  (testing "run of one test for autosubscribe"
    (atests/simple_autosubscribe nil)))
