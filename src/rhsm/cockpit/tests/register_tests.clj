(ns rhsm.cockpit.tests.register-tests
  (:use [test-clj.testng :only [gen-class-testng]]
        [clojure.test :only [is]]
        [com.redhat.qe.verify :only [verify]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [rhsm.gui.tasks.test-config :as c]
            [rhsm.cockpit.locales :as locales]
            [rhsm.cockpit.tasks :as tasks]
            [rhsm.cockpit.web :as web]
            [clojure.string :as str])
  (:import [org.testng.annotations
            BeforeSuite
            AfterSuite
            DataProvider
            Test]
           [rhsm.base SubscriptionManagerCLITestScript]
           [com.redhat.qe.auto.testng TestScript]
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
           ;; org.openqa.selenium.chrome.ChromeDriver
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.By
           org.openqa.selenium.firefox.FirefoxDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait
           java.util.logging.Level))

(def driver (atom nil))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  ;; (.. (new SubscriptionManagerCLITestScript) setupBeforeSuite)
  ;; (c/init)
  (locales/load-catalog)
  (.runCommandAndWait @c/clientcmd "yum -y install cockpit-system")
  (reset! driver  (doto (new FirefoxDriver (doto (DesiredCapabilities/firefox)
                                             (.setCapability "acceptSslCerts" true)
                                             (.setCapability "marionette" false)))
                    (.setLogLevel Level/OFF))))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_]
  (.close @driver ))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"
                       "tier2"
                       "tier3"]
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
                       "tier1"
                       "tier2"
                       "tier3"]
              :dataProvider "run-command"
              :dependsOnMethods ["package_is_installed"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99248"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
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
                        :testCaseID ["RHEL7-99507"]
                        :level `DefTypes$Level/COMPONENT
                        :testtype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
  subscription_status
  [ts driver run-command locale language]
  (log/info "status test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  ;; grab a text "Updating subscription status" - it is next to a rotating icon
  (let [texts (->> (web/retrieving-subscription-status-text driver)
                  str/split-lines
                  (map str/trim)
                  (into [locale]))]
    ;; (is (= (cli/subscription-status run-command locale)
    ;;        (.. (web/subscription-status driver) (getText))))

    ;; is 'retrieving subscription status' translated?
    (when (not= locale "en_US.UTF-8")
      (verify (not= [locale "Retrieving subscription status..." "Updating"] texts)))
    (let [[first-part second-part] texts]
      (locales/verify-against-catalog "Retrieving subscription status..." first-part :locale locale)
      (locales/verify-against-catalog "Updating"  second-part :locale locale))
    (let [register-button-text (.. (web/register-button driver) getText)]
      (when (not= locale "en_US.UTF-8")
        (verify (not= "Register" register-button-text)))
      (locales/verify-against-catalog "Register" register-button-text :locale locale ))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99507"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
  subscription_status_for_each_locale
  [ts driver run-command locale language]
  (subscription_status ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
  register
  [ts driver run-command locale language]
  (log/info "register test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (.. (web/register-button driver) click)
  (.. (web/register-username driver) (sendKeys (into-array [(@c/config :username)])))
  (.. (web/register-password driver) (sendKeys (into-array [(@c/config :password)])))

  (when-not (clojure.string/blank? (@c/config :owner-key))
    (.. (web/register-org driver) (sendKeys (into-array [(@c/config :owner-key)]))))

  (let [actual-status (log/spy :info (.getText (web/subscription-status driver)))]
    ;; click to register
    (.. (web/register-confirm-button driver) click)
    ;; waiting till the dialog disappears
    (.. (WebDriverWait. driver 60)
        (until
         (ExpectedConditions/invisibilityOfElementLocated
          (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]"))))
    ;; wait until the status is changed
    (.. (WebDriverWait. driver 60)
        (until
         (ExpectedConditions/not
          (ExpectedConditions/textToBePresentInElementLocated
           (By/cssSelector "div.subscription-status-ct label")
           actual-status)))))

  (log/info "localization checks for locale:" locale)
  ;; localization of the actual status
  (let [phrase-status-invalid (locales/get-phrase "Status: Invalid" :locale locale)
        phrase-status-valid   (locales/get-phrase "Status: Valid" :locale locale)
        subscription-status (.. (web/subscription-status driver) getText) ]
    (when (not= locale "en_US.UTF-8")
      ;; status must not be any of english translations (ie. it is already translated)
      (verify (not (some #{subscription-status} #{"Status: Invalid" "Status: Valid"}))))
    (is (some #{subscription-status} #{phrase-status-invalid phrase-status-valid}))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_for_each_locale
  [ts driver run-command locale language]
  (register ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_no_field_set
  [ts driver run-command locale language]
  (log/info "register with no field test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (is (= (.. (web/register-button driver) getText) "Register"))
  (.. (web/register-button driver) click)
  (.. (web/register-username driver) clear)
  (.. (web/register-password driver) clear)
  (.. (web/register-org driver) clear)
  (.. (web/register-confirm-button driver) click)
  (let [error-text (.. (web/register-error-box driver) getText)]
    (is (= "Error: Login/password or activation key required to register." error-text))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_no_field_set_for_each_locale
  [ts driver run-command locale language]
  (register_with_no_field_set ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_empty_password
  [ts driver run-command locale language]
  (log/info "register with empty password test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (.. (web/register-button driver) click)
  (.. (web/register-username driver) (sendKeys (into-array [(@c/config :username)])))
  (.. (web/register-password driver) clear)
  (when-not (clojure.string/blank? (@c/config :owner-key))
    (.. (web/register-org driver) (sendKeys (into-array [(@c/config :owner-key)]))))
  (.. (web/register-confirm-button driver) click)
  (let [error-text (.. (web/register-error-box driver) getText)
        error-text-01 "Error: Login/password or activation key required to register."
        error-text-02 (format "Error: Invalid credentials (Registering to: %s:%s%s)"
                              (@c/config :server-hostname)
                              (@c/config :server-port)
                              (@c/config :server-prefix))]
    (is (some #{error-text} [error-text-01 error-text-02]))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_empty_password_for_each_locale
  [ts driver run-command locale language]
  (register_with_empty_password ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_wrong_password
  [ts driver run-command locale language]
  (log/info "register with wrong password test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (.. (web/register-button driver) click)
  (.. (web/register-username driver) (sendKeys (into-array [(@c/config :username)])))
  (.. (web/register-password driver) (sendKeys (into-array ["some really wrong password"])))
  (when-not (clojure.string/blank? (@c/config :owner-key))
    (.. (web/register-org driver) (sendKeys (into-array [(@c/config :owner-key)]))))
  (.. (web/register-confirm-button driver) click)
  (let [error-text (.. (web/register-error-box driver) getText)
        error-text-01 (format "Error: Invalid username or password (To create a login, please visit https://www.redhat.com/wapps/ugc/register.html Registering to: %s:%s%s)"
                              (@c/config :server-hostname)
                              (@c/config :server-port)
                              (@c/config :server-prefix))
        error-text-02 (format "Error: Invalid credentials (Registering to: %s:%s%s)"
                              (@c/config :server-hostname)
                              (@c/config :server-port)
                              (@c/config :server-prefix))]
        (is (some #{error-text} [error-text-01 error-text-02]))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_wrong_password_for_each_locale
  [ts driver run-command locale language]
  (register_with_wrong_password ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_empty_login
  [ts driver run-command locale language]
  (log/info "register with empty login test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (let [subscriptions-iframe (.. (WebDriverWait. driver 60)
                                 (until
                                  (ExpectedConditions/visibilityOfElementLocated
                                   (By/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]"))))]
    (.. driver (switchTo) (defaultContent))
    (.. driver (switchTo) (frame subscriptions-iframe))
    (.. (web/register-button driver) click)
    (.. (web/register-username driver) clear)
    (.. (web/register-password driver)
        (sendKeys (into-array [(@c/config :password)])))
    (.. (web/register-confirm-button driver) click)
    (let [error-text (.. (web/register-error-box driver) getText)]
      (is (= "Error: Login/password or activation key required to register."
             error-text)))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_empty_login_for_each_locale
  [ts driver run-command locale language]
  (register_with_empty_login ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_wrong_login
  [ts driver run-command locale language]
  (log/info "register with wrong login test")
  (run-command "subscription-manager unregister")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (.. (web/register-button driver) click)
  (.. (web/register-username driver)
      (sendKeys (into-array ["some wrong login"])))
  (.. (web/register-password driver)
      (sendKeys (into-array [(@c/config :password)])))
  (.. (web/register-confirm-button driver) click)
  (let [error-text (.. (web/register-error-box driver) getText)
        error-text-01 (format "Error: Invalid username or password (To create a login, please visit https://www.redhat.com/wapps/ugc/register.html Registering to: %s:%s%s)"
                              (@c/config :server-hostname)
                              (@c/config :server-port)
                              (@c/config :server-prefix))
        error-text-02 (format "Error: Invalid credentials (Registering to: %s:%s%s)"
                              (@c/config :server-hostname)
                              (@c/config :server-port)
                              (@c/config :server-prefix))]
    (is (some #{error-text} [error-text-01 error-text-02]))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/NEGATIVE
                        :importance `DefTypes$Importance/MEDIUM
                        :automation `DefTypes$Automation/AUTOMATED}}
  register_with_wrong_login_for_each_locale
  [ts driver run-command locale language]
  (register_with_wrong_login ts driver run-command locale language))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier1"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
  unregister
  [ts driver run-command locale language]
  (log/info "unregister test")
  (run-command "subscription-manager unregister")
  (let [output (->> (format "subscription-manager register --username=%s --password=%s --org=%s"
                            (@c/config :username) (@c/config :password) (@c/config :owner-key))
                    run-command
                    :stdout) ]
    (log/info (format "does a command output '%s' contain of 'Registering to:'?" output))
    (is (.contains output "Registering to:")))
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (.. (web/register-button driver) click)
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/textToBePresentInElementLocated
        (By/cssSelector "div.subscription-status-ct label")
        "Status: System isn't registered"))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]
                        :level `DefTypes$Level/COMPONENT
                        :testype `TestType
                        :component "subscription-manager"
                        :tags ["Tier3"]
                        :posneg `DefTypes$PosNeg/POSITIVE
                        :importance `DefTypes$Importance/HIGH
                        :automation `DefTypes$Automation/AUTOMATED}}
  unregister_for_each_locale
  [ts driver run-command locale language]
  (unregister ts driver run-command locale language))

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
       [@driver (partial run-command @c/clientcmd) "ca_ES.UTF-8" "ca-es"]
       [@driver (partial run-command @c/clientcmd) "da_DK.UTF-8" "da-dk"]
       [@driver (partial run-command @c/clientcmd) "de_DE.UTF-8" "de-de"]
       [@driver (partial run-command @c/clientcmd) "es_ES.UTF-8" "es-es"]
       [@driver (partial run-command @c/clientcmd) "fr_FR.UTF-8" "fr-fr"]
       [@driver (partial run-command @c/clientcmd) "pl_PL.UTF-8" "pl-pl"]
       [@driver (partial run-command @c/clientcmd) "pt_BR.UTF-8" "pt-br"]
       [@driver (partial run-command @c/clientcmd) "tr_TR.UTF-8" "tr-tr"]
       [@driver (partial run-command @c/clientcmd) "uk_UA.UTF-8" "uk-ua"]
       [@driver (partial run-command @c/clientcmd) "zh_CN.UTF-8" "zh-cn"]]
      to-array-2d))

(gen-class-testng)
