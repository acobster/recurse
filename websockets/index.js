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

// Keep track of all connected clients.
const clients = {}
let nextClientId = 0

websocket.on('request', req => {
  connection = req.accept(null, req.origin)

  connection.on('message', msg => {
    console.log(`message: ${msg.utf8Data}`)
  })

  // Keep track of this client's connection so we can cancel its timer
  // when the connection closes.
  const clientId = nextClientId
  clients[clientId] = { connection }
  console.log(`clientId = ${clientId}`)
  sendRandomized(clientId)

  // Cancel the timer on close.
  connection.on('close', () => {
    console.log(`Connection to client ${clientId} closed.`)
    clearTimeout(clients[clientId].timeout)
  })

  // Avoid overwriting existing client connections.
  nextClientId++
})

function sendRandomized(clientId) {
  const payload = { number: Math.random() }
  connection.send(JSON.stringify(payload))

  // Set a timeout and persist a reference to it, so we can cancel it on close.
  clients[clientId].timeout = setTimeout(() => sendRandomized(clientId), 3000)
}

server.listen(8080, () => console.log('listening on 8080'))
