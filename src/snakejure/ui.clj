(ns snakejure.ui
      (:use (clojure.contrib import-static)
	    (snakejure core))
      (:require   (snakejure.levels basic))
      (:import (java.awt Dimension)
	       (java.awt.event ActionListener KeyListener)
	       (javax.swing JPanel Timer JFrame JOptionPane)))

(import-static java.awt.event.KeyEvent VK_SPACE VK_LEFT VK_RIGHT VK_UP VK_DOWN)

(def key-to-dir {VK_RIGHT :right
                 VK_LEFT :left
		 VK_UP :up
		 VK_DOWN :down})
(def size 20)
(def speed 100)
(def dir-keys (set (keys key-to-dir)))

(defn- reset
  "Take level ref and set new level to it."
  [level]
  (dosync (ref-set level (add-d (snakejure.levels.basic/create-level)))))

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

(defn- switch-timer [timer]
  "Stop timer, if it's running.
  Start otherwise."
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

(defn- create-action-listener [level panel]
  (proxy [ActionListener] []
	 (actionPerformed [e]
			  (let-map [@level snake noisers walls] 
			    (update-snake-position level)
			    (when (lose? snake walls) 
			      (JOptionPane/showMessageDialog nil "You lose...")
			      (reset level))
			    (when (win? snake noisers)
			      (JOptionPane/showMessageDialog nil "You win!")
			      (reset level))
			    (.repaint panel)))))


(defn- create-key-listener [level timer]
  (proxy [KeyListener] []
	 (keyPressed [e]
		     (if (= (.getKeyCode e) VK_SPACE)
		       (switch-timer timer))
		     (if (dir-keys (.getKeyCode e))
		       (update-snake-direction level (key-to-dir (.getKeyCode e)))))
	 (keyReleased [e])
	 (keyTyped [e])))

(defn- game-panel [level]
  (let [panel (poor-game-panel level)
	action-listener (create-action-listener level panel)
	timer (Timer. speed action-listener)
	key-listener (create-key-listener level timer)]
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))))

(defn game []
  "Starts game. It will create frame with walls, snake, apple and noisers in it."
  (let [level (ref nil)
	frame (JFrame. "Snake")
	panel (game-panel level)]
    (reset level)
    (doto frame
      (.add panel)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))))
