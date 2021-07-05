// `vst` uses macros, so we'll need to specify that we're using them!
#[macro_use]
extern crate vst;
extern crate rand;

use vst::buffer::AudioBuffer;
use vst::plugin::{Plugin, Info, Category};
use rand::random;

#[derive(Default)]
struct Whisper;

// Implement the Plugin trait that does a bunch of VST stuff.
impl Plugin for Whisper {
    fn get_info(&self) -> Info {
        Info {
            // turns a String into a &str slice reference???
            // can also do String::from("Whisper")
            name: "Whisper".to_string(),
            vendor: "soundofmetal".to_string(),
            unique_id: 20210705,

            inputs: 0,
            outputs: 2, // this is the default

            category: Category::Synth,

            // fill in the rest with default values
            ..Default::default()
        }
    }

    // Generate white noise and write it to the output buffer.
    fn process(&mut self, buffer: &mut AudioBuffer<f32>) {
        // buffer.split() gives us a tuple of in/out buffers.
        // We only care about output, hence the _
        let (_, mut output_buffer) = buffer.split();

        // Loop over output channels (left & right).
        for output_channel in output_buffer.into_iter() {
            for output_sample in output_channel {
                // For every  sample, add a random value from -1.0 to 1.0
                *output_sample = (random::<f32>() - 0.5) * 2.0;
            }
        }
    }
}

plugin_main!(Whisper);