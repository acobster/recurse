(module

  (memory $memory 1)
  (export "memory" (memory $memory))

  (func $theAnswer (export "theAnswer") (result i32)
    i32.const 42))
