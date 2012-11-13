(ns snakejure.server
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)]
        [snakejure.core :as core]))

(def frequency 300)

(def broadcast (grounded-channel))
(def world (atom test-world))

(defn handler [channel info]
  (siphon broadcast channel)
  (receive-all channel
  	#(swap! world assoc-in [:snakes 0 :dir] %)))

(def timer (java.util.Timer.))

(defn next-step []
  (swap! world core/update-world)
  (enqueue broadcast @world))

(defn start-server []
	(let [timer-task (proxy [java.util.TimerTask] []
                 (run [] (next-step)))]
		(println "Starting server at port 12345")
		(start-object-server
			handler
			{:port 12345})
		(.schedule timer timer-task 0 frequency)))