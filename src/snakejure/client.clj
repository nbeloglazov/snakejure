(ns snakejure.client
  (:require [quil.core :as quil]
            [snakejure.core :as core]
            [lamina.core :only (siphon grounded-channel receive-all enqueue)]
            [aleph.object :only (start-object-server object-client)]))

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

(defn next-step [new-world]
  (reset! world new-world)
  (.release semaphore))

(defn key-handler [ch]
  (lamina.core/enqueue ch ({\w :up \s :down \a :left \d :right}
    (quil/raw-key))))

(defn start-client [remote-addr]
  (let [ch @(aleph.object/object-client {:host remote-addr :port 12345})]
    (quil/sketch
           :title "Multiplayer snake"
           :setup setup
           :draw draw
           :size [(* core/field-width cell-size)
                  (* core/field-height cell-size)]
           :key-pressed #(key-handler ch))
    (lamina.core/receive-all ch next-step)))
