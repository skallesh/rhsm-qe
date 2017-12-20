(ns rhsm.cockpit.tests.activation-key-tests
  (:use [test-clj.testng :only [gen-class-testng]]
        [clojure.test :only [is]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [rhsm.gui.tasks.test-config :as c]
            [rhsm.cockpit.tasks :as tasks]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            [rhsm.cockpit.tasks :as tasks]
            [clojure.data.json :as json]
            [org.httpkit.client :as http])
  (:import [org.testng.annotations
            BeforeSuite
            AfterSuite
            AfterClass
            DataProvider
            Test]
           [rhsm.base SubscriptionManagerCLITestScript]
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
           org.testng.SkipException
           ;; org.openqa.selenium.chrome.ChromeDriver
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.By
           org.openqa.selenium.firefox.FirefoxDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait
           java.util.logging.Level))

(def driver (atom nil))
(def created-activation-keys (atom (list)))
(def http-options {:timeout 3000
                   :insecure? true
                   :accept :json
                   :content-type :json
                   :keepalive 30000})


(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  (.runCommandAndWait @c/clientcmd "yum -y install cockpit-system")
  (reset! driver (doto (new FirefoxDriver (doto (DesiredCapabilities/firefox)
                                            (.setCapability "acceptSslCerts" true)
                                            (.setCapability "marionette" false)))
                   (.setLogLevel Level/OFF))))

(defn ^{AfterSuite {:groups ["cleanup"]}}
  cleanup [_]
  (.close @driver))

(defn ^{Test {:groups ["register"
                       "activation-key"
                       "cockpit"
                       "tier1"
                       "tier2"
                       "tier3"]
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

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  delete_all_actually_created_activation_keys
  [ts]
  "delete all activation keys created by this test run"
  (log/info "delete all actually created activation keys")
  (log/info "actually created activation keys to be deleted: " @created-activation-keys)
  (doseq [activation-key @created-activation-keys]
    (let [activation-key-id (-> activation-key :id)
          url-of-the-key (format "%s/activation_keys/%s"
                                 (ctasks/server-url)
                                 activation-key-id)]
      @(http/delete url-of-the-key
                    (assoc http-options
                           :basic-auth [(@c/config :username) (@c/config :password)]))
      (swap! created-activation-keys (fn [coll] (filter (fn [x] (not= (-> x :id) activation-key-id)) coll))))))

(defn ^{Test {:groups ["register"
                       "activation-key"
                       "cockpit"
                       "tier1"]
              :dataProvider "client-with-webdriver-and-activation-key-and-english-locale"
              :dependsOnMethods ["service_is_running"]}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_activation_key
  [ts driver run-command activation-key locale language]
  (log/info "register with activation key test")
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
            (By/id "subscription-register-key")))
          (sendKeys (into-array [(:name activation-key)])))
      (.. (WebDriverWait. driver 60)
          (until
           (ExpectedConditions/visibilityOfElementLocated
            (By/id "subscription-register-org")))
          (sendKeys (into-array [(@c/config :owner-key)])))
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
            "Status: Invalid"))))))

(defn ^{Test {:groups ["register"
                       "cockpit"
                       "tier3"]
              :dataProvider "client-with-webdriver-and-activation-key-and-locale"
              :dependsOnMethods ["service_is_running"]
              }
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-99656"]}}
  register_with_activation_key_for_each_locale
  [ts driver run-command activation-key locale language]
  (register_with_activation_key ts driver run-command activation-key locale language))

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

(defn new_activation_key [ts]
  (let [response
        @(http/post
          (format "%s/owners/%s/activation_keys"
                  (ctasks/server-url) (@c/config :owner-key))
          (assoc http-options
                 :basic-auth [(@c/config :username) (@c/config :password)]
                 :body (-> {:name (format "rhsm-cockpit-tests-%d" (System/currentTimeMillis))}
                           json/json-str)))
        status-of-the-response (-> response :status)]
    (is (->> status-of-the-response (= 200)))
    (let [activation-key (-> response :body json/read-json)]
      (log/info "new activation key has been created" activation-key)
      (swap! created-activation-keys conj activation-key)
      activation-key)))

(defn ^{DataProvider {:name "run-command"}}
  provide_run_command
  "It provides a running Chrome/Firefox instance."
  [_]
  (-> [(partial run-command @c/clientcmd)]
      vector
      to-array-2d))

(defn ^{DataProvider {:name "client-with-webdriver-and-activation-key-and-english-locale"}}
  webdriver_with_activation_key_english_locale
  "It provides a running Chrome/Firefox instance."
  [ts]
  (.. @driver (get (format "http://%s:%d" (@c/config :client-hostname) 9090)))
  (when-not (tasks/is-logged? @driver)
    (tasks/log-user-in @driver
                       (@c/config :cockpit-username)
                       (@c/config :cockpit-password)))

  (-> [[@driver (partial run-command @c/clientcmd) (new_activation_key ts) "en_US.UTF-8" "en-us"]]
      to-array-2d))

(defn ^{DataProvider {:name "client-with-webdriver-and-activation-key-and-locale"}}
  webdriver_with_activation_key_locale
  "It provides a running Chrome/Firefox instance."
  [ts]
  (.. @driver (get (format "http://%s:%d" (@c/config :client-hostname) 9090)))
  (when-not (tasks/is-logged? @driver)
    (tasks/log-user-in @driver
                       (@c/config :cockpit-username)
                       (@c/config :cockpit-password)))

  (-> [[@driver (partial run-command @c/clientcmd) (new_activation_key ts) "en_US.UTF-8" "en-us"]
       [@driver (partial run-command @c/clientcmd) (new_activation_key ts) "ja_JP.UTF-8" "ja-ja"]
       [@driver (partial run-command @c/clientcmd) (new_activation_key ts) "es_ES.UTF-8" "es-es"]
       [@driver (partial run-command @c/clientcmd) (new_activation_key ts) "de_DE.UTF-8" "de-de"]
       [@driver (partial run-command @c/clientcmd) (new_activation_key ts) "fr_FR.UTF-8" "fr-fr"]]
      to-array-2d))

(gen-class-testng)
