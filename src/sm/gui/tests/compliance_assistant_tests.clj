(ns sm.gui.tests.compliance-assistant-tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [sm.gui.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	       gnome.ldtp)
  (:require [sm.gui.tasks :as tasks]
             sm.gui.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test]))
 
(defn- get-table-elements [view col]
  (for [row (range (action getrowcount view))]
    (action getcellvalue view row col))) 
  
(defn- do-to-all-rows-in [view f]
  (let [products-list (get-table-elements view 1)]
    (doseq [item products-list]
      (f item))))

(defn- check-product 
  ([product]
    (let [row (tasks/ui gettablerowindex :compliance-product-view product)]
        (tasks/ui checkrow :compliance-product-view row 0)))
  ([product check?]
    (let [row (tasks/ui gettablerowindex :compliance-product-view product)]
      (if check?
        (tasks/ui checkrow :compliance-product-view row 0)
        (tasks/ui uncheckrow :compliance-product-view row 0)))))

(defn ^{BeforeClass {:groups ["setup"]}}
  register_first [_]
  (with-handlers [(handle :already-registered [e]
                               (recover e :unregister-first))]
    (tasks/register (@config :username) (@config :password))))
    
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
  (register_first nil)
  (tasks/ui click :become-compliant)
  (tasks/ui waittillwindowexist :compliance-assistant-dialog 60)
  (tasks/wait-for-progress-bar)
  (verify (= 1 (action guiexist :compliance-assistant-dialog))))

(defn ^{Test {:groups ["compliance-assistant"]
              :dependsOnMethods ["launch_assistant"]}}
  subscribe_all_products [_]
  (tasks/ui click :first-date)
  (tasks/ui click :update)
  (tasks/wait-for-progress-bar)
  (do-to-all-rows-in :compliance-product-view
      (fn [product] (check-product product)))
)
        

        
(gen-class-testng)
