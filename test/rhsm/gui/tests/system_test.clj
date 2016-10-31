(ns rhsm.gui.tests.system-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools)
  (:import org.testng.SkipException)
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.system_tests :as tests]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.tasks :as tasks]
             [clojure.core.match :refer [match]]
             [rhsm.gui.tests.base :as base]))

(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest no_traceback_on_console_after_ctrl_c_pressed-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (or (and (= (Integer. major) 1) (= (Integer. minor) 17) (>= (Integer. patch) 15))
            (and (= (Integer. major) 1) (> (Integer. minor) 17))
            (> (Integer. major) 1))
      (tests/no_traceback_on_console_after_ctrl_c_pressed nil)
      (is (thrown? java.lang.AssertionError
                   (tests/no_traceback_on_console_after_ctrl_c_pressed nil))))))

(deftest no_traceback_when_network_is_down-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (match (vec (for [v [major minor patch]] (Integer. v)))
               [1 17 (_ :guard #(> % 6))] true
               [1 18 _] true
               [2 _ _] true
               :else false)
      (tests/error_dialog_and_no_traceback_when_network_is_down nil)
      (is (thrown? java.lang.AssertionError (tests/error_dialog_and_no_traceback_when_network_is_down nil))))))
