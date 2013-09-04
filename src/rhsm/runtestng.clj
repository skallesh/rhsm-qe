(ns rhsm.runtestng
  (:use [clojure.pprint])
  (:require [clojure.tools.logging :as log]
            [clojure.xml :as xml]
            [clojure.tools.cli :as cli])
  (:import [org.testng.xml Parser XmlSuite]
           [org.testng TestNG])
  (:gen-class))

(defn get-test-names
  [suite]
  (map (fn [y] (:name (get y :attrs)))
       (filter (fn [x] (= :test (get x :tag)))
               (:content (xml/parse suite)))))

(def usage
  "This is a simplified cli utility for launching them rhsm-qe testng suite.

Usage:   lein run [OPTIONS] [TEST-NAMES..] SUITE.XML
Example: lein run 'GUI: REGISTRATION' 'GUI: FACTS' suites/sm-gui-testng-suite.xml")

(defn -main
  [& args]
  (let [[options args banner]
        (cli/cli args
                 usage
                 ["--list-testnames"
                  "Lists the available test names in the given suite.xml"
                  :default false
                  :flag true]
                 ["-h" "--help"
                  "Show help"
                  :default false
                  :flag true])]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (let [suites (filter (fn [x] (re-matches #".*\.xml" x)) args)
          psuite (if-not (empty? suites) (.parse (new Parser (first suites))) nil)
          tests (filter (fn [x] (nil? (re-matches #".*\.xml" x))) args)
          testng (new TestNG)]
      (cond
       (:list-testnames options)
       (doseq [s suites] (println (str "\n" s ":")) (pprint (get-test-names s)))
       :else (do
               (when psuite (.setXmlSuites testng psuite))
               (if-not (empty? tests) (.setTestNames testng tests))
               (when psuite (.run testng)))))))
