(ns com.redhat.qe.sm.gui.tests.autosubscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))

(def somedir  "/tmp/sm-someProductsSubscribable")
(def alldir "/tmp/sm-allProductsSubscribable")
(def nodir "/tmp/sm-noProductsSubscribable")
(def nonedir  "/tmp/sm-noProductsInstalled")
  
(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn ^{Test {:groups ["autosubscribe"]}}
  register_autosubscribe [_]
    (let [beforesubs (tasks/warn-count)]
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do 
          (tasks/register (@config :username) (@config :password) :autosubscribe true)
          (verify (<= (tasks/warn-count) beforesubs))
          ))))

(defn ^{Test {:groups ["configureProductCertDirForSomeProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  some_products_subscribable [_]
  (verify (= "exists" (.getStdout
                       (.runCommandAndWait @clientcmd
                                           (str  "test -d " somedir " && echo exists"))))))


(defn ^{Test {:groups ["configureProductCertDirForAllProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  all_products_subscribable [_]
  (verify (= "exists" (.getStdout
                       (.runCommandAndWait @clientcmd
                                           (str  "test -d " alldir " && echo exists"))))))

(defn ^{Test {:groups ["configureProductCertDirForNoProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_subscribable [_]
  (verify (= "exists" (.getStdout
                       (.runCommandAndWait @clientcmd
                                           (str  "test -d " nodir " && echo exists"))))))

(defn ^{Test {:groups ["configureProductCertDirForNoProductsInstalled"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_installed [_]
  (verify (= "exists" (.getStdout
                       (.runCommandAndWait @clientcmd
                                           (str  "test -d " nonedir " && echo exists"))))))



(gen-class-testng)

