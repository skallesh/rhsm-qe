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
             [clojure.set :refer [subset?]]
             [rhsm.gui.tests.base :as base]))

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
      "RHEL7" (is (= "dlgAboutsubscription-manager-gui" (-> #spy/d ui/windows :about-dialog :id)))
      "default")))

(deftest list-of-dialogs-after-click-on-about-test
  (testing "list of windows that appear after I click on 'About' button"
    (tasks/restart-app)
    (tasks/ui click :about)
    (Thread/sleep 1000)
    (is (subset? #{"dlg0" "frmDesktop" "dlgWarning" "frmTerminal" "frmSubscriptionManager" "dlgAboutsubscription-manager-gui"} (set (tasks/ui getwindowlist))))))

(deftest verify_about_information-test
  (testing "Test for verify_about_information GUI test"
    (ftests/verify_about_information nil)))
