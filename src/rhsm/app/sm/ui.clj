(ns rhsm.app.sm.ui
  (:require [mount.core :refer [defstate]]
            [rhsm.gui.tasks.ui :as ui]
            [rhsm.gui.tasks.tools :as tt]
            )
  )

(defmethod ui/windows-map-by-rhel-version "RHEL7" [release]
  (assoc ui/windows :register-dialog "register_dialog"))

(defmethod ui/windows-map-by-rhel-version "RHEL6" [release]
  ui/windows)

(defn init-windows []
  (ui/define-windows (ui/windows-map-by-rhel-version (tt/get-release true)))
  )

(defstate windows :start (init-windows))
