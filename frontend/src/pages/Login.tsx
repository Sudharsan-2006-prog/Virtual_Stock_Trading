import { useState } from "react";
import Button from "../components/Button";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [emailError, setEmailError] = useState("");
  const [passwordError, setPasswordError] = useState("");

  const handleLogin = () => {
    let valid = true;

    // Clear previous errors
    setEmailError("");
    setPasswordError("");

    // Email validation
    if (email.trim() === "") {
      setEmailError("Email is required");
      valid = false;
    } else if (!email.includes("@")) {
      setEmailError("Enter a valid email");
      valid = false;
    }

    // Password validation
    if (password.trim() === "") {
      setPasswordError("Password is required");
      valid = false;
    } else if (password.length < 6) {
      setPasswordError("Password must be at least 6 characters");
      valid = false;
    }

    // If everything is valid
    if (valid) {
      console.log("Email:", email);
      console.log("Password:", password);

      alert("Login Successful! (Backend will be connected later)");
    }
  };

  return (
    <div className="min-h-screen flex justify-center items-center bg-slate-100">
      <div className="bg-white shadow-xl rounded-xl p-10 w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-8">
          Login
        </h1>

        <div className="space-y-5">
          {/* Email */}
          <div>
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full border rounded-lg px-4 py-3"
            />

            {emailError && (
              <p className="text-red-500 text-sm mt-1">
                {emailError}
              </p>
            )}
          </div>

          {/* Password */}
          <div>
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full border rounded-lg px-4 py-3"
            />

            {passwordError && (
              <p className="text-red-500 text-sm mt-1">
                {passwordError}
              </p>
            )}
          </div>

          <Button
            text="Login"
            onClick={handleLogin}
          />
        </div>
      </div>
    </div>
  );
}

export default Login;