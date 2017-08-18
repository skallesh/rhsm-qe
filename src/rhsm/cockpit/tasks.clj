(ns rhsm.cockpit.tasks
  (:require [clojure.string :as string]
            ;; [webica.core :as w]
            ;; [webica.by :as by]
            ;; [webica.web-driver :as driver]
            ;; [webica.web-element :as element]
            ;; [webica.remote-web-driver :as browser]
            ;; [webica.web-driver-wait :as wait]
            [clojure.tools.logging :as log]
            )
  )

;; (defn login [user password]
;;   "#login-user-input"
;;   #spy/d (wait/until
;;           (wait/instance 10)
;;           (wait/condition
;;            (fn [driver]
;;              (some? (browser/find-element (by/id "login-user-input"))))))
;;   (println "press Enter") (read-line)
;;   ;; (input-text "#login-user-input" user)
;;   ;; (input-text "#login-password-input" password)
;;   ;; (click "#login-button")
;;   ;; (wait-until #(exists? "#content-user-name"))
;;   )

;; (deftest subscription-dialog
;;   (login (env :sm-cockpit-login-user) (env :sm-cockpit-login-password))
;;   (click {:xpath "//a[@data-target='#tools-panel']"})
;;   (wait-until #(displayed? {:xpath "//a[@href='/subscriptions']"}))
;;   (click {:xpath "//a[@href='/subscriptions']"})
;;   (close))
