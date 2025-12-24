import Navbar from "./components/Navbar";
import { Routes, Route, Navigate } from "react-router-dom";
import Home from "./pages/Home";
import Register from "./pages/register";
import Login from "./pages/login";
import Profile from "./pages/profile";
import Settings from "./pages/Settings";
import { useAuthStore } from "./store/useAuth";
import { useEffect } from "react";
import { Loader } from "lucide-react"

const App = () => {

  const {authUser, checkAuth, isCheckingAuth } = useAuthStore()
  useEffect(() => {
    checkAuth()
  }, [checkAuth]);

  console.log({ authUser });

  if (isCheckingAuth && !authUser) 
    return (
      <div className="flex justify-center items-center h-screen">
        <Loader className="animate-spin size-10" />
      </div>
    );

  return (
    <div>
      <Navbar />
        <Routes>
          <Route path="/" element={ authUser ? <Home /> : <Navigate to="/login" />} />
          <Route path="/register" element={ !authUser ? <Register /> : <Navigate to="/" />} />
          <Route path="/login" element={ !authUser ? <Login /> : <Navigate to="/" />} />
          <Route path="/profile" element={ authUser ? <Profile /> : <Navigate to="/login" />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
    </div>
  )
};

export default App