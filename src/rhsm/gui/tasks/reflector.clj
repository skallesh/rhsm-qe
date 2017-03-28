(ns rhsm.gui.tasks.reflector
  (:import [org.testng.annotations Test AfterSuite]
           (java.nio.file Files))
  (:require [rhsm.gui.tasks.tools :refer [file-list str->Path]]
            [cheshire.core :as ches]
            [test-clj.testng :refer [gen-class-testng]]))

(defn no-ext
  "removes .clj extension"
  [f]
  (let [len (count f)]
    (apply str (take (- len 4) f))))


(defn get-annotations
  [path]
  (let [test? (fn [t] (some (meta t) [Test]))
        get-tests (fn [m] (filter test? m))
        get-fns (fn [n] (vals (ns-publics n)))
        files (file-list path)
        namespaces (map #(-> (str "rhsm.gui.tests." (no-ext %)) symbol) files)]
    (apply require namespaces)
    (flatten (map get-tests (map get-fns namespaces)))))


(def test-types #{:acceptance :tier1 :tier2 :tier3})


(defn get-groups
  ([path]
   (for [s (get-annotations path)]
     (let [m ((meta s) Test)]
       {:name (:name (meta s)) :groups (:groups m)})))
  ([]
   (let [p (str (System/getProperty "user.dir") "/src/rhsm/gui/tests")]
     (get-groups p))))


(defn get-tests-by-group
  "Creates a map with 4 keys: acceptance, tier1, tier2, and tier3

  If a defn belongs to one of these groups, it will be stores in a vector"
  [& {:keys [g path]}]
  (letfn [(updater [curr-val new-val]
            (if (nil? curr-val)
              [new-val]
              (conj curr-val new-val)))
          (gather [acc m]
            (let [{:keys [name groups]} m
                  groups (map keyword groups)
                  group-set (clojure.set/intersection test-types (set groups))]
              (reduce (fn [m k]
                        (update m k updater name)) acc group-set)))]
    (let [g (if g
              g
              (if path
                (get-groups path)
                (get-groups)))]
      (reduce gather {} g))))

(defn -parent-dirs
  "Takes a Path object, returns a lazy sequence of the parent directories"
  [path]
  (let [parent (if path
                 (.getParent path)
                 path)]
    (lazy-seq
      (when path
        (cons path (-parent-dirs parent))))))

(defn parent-dirs
  ([^String path]
   (let [p (str->Path path)]
     (into [] (-parent-dirs p))))
  ([]
   (let [fpath (System/getProperty "user.dir")]
     (parent-dirs fpath))))


(defn ^{AfterSuite {:groups ["cleanup"]}}
  metadata
  "After the suite has run, generate a "
  []
  (let [metadata-json "clojure-metadata.json"
        levels-to-methods (into (sorted-map) (get-tests-by-group))
        json-string (ches/generate-string levels-to-methods {:pretty true})
        parents (parent-dirs (System/getProperty "user.dir"))
        test-output  (.resolve (first parents) "test-output")
        mj-path (.resolve test-output metadata-json)
        metadata-exists? (some #{metadata-json}
                               (file-list (.toString test-output)))]
    (when metadata-exists?
      (Files/delete (.resolve test-output mj-path)))
    (spit (.toString mj-path) json-string)))

(gen-class-testng)