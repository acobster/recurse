/**
 * Just reviewing some Websocket fundamentals.
 * https://www.youtube.com/watch?v=2Nt-ZrNP22A
 */

const http = require('http')
const WebSocketServer = require('websocket').server

let connection;

const server = http.createServer((req, res) => {
  console.log(req)
})

const websocket = new WebSocketServer({
  httpServer: server
})

websocket.on('request', req => {
  connection = req.accept(null, req.origin)
  connection.on('close', e => console.log('CLOSE!'))
  connection.on('message', msg => {
    console.log(`message: ${msg.utf8Data}`)
  })
  sendRandomized()
})

function sendRandomized() {
  const payload = { number: Math.random() }
  connection.send(JSON.stringify(payload))
  setTimeout(sendRandomized, 3000)
}

server.listen(8080, () => console.log('listening on 8080'))
