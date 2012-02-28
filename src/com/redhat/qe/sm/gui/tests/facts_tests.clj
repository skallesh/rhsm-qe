(ns com.redhat.qe.sm.gui.tests.facts-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd
                                                       cli-tasks)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (split-lines split)]
        clojure.contrib.pprint
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test DataProvider]
           [org.apache.xmlrpc XmlRpcException]))

(def gui-facts (atom nil))
(def cli-facts (atom nil))
(def installed-certs (atom nil))

(defn get-cli-facts []
  (let [allfacts (.getStdout
                  (.runCommandAndWait @clientcmd
                                      "subscription-manager facts --list"))
        allfactpairs (split-lines allfacts)
        factslist (into {} (map (fn [fact] (vec (split #": " fact)))
                                allfactpairs))]
    factslist))

(defn map-installed-certs []
  (let [certlist (map (fn [cert]
                        (.productNamespace cert))
                      (.getCurrentProductCerts @cli-tasks))
        productlist (atom {})]
    (doseq [c certlist]
      (let [name (.name c)
            version (.version c)
            arch (.arch c)]
        (if (nil? (@productlist name))
          (reset! productlist
                  (assoc @productlist
                    name
                    {:version version
                     :arch arch})))))
    @productlist))

(defn ^{BeforeClass {:groups ["facts"]}}
  register [_]
  (tasks/register-with-creds)
  (reset! gui-facts (tasks/get-all-facts))
  (reset! cli-facts (get-cli-facts))
  (reset! installed-certs (map-installed-certs)))

(defn ^{Test {:groups ["facts"]
              :dataProvider "guifacts"}}
  match_each_fact [_ fact value]
  (verify (= (@cli-facts fact) value)))

(defn ^{Test {:groups ["facts"]}}
  facts_parity [_]
  (verify (= (count @cli-facts)
             (count @gui-facts))))

(defn ^{Test {:groups ["facts" "blockedByBug-683550"]
              :dataProvider "installed-products"}}
  check_version_arch [_ product index]
  (let [version (:version (@installed-certs product))
        arch (:arch (@installed-certs product))
        guiversion (try (tasks/ui getcellvalue :installed-view index 1)
                        (catch XmlRpcException e nil))
        guiarch (try (tasks/ui getcellvalue :installed-view index 2)
                     (catch XmlRpcException e nil))]
    (verify (= version guiversion))
    (verify (= arch guiarch))))

;; run ^^this^^ in the console with:
;; (doseq [[p i] (ftest/get_installed_products nil :debug true)] (ftest/check_version_arch nil p i))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATA PROVIDERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^{DataProvider {:name "guifacts"}}
  get_facts [_ & {:keys [debug]
                  :or {debug false}}]
  (if-not debug
    (to-array-2d (vec @gui-facts))
    (vec @gui-facts)))

(defn ^{DataProvider {:name "installed-products"}}
  get_installed_products [_ & {:keys [debug]
                               :or {debug false}}]
  (let [prods (tasks/get-table-elements :installed-view 0)
        indexes (range 0 (tasks/ui getrowcount :installed-view))
        prodlist (map (fn [item index] [item index]) prods indexes)]
    (if-not debug
      (to-array-2d (vec prodlist))
      prodlist)))

(defn printfact []
  (pprint (sort @gui-facts))
  (println (str "cli-facts: " (count @cli-facts)))
  (println (str "gui-facts: " (count @gui-facts)))
  (println (str "fact: " (@cli-facts "virt.is_guest")))
  (println (str "fact: " (@gui-facts "virt.is_guest"))))

(gen-class-testng)
