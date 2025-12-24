import { create } from "zustand";
import { axiosInstance } from "../lib/axios";
import toast from "react-hot-toast";

export const useAuthStore = create((set) => ({
    authUser: null, //initially no authenticated user since we do not know yet
    isRegister: false,
    isLogin: false,
    isProfileUpdate: false,
    isCheckingAuth: true, // loading state to check if user is authenticated when refreshing

    checkAuth: async () => {
        try {
            const res = await axiosInstance.get("/auth/pulse");

            set({ authUser:res.data});

        } catch (error) {
            console.log("Error checking auth:", error);
            set({ authUser: null });
            
        } finally {
            set({ isCheckingAuth: false });
        }
    },

    register: async (data) => {
        set({ isRegister: true });
        try {
            const res = await axiosInstance.post("/auth/register", data);
            set({ authUser: res.data });
            toast.success("Registration successful! Please login.");

        } catch (error) {
            toast.error(error.response?.data?.message);

        } finally {
            set({ isRegister: false });
        }
    },

    logout: async () => {
        try {
            await axiosInstance.post("/auth/logout");
            set({ authUser: null });
            toast.success("Logged out successfully");

        } catch (error) {
            toast.error(error.response.data.message);
        }
    },
}));