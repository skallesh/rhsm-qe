(ns rhsm.cockpit.tests.locales
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [rhsm.gui.tasks.test-config :as c]
            [rhsm.cockpit.tasks :as tasks])
  (:import [org.testng.annotations
            BeforeSuite
            AfterSuite
            DataProvider
            Test]
           [rhsm.base SubscriptionManagerCLITestScript]
           [com.redhat.qe.auto.testng TestScript]
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]
           [rhsm.base SubscriptionManagerCLITestScript]
           org.testng.SkipException
           ;; org.openqa.selenium.chrome.ChromeDriver
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.By
           org.openqa.selenium.firefox.FirefoxDriver
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait
           java.util.logging.Level))


(defn is-register-button-localized?
  [ts driver run-command locale language]
  (let [button-text (.. (WebDriverWait. driver 60)
                        (until
                         (ExpectedConditions/visibilityOfElementLocated
                          (By/cssSelector "div.subscription-status-ct button")))
                        getText)
        localized-text (-> (format "LANG=%s gettext rhsm Register" locale)
                           run-command
                           :stdout
                           string/trim)
        ]
    (= button-text localized-text)))

(defn is-subscription-status-localized?
  [ts driver run-command locale language]
  )

(defn is-list-of-products-localized?
  [ts driver run-command locale language]
  )

