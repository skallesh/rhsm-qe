(ns rhsm.cockpit.tests.proxy-tests
  (:use [test-clj.testng :only [gen-class-testng]]
        [clojure.test :only [is]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [rhsm.gui.tasks.test-config :as c]
            [rhsm.cockpit.tasks :as tasks])
  (:import [org.testng.annotations
            BeforeSuite
            AfterSuite
            DataProvider
            Test]
           [com.github.redhatqe.polarize.metadata
            TestDefinition
            TestType
            DefTypes
            DefTypes$Project
            DefTypes$TestTypes
            DefTypes$Subtypes
            DefTypes$Level
            DefTypes$PosNeg
            DefTypes$Importance
            DefTypes$Automation
            LinkedItem]
           [rhsm.base SubscriptionManagerCLITestScript]
           org.testng.SkipException
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.By
           org.openqa.selenium.firefox.FirefoxDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait))

(def driver (atom nil))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (.runCommandAndWait @c/clientcmd "yum -y install cockpit-system")
  (reset! driver  (new FirefoxDriver (let [cap (DesiredCapabilities/firefox)]
                                       (.setCapability cap "acceptSslCerts" true)
                                       (.setCapability cap "marionette" false)
                                       cap))))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_]
  (.close @driver ))

(defn ^{Test {:groups ["proxy" "cockpit" "tier2" "tier3"]
              :dataProvider "client-with-webdriver-and-english-locale"}}
  auth_proxy_works
  [ts driver run-command locale language]

  ;; curl -k --proxy-user redhat:redhat \
  ;;      -x auto-services.usersys.redhat.com:3128 \
  ;;      https://jstavel-candlepin.usersys.redhat.com:8443/candlepin/status
  (let [stdout (-> (format "curl -k --proxy-user %s:%s -x %s:%s https://%s:%s%s/status"
                           (@c/config :basicauth-proxy-username)
                           (@c/config :basicauth-proxy-password)

                           (@c/config :basicauth-proxy-hostname)
                           (@c/config :basicauth-proxy-port)

                           (@c/config :server-hostname)
                           (@c/config :server-port)
                           (@c/config :server-prefix))
                   run-command
                   :stdout)]
    (is (clojure.string/includes? stdout "managerCapabilities"))))

(defn ^{Test {:groups ["proxy" "cockpit" "tier2" "tier3"]
              :dataProvider "client-with-webdriver-and-english-locale"}}
  noauth_proxy_works
  [ts driver run-command locale language]

  ;; curl -k -x auto-services.usersys.redhat.com:3129 \
  ;;      https://jstavel-candlepin.usersys.redhat.com:8443/candlepin/status
  (let [stdout (-> (format "curl -k -x %s:%s https://%s:%s%s/status"
                           (@c/config :noauth-proxy-hostname)
                           (@c/config :noauth-proxy-port)

                           (@c/config :server-hostname)
                           (@c/config :server-port)
                           (@c/config :server-prefix))
                   run-command
                   :stdout)]
    (is (clojure.string/includes? stdout "managerCapabilities"))))

(defn ^{Test {:groups ["proxy"
                       "cockpit"
                       "tier2"
                       "blockedByBug-1498778"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["auth_proxy_works" "service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99668"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier2"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_auth_proxy
  [ts driver run-command locale language]
  (log/info "register with auth proxy test")
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
            (By/id "subscription-proxy-use")))
          (click))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-proxy-server")))
          (sendKeys (into-array [(@c/config :basicauth-proxy-hostname)
                                 ":"
                                 (@c/config :basicauth-proxy-port)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-proxy-user")))
          (sendKeys (into-array [(@c/config :basicauth-proxy-username)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-proxy-password")))
          (sendKeys (into-array [(@c/config :basicauth-proxy-password)])))
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
      (when-not (clojure.string/blank? (@c/config :owner-key))
        (.. (WebDriverWait. driver 60)
            (until
             (ExpectedConditions/visibilityOfElementLocated
              (By/id "subscription-register-org")))
            (sendKeys (into-array [(@c/config :owner-key)]))))
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
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/textToBePresentInElementLocated
            (By/cssSelector "div.subscription-status-ct label")
            "Status: System isn't registered"))))))

(defn ^{Test {:groups ["proxy"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["auth_proxy_works" "service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99668"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_auth_proxy_for_each_locale
  [ts driver run-command locale language]
  (register_with_auth_proxy ts driver run-command locale language))

(defn ^{Test {:groups ["proxy"
                       "cockpit"
                       "tier2"
                       "blockedByBug-1498778"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["noauth_proxy_works" "service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99668"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier2"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_noauth_proxy
  [ts driver run-command locale language]
  (log/info "register with noauth proxy test")
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
            (By/id "subscription-proxy-use")))
          (click))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-proxy-server")))
          (sendKeys (into-array [(@c/config :noauth-proxy-hostname)
                                 ":"
                                 (@c/config :noauth-proxy-port)])))
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
      (when-not (clojure.string/blank? (@c/config :owner-key))
        (.. (WebDriverWait. driver 60)
            (until
             (ExpectedConditions/visibilityOfElementLocated
              (By/id "subscription-register-org")))
            (sendKeys (into-array [(@c/config :owner-key)]))))
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
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/textToBePresentInElementLocated
            (By/cssSelector "div.subscription-status-ct label")
            "Status: System isn't registered"))))))

(defn ^{Test {:groups ["proxy"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["auth_proxy_works" "service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99668"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_noauth_proxy_for_each_locale
  [ts driver run-command locale language]
  (register_with_noauth_proxy ts driver run-command locale language))

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
