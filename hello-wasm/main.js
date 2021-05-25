import fs from 'fs'
import wabt from 'wabt'

wabt().then(wabt => {
  const wat    = wabt.parseWat("main.wat", fs.readFileSync("main.wat", "utf8"))
  const bin    = wat.toBinary({
    console: { log: console.log }
  })
  const module = WebAssembly.instantiate(bin.buffer)

  module.then(wasm => {
    const {theAnswer, fib} = wasm.instance.exports
    console.log("the answer is:", theAnswer())
    console.log("fib(0):", fib(0))
    console.log("fib(1):", fib(1))
    console.log("fib(2):", fib(2))
    console.log("fib(3):", fib(3))
    console.log("fib(4):", fib(4))
    console.log("fib(8):", fib(8))
  })
})
