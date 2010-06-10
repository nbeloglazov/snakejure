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
(def dir-keys (set (keys key-to-dir)))

(defn reset
  "Takes refs of snake, apple, noisers and resets game.
  It will set to refs new generated snake, apple and ref.
  Snake will be 1 unit length."
  [snake apple noisers walls]
  (let [level (snakejure.levels.basic/create-level)]
    (dosync
     (ref-set walls (level :walls))
     (ref-set snake (level :snake))
     (ref-set apple (level :apple))
     (ref-set noisers (level :noisers)))))



(defn draw-point 
  "Draws point on given graphics g."
  [g [y x]]
  (.fillRect g (* size x) (* size y) size size))

(defmulti paint (fn [g object & _] (:type object)))

(defmethod paint :snake [g {:keys [body color]}]
  (.setColor g color)
  (doseq [part body] (draw-point g part)))

(defmethod paint :apple [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :wall [g {:keys [location color]}]
  (.setColor g color)
  (draw-point g location))

(defmethod paint :noiser [g {:keys [location color-noise color-silence]} snake]
  (if (overlaps-body? snake location)
    (.setColor g color-noise)
    (.setColor g color-silence))
  (draw-point g location))

(defn switch-timer [timer]
  "Stop timer, if it's running.
  Start otherwise."
  (if (.isRunning timer) 
    (.stop timer)
    (.restart timer)))

      
(defn game-panel 
  "Creates proxy...
  See source :)"
  [frame snake apple noisers walls]
  (proxy [JPanel ActionListener] []
	 (paintComponent [g]
			 (proxy-super paintComponent g)
			 (paint g @apple)
			 (paint g @snake)
			 (doseq [noiser @noisers] (paint g noiser @snake))
			 (doseq [wall @walls] (paint g wall)))
	 (actionPerformed [e]
			  (update-position snake apple walls)
			  (when (lose? @snake @walls) 
			    (JOptionPane/showMessageDialog frame "You lose...")
			    (reset snake apple noisers walls))
			  (when (win? @snake @noisers)
			    (JOptionPane/showMessageDialog frame "You win!")
			    (reset snake apple noisers walls))
			  (.repaint this))
	 (getPreferredSize []
			   (Dimension. (* (inc (dec width)) size) 
				       (* (inc (dec height)) size)))))

(defn create-key-listener [snake timer]
  (proxy [KeyListener] []
	 (keyPressed [e]
		     (if (= (.getKeyCode e) VK_SPACE)
		       (switch-timer timer))
		     (if (dir-keys (.getKeyCode e))
		       (update-direction snake (key-to-dir (.getKeyCode e)))))
	 (keyReleased [e])
	 (keyTyped [e])))

(defn game []
  "Starts game. It will create frame with walls, snake, apple and noisers in it."
  (let [walls (ref nil)
	snake (ref nil)
	apple (ref nil)
	noisers (ref nil)
	frame (JFrame. "Snake")
	panel (game-panel frame snake apple noisers walls)
	timer (Timer. speed panel)
	key-listener (create-key-listener snake timer)]
    (reset snake apple noisers walls)
    (doto panel
      (.setFocusable true)
      (.addKeyListener key-listener))
    (doto frame
      (.add panel)
      (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
      (.pack)
      (.setVisible true))
    (.start timer)))
