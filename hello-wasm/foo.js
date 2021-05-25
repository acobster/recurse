import fs from 'fs'

WebAssembly.compile(new Uint8Array(
  `00 61 73 6d  01 00 00 00  01 09 02 60  00 00 60 01
7f 01 7f 03  03 02 00 01  05 04 01 00  80 01 07 07
01 03 66 6f  6f 00 01 08  01 00 0a 3a  02 02 00 0b
35 01 01 7f  20 00 41 04  6c 21 01 03  40 01 01 01
0b 03 7f 41  01 0b 20 01  41 e4 00 6c  41 cd 02 20
01 1b 21 01  41 00 20 01  36 02 00 41  00 21 01 41
00 28 02 00  0f 0b 0b 0e  01 00 41 00  0b 08 00 00
00 00 2c 00  00 00`.split(/[\s\r\n]+/g).map(v => parseInt(v, 16))
)).then(mod => {
  let m = new WebAssembly.Instance(mod)
  console.log('foo(1) =>', m.exports.foo(1))
  console.log('foo(2) =>', m.exports.foo(2))
  console.log('foo(3) =>', m.exports.foo(3))
})

