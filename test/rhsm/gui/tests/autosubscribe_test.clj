(ns rhsm.gui.tests.autosubscribe-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.autosubscribe_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             ;[rhsm.gui.tests.testbase :as testbase]
             ))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/setup nil)
                      (f)
                      (tests/cleanup nil)))

(deftest simple_autosubscribe-test
  (testing "run of one test for autosubscribe"
    ;; (println "----> product cert dir: " (tasks/conf-file-value "productCertDir"))
    ;; (println "----> num of certs:" (-> "ls -1 /tmp/sm-allProductsSubscribableByOneCommonServiceLevel | wc -l" tools/run-command :stdout clojure.string/trim))
    ;; (println "----------- moveOriginalProductCertDefaultDirFilesBeforeClass ----------------")
    (.moveOriginalProductCertDefaultDirFilesBeforeClass @tests/complytests)
    ;; (println "----> product cert dir: " (tasks/conf-file-value "productCertDir"))
    ;; (println "----> num of certs:" (-> "ls -1 /tmp/sm-allProductsSubscribableByOneCommonServiceLevel | wc -l" tools/run-command :stdout clojure.string/trim))
    ;; (println "----------- setupProductCertDirsBeforeClass ----------------")
    (.setupProductCertDirsBeforeClass @tests/complytests)
    ;; (println "----> product cert dir: " (tasks/conf-file-value "productCertDir"))
    ;; (println "----> num of certs:" (-> "ls -1 /tmp/sm-allProductsSubscribableByOneCommonServiceLevel | wc -l" tools/run-command :stdout clojure.string/trim))
    ;; (println "----------- configureProductCertDirForAllProductsSubscribableByOneCommonServiceLevel ----------------")
    (.configureProductCertDirForAllProductsSubscribableByOneCommonServiceLevel @tests/complytests)
    ;; (println "----> product cert dir: " (tasks/conf-file-value "productCertDir"))
    ;; (println "----> num of certs:" (-> "ls -1 /tmp/sm-allProductsSubscribableByOneCommonServiceLevel | wc -l" tools/run-command :stdout clojure.string/trim))
    ;; (println "----------- simple_autosubscribe ----------------")
    (tests/simple_autosubscribe nil)))
