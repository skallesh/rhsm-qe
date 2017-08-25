(ns rhsm.cockpit.tests.register-tests
  (:use [test-clj.testng :only [gen-class-testng]]
        [clojure.test :only [is]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [rhsm.gui.tasks.test-config :as c]
            [rhsm.cockpit.tasks :as tasks]
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
           ;; org.openqa.selenium.chrome.ChromeDriver
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.By
           org.openqa.selenium.firefox.FirefoxDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait))

(def driver (atom nil))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (c/init)
  (reset! driver  (new FirefoxDriver (let [cap (DesiredCapabilities/firefox)]
                                       (.setCapability cap "acceptSslCerts" true)
                                       (.setCapability cap "marionette" false)
                                       cap))))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_])

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "run-command"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  package_is_installed
  [ts run-command]
  (log/info "package_is_installed")
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
  (log/info "service_is_running")
  (let [result (->> "systemctl status cockpit.service"
                    run-command
                    :stdout)]
    (is (.contains result "cockpit.service - Cockpit Web Service"))
    (is (re-find #"\n[ \t]+Loaded:[ \t]+loaded" result))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  register
  [ts driver run-command locale language]
  (log/info "register test")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//a[@href='/subscriptions']")))
      click)
  (let [subscriptions-iframe (.. (WebDriverWait. driver 60)
                                 (until
                                  (ExpectedConditions/visibilityOfElementLocated
                                   (By/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]"))))]
    (.. driver (switchTo) (defaultContent))
    (.. driver (switchTo) (frame subscriptions-iframe))
    (let [status (->> "subscription-manager status"
                      run-command
                      :stdout
                      (re-find #"Status:[\ \t]+([^\n]+)")
                      first)]
      (let [subscriptions-status (.. (WebDriverWait. driver 60)
                                     (until
                                        (ExpectedConditions/visibilityOfElementLocated
                                         (By/cssSelector "div.subscription-status-ct label")))
                                     (getText))]
        (is (= status subscriptions-status))
        (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                       (until
                                        (ExpectedConditions/visibilityOfElementLocated
                                         (By/cssSelector "div.subscription-status-ct button")))
                                       (getText))]
          (if (.contains status "Unknown")
            (is (= subscriptions-action "Register"))
            (is (= subscriptions-action "Unregister")))
          )
        )
      )
    )
  )


;; (defn ^{Test {:groups ["register"
;;                        "cockpit"
;;                        "tier1"]
;;               :dataProvider "client-with-webdriver-with-locale"
;;               :dependsOnMethods ["service_is_running"]}
;;         TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
;;   register
;;   [ts driver run-command]
;;   (let [subscriptions-link (.. (WebDriverWait. driver 60)
;;                                (until
;;                                 (ExpectedConditions/visibilityOfElementLocated
;;                                  (by/xpath "//a[@href='/subscriptions']"))))]
;;     (element/click subscriptions-link)
;;     (let [subscriptions-iframe (.. (WebDriverWait. driver 60)
;;                                    (until
;;                                     (ExpectedConditions/visibilityOfElementLocated
;;                                      (by/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]"))))]
;;       (.. driver (switchTo) (defaultContent))
;;       (.. driver (switchTo) (frame subscriptions-iframe))
;;       (let [status (->> "subscription-manager status"
;;                         run-command
;;                         :stdout
;;                         (re-find #"Status:[\ \t]+([^\n]+)")
;;                         first)]
;;         (let [subscriptions-status (.. (WebDriverWait. driver 60)
;;                                        (until
;;                                         (ExpectedConditions/visibilityOfElementLocated
;;                                          (by/css-selector "div.subscription-status-ct label")))
;;                                        (getText))]
;;           (is (= status subscriptions-status))
;;           (let [subscriptions-action (.. (WebDriverWait. driver 60)
;;                                          (until
;;                                           (ExpectedConditions/visibilityOfElementLocated
;;                                            (by/css-selector "div.subscription-status-ct button")))
;;                                          (getText))]
;;             (if (.contains status "Unknown")
;;               (is (= subscriptions-action "Register"))
;;               (is (= subscriptions-action "Unregister")))))))))

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

(defn ^{DataProvider {:name "run-command"}}
  provide_run_command
  "It provides a running Chrome/Firefox instance."
  [_]
  (-> [(partial run-command @c/clientcmd)]
      vector
      to-array-2d))

(defn ^{DataProvider {:name "client-with-webdriver-and-locale"}}
  webdriver_with_locale
  "It provides a running Chrome/Firefox instance."
  [_]
  (.. @driver (get (format "http://%s:%d" (@c/config :client-hostname) 9090)))
  (when-not (tasks/is-logged? @driver)
    (tasks/log-user-in @driver
                       (@c/config :cockpit-username)
                       (@c/config :cockpit-password)))

  (-> [[@driver (partial run-command @c/clientcmd) "en_US.UTF-8" "en-us"]
       [@driver (partial run-command @c/clientcmd) "ja_JP.UTF-8" "ja-ja"]
       [@driver (partial run-command @c/clientcmd) "es_ES.UTF-8" "es-es"]
       [@driver (partial run-command @c/clientcmd) "de_DE.UTF-8" "de-de"]
       [@driver (partial run-command @c/clientcmd) "fr_FR.UTF-8" "fr-fr"]]
      to-array-2d))

;; (defn ^{DataProvider {:name "client-with-webdriver-and-locale"}}
;;   webdriver
;;   "It provides a running Chrome/Firefox instance."
;;   [_]
;;   (let [driver ;;(chrome/start-chrome)
;;         (firefox/start-firefox)]
;;     (log-user-in driver
;;                  (@c/config :client-hostname)
;;                  (@c/config :cockpit-username)
;;                  (@c/config :cockpit-password))
;;     (set-users-locale driver "es-es") ;; ja-ja, es-es en-us
;;     (-> [driver (partial run-command @c/clientcmd)]
;;         vector
;;         to-array-2d)))

(gen-class-testng)
