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
  cleanup [_]
  (.close @driver ))

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
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99507"]}}
  subscription_status
  [ts driver run-command locale language]
  (log/info "status test")
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
        (is (= status subscriptions-status))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99507"]}}
  subscription_status_for_each_locale
  [ts driver run-command locale language]
  (subscription_status ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register
  [ts driver run-command locale language]
  (log/info "register test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          (sendKeys (into-array [(@c/config :username)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          (sendKeys (into-array [(@c/config :password)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      ;; waiting till the dialog disappears
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/invisibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]"))))
      (let [subscriptions-status (.. (WebDriverWait. driver 60)
                                     (until
                                      (ExpectedConditions/visibilityOfElementLocated
                                       (By/cssSelector "div.subscription-status-ct label")))
                                     (getText))]
        (is (= "Status: Invalid" subscriptions-status))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_for_each_locale
  [ts driver run-command locale language]
  (register ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_no_field_set
  [ts driver run-command locale language]
  (log/info "register with no field test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          clear)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          clear)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      (let [error-element (.. (WebDriverWait. driver 60)
                              (until
                               (ExpectedConditions/visibilityOfElementLocated
                                (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]"))))]
        (is (= "Error: Login/password or activation key required to register."
               (.. error-element getText)))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_no_field_set_for_each_locale
  [ts driver run-command locale language]
  (register_with_no_field_set ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_empty_password
  [ts driver run-command locale language]
  (log/info "register with empty password test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          (sendKeys (into-array [(@c/config :username)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          clear)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      (let [error-element (.. (WebDriverWait. driver 60)
                              (until
                               (ExpectedConditions/visibilityOfElementLocated
                                (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]"))))]
        (is (= "Error: Login/password or activation key required to register."
               (.. error-element getText)))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_empty_password_for_each_locale
  [ts driver run-command locale language]
  (register_with_empty_password ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_wrong_password
  [ts driver run-command locale language]
  (log/info "register with wrong password test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          (sendKeys (into-array [(@c/config :username)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          (sendKeys (into-array ["some really wrong password"])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      (let [error-element (.. (WebDriverWait. driver 60)
                              (until
                               (ExpectedConditions/visibilityOfElementLocated
                                (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]"))))]
        (is (= "Error: Invalid username or password (To create a login, please visit https://www.redhat.com/wapps/ugc/register.html Registering to: subscription.rhsm.stage.redhat.com:443/subscription)"
               (.. error-element getText)))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_wrong_password_for_each_locale
  [ts driver run-command locale language]
  (register_with_wrong_password ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  register_with_empty_login
  [ts driver run-command locale language]
  (log/info "register with empty login test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          clear)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          (sendKeys (into-array [(@c/config :password)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      (let [error-element (.. (WebDriverWait. driver 60)
                              (until
                               (ExpectedConditions/visibilityOfElementLocated
                                (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]"))))]
        (is (= "Error: Login/password or activation key required to register."
               (.. error-element getText)))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_empty_login_for_each_locale
  [ts driver run-command locale language]
  (register_with_empty_login ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_wrong_login
  [ts driver run-command locale language]
  (log/info "register with wrong login test")
  (run-command "subscription-manager unregister")
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
    (let [subscriptions-action (.. (WebDriverWait. driver 60)
                                   (until
                                    (ExpectedConditions/visibilityOfElementLocated
                                     (By/cssSelector "div.subscription-status-ct button"))))]
      (is (= (.. subscriptions-action getText) "Register"))
      (.. subscriptions-action click)
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-username")))
          (sendKeys (into-array ["some wrong login"])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-password")))
          (sendKeys (into-array [(@c/config :password)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))
          click)
      (let [error-element (.. (WebDriverWait. driver 60)
                              (until
                               (ExpectedConditions/visibilityOfElementLocated
                                (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]"))))]
        (is (= "Error: Invalid username or password (To create a login, please visit https://www.redhat.com/wapps/ugc/register.html Registering to: subscription.rhsm.stage.redhat.com:443/subscription)"
               (.. error-element getText)))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_wrong_login_for_each_locale
  [ts driver run-command locale language]
  (register_with_wrong_login ts driver run-command locale language))

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

(defn ^{DataProvider {:name "client-with-webdriver-and-english-locale"}}
  webdriver_with_english_locale
  "It provides a running Chrome/Firefox instance."
  [_]
  (.. @driver (get (format "http://%s:%d" (@c/config :client-hostname) 9090)))
  (when-not (tasks/is-logged? @driver)
    (tasks/log-user-in @driver
                       (@c/config :cockpit-username)
                       (@c/config :cockpit-password)))

  (-> [[@driver (partial run-command @c/clientcmd) "en_US.UTF-8" "en-us"]]
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

(gen-class-testng)
