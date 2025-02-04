(ns myapp.core.events
  (:require [re-frame.core :as rfc]
            [reitit.frontend.easy :as reititeasy]
            [reitit.frontend.controllers :as rfcontrol]))
;; Triggering navigation from events.
(rfc/reg-fx :push-state
            (fn [route]
              (js/console.log "reg-fx :push-state:" route)
              (apply reititeasy/push-state route)))

;;; Events ;;;
(rfc/reg-event-db ::initialize-db
                  (fn [db _]
                    (if db
                      db
                      {:current-route nil})))

(rfc/reg-event-fx ::push-state
                  (fn [_ [_ & route]]
                    (js/console.log "::push-state -> Navigating to route:" route)
                    {:push-state route}))

(rfc/reg-event-db ::navigated
                  (fn [db [_ new-match]]
                    (let [old-match (:current-route db)
                          controllers (rfcontrol/apply-controllers (:controllers old-match) new-match)]
                      (assoc db :current-route (assoc new-match :controllers controllers)))))

;;; Subscriptions ;;;
(rfc/reg-sub ::current-route
             (fn [db]
               (:current-route db)))