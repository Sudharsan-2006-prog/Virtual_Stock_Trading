import { useMemo } from "react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  BarChart,
  Bar,
  Cell,
  PieChart,
  Pie,
  Legend
} from "recharts";
import { TrendingUp, Compass, Award, Frown, ArrowUpRight, ArrowDownRight, Download } from "lucide-react";
import { exportToCSV } from "../../utils/CSVExporter";

interface AnalyticsTabProps {
  analyticsData: any;
  portfolio: any[];
}

const SECTOR_COLORS: { [key: string]: string } = {
  Technology: "#6366f1", // Indigo
  Banking: "#3b82f6",    // Blue
  Energy: "#f59e0b",     // Amber
  Healthcare: "#10b981",  // Emerald
  Others: "#8b5cf6",     // Violet
};

export default function AnalyticsTab({ analyticsData, portfolio }: AnalyticsTabProps) {
  const { portfolioAnalytics, transactionAnalytics, portfolioHistory } = analyticsData;

  // Process sector data for the Pie Chart
  const sectorData = useMemo(() => {
    if (!portfolioAnalytics?.sectorDistribution) return [];
    return Object.entries(portfolioAnalytics.sectorDistribution)
      .map(([name, value]) => ({
        name,
        value: Number(value),
      }))
      .filter((item) => item.value > 0);
  }, [portfolioAnalytics]);

  // Process holdings value for Bar Chart
  const holdingsBarData = useMemo(() => {
    if (!portfolio || portfolio.length === 0) return [];
    return portfolio.map((item) => {
      const invested = item.currency === "USD" ? item.investedAmount * 83.0 : item.investedAmount;
      const current = item.currency === "USD" ? item.marketValue * 83.0 : item.marketValue;
      return {
        symbol: item.stockSymbol,
        invested: Number(invested.toFixed(2)),
        current: Number(current.toFixed(2)),
      };
    });
  }, [portfolio]);

  // Process daily changes from history
  const dailyChangeData = useMemo(() => {
    if (!portfolioHistory || portfolioHistory.length <= 1) return [];
    const points = [];
    for (let i = 1; i < portfolioHistory.length; i++) {
      const prev = portfolioHistory[i - 1].portfolioValue;
      const curr = portfolioHistory[i].portfolioValue;
      const date = portfolioHistory[i].date;
      points.push({
        date: date.substring(5), // Short date (MM-DD)
        change: Number((curr - prev).toFixed(2)),
      });
    }
    return points;
  }, [portfolioHistory]);

  const handleExportAnalytics = () => {
    const headers = ["Metric", "Value"];
    const rows = [
      ["Total Investment (INR Equivalent)", `₹${portfolioAnalytics.totalInvestment.toLocaleString()}`],
      ["Current Value (INR Equivalent)", `₹${portfolioAnalytics.currentValue.toLocaleString()}`],
      ["Total Return", `₹${portfolioAnalytics.totalReturn.toLocaleString()}`],
      ["Profit/Loss Percent", `${portfolioAnalytics.profitLossPercent}%`],
      ["Annualized Return (CAGR)", `${portfolioAnalytics.annualizedReturn.toFixed(2)}%`],
      ["Portfolio Diversity Score", `${portfolioAnalytics.diversityScore}/100`],
      ["Today's Profit/Loss", `₹${portfolioAnalytics.todayProfitLoss.toLocaleString()}`],
      ["Best Performing Stock", portfolioAnalytics.bestPerformingStock],
      ["Worst Performing Stock", portfolioAnalytics.worstPerformingStock],
      ["Top Gainer Today", portfolioAnalytics.topGainer],
      ["Top Loser Today", portfolioAnalytics.topLoser],
      ["Number of Trades", transactionAnalytics.numTrades],
      ["Total Buys", `₹${transactionAnalytics.totalBuys.toLocaleString()}`],
      ["Total Sells", `₹${transactionAnalytics.totalSells.toLocaleString()}`],
      ["Average Holding Period", `${transactionAnalytics.averageHoldingPeriodDays.toFixed(1)} days`],
      ["Largest Gain in Single Sale", `₹${transactionAnalytics.largestGain.toLocaleString()}`],
      ["Largest Loss in Single Sale", `₹${transactionAnalytics.largestLoss.toLocaleString()}`],
    ];

    exportToCSV("virtual_trade_portfolio_analytics.csv", headers, rows);
  };

  const getDiversityLabel = (score: number) => {
    if (score >= 80) return { label: "Well Diversified", color: "text-emerald-400 bg-emerald-500/10 border-emerald-500/20" };
    if (score >= 50) return { label: "Moderate Diversification", color: "text-amber-400 bg-amber-500/10 border-amber-500/20" };
    return { label: "Highly Concentrated (Risky)", color: "text-rose-400 bg-rose-500/10 border-rose-500/20" };
  };

  const divStatus = getDiversityLabel(portfolioAnalytics?.diversityScore || 0);

  const formattedHistoryData = useMemo(() => {
    if (!portfolioHistory) return [];
    return portfolioHistory.map((item: any) => ({
      ...item,
      date: item.date.substring(5), // truncate year
    }));
  }, [portfolioHistory]);

  return (
    <div className="space-y-6">
      {/* Tab Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-slate-800 pb-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight">Performance & Risk Analytics</h2>
          <p className="text-sm text-slate-400">Deep-dive insights, compounding rates, and allocation reports.</p>
        </div>
        <button
          onClick={handleExportAnalytics}
          className="flex items-center gap-2 px-4 py-2 bg-slate-900 border border-slate-800 text-slate-300 hover:text-white hover:bg-slate-850 rounded-xl text-sm font-semibold transition cursor-pointer"
        >
          <Download className="w-4 h-4" />
          <span>Export Analytics</span>
        </button>
      </div>

      {/* Grid of Key Analytics Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* CAGR */}
        <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-5 shadow-xl relative overflow-hidden flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Annualized Return (CAGR)</p>
              <h4 className="text-2xl font-black text-slate-100 mt-2">
                {portfolioAnalytics?.annualizedReturn.toFixed(2)}%
              </h4>
            </div>
            <span className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
              <TrendingUp className="w-5 h-5" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-4 leading-relaxed">
            Theoretical annual growth rate compounded since your first trade.
          </p>
        </div>

        {/* Diversity Score */}
        <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-5 shadow-xl relative overflow-hidden flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Diversity Score</p>
              <h4 className="text-2xl font-black text-slate-100 mt-2">
                {portfolioAnalytics?.diversityScore?.toFixed(0)}/100
              </h4>
            </div>
            <span className="p-2 rounded-xl bg-violet-500/10 text-violet-400 border border-violet-500/20">
              <Compass className="w-5 h-5" />
            </span>
          </div>
          <div className="mt-4">
            <span className={`inline-block text-[10px] font-bold px-2 py-0.5 rounded-full border ${divStatus.color}`}>
              {divStatus.label}
            </span>
          </div>
        </div>

        {/* Best Stock */}
        <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-5 shadow-xl relative overflow-hidden flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Best Performing Stock</p>
              <h4 className="text-lg font-black text-emerald-400 mt-2.5 truncate max-w-[150px]">
                {portfolioAnalytics?.bestPerformingStock !== "N/A" ? portfolioAnalytics.bestPerformingStock.split(" ")[0] : "N/A"}
              </h4>
            </div>
            <span className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Award className="w-5 h-5" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-3 flex items-center gap-1">
            <ArrowUpRight className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
            <span className="text-emerald-400 font-semibold">{portfolioAnalytics?.bestPerformingStock?.includes("(") ? portfolioAnalytics.bestPerformingStock.substring(portfolioAnalytics.bestPerformingStock.indexOf("(")) : ""}</span>
            overall return
          </p>
        </div>

        {/* Worst Stock */}
        <div className="bg-slate-900/40 backdrop-blur-md border border-slate-800/80 rounded-2xl p-5 shadow-xl relative overflow-hidden flex flex-col justify-between">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Worst Performing Stock</p>
              <h4 className="text-lg font-black text-rose-400 mt-2.5 truncate max-w-[150px]">
                {portfolioAnalytics?.worstPerformingStock !== "N/A" ? portfolioAnalytics.worstPerformingStock.split(" ")[0] : "N/A"}
              </h4>
            </div>
            <span className="p-2 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20">
              <Frown className="w-5 h-5" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-3 flex items-center gap-1">
            <ArrowDownRight className="w-3.5 h-3.5 text-rose-400 shrink-0" />
            <span className="text-rose-400 font-semibold">{portfolioAnalytics?.worstPerformingStock?.includes("(") ? portfolioAnalytics.worstPerformingStock.substring(portfolioAnalytics.worstPerformingStock.indexOf("(")) : ""}</span>
            overall return
          </p>
        </div>
      </div>

      {/* Charts Grid Row 1: Net Worth Line Chart & Sector Pie Chart */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Portfolio Growth Line Chart */}
        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-6 shadow-xl lg:col-span-2 flex flex-col justify-between min-h-[380px]">
          <div className="mb-4">
            <h3 className="text-base font-bold text-white">Portfolio Growth</h3>
            <p className="text-xs text-slate-500">Historical asset value (Cash + Stocks) over the past 30 days.</p>
          </div>
          <div className="flex-1 w-full h-[280px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={formattedHistoryData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorNetWorth" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                <Tooltip
                  contentStyle={{ backgroundColor: "#0f172a", borderColor: "#1e293b", borderRadius: "12px", color: "#fff" }}
                  formatter={(value: any) => [`₹${Number(value).toLocaleString()}`, "Value"]}
                />
                <Area type="monotone" dataKey="portfolioValue" stroke="#3b82f6" strokeWidth={2} fillOpacity={1} fill="url(#colorNetWorth)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Asset Allocation Pie Chart */}
        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-6 shadow-xl flex flex-col justify-between min-h-[380px]">
          <div className="mb-4">
            <h3 className="text-base font-bold text-white">Sector Distribution</h3>
            <p className="text-xs text-slate-500">Allocation breakdown by company sectors.</p>
          </div>
          <div className="flex-1 flex items-center justify-center h-[200px]">
            {sectorData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={sectorData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {sectorData.map((entry: any, index) => (
                      <Cell key={`cell-${index}`} fill={SECTOR_COLORS[entry.name] || "#64748b"} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ backgroundColor: "#0f172a", borderColor: "#1e293b", borderRadius: "12px", color: "#fff" }}
                    formatter={(value: any) => [`₹${Number(value).toLocaleString()}`, "Asset Value"]}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="text-sm text-slate-500 font-medium">No stock holdings to compute sectors</p>
            )}
          </div>
          {/* Custom Legend */}
          {sectorData.length > 0 && (
            <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
              {sectorData.map((item: any) => (
                <div key={item.name} className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: SECTOR_COLORS[item.name] || "#64748b" }} />
                  <span className="text-slate-400 truncate">{item.name}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Charts Grid Row 2: Daily Profit/Loss & Individual Holdings Value */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Profit/Loss Bar Chart */}
        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-6 shadow-xl min-h-[350px] flex flex-col justify-between">
          <div className="mb-4">
            <h3 className="text-base font-bold text-white">Daily Profit / Loss</h3>
            <p className="text-xs text-slate-500">Day-over-day changes in total assets value.</p>
          </div>
          <div className="flex-1 w-full h-[240px]">
            {dailyChangeData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={dailyChangeData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="date" stroke="#64748b" fontSize={10} tickLine={false} />
                  <YAxis stroke="#64748b" fontSize={10} tickLine={false} />
                  <Tooltip
                    contentStyle={{ backgroundColor: "#0f172a", borderColor: "#1e293b", borderRadius: "12px", color: "#fff" }}
                    formatter={(value: any) => [`₹${Number(value).toLocaleString()}`, "P/L Shift"]}
                  />
                  <Bar dataKey="change">
                    {dailyChangeData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.change >= 0 ? "#10b981" : "#f43f5e"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-500 text-sm">
                Need at least 2 days of historical portfolio balance points
              </div>
            )}
          </div>
        </div>

        {/* Holdings Value Chart (Invested vs Current) */}
        <div className="bg-slate-900 border border-slate-800/80 rounded-2xl p-6 shadow-xl min-h-[350px] flex flex-col justify-between">
          <div className="mb-4">
            <h3 className="text-base font-bold text-white">Investment Value per Ticker</h3>
            <p className="text-xs text-slate-500">Comparison between cost basis and current market value (INR equivalent).</p>
          </div>
          <div className="flex-1 w-full h-[240px]">
            {holdingsBarData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={holdingsBarData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="symbol" stroke="#64748b" fontSize={11} tickLine={false} />
                  <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                  <Tooltip
                    contentStyle={{ backgroundColor: "#0f172a", borderColor: "#1e293b", borderRadius: "12px", color: "#fff" }}
                    formatter={(value: any) => [`₹${Number(value).toLocaleString()}`]}
                  />
                  <Legend verticalAlign="top" height={36} wrapperStyle={{ fontSize: "11px", color: "#94a3b8" }} />
                  <Bar dataKey="invested" name="Cost Basis" fill="#475569" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="current" name="Market Value" fill="#3b82f6" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-full text-slate-500 text-sm">
                Holdings list is empty
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Extra Top Gainer / Loser details */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center text-lg font-bold">↑</span>
            <div>
              <p className="text-xs text-slate-500 font-semibold uppercase">Daily Top Gainer</p>
              <h4 className="text-sm font-bold text-slate-200 mt-1">{portfolioAnalytics?.topGainer}</h4>
            </div>
          </div>
          <span className="text-xs font-semibold text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">Highest Daily Gain</span>
        </div>

        <div className="p-4 rounded-2xl bg-rose-500/5 border border-rose-500/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="w-10 h-10 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center text-lg font-bold">↓</span>
            <div>
              <p className="text-xs text-slate-500 font-semibold uppercase">Daily Top Loser</p>
              <h4 className="text-sm font-bold text-slate-200 mt-1">{portfolioAnalytics?.topLoser}</h4>
            </div>
          </div>
          <span className="text-xs font-semibold text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20">Lowest Daily Loss</span>
        </div>
      </div>
    </div>
  );
}
