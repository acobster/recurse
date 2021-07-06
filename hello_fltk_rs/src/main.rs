extern crate anyhow;

use cpal::{Sample, StreamConfig};
use cpal::traits::{DeviceTrait, HostTrait, StreamTrait};
//use fltk::{app, prelude::*, window::Window};
use ringbuf::RingBuffer;

fn main() -> anyhow::Result<()> {
    // Set up an audio Device.
    let host = cpal::default_host();

    let input = host.default_input_device()
        .expect("no input device available.");
    let output = host.default_output_device()
        .expect("no output device available.");
    println!("Input device: {}", input.name()?);
    println!("Output device: {}", output.name()?);

    let config: StreamConfig = output.default_input_config()?.into();
    println!("Output config:  {:?}", config);

    let default_latency = 300.0;

    // configure latency between input/output
    let latency_frames = (default_latency / 1000.0) * config.sample_rate.0 as f32;
    let latency_samples = latency_frames as usize * config.channels as usize;

    let ring = RingBuffer::new(latency_samples * 2);
    let (mut producer, mut consumer) = ring.split();

    for _ in 0..latency_samples {
        // "this should never fail"
        producer.push(0.0).unwrap();
    }

    let input_data_fn = move |data: &[f32], _: &cpal::InputCallbackInfo| {
        let mut output_fell_behind = false;
        for &sample in data {
            if producer.push(sample).is_err() {
                output_fell_behind = true;
            }
        }
        if output_fell_behind {
            eprintln!("output stream fell behind: try increasing latency");
        }
    };

    let output_data_fn = move |data: &mut [f32], _: &cpal::OutputCallbackInfo| {
        let mut input_fell_behind = false;
        for sample in data {
            *sample = match consumer.pop() {
                Some(s) => s,
                None => {
                    input_fell_behind = true;
                    0.0
                }
            }
        }
        if input_fell_behind {
            eprintln!("input stream fell behind: try increasing latency");
        }
    };

    let input_stream = input.build_input_stream(&config, input_data_fn, err_fn)?;
    let output_stream = output.build_output_stream(&config, output_data_fn, err_fn)?;
    println!("Successfully built streams.");

    println!("Playing with latency={}ms", default_latency);
    input_stream.play()?;
    output_stream.play()?;

    std::thread::sleep(std::time::Duration::from_secs(3));
    drop(input_stream);
    drop(output_stream);
    println!("Done!");

    //let app = app::App::default();
    //let mut window = Window::new(100, 100, 400, 300, "Hello from Rust! 🦀");
    //window.end();
    //window.show();
    //app.run().unwrap();

    //match config.sample_format() {
    //    SampleFormat::F32 => run::<f32>(&output, &config.into()),
    //    SampleFormat::I16 => run::<i16>(&output, &config.into()),
    //    SampleFormat::U16 => run::<u16>(&output, &config.into()),
    //}

    Ok(())
}

fn err_fn(err: cpal::StreamError) {
    eprintln!("an error occurred on stream: {}", err);
}

pub fn run<T>(output: &cpal::Device, config: &cpal::StreamConfig) -> Result<(), anyhow::Error>
where T: Sample, {
    let sample_rate = config.sample_rate.0 as f32;
    let channels = config.channels as usize;

    // Basic sinusoid at A 440
    let mut sample_clock = 0f32;
    // https://pages.mtu.edu/~suits/notefreqs.html
    let mut next_value = move || {
        sample_clock = (sample_clock + 1.0) % sample_rate;
        (sample_clock * 440.0 * std::f32::consts::PI / sample_rate).sin()
    };

    let err_fn = |err| eprintln!("ERROR: {}", err);

    let stream = output.build_output_stream(
        config,
        move |data: &mut [T], _: &cpal::OutputCallbackInfo| {
            write_data(data, channels, &mut next_value)
        },
        err_fn,
    );

    match stream {
        Err(_) => panic!("OH NO"),
        Ok(_) => println!("ok"),
    }

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
