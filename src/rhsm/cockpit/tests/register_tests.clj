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
  (c/init)
  (chrome/start-chrome))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_]
  (browser/quit))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver"}
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

(gen-class-testng)

(comment
  (def client-hostname "jstavel-rhel7-latest-server.usersys.redhat.com")
  (def client-cockpit-username "root")
  (def client-cockpit-password "redhat")

  ;;(def driver1 (ChromeDriver. "/usr/local/bin/chromedriver"))
  ;;(def driver (chrome/start-chrome "/usr/local/bin/chromedriver"))
  (System/setProperty "webdriver.chrome.driver" "/usr/local/bin/chromedriver")
  (def dd (chrome/start-chrome))
  (def dd2 (ChromeDriver.))
  (browser/get (format "http://%s:%d" client-hostname 9090))
  (browser/get dd2 "http://www.google.com")
  (def el (browser/find-element-by-css-selector ".page"))
  (def el2 (browser/find-element-by-css-selector dd2 ".page"))
  (element/get-text el2)
  (.get driver (format "http://%s:%d" client-hostname 9090))
  ;;(.findElement driver (by/id "login-user-input"))
  ;;(driver/find-element driver (by/id "login-user-input"))

  (def login-user-input (.until (new WebDriverWait dd 10)
                                (ExpectedConditions/visibilityOfElementLocated
                                 (by/id "login-user-input"))))
  (def login-password-input (.until (new WebDriverWait dd 10)
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (by/id "login-password-input"))))
  (def login-button (.until (new WebDriverWait dd 10)
                            (ExpectedConditions/visibilityOfElementLocated
                             (by/id "login-button"))))
  
  ;;(def login-user-input (browser/find-element (by/id "login-user-input")))
  ;;(def login-password-input (browser/find-element (by/id "login-password-input")))
  ;; (def login-button (.findElement driver (by/id "login-button")))

  (element/send-keys login-user-input client-cockpit-username)
  (element/send-keys login-password-input client-cockpit-password)
  (element/click login-button)
  (def subscriptions-link (.. (WebDriverWait. dd 10)
                              (until (ExpectedConditions/visibilityOfElementLocated
                                      (by/xpath "//a[@href='/subscriptions']")))))
  ;;(def subscriptions-link (browser/find-element (by/xpath "//a[@href='/subscriptions']")))
  (element/click subscriptions-link)
  (def subscriptions-iframe (browser/find-element
                             dd
                             (by/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]")))
  ;;(element/get-text content)
  (.. dd (switchTo) (defaultContent))
  (.. dd (switchTo) (frame subscriptions-iframe))
  (.. dd (findElement (by/css-selector "div.subscription-status-ct")))
  (def subscriptions-status
    (->> (by/css-selector "div.subscription-status-ct label")
         (browser/find-element dd)
         (element/get-text)))
  (def subscriptions-action
    (->> (by/css-selector "div.subscription-status-ct button")
         (browser/find-element dd)
         (element/get-text)))
  (.. (WebDriverWait. dd 60)
      (until (ExpectedConditions/visibilityOfElementLocated
              (by/css-selector "div.subscription-status-ct label")))
      (getText))
  )
