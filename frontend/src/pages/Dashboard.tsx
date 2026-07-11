function Dashboard() {
  return (
    <div className="min-h-screen bg-slate-100 p-8">

      <h1 className="text-4xl font-bold text-slate-800">
        Dashboard
      </h1>

      <p className="mt-3 text-gray-600">
        Welcome to your Virtual Stock Trading Dashboard.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-10">

        <div className="bg-white rounded-xl shadow-md p-6">
          <h2 className="text-xl font-semibold">
            Portfolio Value
          </h2>

          <p className="text-3xl text-green-600 mt-4">
            ₹0
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-md p-6">
          <h2 className="text-xl font-semibold">
            Available Balance
          </h2>

          <p className="text-3xl text-blue-600 mt-4">
            ₹100,000
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-md p-6">
          <h2 className="text-xl font-semibold">
            Total Profit/Loss
          </h2>

          <p className="text-3xl text-red-500 mt-4">
            ₹0
          </p>
        </div>

      </div>

    </div>
  );
}

export default Dashboard;