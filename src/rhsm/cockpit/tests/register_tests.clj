(ns rhsm.cockpit.tests.register-tests
  (:use [test-clj.testng :only [gen-class-testng]]
        [clojure.test :only [is]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [webica.core :as w]
            [webica.by :as by]
            [webica.web-driver :as driver]
            [webica.firefox-driver :as firefox]
            [webica.chrome-driver :as chrome]
            [webica.web-element :as element]
            [webica.remote-web-driver :as browser]
            [webica.web-driver-wait :as wait]
            [rhsm.gui.tasks.test-config :as c]
            ;;[rhsm.cockpit.tasks :as tasks]
            )
  (:import [org.testng.annotations
            BeforeSuite
            AfterSuite
            DataProvider
            Test]
           [rhsm.base SubscriptionManagerCLITestScript]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]
           org.testng.SkipException
           org.openqa.selenium.chrome.ChromeDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (c/init))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_]
  (browser/quit))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "run-command"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  package_is_installed
  [ts run-command]
  (->> "rpm -qa | grep cockpit"
       run-command
       :stdout
       clojure.string/split-lines
       (filter (partial re-find #"^cockpit-"))
       count
       (< 1) ;; let the num of rows is larger that 1
       is))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "run-command"
              :dependsOnMethods ["package_is_installed"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  service_is_running
  [ts run-command]
  "[root@jstavel-rhel7-latest-server ~]# systemctl status cockpit.service
● cockpit.service - Cockpit Web Service
   Loaded: loaded (/usr/lib/systemd/system/cockpit.service; static; vendor preset: disabled)"
  (let [result #spy/d (->> "systemctl status cockpit.service"
                           run-command
                           :stdout)]
    (is (.contains result "cockpit.service - Cockpit Web Service"))
    (is (re-find #"\n[ \t]+Loaded:[ \t]+loaded" result))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  register
  [ts driver run-command]
  (browser/get driver (format "http://%s:%d" (@c/config :client-hostname) 9090))
  (let [login-user-input (.. (WebDriverWait. driver 10)
                             (until (ExpectedConditions/visibilityOfElementLocated
                                     (by/id "login-user-input"))))
        login-password-input (.. (WebDriverWait. driver 10)
                                 (until (ExpectedConditions/visibilityOfElementLocated
                                         (by/id "login-password-input"))))
        login-button (.. (WebDriverWait. driver 10)
                         (until (ExpectedConditions/visibilityOfElementLocated
                                 (by/id "login-button"))))]
    (element/send-keys login-user-input (@c/config :cockpit-username))
    (element/send-keys login-password-input (@c/config :cockpit-password))
    (element/click login-button)
    (let [subscriptions-link (.. (WebDriverWait. driver 60)
                                 (until (ExpectedConditions/visibilityOfElementLocated
                                         (by/xpath "//a[@href='/subscriptions']"))))]
      (element/click subscriptions-link)
      (let [subscriptions-iframe (.. (WebDriverWait. driver 60)
                                     (until (ExpectedConditions/visibilityOfElementLocated
                                             (by/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]"))))]
        (.. driver (switchTo) (defaultContent))
        (.. driver (switchTo) (frame subscriptions-iframe))
        (let [status (->> "subscription-manager status"
                          run-command
                          :stdout
                          (re-find #"Status:[\ \t]+([^\n]+)")
                          first)]
          (let [subscriptions-status (.. (WebDriverWait. driver 60)
                                         (until (ExpectedConditions/visibilityOfElementLocated
                                                 (by/css-selector "div.subscription-status-ct label")))
                                         (getText))]
            (is (= status subscriptions-status))
            (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                           (until (ExpectedConditions/visibilityOfElementLocated
                                                   (by/css-selector "div.subscription-status-ct button")))
                                           (getText))]
              (if (.contains status "Unknown")
                (is (= subscriptions-action "Register"))
                (is (= subscriptions-action "Unregister"))))))))))

(defn run-command
  "Runs a given command on the client using SSHCommandRunner()."
  [runner command]
  (let [result (.runCommandAndWait runner command)
        out (.getStdout result)
        err (.getStderr result)
        exit (.getExitCode result)]
    {:stdout out
     :stderr err
     :exitcode exit}))

(defn ^{DataProvider {:name "client-with-webdriver"}}
  webdriver
  "It provides a running Chrome/Firefox instance."
  [_]
  (let [driver (chrome/start-chrome)]
    (-> [driver (partial run-command @c/clientcmd)]
        vector
        to-array-2d)))

(defn ^{DataProvider {:name "run-command"}}
  provide_run_command
  "It provides a running Chrome/Firefox instance."
  [_]
  (-> [(partial run-command @c/clientcmd)]
      vector
      to-array-2d))

(gen-class-testng)
