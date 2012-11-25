(ns snakejure.bots.suicide
  (:require [snakejure.core :as core])
  (:use [lamina.core :only (receive-all enqueue close)]
        [aleph.object :only (object-client)]
        [clojure.set :only (difference union intersection)]))


(defn neib [cell [dx dy]]
  (-> cell
      (update-in [0] #(mod (+ % dx) core/field-width))
      (update-in [1] #(mod (+ % dy) core/field-height))))

(defn neibs [cell]
  (map #(neib cell %) [[1 0] [0 1] [-1 0] [0 -1]]))

(defn next-wave [cells forbidden]
  (difference (into #{} (mapcat neibs cells))
              forbidden
              cells))

(defn bfs [cells goals forbidden]
  (if (empty? cells)
    nil
    (let [found (first (filter goals cells))]
      (if (nil? found)
        (recur (next-wave cells forbidden)
               goals
               (union cells forbidden))
        (-> found meta :dir)))))

(defn find-move
  ([head goals forbidden]
     (let [init (map (fn [[dir d]]
                       (with-meta
                         (neib head d)
                         {:dir dir}))
                     core/dirs)]
       (or (bfs (set init) (set goals) (conj forbidden head))
           (rand-nth (keys core/dirs)))))
  ([{:keys [snakes walls]} bot-id]
     (let [{enemies false [bot] true} (group-by #(= bot-id (:id%)) snakes)]
       (cond (nil? bot) nil
             (nil? enemies) (:dir bot)
             :default (let [head (first (:body bot))
                            goals (map #(first (:body %)) enemies)
                            forbidden (into walls (mapcat #(rest (:body %)) snakes))]
                        (find-move head goals forbidden))))))

(defn move [server dir]
  (enqueue server {:type :move-snake
                    :dir dir}))

(defmulti handle-message (fn [state message] (:type message)))

(defmethod handle-message :snake-created
  [{:keys [bot-id]} {:keys [id]}]
  (println "Got id" id)
  (reset! bot-id id))

(defmethod handle-message :update-world
  [{:keys [bot-id server]} {:keys [world]}]
  (when-not (nil? @bot-id)
    (if-let [dir (find-move world @bot-id)]
      (move server dir)
      (enqueue server {:type :create-snake}))))

(defn start [host]
  (let [server @(object-client {:host host :port 12345})]
    (receive-all server (partial handle-message
                                {:bot-id (atom nil)
                                 :server server}))
    (enqueue server
             {:type :create-snake})
    (fn [] (close server))))
