(ns snakejure.server
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue on-closed channel)]
        [aleph.object :only (start-object-server object-client)])
  (:require [snakejure.core :as core]))

(def frequency 300)

(def ground (grounded-channel))
(def broadcast (channel))

(def world (atom (-> core/test-world
                     (core/add-snake "bot1")
                     (core/add-snake "bot2"))))

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
  (let [id (swap! max-snake-id inc)]
    (swap! client-to-snake-map assoc channel id)
    (swap! world core/add-snake id)
    (println "Snake created" id)
    (enqueue channel
             {:type :snake-created
              :id id})))

(defn client-disconnected [channel]
  (let [id (@client-to-snake-map channel)]
    (swap! world core/remove-snake id))
  (swap! client-to-snake-map dissoc channel)
  (println "Client disconnected"))

(defn handler [channel info]
  (println "New Client connected " info)
  (println "Number of clients " (count @client-to-snake-map))
  (on-closed channel #(client-disconnected channel))
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
    (siphon broadcast ground)
    (.schedule timer timer-task 0 frequency)
    #(do
       (println "Stopping server")
       (stop-server)
       (.cancel timer))))