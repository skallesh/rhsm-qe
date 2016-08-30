(ns rhsm.gui.tests.proxy-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.proxy_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.testbase :as testbase]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/setup nil)
                      (f)
                      (tests/cleanup nil)))

;; (deftest bad_proxy-test
;;   (tests/test_bad_proxy nil))

;; (deftest proxy_test-test
;;   (tests/bad_proxy nil))

(deftest test_proxy_with_blank_credentials-test
  (tests/test_proxy_with_blank_credentials nil))

;; (deftest proxy_noauth_connect-test
;;   (tests/proxy_noauth_connect nil))
