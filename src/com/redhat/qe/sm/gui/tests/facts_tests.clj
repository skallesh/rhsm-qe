(ns com.redhat.qe.sm.gui.tests.facts-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (split-lines split)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test DataProvider]))

(def gui-facts (atom nil))
(def cli-facts (atom nil))

(defn get-cli-facts []
  (let [allfacts (.getStdout
                  (.runCommandAndWait @clientcmd
                                      "subscription-manager facts --list"))
        allfactpairs (split-lines allfacts)
        factslist (into {} (map (fn [fact] (vec (split #": " fact)))
                                allfactpairs))]
    factslist))

(defn ^{BeforeClass {:groups ["facts"]}}
  register [_]
  (let [ownername (tasks/get-owner-display-name (@config :username)
                                                (@config :password)
                                                (@config :owner-key))]
    (with-handlers [(handle :already-registered [e]
                            (recover e :unregister-first))]
      (tasks/register (@config :username)
                      (@config :password)
                      :owner ownername)))
  (reset! gui-facts (tasks/get-all-facts))
  (reset! cli-facts (get-cli-facts))) 

(defn ^{Test {:groups ["facts"]
              :dataProvider "guifacts"}}
  match_each_fact [_ fact value]
  (verify (= (cli-facts fact) value)))

(defn ^{Test {:groups ["facts"]}}
  facts_parity [_]
  (verify (= (count @cli-facts)
             (count @gui-facts))))


(defn ^{DataProvider {:name "guifacts"}}
  get_facts [_]
  (to-array-2d (vec @gui-facts)))

