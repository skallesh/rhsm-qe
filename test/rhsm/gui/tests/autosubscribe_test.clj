(ns rhsm.gui.tests.autosubscribe-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.autosubscribe_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             ))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/setup nil)
                      (f)
                      (tests/cleanup nil)))

(deftest simple_autosubscribe-test
  (testing "run of one test for autosubscribe"
    (.moveOriginalProductCertDefaultDirFilesBeforeClass @tests/complytests)
    (.setupProductCertDirsBeforeClass @tests/complytests)
    (.configureProductCertDirForAllProductsSubscribableByOneCommonServiceLevel @tests/complytests)
    (tests/simple_autosubscribe nil)))
