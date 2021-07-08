use std::sync::mpsc::channel;
use std::thread;
use std::time::Duration;

fn main() {
    let (send, recv) = channel();

    // Spawn a thread that will own our Receiver.
    let handle = thread::spawn(move || {
        loop {
            match recv.recv() {
                Ok(x) => println!("received: {}", x),
                Err(_) => println!("nothing there."),
            }
        }
    });

    for x in 0..10 {
        send.send(x.to_string()).unwrap();
        // Wait for a bit so we can see our Receiver thread handle the stuff
        // we send to it.
        thread::sleep(Duration::from_millis(100));
    }

    // This is the same as if we didn't instantiate handle at all, and just
    // let Rust immediately drop `thread::spawn()`'s return value.
    drop(handle);

    // We could also do `handle.join()`, but that would also require our
    // recv loop to terminate. Right now it just loops forever until the main
    // thread terminates when we call `drop()`, which is actually what we'd
    // want in many cases of long-running apps responding to user input.
}
