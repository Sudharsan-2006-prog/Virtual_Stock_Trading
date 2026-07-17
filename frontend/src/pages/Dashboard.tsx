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
  removeFromWatchlist,
  searchMarket,
  getMarketPrice
} from "../services/api";

const SkeletonCard = () => (
  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl animate-pulse space-y-4">
    <div className="h-4 bg-slate-800 rounded w-1/2" />
    <div className="h-8 bg-slate-800 rounded w-3/4" />
    <div className="h-3 bg-slate-800 rounded w-1/3" />
  </div>
);

const SkeletonTable = () => (
  <div className="space-y-3 animate-pulse p-4">
    <div className="h-8 bg-slate-800 rounded w-full" />
    <div className="h-6 bg-slate-850 rounded w-full" />
    <div className="h-6 bg-slate-800 rounded w-full" />
    <div className="h-6 bg-slate-850 rounded w-full" />
  </div>
);

const SkeletonWatchlist = () => (
  <div className="space-y-3 animate-pulse">
    <div className="h-14 bg-slate-850 rounded-xl w-full" />
    <div className="h-14 bg-slate-850 rounded-xl w-full" />
    <div className="h-14 bg-slate-850 rounded-xl w-full" />
  </div>
);

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
  const [tradePrice, setTradePrice] = useState(0.0);
  const [tradeDailyChange, setTradeDailyChange] = useState(0.0);
  const [tradeChangePercent, setTradeChangePercent] = useState(0.0);
  const [tradeExchange, setTradeExchange] = useState("");
  const [tradeCurrency, setTradeCurrency] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  const [watchlistModalOpen, setWatchlistModalOpen] = useState(false);
  const [watchSymbol, setWatchSymbol] = useState("");
  const [watchCompanyName, setWatchCompanyName] = useState("");

  const [searchResults, setSearchResults] = useState<{ symbol: string; name: string; exchange?: string; currency?: string; country?: string; }[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);

  useEffect(() => {
    const storedName = localStorage.getItem("fullName");
    const storedEmail = localStorage.getItem("email");
    if (storedName) setUserName(storedName);
    if (storedEmail) setUserEmail(storedEmail);
  }, []);

  useEffect(() => {
    if (searchQuery.trim().length >= 1) {
      setSearchLoading(true);
      const delayDebounceFn = setTimeout(() => {
        searchMarket(searchQuery)
          .then((res) => {
            setSearchResults(res.data);
            setSearchLoading(false);
          })
          .catch(() => {
            setSearchLoading(false);
          });
      }, 300); // 300ms debounce
      return () => clearTimeout(delayDebounceFn);
    } else {
      setSearchResults([]);
      setShowSuggestions(false);
    }
  }, [searchQuery]);

  useEffect(() => {
    if (!tradeModalOpen) {
      setSearchResults([]);
      setShowSuggestions(false);
      setSearchQuery("");
      setTradeSymbol("");
      setTradeCompanyName("");
      setTradeQuantity(1);
      setTradePrice(0.0);
      setTradeDailyChange(0.0);
      setTradeChangePercent(0.0);
      setTradeExchange("");
      setTradeCurrency("");
    }
  }, [tradeModalOpen]);

  const handleSelectSuggestion = async (symbol: string, name: string) => {
    const matched = searchResults.find(r => r.symbol === symbol);
    setTradeSymbol(symbol);
    setTradeCompanyName(name);
    setSearchQuery(`${name} (${symbol})`);
    setShowSuggestions(false);
    
    if (matched) {
      setTradeExchange(matched.exchange || "");
      setTradeCurrency(matched.currency || "");
    }
    
    // Fetch live price and detailed quote for the symbol
    try {
      const priceRes = await getMarketPrice(symbol);
      if (priceRes.data) {
        setTradePrice(priceRes.data.currentPrice ?? 0.0);
        setTradeDailyChange(priceRes.data.change ?? 0.0);
        setTradeChangePercent(priceRes.data.changePercent ?? 0.0);
        if (priceRes.data.exchange) setTradeExchange(priceRes.data.exchange);
        if (priceRes.data.currency) setTradeCurrency(priceRes.data.currency);
      }
    } catch (err) {
      console.error("Error fetching symbol price", err);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("email");
    localStorage.removeItem("fullName");
    navigate("/");
  };

  // Queries
  const { data: walletData, isLoading: walletLoading } = useQuery({
    queryKey: ["wallet"],
    queryFn: () => getWalletBalance().then((res) => res.data),
    refetchInterval: 30000,
  });

  const { data: portfolio, isLoading: portfolioLoading, isError: portfolioError } = useQuery({
    queryKey: ["portfolio"],
    queryFn: () => getPortfolio().then((res) => res.data),
    refetchInterval: 30000,
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ["transactions"],
    queryFn: () => getTransactions().then((res) => res.data),
    refetchInterval: 30000,
  });

  const { data: watchlist, isLoading: watchlistLoading, isError: watchlistError } = useQuery({
    queryKey: ["watchlist"],
    queryFn: () => getWatchlist().then((res) => res.data),
    refetchInterval: 30000,
  });

  const hasMarketError = portfolioError || watchlistError;

  // Mutations
  const buyMutation = useMutation({
    mutationFn: (data: any) => buyStock(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      setTradeModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data?.error || err.response?.data?.message || "Error buying stock")
  });

  const sellMutation = useMutation({
    mutationFn: (data: any) => sellStock(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wallet"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio"] });
      queryClient.invalidateQueries({ queryKey: ["transactions"] });
      setTradeModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data?.error || err.response?.data?.message || "Error selling stock")
  });

  const addWatchlistMutation = useMutation({
    mutationFn: (data: any) => addToWatchlist(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["watchlist"] });
      setWatchlistModalOpen(false);
    },
    onError: (err: any) => alert(err.response?.data?.error || err.response?.data?.message || "Error adding to watchlist")
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
  const portfolioValue = portfolio?.reduce((acc: number, item: any) => {
    const val = item.marketValue || 0;
    return acc + (item.currency === "USD" ? val * 83.0 : val);
  }, 0) || 0;
  
  const totalProfitLoss = portfolio?.reduce((acc: number, item: any) => {
    const val = item.profitLoss || 0;
    return acc + (item.currency === "USD" ? val * 83.0 : val);
  }, 0) || 0;
  
  const todayProfitLoss = portfolio?.reduce((acc: number, item: any) => {
    const val = item.todayProfitLoss || 0;
    return acc + (item.currency === "USD" ? val * 83.0 : val);
  }, 0) || 0;
  
  const activePositions = portfolio?.length || 0;
  const watchlistCount = watchlist?.length || 0;

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
          {/* Error Banner */}
          {hasMarketError && (
            <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl flex items-center gap-3">
              <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span className="font-semibold text-sm">Unable to fetch live market data. Using cached or fallback data.</span>
            </div>
          )}

          {/* Top Cards Grid */}
          {walletLoading || portfolioLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <SkeletonCard />
              <SkeletonCard />
              <SkeletonCard />
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* Wallet Card */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl" />
                <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Available Balance</h3>
                <p className="text-3xl font-extrabold text-blue-400 mt-4">₹{walletBalance.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
              </div>

              {/* Portfolio Value */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
                <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Portfolio Value (INR Equivalent)</h3>
                <p className="text-3xl font-extrabold text-slate-200 mt-4">₹{portfolioValue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
                <p className="text-xs text-slate-500 mt-2">{activePositions} active positions</p>
              </div>

              {/* Total Profit/Loss */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
                <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider">Profit / Loss (INR Equivalent)</h3>
                <div className="flex justify-between items-baseline mt-4">
                  <div>
                    <p className="text-xs text-slate-500">Total P/L</p>
                    <p className={`text-2xl font-extrabold ${totalProfitLoss >= 0 ? "text-green-400" : "text-red-400"}`}>
                      ₹{totalProfitLoss.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-slate-500">Today's P/L</p>
                    <p className={`text-lg font-bold ${todayProfitLoss >= 0 ? "text-green-400" : "text-red-400"}`}>
                      {todayProfitLoss >= 0 ? "+" : ""}₹{todayProfitLoss.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Portfolio Section */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-3">
              <h3 className="text-lg font-bold text-white mb-4">Your Portfolio</h3>
              {portfolioLoading ? (
                <SkeletonTable />
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm text-slate-400">
                    <thead className="text-xs uppercase bg-slate-800 text-slate-300">
                      <tr>
                        <th className="px-4 py-3">Symbol</th>
                        <th className="px-4 py-3">Company</th>
                        <th className="px-4 py-3">Exchange</th>
                        <th className="px-4 py-3">Quantity</th>
                        <th className="px-4 py-3">Average Buy Price</th>
                        <th className="px-4 py-3">Current Market Price</th>
                        <th className="px-4 py-3">Investment</th>
                        <th className="px-4 py-3">Current Value</th>
                        <th className="px-4 py-3">Profit/Loss</th>
                        <th className="px-4 py-3">Profit/Loss %</th>
                        <th className="px-4 py-3">Currency</th>
                      </tr>
                    </thead>
                    <tbody>
                      {portfolio?.length > 0 ? portfolio.map((item: any) => {
                        const plPercent = item.investedAmount > 0 ? (item.profitLoss / item.investedAmount) * 100 : 0;
                        const symbolPrefix = item.currency === "USD" ? "$" : "₹";
                        return (
                          <tr key={item.id} className="border-b border-slate-800 hover:bg-slate-850/30 transition">
                            <td className="px-4 py-3 font-semibold text-white">{item.stockSymbol}</td>
                            <td className="px-4 py-3 text-slate-300 max-w-[150px] truncate">{item.companyName}</td>
                            <td className="px-4 py-3 text-slate-400 font-semibold text-xs uppercase">{item.exchange || "NSE"}</td>
                            <td className="px-4 py-3">{item.quantity}</td>
                            <td className="px-4 py-3">{symbolPrefix}{item.averageBuyPrice.toFixed(2)}</td>
                            <td className="px-4 py-3 font-semibold text-white">{symbolPrefix}{item.currentPrice.toFixed(2)}</td>
                            <td className="px-4 py-3">{symbolPrefix}{item.investedAmount.toFixed(2)}</td>
                            <td className="px-4 py-3 font-semibold text-slate-200">{symbolPrefix}{item.marketValue.toFixed(2)}</td>
                            <td className={`px-4 py-3 font-bold ${item.profitLoss >= 0 ? "text-green-400" : "text-red-400"}`}>
                              {item.profitLoss >= 0 ? "+" : ""}{symbolPrefix}{item.profitLoss.toFixed(2)}
                            </td>
                            <td className={`px-4 py-3 font-bold ${plPercent >= 0 ? "text-green-400" : "text-red-400"}`}>
                              {plPercent >= 0 ? "↑" : "↓"} {Math.abs(plPercent).toFixed(2)}%
                            </td>
                            <td className="px-4 py-3 text-slate-400 text-xs font-semibold uppercase">{item.currency || "INR"}</td>
                          </tr>
                        );
                      }) : (
                        <tr>
                          <td colSpan={11} className="text-center py-6 text-slate-500 font-medium">No stocks in portfolio</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Bottom Columns Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Watchlist Section */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-1">
              <div className="flex justify-between items-center mb-4">
                <div className="flex items-center gap-2">
                  <h3 className="text-lg font-bold text-white">Watchlist</h3>
                  <span className="text-xs bg-slate-800 text-slate-400 px-2 py-0.5 rounded-full font-semibold">{watchlistCount} stocks</span>
                </div>
                <button 
                  disabled={watchlistLoading}
                  onClick={() => setWatchlistModalOpen(true)} 
                  className="text-xs text-blue-400 hover:text-blue-300 font-semibold disabled:opacity-50"
                >
                  + Add
                </button>
              </div>
              {watchlistLoading ? (
                <SkeletonWatchlist />
              ) : (
                <div className="space-y-4">
                  {watchlist?.length > 0 ? watchlist.map((stock: any) => {
                    const symbolPrefix = stock.currency === "USD" ? "$" : "₹";
                    return (
                      <div key={stock.id} className="flex justify-between items-center p-3 rounded-xl bg-slate-950/40 border border-slate-850 hover:bg-slate-900/50 transition">
                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5">
                            <h4 className="font-bold text-sm truncate">{stock.stockSymbol}</h4>
                            <span className="text-[10px] bg-slate-800 text-slate-400 px-1 py-0.5 rounded uppercase font-semibold">{stock.exchange || "NSE"}</span>
                          </div>
                          <p className="text-xs text-slate-500 truncate">{stock.companyName}</p>
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                          <div className="text-right">
                            <p className="text-sm font-bold text-slate-200">
                              {symbolPrefix}{(stock.currentPrice ?? 0).toFixed(2)}
                            </p>
                            <p className={`text-xs font-semibold ${(stock.dailyChange ?? 0) >= 0 ? "text-green-400" : "text-red-400"} flex items-center justify-end gap-0.5`}>
                              {(stock.dailyChange ?? 0) >= 0 ? "↑" : "↓"} {Math.abs(stock.changePercent ?? 0).toFixed(2)}%
                            </p>
                          </div>
                          <button 
                            disabled={removeWatchlistMutation.isPending}
                            onClick={() => removeWatchlistMutation.mutate(stock.id)}
                            className="text-slate-500 hover:text-red-400 text-lg font-bold disabled:opacity-50"
                            title="Remove from Watchlist"
                          >
                            ×
                          </button>
                        </div>
                      </div>
                    );
                  }) : (
                    <p className="text-sm text-slate-500 text-center py-4">Watchlist is empty</p>
                  )}
                </div>
              )}
            </div>

            {/* Recent Transactions */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl lg:col-span-2 flex flex-col min-h-[300px]">
              <h3 className="text-lg font-bold text-white mb-4">Recent Transactions</h3>
              {transactionsLoading ? (
                <SkeletonTable />
              ) : transactions?.length > 0 ? (
                <div className="overflow-x-auto flex-1">
                  <table className="w-full text-left text-sm text-slate-400">
                    <thead className="text-xs uppercase bg-slate-800 text-slate-300">
                      <tr>
                        <th className="px-4 py-3">Type</th>
                        <th className="px-4 py-3">Symbol</th>
                        <th className="px-4 py-3">Qty</th>
                        <th className="px-4 py-3">Price</th>
                        <th className="px-4 py-3">Total</th>
                        <th className="px-4 py-3">Currency</th>
                        <th className="px-4 py-3">Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      {transactions.slice(0, 10).map((t: any) => {
                        const symbolPrefix = t.currency === "USD" ? "$" : "₹";
                        return (
                          <tr key={t.id} className="border-b border-slate-800 hover:bg-slate-850/30 transition">
                            <td className={`px-4 py-3 font-bold ${t.transactionType === "BUY" ? "text-green-400" : "text-red-400"}`}>
                              {t.transactionType}
                            </td>
                            <td className="px-4 py-3 font-semibold text-white">{t.stockSymbol}</td>
                            <td className="px-4 py-3">{t.quantity}</td>
                            <td className="px-4 py-3">{symbolPrefix}{t.price.toFixed(2)}</td>
                            <td className="px-4 py-3">{symbolPrefix}{t.totalAmount.toFixed(2)}</td>
                            <td className="px-4 py-3 text-slate-400 text-xs font-semibold uppercase">{t.currency || "INR"}</td>
                            <td className="px-4 py-3 text-xs">{new Date(t.timestamp).toLocaleDateString()}</td>
                          </tr>
                        );
                      })}
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
                    You haven't bought or sold any stocks yet. Place a order to view your transaction logging.
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
              <div className="relative">
                <label className="block text-sm text-slate-400 mb-1">Search Stock (Symbol or Name)</label>
                <input 
                  required 
                  disabled={buyMutation.isPending || sellMutation.isPending}
                  value={searchQuery} 
                  onChange={e => {
                    setSearchQuery(e.target.value);
                    setShowSuggestions(true);
                  }} 
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white disabled:opacity-50" 
                  placeholder="Type stock symbol or name (e.g. Apple or Reliance)" 
                  autoComplete="off"
                />
                {showSuggestions && (searchResults.length > 0 || searchLoading) && (
                  <div className="absolute z-50 left-0 right-0 mt-1 bg-slate-950 border border-slate-800 rounded-lg max-h-48 overflow-y-auto shadow-2xl">
                    {searchLoading ? (
                      <div className="p-3 text-sm text-slate-500 text-center flex items-center justify-center gap-2">
                        <span className="animate-spin text-lg">⏳</span>
                        <span>Searching stocks...</span>
                      </div>
                    ) : (
                      searchResults.map((item) => (
                        <div 
                          key={item.symbol} 
                          onClick={() => handleSelectSuggestion(item.symbol, item.name)}
                          className="p-2.5 hover:bg-slate-800 cursor-pointer flex justify-between items-center text-sm border-b border-slate-900 last:border-0"
                        >
                          <div className="flex flex-col text-left">
                            <span className="font-bold text-blue-400">{item.symbol}</span>
                            <span className="text-slate-500 text-[10px] uppercase font-semibold">{item.exchange || "NSE"} • {item.currency || "INR"}</span>
                          </div>
                          <div className="text-right flex flex-col min-w-0">
                            <span className="font-semibold text-slate-200 truncate max-w-[180px]">{item.name}</span>
                            <span className="text-slate-500 text-[10px]">{item.country || "India"}</span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>

              {tradeSymbol && (
                <div className="p-4 rounded-xl bg-slate-950 border border-slate-850 space-y-2.5">
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Stock Name:</span>
                    <span className="font-bold text-white text-right truncate max-w-[180px]">{tradeCompanyName}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Ticker Symbol:</span>
                    <span className="font-bold text-blue-400">{tradeSymbol}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Exchange:</span>
                    <span className="font-bold text-slate-300">{tradeExchange}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Market Price:</span>
                    <span className="font-bold text-white">
                      {tradeCurrency === "USD" ? "$" : "₹"}{tradePrice.toFixed(2)}
                    </span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-400">Daily Change:</span>
                    <span className={`font-bold ${(tradeDailyChange ?? 0) >= 0 ? "text-green-400" : "text-red-400"}`}>
                      {(tradeDailyChange ?? 0) >= 0 ? "+" : ""}{(tradeDailyChange ?? 0).toFixed(2)} ({(tradeChangePercent ?? 0).toFixed(2)}%)
                    </span>
                  </div>
                </div>
              )}

              <div>
                <label className="block text-sm text-slate-400 mb-1">Quantity</label>
                <input 
                  required 
                  type="number" 
                  min="1" 
                  disabled={buyMutation.isPending || sellMutation.isPending}
                  value={tradeQuantity} 
                  onChange={e => setTradeQuantity(Number(e.target.value))} 
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white disabled:opacity-50" 
                />
              </div>

              {tradeSymbol && (
                <div className="flex justify-between items-center py-2.5 border-t border-slate-855">
                  <span className="text-sm text-slate-400">Estimated Total:</span>
                  <span className="text-lg font-extrabold text-white">
                    {tradeCurrency === "USD" ? "$" : "₹"}{(tradePrice * tradeQuantity).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </span>
                </div>
              )}
              <button 
                type="submit" 
                disabled={buyMutation.isPending || sellMutation.isPending}
                className={`w-full py-3 rounded-xl font-bold text-white mt-2 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${tradeType === "BUY" ? "bg-green-600 hover:bg-green-700" : "bg-red-600 hover:bg-red-700"}`}
              >
                {(buyMutation.isPending || sellMutation.isPending) && (
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                )}
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
                <input required value={watchSymbol} onChange={e => setWatchSymbol(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. AAPL or RELIANCE" />
              </div>
              <div>
                <label className="block text-sm text-slate-400 mb-1">Company Name</label>
                <input required value={watchCompanyName} onChange={e => setWatchCompanyName(e.target.value)} className="w-full bg-slate-950 border border-slate-800 rounded-lg p-2.5 text-white" placeholder="e.g. Apple Inc. or Reliance Industries" />
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