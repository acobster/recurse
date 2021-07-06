extern crate anyhow;

use cpal::{Data, Sample};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use fltk::{app, prelude::*, window::Window};

fn main() -> anyhow::Result<()> {
    // Set up an audio Device.
    let host = cpal::default_host();
    let device = host.default_output_device()
        .expect("no output device available.");
    println!("Default output device chosen: {}", device.name()?);

    //let config = device.default_output_config

    let app = app::App::default();
    let mut window = Window::new(100, 100, 400, 300, "Hello from Rust! 🦀");
    window.end();
    window.show();
    app.run().unwrap();

    Ok(())
}
