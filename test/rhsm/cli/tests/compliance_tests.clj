(ns rhsm.cli.tests.compliance-tests
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.base :as base])
  (:import [rhsm.cli.tests ComplianceTests]))

;; ;; initialization of our testware
;; (use-fixtures :once (fn [f]
;;                       (base/startup nil)
;;                       (tests/setup nil)
;;                       (f)
;;                       (tests/cleanup nil)))

(deftest access-to-configure-fields-test
  (let [complianceTests (new ComplianceTests)]
    (is (= false (.getConfigureProductCertDirForAllProductsSubscribableByOneCommonServiceLevelCompleted complianceTests)))))
