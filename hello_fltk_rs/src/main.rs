extern crate anyhow;

use cpal::{Data, Sample, SampleFormat};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
use fltk::{app, prelude::*, window::Window};

fn main() -> anyhow::Result<()> {
    // Set up an audio Device.
    let host = cpal::default_host();
    let device = host.default_output_device()
        .expect("no output device available.");
    println!("Output device: {}", device.name()?);

    let config = device.default_output_config().unwrap();
    println!("Output config:  {:?}", config);

    let app = app::App::default();
    let mut window = Window::new(100, 100, 400, 300, "Hello from Rust! 🦀");
    window.end();
    window.show();
    app.run().unwrap();

    match config.sample_format() {
        SampleFormat::F32 => run::<f32>(&device, &config.into()),
        SampleFormat::I16 => run::<i16>(&device, &config.into()),
        SampleFormat::U16 => run::<u16>(&device, &config.into()),
    }
}

pub fn run<T>(device: &cpal::Device, config: &cpal::StreamConfig) -> Result<(), anyhow::Error>
where T: Sample, {
    let sample_rate = config.sample_rate.0 as f32;
    let channels = config.channels as usize;

    // Basic sinusoid at A 880
    let mut sample_clock = 0f32;
    let mut next_value = move || {
        sample_clock = (sample_clock + 1.0) % sample_rate;
        (sample_clock * 440.0 * 2.0 * std::f32::consts::PI / sample_rate).sin()
    };

    let err_fn = |err| eprintln!("ERROR: {}", err);

    let stream = device.build_output_stream(
        config,
        move |data: &mut [T], _: &cpal::OutputCallbackInfo| {
            write_data(data, channels, &mut next_value)
        },
        err_fn,
    );

    std::thread::sleep(std::time::Duration::from_millis(1000));

    Ok(())
}

fn write_data<T>(output: &mut [T], channels: usize, next_sample: &mut dyn FnMut() -> f32)
where T: Sample, {
    for frame in output.chunks_mut(channels) {
        let value: T = Sample::from::<f32>(&next_sample());
        for sample in frame.iter_mut() {
            *sample = value;
        }
    }
}
