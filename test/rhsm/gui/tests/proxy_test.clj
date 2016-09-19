(ns rhsm.gui.tests.proxy-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.proxy_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tasks.test-config :as c])
  (:use gnome.ldtp
        [com.redhat.qe.verify :only (verify)]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/setup nil)
                      (f)
                      (tests/cleanup nil)))

(deftest no_litter_in_location_when_using_proxy-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (or (and (= (Integer. major) 1) (>= (Integer. minor) 17))
            (> (Integer. major) 1))
      (tests/no_litter_in_location_when_using_proxy nil)
      (is (thrown? java.lang.AssertionError
                   (tests/no_litter_in_location_when_using_proxy nil))))))

(deftest test-connection-is-blocked-before-all-fields-are-set-test
    (let [{:keys [major minor patch]} #spy/d (tools/subman-version)]
      (if (or (and (= (Integer. major) 1) (>= (Integer. minor) 17))
              (> (Integer. major) 1))
        (tests/test_connection_button_is_blocked_before_all_fields_are_set nil)
        (is (thrown? java.lang.AssertionError
                     (tests/test_connection_button_is_blocked_before_all_fields_are_set nil))))))
