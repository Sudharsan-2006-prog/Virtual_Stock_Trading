import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ComposedChart, Area, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from "recharts";
import { ArrowLeft, Globe, Briefcase, Award, TrendingUp, DollarSign, Volume2, ShieldAlert } from "lucide-react";
import { getMarketHistory, getCompanyInfo } from "../../services/api";

interface StockDetailViewProps {
  symbol: string;
  onBack: () => void;
  onOpenTradeModal: (type: "BUY" | "SELL", symbol: string, companyName: string, price: number, exchange: string, currency: string) => void;
}

export default function StockDetailView({ symbol, onBack, onOpenTradeModal }: StockDetailViewProps) {
  const [range, setRange] = useState<string>("1M");

  // Fetch company profile details
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ["companyProfile", symbol],
    queryFn: () => getCompanyInfo(symbol).then((res) => res.data),
    refetchInterval: 60000,
  });

  // Fetch historical quote chart points based on selected range
  const { data: historyData, isLoading: historyLoading } = useQuery({
    queryKey: ["marketHistory", symbol, range],
    queryFn: () => getMarketHistory(symbol, range).then((res) => res.data),
    refetchInterval: 30000,
  });

  // Calculate current price and changes from profile or fallback
  const currencyPrefix = profile?.currency === "USD" ? "$" : "₹";
  const points = historyData?.history || [];
  
  // Get latest point for real-time closing/daily change calculations if possible
  const latestPoint = points.length > 0 ? points[points.length - 1] : null;
  const previousPoint = points.length > 1 ? points[points.length - 2] : null;
  
  const currentPrice = latestPoint ? latestPoint.close : 0;
  const priceChange = latestPoint && previousPoint ? latestPoint.close - previousPoint.close : 0;
  const priceChangePercent = latestPoint && previousPoint && previousPoint.close > 0 
    ? (priceChange / previousPoint.close) * 100 
    : 0;

  const formatMarketCap = (cap: number | undefined) => {
    if (!cap) return "N/A";
    const prefix = profile?.currency === "USD" ? "$" : "₹";
    if (cap >= 1000) {
      return `${prefix}${(cap / 1000).toFixed(2)}T`;
    }
    return `${prefix}${cap.toFixed(2)}B`;
  };

  // Custom tooltips to show Open, High, Low, Close, and Volume details on hover
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-slate-950 border border-slate-800 rounded-xl p-3.5 shadow-2xl space-y-1.5 text-xs text-slate-300">
          <p className="font-bold text-slate-400 border-b border-slate-850 pb-1 mb-1">{data.date}</p>
          <div className="flex justify-between gap-6">
            <span>Open:</span>
            <span className="font-semibold text-white">{currencyPrefix}{data.open.toFixed(2)}</span>
          </div>
          <div className="flex justify-between gap-6">
            <span>High:</span>
            <span className="font-semibold text-emerald-400">{currencyPrefix}{data.high.toFixed(2)}</span>
          </div>
          <div className="flex justify-between gap-6">
            <span>Low:</span>
            <span className="font-semibold text-rose-400">{currencyPrefix}{data.low.toFixed(2)}</span>
          </div>
          <div className="flex justify-between gap-6">
            <span>Close:</span>
            <span className="font-bold text-white">{currencyPrefix}{data.close.toFixed(2)}</span>
          </div>
          <div className="flex justify-between gap-6 border-t border-slate-850 pt-1 mt-1 text-[10px] text-slate-500">
            <span>Volume:</span>
            <span>{Number(data.volume).toLocaleString()}</span>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="space-y-6">
      {/* Back Button and Core Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <button
          onClick={onBack}
          className="flex items-center gap-2 text-sm text-slate-400 hover:text-white transition cursor-pointer font-semibold"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Dashboard</span>
        </button>

        {/* Action CTAs */}
        {!profileLoading && profile && (
          <div className="flex items-center gap-3 w-full sm:w-auto">
            <button
              onClick={() => onOpenTradeModal("BUY", symbol, profile.companyName, currentPrice || 100.0, profile.exchange, profile.currency)}
              className="flex-1 sm:flex-none px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-sm font-bold transition shadow-lg shadow-emerald-600/10 cursor-pointer"
            >
              Buy Stock
            </button>
            <button
              onClick={() => onOpenTradeModal("SELL", symbol, profile.companyName, currentPrice || 100.0, profile.exchange, profile.currency)}
              className="flex-1 sm:flex-none px-6 py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-sm font-bold transition shadow-lg shadow-rose-600/10 cursor-pointer"
            >
              Sell Stock
            </button>
          </div>
        )}
      </div>

      {/* Profile Header and Live Quotes */}
      {profileLoading ? (
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-slate-800 rounded w-1/4" />
          <div className="h-8 bg-slate-800 rounded w-1/3" />
        </div>
      ) : (
        profile && (
          <div className="flex flex-col md:flex-row md:justify-between md:items-end gap-4 border-b border-slate-800 pb-5">
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl md:text-3xl font-black text-white">{profile.companyName}</h1>
                <span className="text-xs bg-slate-800 border border-slate-700 text-slate-400 px-2 py-0.5 rounded font-bold uppercase">
                  {profile.exchange}
                </span>
              </div>
              <p className="text-sm font-bold text-blue-400 mt-1">{symbol.toUpperCase()}</p>
            </div>

            {/* Price section */}
            <div className="text-left md:text-right">
              <p className="text-3xl font-black text-white">
                {currencyPrefix}
                {(currentPrice || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </p>
              <div className={`flex items-center gap-1 text-sm font-bold mt-1 ${priceChange >= 0 ? "text-emerald-400" : "text-rose-400"}`}>
                <span>{priceChange >= 0 ? "↑" : "↓"}</span>
                <span>{currencyPrefix}{Math.abs(priceChange).toFixed(2)}</span>
                <span>({priceChangePercent.toFixed(2)}%)</span>
                <span className="text-xs text-slate-500 font-semibold ml-1.5">Today</span>
              </div>
            </div>
          </div>
        )
      )}

      {/* Main Stock Analysis Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Interactive Chart Area */}
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl flex flex-col justify-between min-h-[420px]">
          {/* Header Timeline Selector */}
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-sm font-bold text-slate-400">Interactive Historical Chart</h3>
            <div className="flex bg-slate-950 p-1 border border-slate-850 rounded-xl">
              {["1D", "1W", "1M", "3M", "6M", "1Y"].map((btn) => (
                <button
                  key={btn}
                  onClick={() => setRange(btn)}
                  className={`px-3 py-1 text-xs font-bold rounded-lg transition cursor-pointer ${
                    range === btn ? "bg-blue-600 text-white shadow" : "text-slate-500 hover:text-slate-300"
                  }`}
                >
                  {btn}
                </button>
              ))}
            </div>
          </div>

          {/* Interactive Chart Container */}
          <div className="flex-1 w-full h-[280px] relative">
            {historyLoading ? (
              <div className="absolute inset-0 flex items-center justify-center bg-slate-900/60 z-10">
                <span className="animate-spin text-2xl text-blue-500">⏳</span>
              </div>
            ) : points.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={points} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#3b82f6" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                  <XAxis dataKey="date" stroke="#64748b" fontSize={10} tickLine={false} />
                  <YAxis yAxisId="price" stroke="#64748b" fontSize={10} domain={["auto", "auto"]} tickLine={false} />
                  <YAxis yAxisId="volume" stroke="none" orientation="right" domain={[0, (dataMax: any) => dataMax * 4]} />
                  <Tooltip content={<CustomTooltip />} />
                  
                  {/* Price Area Area */}
                  <Area
                    yAxisId="price"
                    type="monotone"
                    dataKey="close"
                    stroke="#3b82f6"
                    strokeWidth={2}
                    fillOpacity={1}
                    fill="url(#colorPrice)"
                  />
                  
                  {/* Volume Overlay Bars */}
                  <Bar
                    yAxisId="volume"
                    dataKey="volume"
                    fill="#1e293b"
                    opacity={0.4}
                    barSize={6}
                  />
                </ComposedChart>
              </ResponsiveContainer>
            ) : (
              <div className="absolute inset-0 flex items-center justify-center text-slate-500 text-sm">
                No history data points available
              </div>
            )}
          </div>
        </div>

        {/* Key Statistics Grid */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl flex flex-col justify-between">
          <h3 className="text-sm font-bold text-slate-400 mb-4 border-b border-slate-800 pb-2">Key Statistics</h3>
          {profileLoading ? (
            <div className="space-y-4 animate-pulse">
              <div className="h-6 bg-slate-800 rounded w-full" />
              <div className="h-6 bg-slate-800 rounded w-full" />
              <div className="h-6 bg-slate-800 rounded w-full" />
            </div>
          ) : (
            profile && (
              <div className="space-y-4 flex-1 flex flex-col justify-center">
                {/* Market Cap */}
                <div className="flex justify-between items-center text-sm py-1.5 border-b border-slate-850">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Globe className="w-4 h-4" />
                    Market Cap
                  </span>
                  <span className="font-extrabold text-slate-200">{formatMarketCap(profile.marketCap)}</span>
                </div>

                {/* P/E Ratio */}
                <div className="flex justify-between items-center text-sm py-1.5 border-b border-slate-850">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <TrendingUp className="w-4 h-4" />
                    P/E Ratio
                  </span>
                  <span className="font-extrabold text-slate-200">
                    {profile.peRatio ? profile.peRatio.toFixed(2) : "N/A"}
                  </span>
                </div>

                {/* Dividend Yield */}
                <div className="flex justify-between items-center text-sm py-1.5 border-b border-slate-850">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <DollarSign className="w-4 h-4" />
                    Div Yield
                  </span>
                  <span className="font-extrabold text-slate-200">
                    {profile.dividendYield && profile.dividendYield > 0 ? `${profile.dividendYield.toFixed(2)}%` : "0.00%"}
                  </span>
                </div>

                {/* 52w High */}
                <div className="flex justify-between items-center text-sm py-1.5 border-b border-slate-850">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Award className="w-4 h-4 text-emerald-400" />
                    52W High
                  </span>
                  <span className="font-extrabold text-emerald-400">
                    {profile.fiftyTwoWeekHigh ? `${currencyPrefix}${profile.fiftyTwoWeekHigh.toFixed(2)}` : "N/A"}
                  </span>
                </div>

                {/* 52w Low */}
                <div className="flex justify-between items-center text-sm py-1.5 border-b border-slate-850">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <ShieldAlert className="w-4 h-4 text-rose-400" />
                    52W Low
                  </span>
                  <span className="font-extrabold text-rose-400">
                    {profile.fiftyTwoWeekLow ? `${currencyPrefix}${profile.fiftyTwoWeekLow.toFixed(2)}` : "N/A"}
                  </span>
                </div>

                {/* Volume */}
                <div className="flex justify-between items-center text-sm py-1.5">
                  <span className="text-slate-500 flex items-center gap-1.5">
                    <Volume2 className="w-4 h-4" />
                    Trading Volume
                  </span>
                  <span className="font-extrabold text-slate-200">
                    {profile.volume ? Number(profile.volume).toLocaleString() : "N/A"}
                  </span>
                </div>
              </div>
            )
          )}
        </div>
      </div>

      {/* About Company & Description */}
      {!profileLoading && profile && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-4">
          <h3 className="text-sm font-bold text-slate-400 flex items-center gap-1.5">
            <Briefcase className="w-4.5 h-4.5 text-blue-400" />
            About {profile.companyName}
          </h3>
          <p className="text-sm text-slate-400 leading-relaxed font-medium">
            {profile.description || "Company description details not provided."}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-3 text-xs border-t border-slate-800/80">
            <div>
              <span className="text-slate-500">Sector</span>
              <p className="font-bold text-slate-300 mt-0.5">{profile.sector || "Others"}</p>
            </div>
            <div>
              <span className="text-slate-500">Industry</span>
              <p className="font-bold text-slate-300 mt-0.5">{profile.industry || "Conglomerate"}</p>
            </div>
            {profile.website && (
              <div>
                <span className="text-slate-500">Official Website</span>
                <p className="mt-0.5">
                  <a
                    href={profile.website}
                    target="_blank"
                    rel="noreferrer"
                    className="font-bold text-blue-400 hover:text-blue-300 hover:underline flex items-center gap-1.5 transition"
                  >
                    <Globe className="w-3.5 h-3.5" />
                    Visit Website
                  </a>
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
