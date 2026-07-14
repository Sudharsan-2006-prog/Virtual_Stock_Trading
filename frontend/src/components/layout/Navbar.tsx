import { Link, useNavigate } from "react-router-dom";

function Navbar() {
  const token = localStorage.getItem("token");
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("fullName");
    navigate("/");
  };

  return (
    <nav className="bg-slate-900/80 backdrop-blur-md border-b border-slate-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto flex justify-between items-center px-6 py-4">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 text-xl font-bold bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent hover:opacity-90 transition">
          <span className="text-2xl">📈</span>
          <span>VirtualTrade</span>
        </Link>

        {/* Navigation */}
        <div className="hidden md:flex items-center gap-8 text-slate-300">
          <Link to="/" className="hover:text-blue-400 transition font-medium">
            Home
          </Link>
          <a href="/#features" className="hover:text-blue-400 transition font-medium">
            Features
          </a>
          <a href="/#how-it-works" className="hover:text-blue-400 transition font-medium">
            How It Works
          </a>
        </div>

        {/* Right Side / Auth Actions */}
        <div className="flex items-center gap-4">
          {token ? (
            <>
              <Link
                to="/dashboard"
                className="px-4 py-2 text-sm font-semibold rounded-lg bg-blue-600 text-white hover:bg-blue-500 transition shadow-lg shadow-blue-600/20"
              >
                Dashboard
              </Link>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-semibold rounded-lg border border-slate-700 text-slate-300 hover:bg-slate-800 transition"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="px-4 py-2 text-sm font-semibold text-slate-300 hover:text-white transition"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 text-sm font-semibold rounded-lg bg-blue-600 text-white hover:bg-blue-500 transition shadow-lg shadow-blue-600/20"
              >
                Get Started
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;