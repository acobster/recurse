(module

  (func $theAnswer (export "theAnswer") (result i32)
    i32.const 42)

  ;; Recursive version of fib()
  ;;(func $fib (export "fib") (param $n i32) (result i32)
  ;;  (if (result i32) (i32.lt_s (get_local $n) (i32.const 2))
  ;;    (then
  ;;      (i32.const 1))
  ;;    (else
  ;;      (i32.mul
  ;;        (get_local $n)
  ;;        (call $fib (i32.sub (get_local $n) (i32.const 1)))))))

  ;; I'm not sure what a "canonical" fibonacci implementation is actually
  ;; supposed to do with 0...whatevs
  (func $fib (export "fib") (param $n i32) (result i32)
    (local $prod i32)
    (if (result i32) (i32.lt_s (get_local $n) (i32.const 1))
      ;; handle < 1
      (then
        (i32.const -1))
      (else
        ;; prod = n
        (set_local $prod (get_local $n))
        (block
          ;; while n > 1
          (loop (br_if 1 (i32.eq (i32.const 1) (get_local $n)))
            (set_local $n (i32.sub (get_local $n) (i32.const 1)))
            (set_local $prod (i32.mul (get_local $prod) (get_local $n)))
            (br 0)))
        (get_local $prod))))

  )
