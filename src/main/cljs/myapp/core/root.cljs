(ns myapp.core.root
  (:require [reagent.dom.client :as rdc]))

(defonce root-container
         (rdc/create-root (js/document.getElementById "app")))