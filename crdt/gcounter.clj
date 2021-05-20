;; A simple grow-only counter CRDT, AKA G-Counter.
;; Based on:
;; - https://www.youtube.com/watch?v=OOlnp2bZVRs
;; - https://acobster.keybase.pub/recurse/crdts

;; To start a REPL:
;; $ clj -m nrepl.cmdline --interactive
(ns gcounter
  (:require
    [clojure.core.async :refer [chan <! put! mult tap untap go-loop]]))

;; Define a simple top-level API:
;; `value`
;; `increment`

;; debug helper
(defn prnf [& args]
  (println (apply format args)))

;; PLUMBING
;; This isn't really part of our CRDT implementation or API, think of it as
;; basically the network topology.

;; First, set up a global channel to broadcast over.
(def broadcast-channel (chan))
;; Now set up a global endpoint where all nodes can listen for broadcasts.
(def endpoint (mult broadcast-channel))

(def subscriptions (atom #{}))

(defn broadcast!
  "Broadcast the message x. Can be literally any value."
  [x]
  (prnf "Broadcasting: %s" x)
  (put! broadcast-channel x))

(defn subscribe!
  "Subscribe to the global endpoint, calling (f message) for each message
  received. Returns a channel tapped into the endpoint channel."
  [f]
  (let [subscription (chan)]
    (tap endpoint subscription)
    (swap! subscriptions conj subscription)
    (go-loop []
             (let [message (<! subscription)]
               ;; Simulate network latency! 🐙
               (Thread/sleep (rand-int 3000))
               (f message))
             (recur))
    subscription))

(defn unsub-all! []
  (doall (for [subscription @subscriptions]
           (untap endpoint subscription)))
  (reset! subscriptions #{}))


(comment
  (subscribe! (fn [message] (prnf "Node 1: %s" message)))
  (subscribe! (fn [message] (prnf "Node 2: %s" message)))

  (broadcast! [3, 2, 1])
  (broadcast! [1, 1, 2])

  (deref subscriptions)
  (unsub-all!)

  )


;; join is a merge using max(): merge([3, 2, 0], [2, 2, 1]) => [3, 2, 1]
;; value is just sum(): value([3, 2, 1]) => 6

(defn join [a b]
  (vec (map max a b)))

(defn sum [v]
  (reduce + v))


(defn node [idx]
  (atom {:idx idx
         :state [0 0 0]}))

(def nodes [(node 0) (node 1) (node 2)])

(defn subscribe-node! [idx]
  (subscribe! (fn [new-state]
                (let [node (get nodes idx)
                      merged (join new-state (get @node :state))]
                  (prnf "merged at idx %s: %s" idx merged)
                  (swap! node assoc :state merged)))))


(defn increment [{:keys [idx state]}]
  (let [state (update state idx inc)]
    (prnf "new state at node %s: %s" idx state)
    ;; here's where we're muddying the waters a little bit by using global
    ;; state - we have access to all nodes at this point and we need to
    ;; swap! a specific one. In a truly distribute system, increment would
    ;; not have access to all nodes: we'd only be operating on a single nodes'
    ;; local state.
    (swap! (get nodes idx) assoc :state state)
    (broadcast! state)))

(defn value [{:keys [state]}]
  (sum state))


(comment
  (value [3, 2, 1])
  (= [3, 2, 1] (join [3, 2, 0] [2, 2, 1]))

  (broadcast! [2 0 0])
  (broadcast! [0 1 0])
  (broadcast! [0 1 3])

  (subscribe-node! 0)
  (subscribe-node! 1)
  (subscribe-node! 2)

  ;; Evaluate this in the REPL to see DISTRIBUTED VALUES converge on the
  ;; true global value.
  (doall (for [_ (range 3)]
           (do
             (Thread/sleep (rand-int 3000))
             (increment (deref (get nodes (rand-int 3))))
             (prnf "DISTRIBUTED VALUES: %s"
                   (vec (map (fn [node] (value @node)) nodes)))
             ;; Note that by the time we get here, some or all subscribed nodes
             ;; may not be up to date because of "network" latency 🐙
             :done)))

  (map deref nodes)
  (map (fn [node] (value @node)) nodes)

  )
