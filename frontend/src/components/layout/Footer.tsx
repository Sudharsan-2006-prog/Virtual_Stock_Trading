function Footer() {
  return (
    <footer className="bg-slate-950 border-t border-slate-900 py-12 text-slate-500 text-sm">
      <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-6">
        <div className="flex items-center gap-2">
          <span className="text-xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
            VirtualTrade
          </span>
          <span>© {new Date().getFullYear()}. All rights reserved.</span>
        </div>

        <div className="flex gap-8">
          <a href="#features" className="hover:text-slate-300 transition">
            Features
          </a>
          <a href="#how-it-works" className="hover:text-slate-300 transition">
            How It Works
          </a>
          <a href="#" className="hover:text-slate-300 transition">
            Privacy Policy
          </a>
        </div>
      </div>
    </footer>
  );
}

export default Footer;
