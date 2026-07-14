import { Link } from "react-router-dom";

function Hero() {
  const token = localStorage.getItem("token");

  return (
    <section className="relative min-h-[85vh] flex flex-col justify-center items-center px-6 overflow-hidden bg-slate-950">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,_var(--tw-gradient-stops))] from-blue-900/20 via-slate-950 to-slate-950" />
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-indigo-500/10 rounded-full blur-[120px]" />

      <div className="relative z-10 text-center max-w-4xl mx-auto flex flex-col items-center">
        {/* Animated Badge */}
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-blue-500/30 bg-blue-500/10 text-blue-400 text-xs font-semibold uppercase tracking-wider mb-6 animate-pulse">
          🚀 Zero-Risk Investing Simulator
        </div>

        <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight text-white leading-tight">
          Master the Market Without the{" "}
          <span className="bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-400 bg-clip-text text-transparent">
            Financial Risk
          </span>
        </h1>

        <p className="mt-6 text-lg md:text-xl text-slate-400 max-w-2xl font-light leading-relaxed">
          Practice stock trading in real-time. Learn strategies, track market movements, and build your confidence using our state-of-the-art simulator.
        </p>

        <div className="mt-10 flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
          <Link
            to={token ? "/dashboard" : "/register"}
            className="px-8 py-4 rounded-xl font-bold bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white shadow-lg shadow-blue-500/25 transition-all text-center"
          >
            Start Trading Now
          </Link>
          <a
            href="#features"
            className="px-8 py-4 rounded-xl font-bold border border-slate-800 bg-slate-900/50 text-slate-300 hover:bg-slate-800/80 hover:text-white transition-all text-center"
          >
            Explore Features
          </a>
        </div>
      </div>
    </section>
  );
}

export default Hero;