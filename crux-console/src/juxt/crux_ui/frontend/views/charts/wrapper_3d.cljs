(ns juxt.crux-ui.frontend.views.charts.wrapper-3d
  (:require [reagent.core :as r]
            ["plotly.js-gl3d-dist" :as Plotly]
            [garden.core :as garden]))


(def ^:private plot-styling
  [:style
   (garden/css
     [:.plotly-container
      {:height :100%}])])

(defn plotly-wrapper
  [data layout]
  (let [-inst      (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (reset! -inst (.newPlot Plotly
                                 (r/dom-node this)
                                 (clj->js data)
                                 (clj->js layout))))

       :reagent-render
       (fn [_ _ _]
         [:div.plotly-container
          [:style plot-styling]])})))