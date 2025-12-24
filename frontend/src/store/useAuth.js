import { create } from "zustand";
import { axiosInstance } from "../lib/axios";

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
    }
}));