(ns rhsm.gui.tests.repo-test
  (:use gnome.ldtp
        [com.redhat.qe.verify :only (verify)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.repo_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tasks.ui :as ui]
             [clojure.core.match :refer [match]]
             [rhsm.gui.tests.base :as base])
  (:import org.testng.SkipException))

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/setup nil)
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

(deftest repo-list-sortable-elements
  (if (tasks/ui showing? :register-system)
    (tasks/register-with-creds))
  (tasks/subscribe_all)
  (tests/assert-and-open-repo-dialog)
  (is (= "Repository ID" (-> ui/elements :repo-table-repository-id :id)))
  (tasks/ui click :repo-table-repository-id))

(deftest repo-list-sortable-test
  (let [{:keys [major minor patch]} (tools/subman-version)]
    (if (or (and (= (Integer. major) 1) (= (Integer. minor) 17) (> (Integer. patch) 15))
            (and (= (Integer. major) 1) (> (Integer. minor) 18))
            (> (Integer. major) 1))
      (tests/check_repo_table_sortable nil)
      (is (thrown? AssertionError (tests/check_repo_table_sortable nil))))))

(deftest check_repo_visible-test
  (tests/check_repo_disabled nil))

(deftest check_repo_message_unsubscribed-test
  (tests/check_repo_message_unsubscribed nil))
