(ns com.redhat.qe.sm.gui.tests.autosubscribe-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (trim)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
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

(defn- kill-app []
  (.runCommandAndWait @clientcmd "killall -9 subscription-manager-gui")
  (tasks/ui waittillwindownotexist :main-window 30))

(defn- restart-gui []
  (kill-app)
  (tasks/start-app))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  ;; https://bugzilla.redhat.com/show_bug.cgi?id=723051
  ;; this bug crashes everything, so fail the BeforeClass if this is open
  (verify (not (.isBugOpen (BzChecker/getInstance) "723051")))
  
  (kill-app)
  (reset! complytests (ComplianceTests. ))
  (.setupProductCertDirsBeforeClass @complytests)
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (tasks/start-app))

(defn ^{AfterClass {:groups ["cleanup"]}}
  cleanup [_]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (.configureProductCertDirAfterClass @complytests)
  (restart-gui))

(defn ^{Test {:groups ["autosubscribe"]}}
  register_autosubscribe [_]
  (let [beforesubs (tasks/warn-count)
        user (@config :username)
        pass (@config :password)
        key  (@config :owner-key)
        ownername (tasks/get-owner-display-name user pass key)]
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
  (restart-gui)
  (verify (dirsetup? somedir))
  )


(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForAllProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  all_products_subscribable [_]
  (restart-gui)
  (verify (dirsetup? alldir))
  )

(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForNoProductsSubscribable"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_subscribable [_]
  (restart-gui)
  (verify (dirsetup? nodir))
  )

(defn ^{Test {:groups ["autosubscribe"
                       "configureProductCertDirForNoProductsInstalled"]
              :dependsOnMethods ["register_autosubscribe"]}}
  no_products_installed [_]
  (restart-gui)
  (verify (dirsetup? nonedir))
  )



(gen-class-testng)

