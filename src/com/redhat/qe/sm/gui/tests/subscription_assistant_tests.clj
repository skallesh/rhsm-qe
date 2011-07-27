(ns com.redhat.qe.sm.gui.tests.subscription-assistant-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations AfterClass BeforeClass BeforeGroups Test]
           [com.redhat.qe.sm.cli.tests ComplianceTests]))

(def complytests (atom nil))

(defn- check-product 
  ([product]
    (let [row (tasks/ui gettablerowindex :subscription-product-view product)]
        (tasks/ui checkrow :subscription-product-view row 0)))
  ([product check?]
    (let [row (tasks/ui gettablerowindex :subscription-product-view product)]
      (if check?
        (tasks/ui checkrow :subscription-product-view row 0)
        (tasks/ui uncheckrow :subscription-product-view row 0)))))        

(defn check-all-products []
  (tasks/do-to-all-rows-in :subscription-product-view 1
      (fn [product] (check-product product))))

(defn- register []
  (with-handlers [(handle :already-registered [e]
                          (recover e :unregister-first))]
    (let [user (@config :username)
          pass (@config :password)
          owner (tasks/get-owner-display-name user
                                              pass
                                              (@config :owner-key))]
      (tasks/register user pass :owner owner))))


(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (tasks/kill-app)
  (reset! complytests (ComplianceTests. ))
  (.setupProductCertDirsBeforeClass @complytests)
  (.runCommandAndWait @clientcmd "subscripton-manager unregister")
  (tasks/start-app)
  (register))
    
(defn ^{AfterClass {:groups ["setup"]}}
  exit_subscription_assistant [_]
  (if (= 1 (tasks/ui guiexist :subscription-assistant-dialog))
    (tasks/ui closewindow :subscription-assistant-dialog)))    
    
(defn ^{Test {:groups ["subscription-assistant"]}}
  register_warning [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)) 
  (if-not (tasks/compliance?)
    (do (tasks/ui click :update-certificates)
        (tasks/ui waittillwindowexist :information-dialog 60)
        (verify (= 1 (action guiexist :information-dialog "You must register*")))
        (tasks/ui click :info-ok))))
        
(defn ^{Test {:groups ["subscription-assistant"]}}
  launch_assistant [_]
  (register)
  (tasks/ui click :update-certificates)
  (tasks/ui waittillwindowexist :subscription-assistant-dialog 60)
  (tasks/wait-for-progress-bar)
  (verify (= 1 (action guiexist :subscription-assistant-dialog))))

(defn- reset-assistant []
  (exit_subscription_assistant nil)
  (launch_assistant nil))

(defn ^{Test {:groups ["subscription-assistant" "blockedByBug-703997"]
              :dependsOnMethods ["launch_assistant"]}}
  subscribe_all_products [_]
  (reset-assistant)
  (tasks/ui click :first-date)
  (tasks/ui click :update)
  (tasks/wait-for-progress-bar)
  (check-all-products)
  (tasks/sleep 5000)
  (let [subscription-list (tasks/get-table-elements :assistant-subscription-view 0)
        nocomply-count (atom (tasks/warn-count))]
    (doseq [item subscription-list]
      (with-handlers  [(ignore :subscription-not-available)] 
        (tasks/assistant-subscribe item)
        (let [warn-count (tasks/warn-count)]
          (if-not (= 0 @nocomply-count)
            (do (verify (< warn-count @nocomply-count))
                (reset! nocomply-count warn-count))))
        (check-all-products)))))

(comment
  (defn ^{Test {:groups ["subscription-assistant" "blockedByBug-703997"]
                :dependsOnMethods ["launch_assistant"]
                :dataProvider "multi-entitle"}}
    check_quantities [_ subscription]
    
    (let [subscription-list (tasks/get-table-elements :assistant-subscription-view 0)]
      (doseq [item subscription-list]
        )))

  (defn ^{DataProvider {:name "multi-entitle"}}
    get_multi_entitle_subscriptions [_]
    (reset-assistant)
    (tasks/ui click :first-date)
    (tasks/ui click :update)
    (tasks/wait-for-progress-bar)
    (check-all-products)
    (tasks/sleep 5000)
    ))

(defn ^{Test {:groups ["subscription-assistant"
                       "configureProductCertDirForSomeProductsSubscribable"]
              :dependsOnMethods ["launch_assistant"]}}
  some_products_subscribable [_]
  (tasks/restart-app)
  (let [beforedate (tasks/first-date-of-noncomply)]
    (subscribe_all_products nil)
    (verify (= (tasks/first-date-of-noncomply) beforedate))
    (verify (< 0 (tasks/ui getrowcount :subscription-product-view)))
    (verify (= 0 (tasks/ui getrowcount :assistant-subscription-view)))))


(defn ^{Test {:groups ["subscription-assistant"
                       "configureProductCertDirForAllProductsSubscribable"]
              :dependsOnMethods ["launch_assistant"]}}
  all_products_subscribable [_]
  (tasks/restart-app)
  (let [beforedate (tasks/first-date-of-noncomply)]
    (subscribe_all_products nil)
    (verify tasks/compliance?)
    (verify (not (= (tasks/first-date-of-noncomply) beforedate)))))

(comment 
  (defn ^{Test {:groups ["subscription-assistant"
                         "configureProductCertDirForNoProductsSubscribable"]
                :dependsOnMethods ["launch_assistant"]}}
    no_products_subscribable [_]
    (tasks/restart-app)
    (register)
    (verify (< 0 (tasks/ui getrowcount :subscription-product-view)))
    (check-all-products)
    (verify (= 0 (tasks/ui getrowcount :assistant-subscription-view))))

  
  (defn ^{Test {:groups ["subscription-assistant"
                         "configureProductCertDirForNoProductsInstalled"]
                :dependsOnMethods ["launch_assistant"]}}
    no_products_installed [_]
    (reset-assistant)))


;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=676371

        
(gen-class-testng)
