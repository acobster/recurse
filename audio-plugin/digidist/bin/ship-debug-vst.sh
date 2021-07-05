#!/usr/bin/env bash

set -ex

# Builds and ships the VST plugin (shared lib) to /usr/local/lib/vst/libwhisper.so

cargo build
cp target/debug/libwhisper.so /usr/local/lib/vst

RUST_BACKTRACE=full \
LD_LIBRARY_PATH=/opt/Ardour-6.7.0/lib \
/opt/Ardour-6.7.0/lib/ardour-vst-scanner -f /usr/local/lib/vst/libwhisper.so

echo 'built to /usr/local/lib/vst/libwhisper.so'