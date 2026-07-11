function Features() {
  const features = [
    {
      title: "Real-Time Market",
      description: "Track live stock prices and market trends instantly.",
      icon: "📈",
    },
    {
      title: "Virtual Trading",
      description: "Practice buying and selling stocks without risking money.",
      icon: "💹",
    },
    {
      title: "AI Assistant",
      description: "Get intelligent insights and recommendations powered by AI.",
      icon: "🤖",
    },
  ];

  return (
    <section id="features" className="py-20 bg-white">
      <h2 className="text-4xl font-bold text-center text-slate-800">
        Features
      </h2>

      <div className="max-w-6xl mx-auto mt-14 grid md:grid-cols-3 gap-8 px-6">
        {features.map((feature) => (
          <div
            key={feature.title}
            className="bg-slate-100 rounded-xl shadow-md p-8 hover:shadow-xl transition"
          >
            <div className="text-5xl">{feature.icon}</div>

            <h3 className="text-2xl font-semibold mt-5">
              {feature.title}
            </h3>

            <p className="text-gray-600 mt-4">
              {feature.description}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Features;