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
             [clojure.core.match :refer [match]]
             [clojure.set :refer [subset?]]
             [rhsm.gui.tests.base :as base])
  (:import   org.testng.SkipException))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (itests/create_certs nil)
                      (ftests/register nil)
                      (f)))

(deftest window-system-registration-is-available-test
  (testing "Test such that a window 'System Registration' is named as register_dialog for RHEL7"
    (case (tt/get-release)
      "RHEL6" (is (= "About Subscription Manager" (-> ui/windows :about-dialog :id)))
      "RHEL7" (is (= "dlgAboutsubscription-manager-gui" (-> ui/windows :about-dialog :id)))
      "default")))

(deftest list-of-dialogs-after-click-on-about-test
  (testing "list of windows that appear after I click on 'About' button"
    (tasks/restart-app)
    (tasks/ui click :about)
    (Thread/sleep 1000)
    (case (tt/get-release)
      "RHEL6" (is (subset? #{"frmSubscriptionManager" "dlgAboutSubscriptionManager"} (set (tasks/ui getwindowlist))))
      "RHEL7" (is (subset? #{"frmSubscriptionManager" "dlgAboutsubscription-manager-gui"} (set (tasks/ui getwindowlist))))
      "default")))

(deftest verify_about_information-test
  (testing "A test which verifies that a test verify_about_information raises SkipException for newer versions of RHEL"
    (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tt/get-release :version))]
      (match [major minor]
             ["6" _] (ftests/verify_about_information nil)
             ["7" (a :guard #(>= (Integer. %) 3))] (is (thrown? SkipException (ftests/verify_about_information nil)))
             :else (ftests/verify_about_information nil)))))

(deftest verify_about_information_without_close_button-test
  (testing "A test which verifies that a test verify_about_information_without_close_button raises SkipException for older RHEL versions than 7.3"
    (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tt/get-release :version))]
      (match [ major minor]
             ["6" _] (is (thrown? SkipException (ftests/verify_about_information_without_close_button nil)))
             ["7" (a :guard #(>= (Integer. %) 3))] (ftests/verify_about_information_without_close_button nil)
             :else (is (thrown? SkipException (ftests/verify_about_information_without_close_button nil)))))))


(deftest check_available_releases
  (ftests/check_available_releases nil))

(deftest check_releases
  (ftests/check_releases nil))

(deftest check_available_releases
  (ftests/check_available_service_levels nil))

(deftest verify_update_facts-test
  (ftests/verify_update_facts nil))
