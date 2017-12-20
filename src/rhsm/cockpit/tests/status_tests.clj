(ns rhsm.cockpit.tests.status-tests
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
  ;; (locales/load-catalog)
  "installs scripts usable for playing with a file /etc/rhsm/rhsm.conf"
  (.runCommandAndWait @c/clientcmd "mkdir -p ~/bin")
  (.runCommandAndWait @c/clientcmd "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/get-config-value.py > get-config-value.py")
  (.runCommandAndWait @c/clientcmd "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/set-config-value.py > set-config-value.py")
  (.runCommandAndWait @c/clientcmd "chmod 755 ~/bin/get-config-value.py")
  (.runCommandAndWait @c/clientcmd "chmod 755 ~/bin/set-config-value.py")
  ;;-----------------------
  (.runCommandAndWait @c/clientcmd "yum -y install cockpit-system")
  (reset! driver  (doto (new FirefoxDriver (doto (DesiredCapabilities/firefox)
                                             (.setCapability "acceptSslCerts" true)
                                             (.setCapability "marionette" false)))
                    (.setLogLevel Level/OFF))))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  close_selenium_driver [_]
  (.close @driver))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  set_dbus_service_back [_]
  (.runCommandAndWait @c/clientcmd
                      (str " mv "
                           " /etc/dbus-1/system.d/com.redhat.SubscriptionManager.conf.stopped "
                           " /etc/dbus-1/system.d/com.redhat.SubscriptionManager.conf "))
  (.runCommandAndWait @c/clientcmd "systemctl restart dbus.service"))

(defn ^{Test {:groups ["status" "cockpit" "tier1" "tier2" "tier3"]
              :dataProvider "run-command"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99248"]}}
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

(defn ^{Test {:groups ["status" "cockpit" "tier2" "blockedByBug-1511168"]
              :dataProvider "client-with-webdriver-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL-113346"]}}
  cockpit_should_inform_a_user_when_subscription_manager_is_not_installed
  [ts driver run-command locale language]
  (log/info "cockpit should inform a user when subscription manager is not installed.")
  (run-command (str "mv " " /etc/dbus-1/system.d/com.redhat.SubscriptionManager.conf "
                    " /etc/dbus-1/system.d/com.redhat.SubscriptionManager.conf.stopped "))
  (run-command "systemctl restart dbus.service")
  (.. driver (switchTo) (defaultContent))
  (tasks/set-user-language driver language)
  (.. (web/subscriptions-menu-link driver) click)
  (.. driver (switchTo) (defaultContent))
  (.. driver (switchTo) (frame (web/subscriptions-iframe driver)))
  (let [messages (->> (web/message-subscription-manager-is-not-installed driver)
                     (.getText)
                     (str/split-lines)
                     (map str/trim)
                     (into []))]
    (verify (= ["Couldn't get system subscription status. Please ensure subscription-manager is installed." "Unable to connect"]  messages))))

(defn ^{Test {:groups ["status"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  cockpit_should_inform_a_user_when_subscription_manager_is_not_installed_for_each_locale
  [ts driver run-command locale language]
  (cockpit_should_inform_a_user_when_subscription_manager_is_not_installed ts driver run-command locale language))

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
