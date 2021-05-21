# CRDT experiments!

Based on [Defanging Order Theory](https://www.youtube.com/watch?v=OOlnp2bZVRs) by John Mumm. My notes on the talk are [here](https://acobster.keybase.pub/recurse/crdts).

## G-Counter simulation

Read `gcounter.clj` to understand what's going on here in detail.

Say we have three nodes on a network. You want each node to keep track of some global state: a single integer. That's our counter. You can't decrement the global count. It can only go up.

Each node has its own G-Counter: a simple CRDT representing that node's **local view** of our grow-only counter. Now let's say the nodes can only talk to each other indirectly, over a simple pub/sub API with two functions: `broadcast!` and `subscribe!`. To converge on the global value, each node sends the other nodes vectors of how many times it thinks it and the other nodes have incremented:

```
[5 3 2]
```

When a node sends this over the wire, it's saying "I think node 0 has 5 increments, node 1 has 3, and node 2 has 2."

To compute the global count, each node need only add up the increment counts it knows about for each node. In other words, the elements in the vector. This is the equivalent of a user asking "how many clicks has this button gotten?" of an edge server. That server may not have the "true" global count, but it is good enough for rock 'n' roll. This is why CRDTs rock.

Running the main loop towards the bottom of `gcounter.clj` prints something like this:

```cl
; eval (current-form): (do (dotimes [_ 3] (Thread/sleep (rand-int 3000)) (increment (...
; (out) Broadcasting: [0 3 1]
; (out) DISTRIBUTED VALUES:
; (out) 0: 3
; (out) 1: 3
; (out) 2: 4
; (out) merged at node 2: [0 3 1]
; (out) merged at node 1: [0 3 1]
; (out) merged at node 0: [0 3 1]
; (out) Broadcasting: [0 4 1]
; (out) DISTRIBUTED VALUES:
; (out) 0: 4
; (out) 1: 5
; (out) 2: 4
; (out) merged at node 0: [0 4 1]
; (out) Broadcasting: [0 3 2]
; (out) DISTRIBUTED VALUES:
; (out) 0: 5
; (out) 1: 5
; (out) 2: 5
; (out) Done incrementing.
nil
; (out) merged at node 1: [0 4 1]
; (out) merged at node 0: [0 4 2]
; (out) merged at node 2: [0 4 2]
; (out) merged at node 2: [0 4 2]
; (out) merged at node 1: [0 4 2]
```

There are a couple cool things to notice here.

First, note that we can't necessarily tell at a glance which node is broadcasting, and that's okay. We don't need to know that information. All each node needs to know is that _something changed_ and it's time to merge in some new state.

Second, note that the DISTRIBUTED VALUES - the values each node understands the global count to be - may not necessarily line up with everything we know (omnisciently) to have been broadcast. For example, when we're `done incrementing`, all the nodes think the global value is 5. But we know - again, omnisciently, by virtue of running this simulation - that there have been broadcasts of `[0 4 1]` and `[0 3 2]`. Taking the join of these broadcasts gives us the vector `[0 4 2]`, for a true global value of 0 + 4 + 2 = 6. But none of the nodes know that by the time the main loop finishes running (that's the final `nil` in the output).

It's only after all nodes have finished getting their subscriptions that they converge on that value.

Let's check the internal state of each node once all the merges have happened:

```clj
; eval (current-form): (map deref nodes)
({:idx 0, :state [0 4 2]}
 {:idx 1, :state [0 4 2]}
 {:idx 2, :state [0 4 2]})
```

And the global value at each node:

```clj
; eval (current-form): (map value nodes)
(6 6 6)
```

Eventual consistency in action! 🤘

One more thing: You may have noticed in the output that node 2 merges the same vector twice:

```clj
; (out) merged at node 2: [0 4 2]
; (out) merged at node 2: [0 4 2]
```

I'll leave the why and how this happens as an exercise for the reader. The takeaway is that this is the property of idempotence coming into play: because we've constructed our counter to tolerate duplicate merge operations, we correctly converge on the true global value when they happen.