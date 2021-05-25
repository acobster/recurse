import fs from 'fs'
import wabt from 'wabt'

wabt().then(wabt => {
  const wat    = wabt.parseWat("main.wat", fs.readFileSync("main.wat", "utf8"))
  const bin    = wat.toBinary({})
  const module = WebAssembly.instantiate(bin.buffer)

  module.then(wasm => {
    const {theAnswer} = wasm.instance.exports
    console.log("the answer is:", theAnswer())
  })
})
