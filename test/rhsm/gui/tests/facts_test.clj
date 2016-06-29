(ns rhsm.gui.tests.facts-test
  (:use gnome.ldtp
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.facts_tests :as ftests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tests.import_tests :as itests]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [rhsm.gui.tests.base :as base]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (itests/create_certs nil)
                      (ftests/register nil)
                      (f)))


(deftest verify_about_information-test
  (testing "Test for verify_about_information GUI test"
    (ftests/verify_about_information nil)))
