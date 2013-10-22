(ns rhsm.gui.tests.stacking_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [clojure.string :only (split
                               split-lines
                               blank?
                               join
                               trim-newline
                               trim)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            AfterClass
            BeforeGroups
            AfterGroups
            Test
            DataProvider]))

(def prod-dir (atom {}))
(def sub (atom {}))

(def stacking-dir "/tmp/stacking-dir/")

(defn random-subscription
  "Helper function to choose subscriptions with quantity more than one"
  [subscriptions]
  (reset! sub (rand-nth subscriptions)) 
  (while (= "1" (re-find #"\d+"
                         (tasks/ui getcellvalue :all-subscriptions-view 
                                   (tasks/skip-dropdown :all-subscriptions-view @sub)
                                   3)))
    (reset! sub (rand-nth subscriptions)))
  @sub)

(defn is-date?
  "Verifies if the the object is a valid date object"
  [date]
  (let [split-date (split date #"/")
        count-split-date (count split-date)
        date-obj-len '(2 2 4)]
    (if (= 10 (count date))
      (if (= 3 count-split-date)
        (do
          (doseq [[x y] (map list date-obj-len split-date)]
            (if-not (= (count y) x)
              (let [result false]
                result))) true) false) false)))

(defn is-equal?
  "Verifies if the string lists are equal"
  [a b]
  (let [a-int (into [] (map #(Integer. (re-find  #"\d+" %)) a))
        b-int (into [] (map #(Integer. (re-find  #"\d+" %)) b))
        equal (map (fn [i j] (> i j)) a-int  b-int)]
    (if (some #(= false %) equal) false true)))

(defn repeat-cmd
  [n cmd]
  (apply str (repeat n cmd)))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (tasks/restart-app :reregister? true)
  (if (not (bash-bool (:exitcode (run-command (str "test -d " stacking-dir)))))
    (run-command (str "mkdir " stacking-dir)))
  (reset! prod-dir (tasks/conf-file-value "productCertDir"))
  (let [stackable-pems (tasks/get-stackable-pem-files)
        change-prod-dir (tasks/set-conf-file-value "productCertDir" stacking-dir)
        ret-val (fn [pem-file] (:exitcode (run-command (str "cp  " @prod-dir "/" pem-file "  " stacking-dir))))]
    (map ret-val stackable-pems)))

(defn ^{Test {:groups ["stacking"
                       "blockedByBug-854380"]}}
  assert_subscriptions_displayed
  "Asserts the matching subscriptions are displayed when the system is partially subscribed"
  [_]
  (try
    (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"20\"}")
    (run-command "subscription-manager facts --update")
    (tasks/restart-app :reregister? true)
    (tasks/search :match-installed? true)
    (let [subscriptions (into [] (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdown? true))
          sub-type-map (ctasks/build-subscription-attr-type-map)
          socket-subs  (for [s subscriptions
                             :let [x (get sub-type-map s)]
                             :when (some #(= "sockets" %) x)] s)
          rand-sub (random-subscription socket-subs)]
      (tasks/skip-dropdown :all-subscriptions-view rand-sub)
      (tasks/ui generatekeyevent (str
                                  (repeat-cmd 3 "<right> ")
                                  "<space> " "1 " "<enter>"))
      (tasks/ui click :attach)
      (tasks/checkforerror)
      (if (bool (tasks/ui guiexist :contract-selection-dialog))
        (do (tasks/ui selectrowindex :contract-selection-table 0)
            (tasks/ui click :attach-contract-selection)
            (tasks/checkforerror)))
      (tasks/restart-app)
      (tasks/ui selecttab :all-available-subscriptions)
      (tasks/search :match-installed? true)
      (verify (<= 0 (tasks/skip-dropdown :all-subscriptions-view rand-sub))))
  (finally
     (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"2\"}")
     (run-command "subscription-manager facts --update")
     (tasks/unsubscribe_all))))

(defn ^{Test {:groups ["stacking"
                       "blockedByBug-990639"]}}
  check_dates_partially_subscribed
  "Checks if start and end dates are displayed for partially subscribed poducts"
  [_]
  (try
    (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"20\"}")
    (run-command "subscription-manager facts --update")
    (tasks/restart-app :reregister? true)
    (tasks/search :match-installed? true)
    (let [subscriptions (into [] (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdown? true))
          sub-type-map (ctasks/build-subscription-attr-type-map)
          socket-subs (for [s subscriptions
                            :let [x (get sub-type-map s)]
                            :when (some #(= "sockets" %) x)] s)
          rand-sub (random-subscription socket-subs)]
      (tasks/skip-dropdown :all-subscriptions-view rand-sub)
      (tasks/ui generatekeyevent (str
                                  (repeat-cmd 3 "<right> ")
                                  "<space> " "1 " "<enter>"))
      (tasks/ui click :attach)
      (tasks/checkforerror)
      (if (bool (tasks/ui guiexist :contract-selection-dialog))
       (do (tasks/ui selectrowindex :contract-selection-table 0)
           (tasks/ui click :attach-contract-selection)
           (tasks/checkforerror)))
      (tasks/ui selecttab :my-installed-products)
      (tasks/do-to-all-rows-in :installed-view 0
                               (fn [sub]
                                 (let [row-num (tasks/skip-dropdown :installed-view sub)]
                                   (if (= (tasks/ui getcellvalue :installed-view row-num 2) "Partially Subscribed")
                                     (do
                                       (verify (is-date? (tasks/ui getcellvalue :installed-view row-num 3)))
                                       (verify (is-date? (tasks/ui getcellvalue :installed-view row-num 4)))))))))
    (finally
     (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"2\"}")
     (run-command "subscription-manager facts --update")
     (tasks/unsubscribe_all))))

(defn ^{Test {:groups ["stacking"
                       "blockedByBug-745965"]}}
  assert_future_cert_status
  "Asserts cert status for future entitilment"
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/search :match-installed? true)
  (let [                                ;getting future date
        date-string (tasks/ui gettextvalue :date-entry)                                        
        date-split (split date-string #"-")
        year (first date-split)
        month (second date-split)
        day (last date-split)
        new-year (+ (Integer. (re-find  #"\d+" year)) 1)

                                        ;creating new directory with single product
        new-prod-dir (str "/tmp/single-pem/")
        existing-prod-dir (tasks/conf-file-value "productCertDir")
        subscriptions (into [] (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdown? true))
        sub-type-map (ctasks/build-subscription-attr-type-map)
        socket-subs (for [s subscriptions
                            :let [x (get sub-type-map s)]
                            :when (some #(= "sockets" %) x)] s)
        rand-sub (random-subscription socket-subs)
        rand-prod (tasks/skip-dropdown :all-subscriptions-view rand-sub)
        rand-prod-list (tasks/get-table-elements :all-available-bundled-products 0)

                                        ;seraching pem files for random product selected
        raw-pem-files (:stdout (run-command
                      (str "cd " existing-prod-dir "; for x in `ls`; do rct cat-cert $x | egrep \"Path\" | cut -d: -f 2; done")))
        pem-file-list (into [] (map clojure.string/trim
                                     (clojure.string/split-lines raw-pem-files)))
        raw-prods (:stdout (run-command
                       (str "cd " existing-prod-dir "; for x in `ls`; do rct cat-cert $x | egrep \"Name\" | cut -d: -f 2; done")))
        prod-list (into [] (map clojure.string/trim (clojure.string/split-lines raw-prods)))
        prod-pem-file-map (zipmap prod-list pem-file-list)
        pem-file (get prod-pem-file-map (first rand-prod-list))

                                        ;copying pem file to new product directory
        repeat-cmd (fn [n cmd] (apply str (repeat n cmd)))]
    (if (not (bash-bool (:exitcode (run-command (str "test -d " new-prod-dir)))))
      (run-command (str "mkdir " new-prod-dir)))
    (run-command (str "cp " existing-prod-dir pem-file "  " new-prod-dir))
    (tasks/set-conf-file-value "productCertDir" new-prod-dir)
    (try+
     (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"20\"}")
     (run-command "subscription-manager facts --update")
     (tasks/restart-app)
     (tasks/ui selecttab :all-available-subscriptions)
     (tasks/ui enterstring :date-entry (str new-year "-" month "-" day))
     (tasks/search :match-installed? true)
     (tasks/skip-dropdown :all-subscriptions-view rand-sub)
     (tasks/ui generatekeyevent (str
                                 (repeat-cmd 3 "<right> ")
                                 "<space> " "1 " "<enter>"))
     (tasks/ui click :attach)
     (tasks/checkforerror)
     (if (bool (tasks/ui guiexist :contract-selection-dialog))
       (do (tasks/ui selectrowindex :contract-selection-table 0)
           (tasks/ui click :attach-contract-selection)
            (tasks/checkforerror)))
     (tasks/ui selecttab :my-installed-products)
     (tasks/do-to-all-rows-in :installed-view 2
                              (fn [status]
                                (verify (= "Future Subscription" status))))
     (verify (not (substring? "does not match" (tasks/ui gettextvalue :main-window "*subscription"))))
     (finally
      (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"2\"}")
      (run-command "subscription-manager facts --update")
      (tasks/set-conf-file-value "productCertDir" existing-prod-dir)
      (run-command (str "rm -rf " new-prod-dir))
      (tasks/unsubscribe_all)
      (tasks/restart-app)))))

(defn ^{Test {:groups ["acceptance"
                       "blockedByBug-827173"]}}
  assert_auto_attach
  "Asserts if autosubscribe is possible when client is partially subscribed"
  [_]
  (try
    (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"20\"}")
    (run-command "subscription-manager facts --update")
    (tasks/restart-app :reregister? true)
    (tasks/search :match-installed? true)
    (let [subscriptions (into [] (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdown? true))
          sub-type-map (ctasks/build-subscription-attr-type-map)
          socket-subs (for [s subscriptions
                            :let [x (get sub-type-map s)]
                            :when (some #(= "sockets" %) x)] s)
          rand-sub (random-subscription socket-subs)
          raw-data (:stdout (run-command "subscription-manager service-level --list"))
          service-level (rand-nth (drop 3 (split-lines raw-data)))]
      (tasks/skip-dropdown :all-subscriptions-view rand-sub)
      (tasks/ui generatekeyevent (str
                                  (repeat-cmd 3 "<right> ")
                                  "<space> " "1 " "<enter>"))
      (tasks/ui click :attach)
      (tasks/checkforerror)
      (if (bool (tasks/ui guiexist :contract-selection-dialog))
       (do (tasks/ui selectrowindex :contract-selection-table 0)
           (tasks/ui click :attach-contract-selection)
            (tasks/checkforerror)))
      (tasks/ui selecttab :my-installed-products)
      (tasks/ui click :auto-attach)
      (sleep 10000)
      (tasks/ui waittillwindowexist :register-dialog 80)
      (tasks/ui click :register-dialog service-level)
      (tasks/ui click :register)
      (if (tasks/ui showing? :register-dialog "Select Service Level")
        (do
          (sleep 3000)
          (tasks/ui click :register)
          (tasks/ui waittillwindownotexist :register-dialog 80)
          (tasks/restart-app))) 
      (verify (= 1 (tasks/ui guiexist :main-window "System is properly subscribed*"))))
    (finally
     (tasks/write-facts "{\"cpu.cpu_socket(s)\": \"2\"}")
     (run-command "subscription-manager facts --update")
     (tasks/unsubscribe_all))))

(comment
  (defn ^{Test {:groups ["acceptance"
                         "blockedByBug-827173"]}}
    assert_quantity_displayed
    "Asserts if quantity displayed for stacking subscriptions is correct"
    [_]
    (try
                                        ;(tasks/write-facts "{\"memory.memtotal\": \"10202520\"}")
                                        ;(run-command "subscription-manager facts --update")
                                        ;(tasks/restart-app :reregister? true)
                                        ;(tasks/search :match-installed? true)
      (let [subscriptions (into [] (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdown? true))
            sub-type-map (ctasks/build-subscription-attr-type-map)
            only-ram (fn [i] (and (not (or (some #(= "cores" %) i) (some #(= "sockets" %) i))) (some #(= "ram" %) i)))
            socket-subs (for [s subscriptions
                              :let [x (get sub-type-map s)]
                              :when (only-ram x)] s)
            rand-sub (random-subscription socket-subs)
            subs-attrs-map (ctasks/build-subscriptions-name-val-map)
            stacking-id (get (get subs-attrs-map rand-sub) "stacking_id")
            filter-func (fn [i] (if (= stacking-id (get (get subs-attrs-map i) "stacking_id")) i))
            subs-stacking-id (into [] (distinct (remove nil? (map filter-func subscriptions))))
            subs-applicable (clojure.set/intersection (into #{} socket-subs) (into #{} subs-stacking-id))
            quantity-func (fn [i] (re-find #"\d" (tasks/ui getcellvalue :all-subscriptions-view
                                                          (tasks/skip-dropdown :all-subscriptions-view i) 3)))
            quantity-before (map quantity-func subs-applicable)
                                        ;raw-data (:stdout (run-command "subscription-manager service-level --list"))
                                        ;service-level (rand-nth (drop 3 (split-lines raw-data)))
            ]
        (comment)
        (tasks/skip-dropdown :all-subscriptions-view rand-sub)
        (tasks/ui generatekeyevent (str
                                    (repeat-cmd 3 "<right> ")
                                    "<space> " "1 " "<enter>"))
        (tasks/ui click :attach)
        (tasks/checkforerror)
        (if (bool (tasks/ui guiexist :contract-selection-dialog))
          (do (tasks/ui selectrowindex :contract-selection-table 0)
              (tasks/ui click :attach-contract-selection)
              (tasks/checkforerror)))
        (tasks/skip-dropdown :all-subscriptions-view rand-sub)
        (tasks/ui click :search)
        (tasks/checkforerror)
        (verify (= quantity-before (map quantity-func subs-applicable)))
                                        ;(map verify)
        (comment
          (map
           (fn [i] (= (re-find #"\d"
                              (tasks/ui getcellvalue :all-subscriptions-view (tasks/skip-dropdown :all-subscriptions-view rand-sub) 3))
                     i))
           (map (fn [i] (re-find #"\d" (tasks/ui getcellvalue :all-subscriptions-view (tasks/skip-dropdown :all-subscriptions-view i) 3)))
                subs-stacking-id)))
                                        ;(clojure.pprint/pprint (clojure.set/intersection (into #{} socket-subs) (into #{} subs-stacking-id)))
                                        ;(clojure.pprint/pprint socket-subs)
                                        ;(clojure.pprint/pprint subs-stacking-id)
                                        ;(clojure.pprint/pprint stacking-id)
        (clojure.pprint/pprint quantity-before)
        (clojure.pprint/pprint (map quantity-func subs-applicable))
        )
      (comment (finally
                (tasks/write-facts "{\"memory.memtotal\": \"1020252\"}")
                (run-command "subscription-manager facts --update")
                (tasks/unsubscribe_all)))
      )))


(comment
  (not-every? false? (map (fn [i] (= "stacking_id" i)) (flatten(map keys(first (vals ans1))))))
  ;where ans -> (ctasks/build-stackable-subscription-map)
  )

(defn ^{AfterClass {:groups ["cleanup"]
                     :alwaysRun true}}
  cleanup [_]
  (run-command (str "rm -rf " stacking-dir))
  (tasks/set-conf-file-value "productCertDir" @prod-dir)
  (tasks/restart-app))

(gen-class-testng)
