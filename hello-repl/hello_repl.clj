(ns hello-repl
  (:require
    [clojure.main :refer [repl repl-read]]))

(defn- prompt []
  (print "hi> "))

(defn- read-wrapper [request-prompt request-exit]
  (let [in (repl-read request-prompt request-exit)
        in (or ({:x request-exit :next request-prompt} in) in)]
    #_
    (when-not (or (identical? request-prompt res)
                  (identical? request-exit res))
      (printf "%s" (type res)))
    in))

(defn- eval-wrapper [x]
  (let [value (eval x)]
    (printf "%s -> %s\n" x (type value))
    value))

(defn -main [& _]
  (repl :prompt prompt :read read-wrapper :eval eval-wrapper))
