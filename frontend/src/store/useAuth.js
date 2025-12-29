import { create } from "zustand";
import { axiosInstance } from "../lib/axios";
import toast from "react-hot-toast";
import { Client as StompClient } from "@stomp/stompjs";

const BASE_URL = "http://localhost:5001";

export const useAuth = create((set, get) => ({
    authUser: null, //initially no authenticated user since we do not know yet
    isRegister: false,
    isLogin: false,
    isProfileUpdate: false,
    isCheckingAuth: true, // loading state to check if user is authenticated when refreshing
    onlineUsers: [],
    socket: null,

    checkAuth: async () => {
        try {
        const res = await axiosInstance.get("/auth/pulse");
        const body = res.data;
        if (body && body.authenticated === false) {
          set({ authUser: null });
        } else {
          set({ authUser: body?.data ?? body });
          get().connectSocket();
        }

        } catch (error) {
            console.log("Error checking auth:", error);
            set({ authUser: null });
            
        } finally {
            set({ isCheckingAuth: false });
        }
    },
    
    login: async (data) => {
        set({ isLogin: true });
        try {
            const res = await axiosInstance.post("/auth/login", data);
            set({ authUser: res.data });
            toast.success("Login successful!");
            get().connectSocket();

        } catch (error) {
            toast.error(error.response?.data?.message);

        } finally {
            set({ isLogin: false });
        }
    },

    register: async (data) => {
        set({ isRegister: true });
        try {
            const res = await axiosInstance.post("/auth/register", data);
            set({ authUser: res.data });
            toast.success("Registration successful! Please login.");
            get().connectSocket();

        } catch (error) {
            toast.error(error.response?.data?.message);

        } finally {
            set({ isRegister: false });
        }
    },

    profile: async (data) => {
        set({ isProfileUpdate: true });
        try {
          const res = await axiosInstance.put("/auth/profile", data);
            set({ authUser: res.data });
          toast.success("Profile updated successfully");
        } catch (error) {
          console.log("error in update profile:", error);
            toast.error(error.response.data.message);
        } finally {
          set({ isProfileUpdate: false });
        }
    },

    logout: async () => {
        try {
            await axiosInstance.post("/auth/logout");
            set({ authUser: null });
            toast.success("Logged out successfully");
        get().disconnectSocket();

        } catch (error) {
        toast.error(error.response?.data?.message || "Logout failed");
        }
    },

    connectSocket: () => {
      const { authUser } = get();
      const existing = get().socket;
      if (!authUser || (existing && existing.active)) return;

      const wsUrl = `${BASE_URL.replace(/^http/, 'ws')}/ws?userId=${authUser._id}`;
      const client = new StompClient({
        reconnectDelay: 5000,
        webSocketFactory: () => new WebSocket(wsUrl),
        onConnect: () => {
          // Subscribe to online users broadcast
          client.subscribe("/topic/onlineUsers", (message) => {
            try {
              const userIds = JSON.parse(message.body);
              set({ onlineUsers: userIds });
            } catch (e) {
              console.error("Failed to parse online users:", e);
            }
          });

          // Fetch a snapshot to avoid missing the immediate broadcast on connect
          axiosInstance
            .get("/presence/online")
            .then((res) => {
              if (Array.isArray(res.data)) {
                set({ onlineUsers: res.data });
              }
            })
            .catch((err) => {
              console.warn("Failed to fetch online users snapshot", err?.response?.status || "");
            });

          // Notify other modules that the socket is connected
          if (typeof window !== "undefined" && typeof window.dispatchEvent === "function") {
            window.dispatchEvent(new CustomEvent("socket-connected"));
          }
        },
        onStompError: (frame) => {
          console.error("STOMP error:", frame.headers["message"], frame.body);
        },
      });

      client.activate();
      set({ socket: client });
    },

  disconnectSocket: () => {
    const client = get().socket;
    if (client && client.active) client.deactivate();
  },
}));