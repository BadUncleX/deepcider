(ns myapp.core.views
  (:require [re-frame.core :as rfc]
            [myapp.core.events :as events]))
;;; Views ;;;
(defn sub-page1 []
  [:div
   [:h1 "This is sub-page 1"]])

(defn sub-page2 []
  [:div
   [:h1 "This is sub-page 2"]])
(defn home-page []
  [:div
   [:h1 "This is home page"]
   [:button.button.is-link
    ;; Dispatch navigate event that triggers a (side)effect.x
    {:on-click #(rfc/dispatch [::events/push-state :sub-page2])}
    "Go to sub-page 2"]])

