(ns rhsm.gui.tests.repo_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [clojure.string :only (split
                               split-lines
                               blank?
                               join
                               trim-newline
                               trim)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            Test
            DataProvider]
           org.testng.SkipException
           [com.redhat.qe.auto.bugzilla BzChecker]))

(def random_row_num (atom nil)) ;; Used to dynamically select a random row number
(def list_row (atom []))       ;; Used to hold probable row numbers
(def no_repos_message "No repositories are available without an attached subscription.")
(def ns-log "rhsm.gui.tests.repo_tests")

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (try+
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (tasks/restart-app)
    (tasks/unregister)
    (catch [:type :not-registered] _)
    (catch Exception e
      (reset! (skip-groups :repo) true)
      (throw e))))

(defn assert-and-open-repo-dialog
  "Asserts if repo-dialog is open if not it opens it"
  []
   (if (not (bool (tasks/ui guiexist :repositories-dialog)))
    (do (tasks/ui click :repositories)
        (tasks/ui waittillwindowexist :repositories-dialog 10)
        (sleep 2000))))

(defn assert-and-subscribe-all
  "Asserts if the system is already subscribed before subscribe_all"
  []
  (let [row-count (tasks/ui getrowcount :installed-view)
        status (tasks/ui gettextvalue :overall-status)]
    (if (substring? (str row-count) status)
      (tasks/subscribe_all)
      (sleep 2000))))

(defn assert-and-remove-all-override
  "Asserts if remove all override button functionality"
  [& {:keys [repo]
      :or {repo nil}}]
  (if (tasks/has-state? :repo-remove-override "enabled")
    (do
      (tasks/ui click :repo-remove-override)
      (tasks/ui waittillwindowexist :question-dialog 10)
      (if-not (nil? repo)
        (verify (substring?
                 repo (tasks/ui gettextvalue
                                :question-dialog "Are you sure*"))))
      (tasks/ui click :yes)
      (tasks/checkforerror)
      (verify (bash-bool (tasks/ui guiexist :question-dialog))))))

(defn ^{Test {:groups ["repo"
                       "tier1"]}}
  check_repo_visible
  "This test checks whether repository option exists
   when system is unregistered"
  [_]
  (tasks/restart-app :unregister? true)
  (tasks/ui click :main-window "System")
  (verify (not (tasks/visible? :repositories))))

(defn ^{Test {:groups ["repo"
                       "tier1"]}}
  check_repo_system_menu
  "This tests for repository option in the system menu"
  [_]
  (if (tasks/ui showing? :register-system)
    (tasks/register-with-creds))
  (try
    (assert-and-open-repo-dialog)
    (verify (bool (tasks/ui guiexist :repositories-dialog)))
    (finally
      (sleep 2000)
      (tasks/ui click :close-repo-dialog))))

(defn ^{Test {:groups ["repo"
                       "tier1"]}}
  check_repo_message_unsubscribed
  "This tests for default static message in repository dialog when unsubscribed"
  [_]
  (try
    (if (tasks/ui showing? :register-system)
      (tasks/register-with-creds))
    (assert-and-open-repo-dialog)
    (verify (bool (tasks/ui guiexist :repositories-dialog)))
    (verify (= no_repos_message (tasks/ui gettextvalue :repo-message)))
    (finally
     (tasks/ui click :close-repo-dialog))))

(defn ^{Test {:groups ["repo"
                       "tier1"
                       "blockedByBug-1095938"]}}
  check_repo_table_populated
  "This tests if repo-table is populated when subscribed"
  [_]
  (try
    (if (tasks/ui showing? :register-system)
      (tasks/register-with-creds))
    (tasks/subscribe_all)
    (assert-and-open-repo-dialog)
    (verify (bool (tasks/ui guiexist :repositories-dialog)))
    (verify (< 0 (tasks/ui getrowcount :repo-table)))
    (finally
     (tasks/ui click :close-repo-dialog)
     (tasks/unsubscribe_all))))

(defn ^{Test {:groups ["repo"
                       "tier2"
                       "blockedByBug-1095938"]}}
  check_repo_remove_override_button
  "This tests if repo-override button is enabled when a row is checked"
  [_]
  (try
    (if (tasks/ui showing? :register-system)
      (tasks/register-with-creds))
    (tasks/subscribe_all)
    (assert-and-open-repo-dialog)
    (verify (bool (tasks/ui guiexist :repositories-dialog)))
    (if (= 0 (tasks/ui getrowcount :repo-table))
      (throw (Exception. "Repositories table is not populated"))
      (do
        (let [row-count (tasks/ui getrowcount :repo-table)
             list-row (into [] (range row-count))
             random-row-num (nth list-row (rand (count list-row)))]
          (tasks/ui selectrowindex :repo-table random-row-num)
          (verify (not (tasks/has-state? :repo-remove-override "enabled")))
          (tasks/ui checkrow :repo-table random-row-num 0)
          (sleep 2000)
          (tasks/ui checkrow :repo-table random-row-num 1)
          (sleep 2000)
          (verify (tasks/has-state? :repo-remove-override "enabled"))
          (assert-and-remove-all-override)
          (sleep 2000)
          (verify (not (tasks/has-state? :repo-remove-override "enabled"))))))
    (finally
     (tasks/ui click :close-repo-dialog)
     (tasks/unsubscribe_all))))

(defn ^{Test {:groups ["repo"
                       "tier3"
                       "blockedByBug-1095938"
                       "assert_remove_all_overrides"]
              :dataProvider "repolist"}}
  enable_repo_remove_all_overrides
  "Enable all repos and click remove all override and check state"
  [_ repo]
  (assert-and-open-repo-dialog)
  (tasks/ui selectrow :repo-table repo)
  (let [row-num (tasks/ui gettablerowindex :repo-table repo)]
    (tasks/ui checkrow :repo-table row-num 1)
    (sleep 2000)
    (tasks/ui checkrow :repo-table row-num 0)
    (sleep 2000))
  (verify (tasks/has-state? :repo-remove-override "enabled"))
  (assert-and-remove-all-override)
  (verify (not (tasks/has-state? :repo-remove-override "enabled"))))

(defn ^{AfterGroups {:groups ["repo"
                              "tier3"]
                     :value ["assert_remove_all_overrides"]
                     :alwaysRun true}}
  after_enable_repo_remove_all_overrides
  [_]
  (tasks/ui click :close-repo-dialog)
  (tasks/unsubscribe_all))

(defn ^{BeforeGroups {:groups ["repo"
                               "tier3"]
                      :value ["assert_override_persistance"]}}
  before_verify_override_persistance
  "Modofies all repos by clicking edit gpg-check"
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/subscribe_all)
  (assert-and-open-repo-dialog)
  (tasks/do-to-all-rows-in :repo-table 1
                           (fn [repo]
                             (sleep 1000)
                             (tasks/ui selectrow :repo-table repo)
                             (let [row-num (tasks/ui gettablerowindex :repo-table repo)]
                               (if (not (bool (tasks/ui verifycheckrow :repo-table row-num 1)))
                                 (do (tasks/ui checkrow :repo-table row-num 1)
                                     (sleep 2000))
                                 (do (tasks/ui checkrow :repo-table row-num 0)
                                     (sleep 2000))))))
  (tasks/ui click :close-repo-dialog)
  (tasks/unsubscribe_all))

(defn ^{Test {:groups ["repo"
                       "tier3"
                       "blockedByBug-1095938"
                       "assert_override_persistance"]
              :dataProvider "repolist"}}
  verify_override_persistance
  "Checks the persistance of repo override after subscriptions are removed"
  [_ repo]
  (assert-and-open-repo-dialog)
  (tasks/ui selectrow :repo-table repo)
  (verify (bool (and (tasks/has-state? :repo-remove-override "visible")
                     (tasks/has-state? :repo-remove-override "enabled")))))

(defn ^{AfterGroups {:groups ["repo"
                              "tier3"]
                     :value ["assert_override_persistance"]
                     :alwaysRun true}}
  after_verify_override_persistance
  [_]
  (assert-and-open-repo-dialog)
  (tasks/do-to-all-rows-in :repo-table 1
                           (fn [repo]
                             (tasks/ui selectrow :repo-table repo)
                             (assert-and-remove-all-override)))
  (tasks/ui click :close-repo-dialog)
  (tasks/unsubscribe_all))

(defn ^{Test {:groups ["repo"
                       "tier3"
                       "blockedByBug-1095938"]
              :value ["assert_repo_dialog_fields"]
              :dataProvider "repolist"}}
  check_repo_name_url
  "Checks if name and URL are populated for all repositories"
  [_ repo]
  (assert-and-open-repo-dialog)
  (tasks/ui selectrow :repo-table repo)
  (verify (not (blank? (tasks/ui gettextvalue :base-url))))
  (verify (not (blank? (tasks/ui gettextvalue :repo-name)))))

(defn ^{AfterGroups {:groups ["repo"
                              "tier3"]
                     :value ["assert_repo_dialog_fields"]
                     :alwaysRun true}}
  after_check_repo_name_url
  [_]
  (if (bool (tasks/ui guiexist :repositories-dialog))
    (tasks/ui click :close-repo-dialog))
  (tasks/unsubscribe_all))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATA PROVIDERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^{DataProvider {:name "repolist"}}
  subscribed_repos [_ & {:keys [debug]
                         :or {debug false}}]
  (log/info (str "======= Starting DataProvider: " ns-log "subscribed_repos()"))
  (if-not (assert-skip :repo)
    (do
      (if (tasks/ui showing? :register-system)
        (tasks/register-with-creds))
      (assert-and-subscribe-all)
      (assert-and-open-repo-dialog)
      (tasks/ui waittillwindowexist :repositories-dialog 10)
      (sleep 3000)
      (let [repos (into [] (map vector (tasks/get-table-elements
                                        :repo-table 2)))]
        (if-not debug
          (to-array-2d repos)
          repos)))
    (to-array-2d [])))

(gen-class-testng)
