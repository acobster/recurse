;; To start an nREPL server:
;;
;; $ clj -A:repl
;;
;; ...which expands to:
;;
;; $ clj -Sdeps '{:deps {nrepl {:mvn/version "0.7.0"} cider/cider-nrepl {:mvn/version "0.25.2"}}}' \
;;       -m nrepl.cmdline \
;;       --middleware '["cider.nrepl/cider-middleware"]'

(ns recurse.parser
  (:require
    [clojure.string :refer [split]]
    [clojure.test :refer [deftest is]]))

(defn- append
  "Helper for consuming while filtering out empty tokens"
  [& args]
  (apply conj (filter some? args)))

(defn- ->token
  "Helper for constructing tokens, abstracting over simple (string)/complex
   tokens"
  [tok elem]
  (cond
    ;; complex token - append elem to the string value inside token
    (vector? tok) (update tok 1 str elem)
    ;; simple string token
    :else (str tok elem)))

(defn tokenize
  "Take a Lispy string and turn it into a vector of tokens"
  [s]
  (loop [cs (split s #"") token nil tokens []]
    (if (seq cs)
      (condp = (first cs)
        ;; NOTE: WE DO NOT CURRENTLY HANDLE STRINGS
        ;; check for special cs
        " " (recur (next cs) nil (append tokens token))
        "," (recur (next cs) nil (append tokens token))
        "(" (recur (next cs) nil (append tokens :oparen))
        ")" (recur (next cs) nil (append tokens token :cparen))
        "[" (recur (next cs) nil (append tokens :osquare))
        "]" (recur (next cs) nil (append tokens token :csquare))
        "{" (recur (next cs) nil (append tokens :ocurly))
        "}" (recur (next cs) nil (append tokens token :ccurly))
        ;; Symbols
        "'" (recur (next cs) [:SYMBOL] tokens)
        ;; normal alphanumeric token
        (recur (next cs) (->token token (first cs)) tokens))
      ;; base case - just return the list of all tokens
      tokens)))

(defn parse-tokens
  "Parse a vector of tokens into a 2-tuple containing the parsed AST and
   the vector of tokens left to be parsed."
  [tokens close]
  (loop [[ast [token & tail]] [[] tokens]]
    (cond
      ;; first check if we've found the start of a new sub-form
      (= token :oparen)
      (let [[sub-form tail] (parse-tokens tail :cparen)]
        (recur [(conj ast sub-form) tail]))

      (= token :ocurly)
      (let [[sub-form tail] (parse-tokens tail :ccurly)]
        (recur [(conj ast (cons :MAP sub-form)) tail]))

      (= token :osquare)
      (let [[sub-form tail] (parse-tokens tail :csquare)]
        (recur [(conj ast (cons :VECTOR sub-form)) tail]))

      ;; we found the end of the current form being parsed
      (= token close) (do
                        (prn 'CLOSE [ast tail])
                        [ast tail])

      ;; consume a single token
      (seq tail) (recur [(conj ast token) tail]))))

(parse-tokens [:oparen "hey!" :there :cparen] nil)

(defn str->ast [s]
  (let [tokens (tokenize s)]
    (first (parse-tokens tokens nil))))

(tokenize "(first (list 1 (+ 2 3) 9))")
(str->ast "(first (list 1 (+ 2 3) 9))")
(str->ast "(first (list,,, {:x, :y, :a ,,, ['one 'two 'three]} 1 (+ 2 3) 9))")

(run "(first (list,,, {:x :y :a ['one 'two 'three]} 1 (+ 2 3) 9))")

(defn run [s]
  (prn 'TODO)
  s)
