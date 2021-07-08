use std::sync::mpsc::channel;
use std::thread;
use std::time::Duration;

fn main() {
    let (send, recv) = channel();

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
        thread::sleep(Duration::from_millis(100));
    }

    drop(handle);
}
