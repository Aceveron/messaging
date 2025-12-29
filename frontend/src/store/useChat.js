import { create } from "zustand";
import toast from "react-hot-toast";
import { axiosInstance } from "../lib/axios";
import { useAuth } from "./useAuth";

export const useChat = create((set, get) => ({
  messages: [],
  users: [],
  selectedUser: null,
  isUsersLoading: false,
  isMessagesLoading: false,
  messageSubscription: null,

  getUsers: async () => {
    set({ isUsersLoading: true });
    try {
      const res = await axiosInstance.get("/messages/users");
      set({ users: res.data });
    } catch (error) {
      toast.error(error.response.data.message);
    } finally {
      set({ isUsersLoading: false });
    }
  },

  getMessages: async (userId) => {
    set({ isMessagesLoading: true });
    try {
      const res = await axiosInstance.get(`/messages/${userId}`);
      set({ messages: res.data });
    } catch (error) {
      toast.error(error.response.data.message);
    } finally {
      set({ isMessagesLoading: false });
    }
  },
  sendMessage: async (messageData) => {
    const { selectedUser, messages } = get();
    try {
      const res = await axiosInstance.post(`/messages/send/${selectedUser._id}`, messageData);
      set({ messages: [...messages, res.data] });
    } catch (error) {
      toast.error(error.response.data.message);
    }
  },

  subscribeToMessages: () => {
    const { selectedUser, messageSubscription } = get();
    if (!selectedUser) return;

    const client = useAuth.getState().socket; // STOMP client
    if (!client) return;

    // Avoid duplicate subscriptions
    if (messageSubscription) return;

    const clearSubscription = () => set({ messageSubscription: null });

    // Ensure we clear state on disconnect so reconnect can resubscribe
    const prevOnDisconnect = client.onDisconnect;
    client.onDisconnect = (frame) => {
      clearSubscription();
      if (typeof prevOnDisconnect === "function") prevOnDisconnect(frame);
    };

    const prevOnWebSocketClose = client.onWebSocketClose;
    client.onWebSocketClose = (event) => {
      clearSubscription();
      if (typeof prevOnWebSocketClose === "function") prevOnWebSocketClose(event);
    };

    const subscribe = () => {
      // Double-check to avoid duplicate subs if connect fires multiple times
      if (get().messageSubscription) return;

      const sub = client.subscribe("/user/topic/messages", (frame) => {
        try {
          const newMessage = JSON.parse(frame.body);
          const isFromSelectedUser = newMessage.senderId === selectedUser._id;
          if (!isFromSelectedUser) return;

          set({ messages: [...get().messages, newMessage] });
        } catch (e) {
          console.error("Failed to parse incoming message:", e);
        }
      });

      set({ messageSubscription: sub });
    };

    if (client.connected) {
      subscribe();
      return;
    }

    // If not yet connected, hook into onConnect to subscribe when ready
    if (client.connected) {
      subscribe();
      return;
    }

    // Fallback: subscribe once the socket-connected event fires
    const handler = () => {
      subscribe();
    };
    if (typeof window !== "undefined" && typeof window.addEventListener === "function") {
      window.addEventListener("socket-connected", handler, { once: true });
    }
  },

  unsubscribeFromMessages: () => {
    const sub = get().messageSubscription;
    if (sub) {
      try { sub.unsubscribe(); }
      catch (e) { console.warn("Failed to unsubscribe STOMP subscription", e); }
    }
    set({ messageSubscription: null });
  },

  setSelectedUser: (selectedUser) => {
    // Keep a single subscription alive; only change selectedUser
    set({ selectedUser });
    // Attempt to ensure subscription exists if socket is connected
    const client = useAuth.getState().socket;
    if (client && client.connected && !get().messageSubscription) {
      // trigger subscription
      get().subscribeToMessages();
    }
  },
}));
