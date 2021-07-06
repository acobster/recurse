use fltk::{app, prelude::*, window::Window};

fn main() {
    let app = app::App::default();
    let mut window = Window::new(100, 100, 400, 300, "Hello from Rust! 🦀");
    window.end();
    window.show();
    app.run().unwrap();
}
