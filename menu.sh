# /bin/sh
java -cp "lib/*":"src":. clojure.main -e "(do (use 'snakejure.menu-gui) (snakejure.menu-gui/show-menu))"