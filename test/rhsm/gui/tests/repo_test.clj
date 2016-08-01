(ns rhsm.gui.tests.repo-test
  (:use gnome.ldtp)
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.repo_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tests.testbase :as testbase])
  (:import org.testng.SkipException))

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (println "------ base/startup ------")
                      (base/startup nil)
                      (println "------ tests/setup ------")
                      (tests/setup nil)
                      (println "------ test ------")
                      (f)))


(deftest check_repo_name_url-test
  (let [repos  (tests/subscribed_repos nil)]
    (tests/check_repo_name_url nil (-> repos first first))))

(deftest enable_repo_remove_all_overrides-test
  (let [repos (tests/subscribed_repos nil)]
    (if (-> (tools/get-release :true) :version (= "7.3"))
      (is (thrown? SkipException
                   (tests/enable_repo_remove_all_overrides nil (-> repos first first))))
      (tests/enable_repo_remove_all_overrides nil (-> repos first first))))
  (tests/after_enable_repo_remove_all_overrides nil))
