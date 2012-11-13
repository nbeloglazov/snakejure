(ns snakejure.server
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)]
        [snakejure.core :as core]))

(def frequency 300)

(def broadcast (grounded-channel))
(def world (atom test-world))
(def client-to-snake-map (atom {}))

(defn handler [channel info]
  (let [occupied (set (concat (:apples @world) (:walls @world) (mapcat :body (:snakes @world))))
        snake-idx (count (:snakes @world))
        snake-head (first (core/rand-cells occupied))
        snake {:body [snake-head] :dir :down}]
    (siphon broadcast channel)
    (swap! client-to-snake-map assoc channel snake-idx)
    (swap! world assoc :snakes (conj (:snakes @world) snake))
    (println "New snake! Number: " snake-idx " Snake: " snake)
    (receive-all channel
      #(swap! world assoc-in [:snakes (@client-to-snake-map channel) :dir] %))))

(def timer (java.util.Timer.))

(defn next-step []
  (swap! world core/update-world)
  (enqueue broadcast @world))

(defn start-server []
  (let [timer-task (proxy [java.util.TimerTask] []
                 (run [] (next-step)))]
    (println "Starting server at port 12345")
    (.schedule timer timer-task 0 frequency)
    (start-object-server
      handler
      {:port 12345})))