(ns com.redhat.qe.sm.gui.tests.compliance-assistant-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations AfterClass BeforeClass BeforeGroups Test]))
 
(defn- check-product 
  ([product]
    (let [row (tasks/ui gettablerowindex :compliance-product-view product)]
        (tasks/ui checkrow :compliance-product-view row 0)))
  ([product check?]
    (let [row (tasks/ui gettablerowindex :compliance-product-view product)]
      (if check?
        (tasks/ui checkrow :compliance-product-view row 0)
        (tasks/ui uncheckrow :compliance-product-view row 0)))))        

(defn- check-all-products []
  (tasks/do-to-all-rows-in :compliance-product-view 1
      (fn [product] (check-product product))))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))))
    
(defn ^{AfterClass {:groups ["setup"]}}
  exit_compliance_assistant [_]
  (if (= 1 (tasks/ui guiexist :compliance-assistant-dialog))
    (tasks/ui closewindow :compliance-assistant-dialog)))    
    
(defn ^{Test {:groups ["compliance-assistant"]}}
  register_warning [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)) 
  (if-not (tasks/compliance?)
    (do (tasks/ui click :become-compliant)
        (tasks/ui waittillwindowexist :information-dialog 60)
        (verify (= 1 (action guiexist :information-dialog "You must register*")))
        (tasks/ui click :info-ok))))
        
(defn ^{Test {:groups ["compliance-assistant"]}}
  launch_assistant [_]
  (register nil)
  (tasks/ui click :become-compliant)
  (tasks/ui waittillwindowexist :compliance-assistant-dialog 60)
  (tasks/wait-for-progress-bar)
  (verify (= 1 (action guiexist :compliance-assistant-dialog))))

(defn- reset-assistant []
  (exit_compliance_assistant nil)
  (launch_assistant nil))

(defn ^{Test {:groups ["compliance-assistant"]
              :dependsOnMethods ["launch_assistant"]}}
  subscribe_all_products [_]
  (reset-assistant)
  (tasks/ui click :first-date)
  (tasks/ui click :update)
  (tasks/wait-for-progress-bar)
  (check-all-products)
  (let [subscription-list (tasks/get-table-elements :compliance-subscription-view 0)
        nocomply-count (atom (tasks/warn-count))]
    (doseq [item subscription-list]
      (with-handlers  [(ignore :subscription-not-available)] 
        (tasks/compliance-subscribe item)
        (let [warn-count (tasks/warn-count)]
          (if-not (= 0 @nocomply-count)
            (do (verify (< warn-count @nocomply-count))
                (reset! nocomply-count warn-count))))
        (check-all-products)))))

(comment
(defn ^{Test {:groups ["compliance-assistant" "SomeProductsSubscribable"]
              :dependsOnMethods ["launch_assistant"]}}
  some_products_subscribable [_]
  (reset-assistant)
  (let [beforedate (tasks/first-date-of-noncomply)]
    (subscribe_all_products nil)
    (verify (= (tasks/first-date-of-noncomply) beforedate))
    (verify (< 0 (tasks/ui getrowcount :compliance-product-view)))
    (verify (= 0 (tasks/ui getrowcount :compliance-subscription-view)))))


(defn ^{Test {:groups ["compliance-assistant" "AllProductsSubscribable"]
              :dependsOnMethods ["launch_assistant"]}}
  all_products_subscribable [_]
  (reset-assistant)
  (let [beforedate (tasks/first-date-of-noncomply)]
    (subscribe_all_products nil)
    (verify tasks/compliance?)
    (verify (not (= (tasks/first-date-of-noncomply) beforedate)))))

(defn ^{Test {:groups ["compliance-assistant" "NoProductsSubscribable"]
              :dependsOnMethods ["launch_assistant"]}}
  no_products_subscribable [_]
  (reset-assistant)
  (verify (< 0 (tasks/ui getrowcount :compliance-product-view)))
  (check-all-products)
  (verify (= 0 (tasks/ui getrowcount :compliance-subscription-view))))

(defn ^{Test {:groups ["compliance-assistant" "NoProductsInstalled"]
              :dependsOnMethods ["launch_assistant"]}}
  no_products_installed [_]
  (reset-assistant))
)


        
(gen-class-testng)
