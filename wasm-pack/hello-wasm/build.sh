#!/usr/bin/env bash

cargo build
wasm-pack build --target web --out-dir public/js
