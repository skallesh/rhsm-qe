(ns rhsm.cockpit.web
  (:require [clojure.tools.logging :as log])
  (:import org.openqa.selenium.By
           org.openqa.selenium.remote.DesiredCapabilities
           org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait))

(defn subscriptions-menu-link [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//a[@href='/subscriptions']")))))

(defn subscriptions-iframe [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//iframe[@name='cockpit1:localhost/subscriptions' and @data-loaded=1]")))))

(comment " a dynamic info 'Retrieving subscription status ...'
<div class="curtains-ct blank-slate-pf">
   <div class="blank-slate-pf-icon"><div class="spinner spinner-lg"></div></div>
   <h1>Retrieving subscription status...</h1>
    <p>Updating</p>
</div>")

(defn retrieving-subscription-status-text [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/cssSelector "#app div.curtains-ct")))
      getText))


(defn subscription-status [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/cssSelector "div.subscription-status-ct label")))))

(defn register-button [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/cssSelector "div.subscription-status-ct button")))))

(defn register-username [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/id "subscription-register-username")))))

(defn register-password [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/id "subscription-register-password")))))

(defn register-org [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/id "subscription-register-org")))))

(defn register-confirm-button [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//div[@class='modal-content']/div/button[contains(@class,'btn-primary')]")))))

(defn register-error-box [driver]
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//div[@class='modal-footer']/div[contains(@class,'dialog-error')]")))))
