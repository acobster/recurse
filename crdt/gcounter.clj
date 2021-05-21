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

(defn unsub-all!
  "Dev helper for cancelling all subscriptions to the endpoint channel."
  []
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


;; Underlying CRDT logic. These are like the internal functions that would
;; live in the core of a CRDT library API, rather than public API functions.

(defn join
  "Merge two increment vectors together by taking the max of each
  corresponding pair at a given index."
  [a b]
  (mapv max a b))

(defn value*
  "Get the current global value based on node-local view of the state."
  [v]
  (reduce + v))

(comment
  (join [3 2 0] [2 2 1])
  ;; => [3 2 1]

  (value* [1 2 3])
  ;; => 6
  )


;; Set up our global state underlying the simulated network. Nodes subscribe
;; themselves to updates from the global endpoint channel, much as they would
;; in a client-side app over a websocket or something.

(declare nodes)

(defn subscribe-node! [idx]
  (subscribe! (fn [new-state]
                (let [node (get nodes idx)
                      merged (join new-state (get @node :state))]
                  (prnf "merged at node %s: %s" idx merged)
                  (swap! node assoc :state merged)))))

(defn node
  "Create a node and subscribe it to updates coming over the endpoint channel."
  [idx]
  (let [local-node (atom {:idx idx
                          :state [0 0 0]})]
    (subscribe-node! idx)
    local-node))

(def nodes [(node 0) (node 1) (node 2)])


;; The "top-level" API in our simulation which uses CRDTs "under the hood"
;; for convergence.

(defn increment [node]
  (let [{:keys [idx state]} @node
        state (update state idx inc)]
    (swap! node assoc :state state)
    (broadcast! state)))

(defn value [node]
  (let [{:keys [state]} @node]
    (value* state)))


(comment
  (value [3, 2, 1])
  (= [3, 2, 1] (join [3, 2, 0] [2, 2, 1]))

  (broadcast! [2 0 0])
  (broadcast! [0 1 0])
  (broadcast! [0 1 3])

  ;; Evaluate this in the REPL to see DISTRIBUTED VALUES converge on the
  ;; true global value.
  (do
    (dotimes [_ 3]
      ;; Pause for up to three seconds.
      (Thread/sleep (rand-int 3000))
      (increment (get nodes (rand-int 3)))
      ;; Print what each node thinks the global value is. For example,
      ;; [3 2 1] means:
      ;; - Node 0 thinks the global value is 3
      ;; - Node 1 thinks the global value is 2
      ;; - Node 2 thinks the global value is 1
      (apply prnf "DISTRIBUTED VALUES:\n0: %s\n1: %s\n2: %s"
                  (mapv value nodes)))
    ;; Note that by the time we get here, some or all subscribed nodes
    ;; may not be up to date because of "network" latency. 🐙
    ;; You'll likely see more "merged..." messages after this. This is the
    ;; nodes converging on the global value: Eventual consistency in action!
    (println "Done incrementing."))

  (map deref nodes)
  (map value nodes)

  )
