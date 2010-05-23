# /bin/sh
java -cp "lib/*":"src":. clojure.main -e "(do (use 'snakejure.ui) (snakejure.ui/game))"