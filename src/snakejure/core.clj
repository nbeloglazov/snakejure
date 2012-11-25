(ns snakejure.core
  (:use [clojure.set :only (difference)]))

(def field-width 40)
(def field-height 30)
(def number-of-apples 5)

(def test-world {:apples #{}
                 :snakes []
                 :walls #{[20 20] [30 30]}})

(def dirs {:right [1 0]
           :left [-1 0]
           :up [0 -1]
           :down [0 1]})

(defn random-color
  ([]
     (->> #(+ 127 (rand-int 127))
          (repeatedly 3)
          vec))
  ([forbidden]
     (let [colors (repeatedly #(random-color))
           forbidden (set forbidden)]
       (->> colors distinct (remove forbidden) first))))

(defn neib-cell [cell dir]
  (let [[new-x new-y] (map + cell (dirs dir))]
    [(mod (+ new-x field-width) field-width)
     (mod (+ new-y field-height) field-height)]))

(defn rand-cells [occupied]
  (->> #(vector (rand-int field-width) (rand-int field-height))
       repeatedly
       (remove occupied)
       distinct))

(defn move-snake [{:keys [body dir] :as snake} apples]
  (let [new-head (neib-cell (first body) dir)
        new-body (if (apples new-head)
                   (cons new-head body)
                   (cons new-head (butlast body)))]
    (assoc snake :body new-body)))

(defn update-apples [snakes walls apples]
  (let [heads (set (map #(first (:body %)) snakes))
        apples (difference apples heads)
        to-add (- number-of-apples (count apples))]
    (if (pos? to-add)
      (let [occupied (set (concat apples walls (mapcat :body snakes)))]
        (->> (rand-cells occupied)
         (take to-add)
         (into apples)))
      apples)))

(defn update-world [world]
  (let [{:keys [snakes apples walls]} world
        new-snakes (map #(move-snake % apples) snakes)
        occupied (frequencies (concat walls
                                      (mapcat :body new-snakes)))
        dead? (fn [snake] (> (occupied (first (:body snake))) 1))
        alive (remove dead? new-snakes)]
    {:walls walls
     :snakes (vec alive)
     :apples (update-apples alive walls apples)}))

(defn add-snake [world id]
  (let [{:keys [apples walls snakes]} world
        occupied (set (concat apples walls (mapcat :body snakes)))
        snake-head (first (rand-cells occupied))
        colors (map :color snakes)
        snake {:body [snake-head]
               :dir :down
               :id id
               :color (random-color colors)}]
    (update-in world [:snakes] conj snake)))

(defn remove-snake [world id]
  (letfn [(remove-by-id [snakes]
            (remove #(= id (:id %)) snakes))]
   (update-in world [:snakes] remove-by-id)))

