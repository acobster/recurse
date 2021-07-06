fn main() {
    // https://docs.rs/soundio/0.2.1/soundio/index.html
    let mut context = soundio::Context::new();
    context.set_app_name("fclooper");
    //match context.connect() {
    //    Ok(_) => println!("we're good."),
    //    Err(e) => panic!("something bad happened: {:?}", e),
    //};

    // this also doesn't work:
    context.connect().expect("something bad happened");

    // I think the problem is this bug:
    // https://github.com/RamiHg/soundio-rs/issues/2
}
