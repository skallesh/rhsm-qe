(ns rhsm.cockpit.locales
  (:require [clojure.string :as s]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.java.io :as io]
            [clojure.data.zip.xml :as zip-xml]
            [clojure.tools.logging :as log]))

(def catalog (atom nil))

(defn load-catalog []
  (->> "resources/cockpit-translations.xml"
       io/input-stream
       xml/parse
       zip/xml-zip
       (reset! catalog)))

(defn gettext [run-command locale text]
  (log/spy :info (-> (format "LANG=%s gettext rhsm \"%s\"" locale text)
                     run-command
                     :stdout
                     s/trim)))

(defn get-phrase [text & {:keys [locale] :or {locale "en_US.UTF-8"}}]
  (let [text-pred (every-pred (zip-xml/attr= :orig text) (zip-xml/tag= :phrase))
        translation (-> @catalog
                        (zip-xml/xml1-> 
                         :translation (zip-xml/attr= :lang locale)
                         zip/node)
                        zip/xml-zip)]
    (assert (not (nil? translation)))
    (loop [loc translation]
      (if (zip/end? loc)
        nil
        (if (text-pred loc)
          (zip-xml/text loc)
          (recur (zip/next loc)))))))

(defn verify-against-catalog [orig string & {:keys [locale] :or {locale "en_US.UTF-8"}}]
  (let [phrase (get-phrase orig :locale locale)
        msg (str " \t locale:  " locale "\n"
                 " \t orig:    " orig "\n"
                 " \t catalog: " phrase "\n"
                 " \t string:  " string) ]
    (if (not= phrase string)
      (AssertionError. (str "Locales verification failed: \n" msg))
      (log/info (str "Locales verified: \n" msg)))))
