(ns rhsm.gui.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [clojure.string :as s]
             [vinyasa.reimport :refer [reimport]]
             )
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [rhsm.cli.tasks CandlepinTasks]
           [rhsm.cli.tasks SubscriptionManagerTasks]
           [com.redhat.qe.tools SSHCommandRunner]
           [rhsm.base SubscriptionManagerBaseTestScript]
           [rhsm.base SubscriptionManagerCLITestScript]
           )
  )

;; (reimport '[com.redhat.qe.tools SSHCommandRunner])
;; (deftest ssh-command-runner-test
;;   (println "def test")
;;   (let [ssh (SSHCommandRunner. "192.168.124.137" "root" ".ssh/rhsm-qe" "dog8code" nil)]
;;     )
;;   )

;;initialization of testing environment
(rhsm.runtestng/before-suite true)

;#spy/d @c/config

;; testing of an environment of our tests
(deftest a11y-configuration-test
  (testing "is it possible to enable a11y?"
    (case (tt/get-release)
             "RHEL7"  (is (s/blank?  (-> "gsettings set org.gnome.desktop.interface toolkit-accessibility true"
                                        tt/run-command :stderr s/trim)))
             "RHEL6" nil
             )
    )

  (case (tt/get-release)
    "RHEL7" (is (= "true" (-> "gsettings get org.gnome.desktop.interface toolkit-accessibility"
                             tt/run-command :stdout s/trim)))
    "RHEL6"  (is (= "true" (-> "gconftool-2 --get /desktop/gnome/interface/accessibility"
                              tt/run-command :stdout s/trim)))
    )
  )

;; (deftest run-subscription-manager-test
;;   (testing "Can we start app?"
;;     (t/restart-app)
;;     (is (some #{"frmSubscriptionManager"} (t/ui getwindowlist)))
;;     )
;;   )

;; (deftest owners-dialog-test
;;   (testing "I have configured owner the right way"
;;     (t/restart-app)
;;     (t/register-system)
;;     (t/ui click :register)
;;     (t/ui settextvalue :redhat-login (:username @c/config))
;;     (t/ui settextvalue :password (:password @c/config))
;;     (t/ui click :register)
;;     (is (some #{"dlgSystemRegistration"} (t/ui getwindowlist)))
;;     (is (t/ui showing? :owner-view))
;;     (is (= "Admin Owner" (t/ui getcellvalue :owner-view 0)))
;;     (is (= "Snow White" (t/ui getcellvalue :owner-view 1)))
;;     )
;;   )

(deftest simple-register-test
  (testing "Simple Register Tests"
    (rtests/setup nil)
    (rtests/simple_register nil (:username @c/config) (:password @c/config) (:owner-key @c/config))
    ;;(t/register (:username @c/config) (:password @c/config)  :owner (:owner-key @c/config))
    )
  )
