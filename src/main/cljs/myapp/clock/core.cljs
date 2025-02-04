(ns myapp.clock.core
  (:require [re-frame.core :as rfc]
            [myapp.core.root :refer [root-container]]
            [myapp.clock.views :refer [ui]]
            [myapp.clock.events :as events]
            [reagent.dom.client :as rdc]))

(defn run []
  (rfc/clear-subscription-cache!)
  (rfc/dispatch-sync [::events/initialize-db])
  (rdc/render root-container [ui])
  )

;; Call the run function to start the clock module
(run)