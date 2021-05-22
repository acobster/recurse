<?php

// Apparently Chromium is more lax; I added this to get it to work in Firefox.
header("Content-Security-Policy: connect-src 'self' ws://localhost:8080;");

?>
<title>Hello, Websockets!</title>

<h1>Websocket playground</h1>

<p>
  Just playing with Websockets. Based on Hussein Nasser's video
  <a href="https://www.youtube.com/watch?v=2Nt-ZrNP22A&t=1945s">
    WebSockets Crash Course - Handshake, Use-cases, Pros & Cons and more</a>.
</p>

<p><strong>Latest number:</strong> <code id="number"></code></p>

<script>
  const numberElem = document.getElementById('number')

  const ws = new WebSocket('ws://localhost:8080')
  ws.onmessage = msg => {
    numberElem.innerText = JSON.parse(msg.data).number
  }

  ws.onopen = () => {
    ws.send('Hi! This is the client opening a connection!')
  }
</script>
