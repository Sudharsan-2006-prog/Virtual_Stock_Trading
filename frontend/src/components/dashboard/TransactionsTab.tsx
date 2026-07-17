import { useState, useMemo } from "react";
import { Search, Download, Filter, DollarSign, Activity, FileText } from "lucide-react";
import { exportToCSV } from "../../utils/CSVExporter";

interface TransactionsTabProps {
  transactions: any[];
  analyticsData: any;
  onSelectStock: (symbol: string) => void;
}

export default function TransactionsTab({ transactions, analyticsData, onSelectStock }: TransactionsTabProps) {
  const { transactionAnalytics } = analyticsData;

  // Filters State
  const [searchQuery, setSearchQuery] = useState("");
  const [exchangeFilter, setExchangeFilter] = useState("ALL");
  const [typeFilter, setTypeFilter] = useState("ALL");
  const [outcomeFilter, setOutcomeFilter] = useState("ALL"); // ALL, PROFIT, LOSS
  const [timeFilter, setTimeFilter] = useState("ALL"); // ALL, 7D, 30D, 90D

  // Clear filters
  const handleResetFilters = () => {
    setSearchQuery("");
    setExchangeFilter("ALL");
    setTypeFilter("ALL");
    setOutcomeFilter("ALL");
    setTimeFilter("ALL");
  };

  // Filter logic
  const filteredTransactions = useMemo(() => {
    if (!transactions) return [];

    return transactions.filter((t) => {
      // 1. Search Query (Symbol or Company)
      const matchesSearch =
        t.stockSymbol.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.companyName.toLowerCase().includes(searchQuery.toLowerCase());

      // 2. Exchange Filter
      const resolvedExchange = t.exchange || "NSE";
      const matchesExchange =
        exchangeFilter === "ALL" || resolvedExchange.toUpperCase() === exchangeFilter.toUpperCase();

      // 3. Type Filter
      const matchesType = typeFilter === "ALL" || t.transactionType === typeFilter;

      // 4. Outcome Filter
      // Since transactions doesn't directly flag profit, we can check how they relate:
      // (This filter is applied mainly to SELL transactions since only sales generate direct realized profit/loss.
      // Or if the outcomeFilter is set, we check if the transaction type is SELL and if it falls in the calculated bounds)
      // Actually, we can approximate: if outcomeFilter is ALL, match all.
      // If PROFIT, we only show SELL trades that had positive delta.
      // Let's check: in our simplified filter, since we don't have transaction-level P/L in DB, we can calculate it on-the-fly
      // for SELL trades against average cost.
      // Let's do a simple chronological costing in JS just like the backend to find which SELLs are profitable!
      // This is a top-tier premium logic extension.
      let matchesOutcome = true;
      if (outcomeFilter !== "ALL") {
        if (t.transactionType !== "SELL") {
          matchesOutcome = false;
        } else {
          // Reconstruct holding cost for this sell to see if profitable
          const isProfitable = calculateSellIsProfitable(t, transactions);
          matchesOutcome = outcomeFilter === "PROFIT" ? isProfitable : !isProfitable;
        }
      }

      // 5. Time Filter
      let matchesTime = true;
      if (timeFilter !== "ALL") {
        const txDate = new Date(t.timestamp);
        const now = new Date();
        const diffTime = Math.abs(now.getTime() - txDate.getTime());
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        if (timeFilter === "7D" && diffDays > 7) matchesTime = false;
        if (timeFilter === "30D" && diffDays > 30) matchesTime = false;
        if (timeFilter === "90D" && diffDays > 90) matchesTime = false;
      }

      return matchesSearch && matchesExchange && matchesType && matchesOutcome && matchesTime;
    });
  }, [transactions, searchQuery, exchangeFilter, typeFilter, outcomeFilter, timeFilter]);

  // Helper to determine if a SELL transaction was profitable by scanning preceding BUYs
  function calculateSellIsProfitable(sellTx: any, allTxs: any[]): boolean {
    // Get preceding transactions chronologically (ascending)
    const ascTxs = [...allTxs].reverse();
    let qtyAccumulated = 0;
    let avgCost = 0;

    for (const tx of ascTxs) {
      if (tx.stockSymbol.toUpperCase() !== sellTx.stockSymbol.toUpperCase()) continue;
      
      const txPrice = tx.currency === "USD" ? tx.price * 83.0 : tx.price;
      
      if (tx.transactionType === "BUY") {
        const totalCost = (avgCost * qtyAccumulated) + (txPrice * tx.quantity);
        qtyAccumulated += tx.quantity;
        avgCost = qtyAccumulated > 0 ? totalCost / qtyAccumulated : 0;
      } else {
        // If this is the sell transaction we're evaluating
        if (tx.id === sellTx.id) {
          const sellPrice = tx.currency === "USD" ? tx.price * 83.0 : tx.price;
          return sellPrice > avgCost;
        }
        qtyAccumulated = Math.max(0, qtyAccumulated - tx.quantity);
        if (qtyAccumulated === 0) avgCost = 0;
      }
    }
    return false;
  }

  const handleExportCSV = () => {
    const headers = [
      "ID",
      "Transaction Type",
      "Stock Symbol",
      "Company Name",
      "Quantity",
      "Execution Price",
      "Total Amount",
      "Exchange",
      "Currency",
      "Timestamp",
    ];

    const rows = filteredTransactions.map((t) => [
      t.id,
      t.transactionType,
      t.stockSymbol,
      t.companyName,
      t.quantity,
      t.price.toFixed(2),
      t.totalAmount.toFixed(2),
      t.exchange || "NSE",
      t.currency || "INR",
      new Date(t.timestamp).toLocaleString(),
    ]);

    exportToCSV("virtual_trade_filtered_transactions.csv", headers, rows);
  };

  return (
    <div className="space-y-6">
      {/* Header Panel */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-slate-800 pb-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight">Trading History & Logs</h2>
          <p className="text-sm text-slate-400">Filter, inspect, and export your trade transaction ledger sheets.</p>
        </div>
        <button
          onClick={handleExportCSV}
          disabled={filteredTransactions.length === 0}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl text-sm font-semibold transition cursor-pointer"
        >
          <Download className="w-4 h-4" />
          <span>Export CSV</span>
        </button>
      </div>

      {/* Trading Stats Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-4 flex items-center gap-3.5 shadow-md">
          <div className="p-2.5 rounded-xl bg-slate-950 text-blue-400 border border-slate-800">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Total Trades</p>
            <h4 className="text-base font-black text-white mt-0.5">{transactionAnalytics?.numTrades || 0}</h4>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-4 flex items-center gap-3.5 shadow-md">
          <div className="p-2.5 rounded-xl bg-slate-950 text-emerald-400 border border-slate-800">
            <DollarSign className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Total Buys</p>
            <h4 className="text-sm font-black text-white mt-0.5">₹{transactionAnalytics?.totalBuys?.toLocaleString() || 0}</h4>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-4 flex items-center gap-3.5 shadow-md">
          <div className="p-2.5 rounded-xl bg-slate-950 text-indigo-400 border border-slate-800">
            <FileText className="w-5 h-5" />
          </div>
          <div>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Avg Hold Period</p>
            <h4 className="text-sm font-black text-white mt-0.5">
              {transactionAnalytics?.averageHoldingPeriodDays?.toFixed(1) || 0} days
            </h4>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-4 flex items-center gap-3.5 shadow-md">
          <div className="p-2.5 rounded-xl bg-slate-950 text-emerald-400 border border-slate-800">
            <span className="text-sm font-extrabold">+</span>
          </div>
          <div>
            <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider">Largest Gain</p>
            <h4 className="text-sm font-black text-emerald-400 mt-0.5">₹{transactionAnalytics?.largestGain?.toLocaleString() || 0}</h4>
          </div>
        </div>
      </div>

      {/* Advanced Filters Card */}
      <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-5 shadow-lg space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/60 pb-3">
          <div className="flex items-center gap-2 text-sm font-bold text-slate-200">
            <Filter className="w-4 h-4 text-blue-400" />
            <span>Search & Advanced Filters</span>
          </div>
          <button
            onClick={handleResetFilters}
            className="text-xs text-blue-400 hover:text-blue-300 font-semibold cursor-pointer"
          >
            Clear Filters
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5">
          {/* Search Box */}
          <div className="relative lg:col-span-1">
            <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-500">
              <Search className="w-4 h-4" />
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950 border border-slate-850 rounded-xl pl-9 pr-3 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
              placeholder="Search ticker or name..."
            />
          </div>

          {/* Exchange Filter */}
          <div>
            <select
              value={exchangeFilter}
              onChange={(e) => setExchangeFilter(e.target.value)}
              className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-sm text-slate-300 focus:outline-none focus:border-blue-500"
            >
              <option value="ALL">All Exchanges</option>
              <option value="NSE">NSE (India)</option>
              <option value="NASDAQ">NASDAQ (US)</option>
            </select>
          </div>

          {/* Type Filter */}
          <div>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-sm text-slate-300 focus:outline-none focus:border-blue-500"
            >
              <option value="ALL">All Types</option>
              <option value="BUY">BUY Orders</option>
              <option value="SELL">SELL Orders</option>
            </select>
          </div>

          {/* Profit/Loss Filter */}
          <div>
            <select
              value={outcomeFilter}
              onChange={(e) => setOutcomeFilter(e.target.value)}
              className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-sm text-slate-300 focus:outline-none focus:border-blue-500"
            >
              <option value="ALL">All Outcomes</option>
              <option value="PROFIT">Profitable Sales</option>
              <option value="LOSS">Loss-making Sales</option>
            </select>
          </div>

          {/* Time Filter */}
          <div>
            <select
              value={timeFilter}
              onChange={(e) => setTimeFilter(e.target.value)}
              className="w-full bg-slate-950 border border-slate-850 rounded-xl px-3 py-2 text-sm text-slate-300 focus:outline-none focus:border-blue-500"
            >
              <option value="ALL">All Time</option>
              <option value="7D">Last 7 Days</option>
              <option value="30D">Last 30 Days</option>
              <option value="90D">Last 90 Days</option>
            </select>
          </div>
        </div>
      </div>

      {/* Transaction Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-400">
            <thead className="text-xs uppercase bg-slate-800 text-slate-300">
              <tr>
                <th className="px-5 py-3">Type</th>
                <th className="px-5 py-3">Symbol</th>
                <th className="px-5 py-3">Company Name</th>
                <th className="px-5 py-3">Qty</th>
                <th className="px-5 py-3">Price</th>
                <th className="px-5 py-3">Total Amount</th>
                <th className="px-5 py-3">Exchange</th>
                <th className="px-5 py-3">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredTransactions.length > 0 ? (
                filteredTransactions.map((t) => {
                  const symbolPrefix = t.currency === "USD" ? "$" : "₹";
                  return (
                    <tr key={t.id} className="hover:bg-slate-850/30 transition">
                      <td className="px-5 py-4">
                        <span
                          className={`inline-block px-2 py-0.5 text-xs font-bold rounded-full ${
                            t.transactionType === "BUY"
                              ? "text-emerald-400 bg-emerald-500/10"
                              : "text-rose-400 bg-rose-500/10"
                          }`}
                        >
                          {t.transactionType}
                        </span>
                      </td>
                      <td className="px-5 py-4">
                        <button
                          onClick={() => onSelectStock(t.stockSymbol)}
                          className="font-bold text-white hover:text-blue-400 transition cursor-pointer text-left focus:outline-none"
                        >
                          {t.stockSymbol}
                        </button>
                      </td>
                      <td className="px-5 py-4 text-slate-300 max-w-[200px] truncate">{t.companyName}</td>
                      <td className="px-5 py-4 font-medium text-slate-200">{t.quantity}</td>
                      <td className="px-5 py-4">
                        {symbolPrefix}
                        {t.price.toFixed(2)}
                      </td>
                      <td className="px-5 py-4 font-semibold text-slate-200">
                        {symbolPrefix}
                        {t.totalAmount.toFixed(2)}
                      </td>
                      <td className="px-5 py-4">
                        <span className="text-[10px] bg-slate-800 border border-slate-700 text-slate-400 px-1.5 py-0.5 rounded font-bold uppercase">
                          {t.exchange || "NSE"}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-xs text-slate-500">
                        {new Date(t.timestamp).toLocaleDateString()} {new Date(t.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={8} className="text-center py-10 text-slate-500 font-medium">
                    No transactions match the selected filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
