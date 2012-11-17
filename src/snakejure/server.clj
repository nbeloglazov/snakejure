(ns snakejure.server
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)])
  (:require [snakejure.core :as core]))

(def frequency 300)

(def broadcast (grounded-channel))
(def world (atom core/test-world))
(def client-to-snake-map (atom {}))
(def max-snake-id (atom 1))

(defn find-snake-idx [id]
  (let [snake-ids (vec (keep :id (:snakes @world)))
        idx (.indexOf snake-ids id)]
    (if (= idx -1) nil idx)))

(defn move-snake [dir id]
  (if-let [idx (find-snake-idx id)]
    (swap! world assoc-in [:snakes (find-snake-idx id) :dir] dir)))

(defmulti handle-message (fn [mes ch] (:type mes)))

(defmethod handle-message :move-snake
  [{:keys [dir]} channel]
  (move-snake dir (@client-to-snake-map channel)))

(defmethod handle-message :create-snake
  [_ channel]
  (let [occupied (set (concat (:apples @world) (:walls @world) (mapcat :body (:snakes @world))))
        snake-head (first (core/rand-cells occupied))
        id (swap! max-snake-id inc)
        snake {:body [snake-head] :dir :down :id id}]
    (swap! client-to-snake-map assoc channel id)
    (swap! world update-in [:snakes] conj snake)
    (enqueue channel
             {:type :snake-created
              :id id})))

(defn handler [channel info]
  (println "New Client connected " info)
  (siphon broadcast channel)
  (receive-all channel #(handle-message % channel)))

(defn next-step []
  (swap! world core/update-world)
  (enqueue broadcast
           {:type :update-world
            :world @world}))

(defn start-server []
  (let [timer (java.util.Timer.)
        timer-task (proxy [java.util.TimerTask] []
                     (run [] (next-step)))
        stop-server (start-object-server
                     handler
                     {:port 12345})]
    (println "Starting server at port 12345")
    (.schedule timer timer-task 0 frequency)
    #(do
       (println "Stopping server")
       (stop-server)
       (.cancel timer))))