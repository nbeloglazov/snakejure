(ns snakejure.client
  (:require [quil.core :as quil]
            [snakejure.core :as core]))

(def cell-size 20)
(def semaphore (java.util.concurrent.Semaphore. 0))
(def frequency 300)

(def test-world {:apples #{}
                 :snakes [{:body [[2 3] [1 3] [1 4] [1 5] [2 5] [3 5] [3 4]]
                           :dir :right}]
                 :walls #{[20 20] [30 30]}})
(def world (atom test-world))

(defn to-real-coords [cell]
  (map #(* cell-size %) cell))

(defn draw-cell [draw-fn cell]
  (let [[real-x real-y] (to-real-coords cell)]
    (draw-fn real-x real-y cell-size cell-size)))

(defn draw-snake [{:keys [body]}]
  (quil/fill 0 255 0)
  (doseq [cell body]
    (draw-cell quil/rect cell)))

(defn draw-apples [apples]
  (quil/fill 255 0 0)
  (doseq [apple apples]
    (draw-cell quil/ellipse apple)))

(defn draw-walls [walls]
  (quil/fill 139 69 19)
  (doseq [wall walls]
    (draw-cell quil/rect wall)))

(defn draw []
  (.acquire semaphore)
  (let [{:keys [snakes apples walls]} @world]
    (quil/background 200)
    (quil/ellipse-mode :corner)
    (doseq [snake snakes]
      (draw-snake snake))
    (draw-apples apples)
    (draw-walls walls)))

(defn setup []
  (quil/smooth)
  (quil/frame-rate 60))

(declare timer)

(when (.isBound #'timer)
  (println "Cancelling timer")
  (.cancel timer))

(def timer (java.util.Timer.))

(defn next-step []
  (swap! world core/update-world)
  (.release semaphore))

(defn run []
  (let [timer-task (proxy [java.util.TimerTask] []
                     (run [] (next-step)))]
    (quil/sketch
           :title "Multiplayer snake"
           :setup setup
           :draw draw
           :size [(* core/field-width cell-size)
                  (* core/field-height cell-size)])
    (.schedule timer timer-task 0 frequency)))
