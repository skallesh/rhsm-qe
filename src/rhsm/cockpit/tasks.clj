(ns rhsm.cockpit.tasks
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import org.openqa.selenium.support.ui.ExpectedConditions
           org.openqa.selenium.support.ui.WebDriverWait
           org.openqa.selenium.By))

(defn is-logged?
  [driver]
  (log/info "is-logged?")
  (log/spy (-> driver
               (.findElements (By/id  "content-user-name"))
               count
               (= 0)
               not)))

(defn set-user-language
  [driver language]
  (log/info "set-user-language")
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/id "content-user-name")))
      click)
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/cssSelector "li.display-language-menu")))
      click)
  (.. (WebDriverWait. driver 60)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath
         (format  "//select[@id='display-language-list']/option[@value='%s']"
                  language))))
      click)
  (.. (WebDriverWait. driver 10)
      (until
       (ExpectedConditions/visibilityOfElementLocated
        (By/xpath "//button[@id='display-language-select-button']")))
      click))

(defn log-user-in
  [driver username password]
  (log/info "log-user-in")
  (.. (WebDriverWait. driver 10)
      (until (ExpectedConditions/visibilityOfElementLocated
              (By/id "login-user-input")))
      (sendKeys (into-array [username])))
  (.. (WebDriverWait. driver 10)
      (until (ExpectedConditions/visibilityOfElementLocated
              (By/id "login-password-input")))
      (sendKeys (into-array [password])))
  (.. (WebDriverWait. driver 10)
      (until (ExpectedConditions/visibilityOfElementLocated
              (By/id "login-button")))
      click))
