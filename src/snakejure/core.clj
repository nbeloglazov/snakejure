(ns snakejure.core
  (:use [lamina.core :only (siphon grounded-channel receive-all enqueue)]
        [aleph.object :only (start-object-server object-client)]))

(def field-width 40)
(def field-height 30)

(def broadcast (grounded-channel))

(defn handler [channel info]
  (siphon broadcast channel)
  (receive-all channel #(enqueue broadcast %)))

(defn start-server []
  (println "Starting server at port 12345")
  (start-object-server
   	handler
  	{:port 12345}))

(defn start-client []
  (object-client {:host "localhost" :port 12345}))