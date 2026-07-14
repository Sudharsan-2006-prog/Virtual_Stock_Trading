function HowItWorks() {
  const steps = [
    {
      number: "01",
      title: "Create an Account",
      description: "Sign up in seconds to secure your personal dashboard and receive an immediate virtual trading balance of ₹100,000.",
    },
    {
      number: "02",
      title: "Monitor the Dashboard",
      description: "Analyze dynamic stock metrics, watch fluctuations, and search for potential target companies on the watchlist.",
    },
    {
      number: "03",
      title: "Place Simulated Trades",
      description: "Buy or sell shares using simulated equity, then track how your decisions impact your portfolio value in real-time.",
    },
  ];

  return (
    <section id="how-it-works" className="py-24 bg-slate-950 border-t border-slate-900">
      <div className="max-w-6xl mx-auto px-6">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-5xl font-extrabold text-white">
            How It Works
          </h2>
          <p className="mt-4 text-slate-400 font-light">
            An intuitive path to acquiring market experience. Walk through our simple three-step process.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {steps.map((step) => (
            <div key={step.number} className="relative bg-slate-900 border border-slate-800/80 rounded-2xl p-8 hover:border-slate-700 transition-all duration-300">
              <div className="absolute top-6 right-8 text-5xl font-black text-slate-800 tracking-tighter select-none">
                {step.number}
              </div>

              <h3 className="text-xl font-bold text-white mb-4 pr-12">
                {step.title}
              </h3>

              <p className="text-slate-400 font-light leading-relaxed text-sm">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default HowItWorks;
