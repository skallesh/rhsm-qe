(ns rhsm.app.sm.ui
  (:require [mount.core :refer [defstate]]
            [rhsm.gui.tasks.ui :as ui]
            )
  )

(defn init-windows []
  (println "init of ui/windows")
  {:register-dialog {:id 10}}
  )

(defstate windows :start (init-windows))
