// https://developer.mozilla.org/en-US/docs/WebAssembly/Rust_to_wasm
//
// "wasm-pack uses wasm-bindgen, another tool, to provide a bridge between the
// types of JavaScript and Rust. It allows JavaScript to call a Rust API with
// a string, or a Rust function to catch a JavaScript exception."
use wasm_bindgen::prelude::*;

#[wasm_bindgen]
extern {
    pub fn alert(s: &str);
    pub fn show(s: &str);
}

pub fn compile(code: &str) -> &str {
    code
}

#[wasm_bindgen]
pub fn compile_and_show(code: &str) {
    show(compile(code));
}
