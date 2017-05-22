(ns rhsm.api.rest
  (:require [org.httpkit.client :as http]
            [rhsm.gui.tasks.test-config :refer [config]]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]))

(def http-options {:timeout 3000
                   :insecure? true
                   :accept :json
                   :content-type :json
                   :keepalive 30000})

(defn activation-key-exists  [activation-key-id]
  (let [response @(http/get (format "%s/activation_keys/%s"
                                    (ctasks/server-url)
                                    activation-key-id)
                            (assoc http-options :basic-auth [(@config :username)
                                                             (@config :password)])
                            )]
    (println response)))
