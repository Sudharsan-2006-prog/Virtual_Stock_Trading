import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getWalletBalance,
  getPortfolio,
  getTransactions,
  getWatchlist,
  buyStock,
  sellStock,
  addToWatchlist,
  removeFromWatchlist
} from "../services/api";

function Dashboard() {
  const [userName, setUserName] = useState("Trader");
  const [userEmail, setUserEmail] = useState("");
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Modals state
  const [tradeModalOpen, setTradeModalOpen] = useState(false);
  const [tradeType, setTradeType] = useState<"BUY" | "SELL">("BUY");
  const [tradeSymbol, setTradeSymbol] = useState("");
  const [tradeCompanyName, setTradeCompanyName] = useState("");
  const [tradeQuantity, setTradeQuantity] = useState(1);
  const [tradePrice, setTradePrice] = useState(150.0); // Default or input

  const [watchlistModalOpen, setWatchlistModalOpen] = useState(false);
  const [watchSymbol, setWatchSymbol] = useState("");
  const [watchCompanyName, setWatchCompanyName] = useState("");

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

  // Queries
  const { data: walletData } = useQuery({
    queryKey: ["wallet"],
    queryFn: () => getWalletBalance().then((res) => res.data),
  });

  const { data: portfolio } = useQuery({
    queryKey: ["portfolio"],
    queryFn: () => getPortfolio().then((res) => res.data),
  });

  const { data: transactions } = useQuery({
    queryKey: ["transactions"],
    queryFn: () => getTransactions().then((res) => res.data),
  });

  const { data: watchlist } = useQuery({
    queryKey: ["watchlist"],
    queryFn: () => getWatchlist().then((res) => res.data),
  });

  // Mutations
  const buyMutation = useMutation({
    mutationFn: (data: any) => buyStock(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      setTradeModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data?.message || "Error buying stock")
  });

  const sellMutation = useMutation({
    mutationFn: (data: any) => sellStock(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      setTradeModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data?.message || "Error selling stock")
  });

  const addWatchlistMutation = useMutation({
    mutationFn: (data: any) => addToWatchlist(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
      setWatchlistModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data || "Error adding to watchlist")
  });

  const removeWatchlistMutation = useMutation({
    mutationFn: (id: number) => removeFromWatchlist(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
    }
  });

  const handleTrade = (e: React.FormEvent) => {
    e.preventDefault();
    if (tradeType === "BUY") {
      buyMutation.mutate({
        stockSymbol: tradeSymbol,
        companyName: tradeCompanyName,
        quantity: tradeQuantity,
        price: tradePrice
      });
    } else {
      sellMutation.mutate({
        stockSymbol: tradeSymbol,
        quantity: tradeQuantity,
        price: tradePrice
      });
    }
  };

  const handleAddWatchlist = (e: React.FormEvent) => {
    e.preventDefault();
    addWatchlistMutation.mutate({
      stockSymbol: watchSymbol,
      companyName: watchCompanyName
    });
  };

  const walletBalance = walletData?.balance || 0;
  const portfolioValue = portfolio?.reduce((acc: number, item: any) => acc + item.marketValue, 0) || 0;
  const totalProfitLoss = portfolio?.reduce((acc: number, item: any) => acc + item.profitLoss, 0) || 0;
  const activePositions = portfolio?.length || 0;

  return (
    <div className="min-h-screen bg-slate-950 text-white flex relative">
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
              onClick={() => { setTradeType("BUY"); setTradeModalOpen(true); }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 transition"
            >
              Trade
            </button>
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
              <p className="text-3xl font-extrabold text-blue-400 mt-4">₹{walletBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
            </div>

            {/* Portfolio Value */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Portfolio Holdings Value</h3>
              <p className="text-3xl font-extrabold text-slate-200 mt-4">₹{portfolioValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
              <p className="text-xs text-slate-500 mt-2">{activePositions} active positions</p>
            </div>

            {/* Total Profit/Loss */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
              <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Total Profit / Loss</h3>
              <p className={`text-3xl font-extrabold mt-4 ${totalProfitLoss >= 0 ? "text-green-400" : "text-red-400"}`}>
                ₹{totalProfitLoss.toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Portfolio Section */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-3">
              <h3 className="text-lg font-bold text-white mb-4">Your Portfolio</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-400">
                  <thead className="text-xs uppercase bg-slate-800 text-slate-300">
                    <tr>
                      <th className="px-4 py-3">Symbol</th>
                      <th className="px-4 py-3">Qty</th>
                      <th className="px-4 py-3">Avg Buy</th>
                      <th className="px-4 py-3">Current</th>
                      <th className="px-4 py-3">Market Val</th>
                      <th className="px-4 py-3">P/L</th>
                    </tr>
                  </thead>
                  <tbody>
                    {portfolio?.length > 0 ? portfolio.map((item: any) => (
                      <tr key={item.id} className="border-b border-slate-800">
                        <td className="px-4 py-3 font-semibold text-white">{item.stockSymbol}</td>
                        <td className="px-4 py-3">{item.quantity}</td>
                        <td className="px-4 py-3">₹{item.averageBuyPrice.toFixed(2)}</td>
                        <td className="px-4 py-3">₹{item.currentPrice.toFixed(2)}</td>
                        <td className="px-4 py-3">₹{item.marketValue.toFixed(2)}</td>
                        <td className={`px-4 py-3 font-semibold ${item.profitLoss >= 0 ? "text-green-400" : "text-red-400"}`}>
                          ₹{item.profitLoss.toFixed(2)}
                        </td>
                      </tr>
                    )) : (
                      <tr>
                        <td colSpan={6} className="text-center py-4 text-slate-500">No stocks in portfolio</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Bottom Columns Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Watchlist Section */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-1">
              <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-bold text-white">Watchlist</h3>
                <button onClick={() => setWatchlistModalOpen(true)} className="text-xs text-blue-400 hover:text-blue-300">+ Add</button>
              </div>
              <div className="space-y-4">
                {watchlist?.length > 0 ? watchlist.map((stock: any) => (
                  <div key={stock.id} className="flex justify-between items-center p-3 rounded-xl bg-slate-950/40 border border-slate-850">
                    <div>
                      <h4 className="font-bold text-sm">{stock.stockSymbol}</h4>
                      <p className="text-xs text-slate-500">{stock.companyName}</p>
                    </div>
                    <button 
                      onClick={() => removeWatchlistMutation.mutate(stock.id)}
                      className="text-slate-500 hover:text-red-400"
                    >
                      ×
                    </button>
                  </div>
                )) : (
                  <p className="text-sm text-slate-500 text-center py-2">Watchlist is empty</p>
                )}
              </div>
            </div>

            {/* Recent Transactions */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-2 flex flex-col min-h-[300px]">
              <h3 className="text-lg font-bold text-white mb-4">Recent Transactions</h3>
              {transactions?.length > 0 ? (
                <div className="overflow-x-auto flex-1">
                  <table className="w-full text-left text-sm text-slate-400">
                    <thead className="text-xs uppercase bg-slate-800 text-slate-300">
                      <tr>
                        <th className="px-4 py-3">Type</th>
                        <th className="px-4 py-3">Symbol</th>
                        <th className="px-4 py-3">Qty</th>
                        <th className="px-4 py-3">Price</th>
                        <th className="px-4 py-3">Total</th>
                        <th className="px-4 py-3">Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.slice(0, 10).map((t: any) => (
                        <tr key={t.id} className="border-b border-slate-800">
                          <td className={`px-4 py-3 font-bold ${t.transactionType === "BUY" ? "text-green-400" : "text-red-400"}`}>
                            {t.transactionType}
                          </td>
                          <td className="px-4 py-3 font-semibold text-white">{t.stockSymbol}</td>
                          <td className="px-4 py-3">{t.quantity}</td>
                          <td className="px-4 py-3">₹{t.price.toFixed(2)}</td>
                          <td className="px-4 py-3">₹{t.totalAmount.toFixed(2)}</td>
                          <td className="px-4 py-3 text-xs">{new Date(t.timestamp).toLocaleDateString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
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
              )}
            </div>
          </div>
        </main>
      </div>

      {/* Trade Modal */}
      {tradeModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-md w-full shadow-2xl relative">
            <button 
              onClick={() => setTradeModalOpen(false)}
              className="absolute top-4 right-4 text-slate-500 hover:text-white"
            >
              ×
            </button>
            <h3 className="text-xl font-bold mb-4">Trade Stock</h3>
            <div className="flex gap-2 mb-4">
              <button 
                className={`flex-1 py-2 rounded-lg font-semibold ${tradeType === "BUY" ? "bg-green-600 text-white" : "bg-slate-800 text-slate-400"}`}
                onClick={() => setTradeType("BUY")}
              >BUY</button>
              <button 
                className={`flex-1 py-2 rounded-lg font-semibold ${tradeType === "SELL" ? "bg-red-600 text-white" : "bg-slate-800 text-slate-400"}`}
                onClick={() => setTradeType("SELL")}
              >SELL</button>
            </div>
            <form onSubmit={handleTrade} className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">Symbol</label>
                <input required value={tradeSymbol} onChange={e => setTradeSymbol(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. AAPL" />
              </div>
              {tradeType === "BUY" && (
                <div>
                  <label className="block text-sm text-slate-400 mb-1">Company Name</label>
                  <input required value={tradeCompanyName} onChange={e => setTradeCompanyName(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. Apple Inc." />
                </div>
              )}
              <div>
                <label className="block text-sm text-slate-400 mb-1">Quantity</label>
                <input required type="number" min="1" value={tradeQuantity} onChange={e => setTradeQuantity(Number(e.target.value))} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">Mock Price</label>
                <input required type="number" step="0.01" min="0.01" value={tradePrice} onChange={e => setTradePrice(Number(e.target.value))} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" />
              </div>
              <button type="submit" className={`w-full py-3 rounded-xl font-bold text-white mt-2 ${tradeType === "BUY" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}`}>
                Confirm {tradeType}
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Watchlist Modal */}
      {watchlistModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-sm w-full shadow-2xl relative">
            <button 
              onClick={() => setWatchlistModalOpen(false)}
              className="absolute top-4 right-4 text-slate-500 hover:text-white"
            >
              ×
            </button>
            <h3 className="text-xl font-bold mb-4">Add to Watchlist</h3>
            <form onSubmit={handleAddWatchlist} className="space-y-4">
              <div>
                <label className="block text-sm text-slate-400 mb-1">Symbol</label>
                <input required value={watchSymbol} onChange={e => setWatchSymbol(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. AAPL" />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">Company Name</label>
                <input required value={watchCompanyName} onChange={e => setWatchCompanyName(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. Apple Inc." />
              </div>
              <button type="submit" className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 font-bold text-white mt-2">
                Add Stock
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;