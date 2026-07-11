function Hero() {
  return (
    <section className="min-h-[60vh] flex flex-col justify-center items-center bg-slate-100">

      <h1 className="text-6xl font-bold text-blue-600">
        Virtual Stock Trading Platform
      </h1>

      <p className="mt-6 text-xl text-gray-600">
        Learn • Invest • Grow
      </p>

      <div className="mt-10 flex gap-5">

        <button className="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700">
          Get Started
        </button>

        <button className="border border-blue-600 text-blue-600 px-8 py-3 rounded-lg hover:bg-blue-50">
          Learn More
        </button>

      </div>

    </section>
  );
}

export default Hero;