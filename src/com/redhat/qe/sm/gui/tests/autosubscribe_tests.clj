(ns com.redhat.qe.sm.gui.tests.autosubscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (trim)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            [com.redhat.qe.sm.gui.tasks.candlepin-tasks :as ctasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass AfterClass BeforeGroups Test]
           [com.redhat.qe.sm.cli.tests ComplianceTests]
           [com.redhat.qe.auto.testng BzChecker]))

(def somedir  "/tmp/sm-someProductsSubscribable")
(def alldir "/tmp/sm-allProductsSubscribable")
(def nodir "/tmp/sm-noProductsSubscribable")
(def nonedir  "/tmp/sm-noProductsInstalled")
(def complytests (atom nil))

(defn- dirsetup? [dir]
  (and 
   (= "exists" (trim
                (.getStdout
                 (.runCommandAndWait @clientcmd
                                     (str  "test -d " dir " && echo exists")))))
   (= dir (tasks/conf-file-value "productCertDir"))))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  ;; https://bugzilla.redhat.com/show_bug.cgi?id=723051
  ;; this bug crashes everything, so fail the BeforeClass if this is open
  (verify (not (.isBugOpen (BzChecker/getInstance) "723051")))
  
  (tasks/kill-app)
  (reset! complytests (ComplianceTests. ))
  (.setupProductCertDirsBeforeClass @complytests)
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (tasks/start-app))

(defn ^{AfterClass {:groups ["cleanup"]}}
  cleanup [_]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (.configureProductCertDirAfterClass @complytests)
  (tasks/restart-app))

(defn ^{Test {:groups ["autosubscribe"]}}
  register_autosubscribe [_]
  (let [beforesubs (tasks/warn-count)
        user (@config :username)
        pass (@config :password)
        key  (@config :owner-key)
        ownername (ctasks/get-owner-display-name user pass key)]
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register user
                          pass
                          :autosubscribe true
                          :owner ownername)
          (verify (<= (tasks/warn-count) beforesubs))))))
 
(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForSomeProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  some_products_subscribable [_]
  (tasks/restart-app)
  (verify (dirsetup? somedir))
  (let [beforesubs (tasks/warn-count)
        user (@config :username)
        pass (@config :password)
        key  (@config :owner-key)
        ownername (ctasks/get-owner-display-name user pass key)]
    (verify (= (str beforesubs)
               (trim (.getStdout
                      (.runCommandAndWait @clientcmd (str "ls " somedir " | wc -l"))))))
    (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register user
                          pass
                          :autosubscribe true
                          :owner ownername)
          (tasks/ui waittillwindownotexist :register-dialog 600)
          (tasks/sleep 20000)
          (verify (<= (tasks/warn-count) beforesubs))
          (verify (not (tasks/compliance?)))))))

(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForAllProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  all_products_subscribable [_]
  (tasks/restart-app)
  (verify (dirsetup? alldir))
  (let [beforesubs (tasks/warn-count)
        user (@config :username)
        pass (@config :password)
        key  (@config :owner-key)
        ownername (ctasks/get-owner-display-name user pass key)]
    (verify (= (str beforesubs)
               (trim (.getStdout
                      (.runCommandAndWait @clientcmd (str "ls " alldir " | wc -l"))))))
    (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register user
                          pass
                          :autosubscribe true
                          :owner ownername)
          (tasks/ui waittillwindownotexist :register-dialog 600)
          (tasks/sleep 20000)
          (verify (= (tasks/warn-count) 0))
          (verify (tasks/compliance?))))))

(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForNoProductsSubscribable"
                       "blockedByBug-743704"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_subscribable [_]
  (tasks/restart-app)
  (verify (dirsetup? nodir))
  (let [beforesubs (tasks/warn-count)
        user (@config :username)
        pass (@config :password)
        key  (@config :owner-key)
        ownername (ctasks/get-owner-display-name user pass key)]
    (verify (= (str beforesubs)
               (trim (.getStdout
                      (.runCommandAndWait @clientcmd (str "ls " nodir " | wc -l"))))))
    (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register user
                          pass
                          :autosubscribe true
                          :owner ownername)
          (tasks/ui waittillwindownotexist :register-dialog 600)
          (tasks/sleep 10000)
          (verify (= (tasks/warn-count) beforesubs))
          (verify (not (tasks/compliance?)))))))

(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForNoProductsInstalled"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_installed [_]
  (tasks/restart-app)
  (verify (dirsetup? nonedir))
  (verify (= 0 (tasks/warn-count)))
  (verify (tasks/compliance?)))


(gen-class-testng)


;; TODO write a separte test for https://bugzilla.redhat.com/show_bug.cgi?id=743704
;;   and restore the override.facts 
