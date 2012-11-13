(ns snakejure.server
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)]
        [snakejure.core :as core]))

(def frequency 300)

(def broadcast (grounded-channel))
(def world (atom test-world))
(def client-to-snake-map (atom {}))
(def max-snake-id (atom 1))

(defn find-snake-idx [id]
  (let [snake-ids (vec (keep :id (:snakes @world)))
        idx (.indexOf snake-ids id)]
    (if (= idx -1) nil idx)))

(defn move-snake-callback [dir id]
  (if-let [idx (find-snake-idx id)]
    (swap! world assoc-in [:snakes (find-snake-idx id) :dir] dir)))

(defn handler [channel info]
  (let [occupied (set (concat (:apples @world) (:walls @world) (mapcat :body (:snakes @world))))
        snake-head (first (core/rand-cells occupied))
        snake {:body [snake-head] :dir :down :id (swap! max-snake-id inc)}]
    (siphon broadcast channel)
    (swap! client-to-snake-map assoc channel @max-snake-id)
    (swap! world assoc :snakes (conj (:snakes @world) snake))
    (println "New snake!" snake)
    (receive-all channel
      #(move-snake-callback % (@client-to-snake-map channel)))))

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