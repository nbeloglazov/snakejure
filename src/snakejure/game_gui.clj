(ns snakejure.game-gui
      (:use (clojure.contrib import-static
			     logging)
	    (snakejure core))
      (:require (snakejure.levels basic))
      (:import (java.awt Dimension)
	       (java.awt.event ActionListener KeyAdapter)
	       (javax.swing JPanel Timer)))

(import-static java.awt.event.KeyEvent VK_SPACE VK_LEFT VK_RIGHT VK_UP VK_DOWN VK_ESCAPE)

(def key-to-dir {VK_RIGHT :right
                 VK_LEFT :left
		 VK_UP :up
		 VK_DOWN :down})
(def size 20)
(def speed 100)
(def dir-keys (set (keys key-to-dir)))

(defn- draw-point 
  "Draws point on given graphics g."
  [g [y x] [dy dx]]
  (.fillRect g (* size (+ x dx)) 
	       (* size (+ y dy)) 
	       size 
	       size))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :snake [g {:keys [body color]} d]
  (.setColor g color)
  (doseq [part body] (draw-point g part d)))

(defmethod paint :apple [g {:keys [location color]} d]
  (.setColor g color)
  (draw-point g location d))

(defmethod paint :wall [g {:keys [location color]} d]
  (.setColor g color)
  (draw-point g location d))

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake d]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location d))

(defn- switch-timer
  "Stop timer, if it's running.
  Start otherwise."
  [timer]
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

(defn- poor-game-panel 
  "Creates proxy...
  See source :)"
  [level]
  (proxy [JPanel ActionListener] []
	 (paintComponent [g]
			 (let-map [@level snake noisers walls apple d]
				  (proxy-super paintComponent g)
				  (paint g apple d)
				  (paint g snake d)
				  (doseq [noiser noisers] (paint g noiser snake d))
				  (doseq [wall walls] (paint g wall d))))
	 (getPreferredSize []
			   (Dimension. (* (inc (dec width)) size) 
				       (* (inc (dec height)) size)))))

(defn- create-action-listener [level panel end-fn]
  (proxy [ActionListener] []
	 (actionPerformed [e]
			  (let-map [@level snake noisers walls] 
			    (update-snake-position level)
			    (trace (level-to-str @level :walls))
			    (when (lose? snake walls) 
			      (end-fn :lose)
			      (switch-timer (.getSource e)))
			    (when (win? snake noisers)
			      (end-fn :win)
			      (switch-timer (.getSource e)))
			    (.repaint panel)))))


(defn- create-key-listener [level timer end-fn]
  (proxy [KeyAdapter] []
	 (keyPressed [e]
		     (let [code (.getKeyCode e)]
		       (when (= code VK_SPACE)
			 (switch-timer timer))
		       (when (= code VK_ESCAPE)
			 (end-fn :exit)
			 (switch-timer timer))
		       (when (dir-keys code)
			 (update-snake-direction level (key-to-dir (.getKeyCode e))))))))

(defn create-game-panel [lvl end-fn]
  "Creates panel, which displays game level.
  end-fn will be called when user wins or loses."
  (info (level-to-str lvl))
  (let [level (ref (add-d lvl))
	panel (poor-game-panel level)
	action-listener (create-action-listener level panel end-fn)
	timer (Timer. speed action-listener)
	key-listener (create-key-listener level timer end-fn)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))))
