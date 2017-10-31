(ns rhsm.cockpit.cli
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]))

(defn subscription-status
  ([run-command locale]
   (let [localized-status (-> (format "LANG=%s gettext rhsm 'Status'" locale)
                              run-command :stdout s/trim)]
     (->> (format "LANG=%s subscription-manager status" locale)
          run-command
          :stdout
          (re-find #"Status:[\ \t]+([^\n]+)")
          first)))
  ([run-command]
   (subscription-status run-command "en_US")))
