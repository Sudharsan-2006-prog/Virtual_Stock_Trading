import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor to add authorization token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Wallet
export const getWalletBalance = () => api.get("/wallet");

// Portfolio
export const getPortfolio = () => api.get("/portfolio");

// Trading
export const buyStock = (data: { stockSymbol: string; companyName: string; quantity: number; price: number }) => api.post("/trade/buy", data);
export const sellStock = (data: { stockSymbol: string; quantity: number; price: number }) => api.post("/trade/sell", data);

// Transactions
export const getTransactions = () => api.get("/transactions");

// Watchlist
export const getWatchlist = () => api.get("/watchlist");
export const addToWatchlist = (data: { stockSymbol: string; companyName: string }) => api.post("/watchlist", data);
export const removeFromWatchlist = (id: number) => api.delete(`/watchlist/${id}`);

export default api;
