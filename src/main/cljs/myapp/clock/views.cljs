(ns myapp.clock.views
  (:require [re-frame.core :as rfc]
            [myapp.clock.events :as events]
            [clojure.string :as str]))

(defn dispatch-timer-event
  []
  (let [now (js/Date.)]
    (rfc/dispatch [::events/timer now])))

(defn start-timer []
  (js/setInterval dispatch-timer-event 1000))

(defonce do-timer (start-timer))

(defn clock
  []
  (let [colour @(rfc/subscribe [::events/time-color])
        time (or @(rfc/subscribe [::events/time]) (js/Date.))]
    (if time
      (let [time-str (-> time .toTimeString (str/split " ") first)]
        [:div {:style {:color       colour
                       :font-size   "128px"
                       :line-height "1.2em"
                       :font-family "HelveticaNeue-UltraLight, Helvetica"}} time-str])
      [:div {:style {:color       colour
                     :font-size   "128px"
                     :line-height "1.2em"
                     :font-family "HelveticaNeue-UltraLight, Helvetica"}} "Loading..."])))

(defn color-input
  []
  (let [color (rfc/subscribe [::events/time-color])
        gettext (fn [e] (-> e .-target .-value))
        emit (fn [e] (rfc/dispatch [::events/time-color-change (gettext e)]))]
    [:div.color-input
     "Display color: "
     [:input {:type      "text"
              :style     {:border "1px solid #CCC"}
              :value     @color ;; subscribe
              :on-change emit}]]))                          ;; <---

(defn ui
  []
  [:div
   [:h1 "The time is now:"]
   [clock]
   [color-input]])