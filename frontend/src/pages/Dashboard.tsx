import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

function Dashboard() {
  const [userName, setUserName] = useState("Trader");
  const [userEmail, setUserEmail] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    const storedName = localStorage.getItem("fullName");
    const storedEmail = localStorage.getItem("email");
    if (storedName) setUserName(storedName);
    if (storedEmail) setUserEmail(storedEmail);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("fullName");
    navigate("/");
  };

  const mockWatchlist = [
    { symbol: "RELIANCE", name: "Reliance Industries", price: "₹2,450.50", change: "+1.25%", isPositive: true },
    { symbol: "TCS", name: "Tata Consultancy Services", price: "₹3,210.20", change: "-0.45%", isPositive: false },
    { symbol: "HDFCBANK", name: "HDFC Bank Ltd.", price: "₹1,650.00", change: "+0.80%", isPositive: true },
    { symbol: "INFY", name: "Infosys Ltd.", price: "₹1,420.45", change: "-1.10%", isPositive: false },
  ];

  return (
    <div className="min-h-screen bg-slate-950 text-white flex">
      {/* Sidebar - Desktop */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col shrink-0 hidden md:flex">
        {/* Brand logo */}
        <div className="p-6 border-b border-slate-800 flex items-center gap-2">
          <span className="text-2xl">📈</span>
          <span className="font-bold text-lg bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">
            VirtualTrade
          </span>
        </div>

        {/* Navigation links */}
        <nav className="flex-1 p-4 space-y-2">
          <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-xl bg-blue-600 text-white font-semibold">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            <span>Dashboard</span>
          </a>
          <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white transition">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Portfolio</span>
          </a>
          <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white transition">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <span>Watchlist</span>
          </a>
          <a href="#" className="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-400 hover:bg-slate-800 hover:text-white transition">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <span>Transactions</span>
          </a>
        </nav>

        {/* User profile footer */}
        <div className="p-4 border-t border-slate-800 flex items-center justify-between gap-2">
          <div className="min-w-0">
            <p className="text-sm font-semibold text-white truncate">{userName}</p>
            <p className="text-xs text-slate-500 truncate">{userEmail}</p>
          </div>
          <button
            onClick={handleLogout}
            className="p-2 text-slate-400 hover:text-red-400 rounded-lg hover:bg-slate-800 transition"
            title="Log Out"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        {/* Header */}
        <header className="bg-slate-900 border-b border-slate-800 px-6 py-4 flex items-center justify-between">
          <h2 className="text-xl font-bold md:hidden flex items-center gap-2">
            <span>📈</span>
            <span className="bg-gradient-to-r from-blue-400 to-indigo-500 bg-clip-text text-transparent">VirtualTrade</span>
          </h2>
          <div className="text-sm font-semibold text-slate-300 hidden md:block">
            Market Status: <span className="text-green-400 font-bold">● Open</span>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-400 hidden sm:inline">Welcome back, {userName}</span>
            <button
              onClick={handleLogout}
              className="md:hidden p-2 text-slate-300 hover:text-red-400 transition"
              title="Log Out"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </button>
          </div>
        </header>

        {/* Dashboard Content Container */}
        <main className="p-6 space-y-6 max-w-6xl w-full mx-auto">
          {/* Top Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Wallet Card */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl" />
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Available Balance</h3>
              <p className="text-3xl font-extrabold text-blue-400 mt-4">₹100,000.00</p>
              <div className="flex gap-3 mt-6">
                <button className="flex-1 py-2 rounded-xl bg-blue-600/10 border border-blue-500/20 text-blue-400 hover:bg-blue-600/20 transition text-sm font-semibold">
                  + Add Funds
                </button>
                <button className="flex-1 py-2 rounded-xl bg-slate-800 border border-slate-700 text-slate-300 hover:bg-slate-700 transition text-sm font-semibold">
                  Withdraw
                </button>
              </div>
            </div>

            {/* Portfolio Value */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Portfolio Holdings Value</h3>
              <p className="text-3xl font-extrabold text-slate-200 mt-4">₹0.00</p>
              <p className="text-xs text-slate-500 mt-2">0 active positions</p>
            </div>

            {/* Total Profit/Loss */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Total Profit / Loss</h3>
              <p className="text-3xl font-extrabold text-slate-200 mt-4">₹0.00</p>
              <span className="inline-flex items-center gap-1 text-xs text-slate-500 mt-2 bg-slate-850 px-2 py-0.5 rounded-full">
                0.00%
              </span>
            </div>
          </div>

          {/* Bottom Columns Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Watchlist Section */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-1">
              <h3 className="text-lg font-bold text-white mb-4">Watchlist</h3>
              <div className="space-y-4">
                {mockWatchlist.map((stock) => (
                  <div key={stock.symbol} className="flex justify-between items-center p-3 rounded-xl bg-slate-950/40 border border-slate-850">
                    <div>
                      <h4 className="font-bold text-sm">{stock.symbol}</h4>
                      <p className="text-xs text-slate-500">{stock.name}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-bold">{stock.price}</p>
                      <span className={`text-xs ${stock.isPositive ? "text-green-400" : "text-red-400"} font-semibold`}>
                        {stock.change}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Recent Transactions Placeholder */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-2 flex flex-col min-h-[300px]">
              <h3 className="text-lg font-bold text-white mb-4">Recent Transactions</h3>
              <div className="flex-1 flex flex-col justify-center items-center text-center p-8">
                <div className="w-16 h-16 rounded-full bg-slate-950/60 border border-slate-800 flex items-center justify-center text-slate-600 mb-4">
                  <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <h4 className="font-bold text-slate-400 text-sm mb-1">No Transactions Record</h4>
                <p className="text-xs text-slate-600 max-w-xs leading-relaxed">
                  You haven't bought or sold any stocks yet. Place a mock order to view your transaction logging.
                </p>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

export default Dashboard;