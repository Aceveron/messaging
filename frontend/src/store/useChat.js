import { create } from "zustand";
import toast from "react-hot-toast";
import { axiosInstance } from "../lib/axios";
import { downloadAndDecryptImage } from "../lib/mediaApi";
import { useAuth } from "./useAuth";

// Local storage helpers to persist unread badges per authenticated user
const UNREAD_KEY_PREFIX = "chat-unread-counts";
const loadUnreadCounts = () => {
  if (typeof window === "undefined") return {};
  const userId = useAuth.getState().authUser?._id;
  if (!userId) return {};
  try {
    const raw = window.localStorage.getItem(`${UNREAD_KEY_PREFIX}:${userId}`);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
};

const persistUnreadCounts = (counts) => {
  if (typeof window === "undefined") return;
  const userId = useAuth.getState().authUser?._id;
  if (!userId) return;
  try {
    window.localStorage.setItem(`${UNREAD_KEY_PREFIX}:${userId}`, JSON.stringify(counts || {}));
  } catch {
    // ignore storage errors
  }
};

// Track conversation order so the last active chat stays on top across refreshes
const ORDER_KEY_PREFIX = "chat-conversation-order";
const loadConversationOrder = () => {
  if (typeof window === "undefined") return [];
  const userId = useAuth.getState().authUser?._id;
  if (!userId) return [];
  try {
    const raw = window.localStorage.getItem(`${ORDER_KEY_PREFIX}:${userId}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const persistConversationOrder = (order) => {
  if (typeof window === "undefined") return;
  const userId = useAuth.getState().authUser?._id;
  if (!userId) return;
  try {
    window.localStorage.setItem(`${ORDER_KEY_PREFIX}:${userId}`, JSON.stringify(order || []));
  } catch {
    // ignore storage errors
  }
};

// Reorder a list of users given a preferred order array of ids
const applyConversationOrder = (users, order) => {
  if (!Array.isArray(users) || !users.length) return users || [];
  if (!Array.isArray(order) || !order.length) return users;
  const byId = new Map(users.map((u) => [u._id, u]));
  const ordered = order
    .map((id) => byId.get(id))
    .filter(Boolean);
  // Append any users not yet in the order array
  users.forEach((u) => {
    if (!order.includes(u._id)) ordered.push(u);
  });
  return ordered;
};

// Move a given userId to the top and persist the new order
const bumpUserToTop = (users, userId) => {
  if (!userId) return { users, order: [] };
  const order = loadConversationOrder();
  const nextOrder = [userId, ...order.filter((id) => id !== userId)];
  persistConversationOrder(nextOrder);
  const nextUsers = applyConversationOrder(users, nextOrder);
  return { users: nextUsers, order: nextOrder };
};

export const useChat = create((set, get) => ({
  // Message thread for the currently selected user
  messages: [],
  // All available chat partners
  users: [],
  // Conversation order cache (not directly used in UI, but kept for hydration)
  conversationOrder: loadConversationOrder(),
  // Who is currently open in the chat panel
  selectedUser: null,
  // Loading flags
  isUsersLoading: false,
  isMessagesLoading: false,
  // STOMP subscription handle
  messageSubscription: null,
  // In-memory media cache keyed by mediaId
  mediaCache: {},
  // Search state shared across navbar and chat header
  userSearchTerm: "",
  messageSearchTerm: "",
  searchResultIndex: 0,
  pendingMessageId: null,
  // Unread counters keyed by userId (hydrated from storage)
  unreadCounts: loadUnreadCounts(),

  // Fetch contact list
  getUsers: async () => {
    set({ isUsersLoading: true });
    try {
      const res = await axiosInstance.get("/messages/users");
      const persistedUnread = loadUnreadCounts();
      const persistedOrder = loadConversationOrder();
      const orderedUsers = applyConversationOrder(res.data, persistedOrder);
      set((state) => ({
        users: orderedUsers,
        unreadCounts: Object.keys(persistedUnread).length
          ? { ...state.unreadCounts, ...persistedUnread }
          : state.unreadCounts,
        conversationOrder: persistedOrder,
      }));
    } catch (error) {
      toast.error(error.response.data.message);
    } finally {
      set({ isUsersLoading: false });
    }
  },

  // Fetch message history with a user and clear unread badge
  getMessages: async (userId) => {
    set({ isMessagesLoading: true });
    try {
      const res = await axiosInstance.get(`/messages/${userId}`);
      set({ messages: res.data });
      set((state) => {
        const nextUnread = { ...state.unreadCounts, [userId]: 0 };
        persistUnreadCounts(nextUnread);
        return { unreadCounts: nextUnread };
      });
      // Pre-fetch media for received messages
      await get().prefetchMedia(res.data);
    } catch (error) {
      toast.error(error.response.data.message);
    } finally {
      set({ isMessagesLoading: false });
    }
  },
  // Send with optimistic UI update and keep chat ordered
  sendMessage: async (messageData) => {
    const { selectedUser, messages } = get();
    const { authUser } = useAuth.getState();
    
    try {
      // Create unique temporary ID
      const tempId = `temp-${Date.now()}-${Math.random()}`;
      
      // Create optimistic message for immediate display
      const optimisticMessage = {
        _id: tempId,
        text: messageData.text || "",
        mediaId: messageData.mediaId || null,
        encryptedKey: messageData.encryptedKey || null,
        iv: messageData.iv || null,
        hash: messageData.hash || null,
        mimeType: messageData.mimeType || null,
        fileSize: messageData.fileSize || null,
        // Local-only preview URL to show immediately
        previewUrl: messageData.previewUrl || null,
        senderId: authUser._id,
        receiverId: selectedUser._id,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      
      // Display message immediately (optimistic update)
      const messagesWithOptimistic = [...messages, optimisticMessage];
      set({ messages: messagesWithOptimistic });
      
      // Send to server in background
      const res = await axiosInstance.post(`/messages/send/${selectedUser._id}`, messageData);
      
      // Replace optimistic message with real one from server
      const updatedMessages = messagesWithOptimistic.map(msg => 
        msg._id === tempId ? { ...res.data, previewUrl: msg.previewUrl || null } : msg
      );
      set((state) => {
        const targetId = selectedUser?._id;
        const { users: nextUsers, order } = bumpUserToTop(state.users, targetId);
        return {
          messages: updatedMessages,
          users: nextUsers,
          conversationOrder: order,
        };
      });

      // Pre-fetch media for the newly confirmed message
      await get().prefetchMedia([res.data]);
    } catch (error) {
      // Remove optimistic message on error and show error toast
      set({ messages: messages });
      toast.error(error.response?.data?.message || "Failed to send message");
    }
  },

  subscribeToMessages: () => {
    const { messageSubscription } = get();
    const { authUser } = useAuth.getState();
    if (!authUser?._id) return;

    const client = useAuth.getState().socket; // STOMP client
    if (!client) return;

    // Avoid duplicate subscriptions
    if (messageSubscription) return;

    // Reset handle so future reconnects can resubscribe
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

    // Create the STOMP topic subscription for this user
    const subscribe = () => {
      // Double-check to avoid duplicate subs if connect fires multiple times
      if (get().messageSubscription) return;

      const sub = client.subscribe(`/topic/messages/${authUser._id}`, (frame) => {
        try {
          const newMessage = JSON.parse(frame.body);
          const otherUserId =
            newMessage.senderId === authUser._id ? newMessage.receiverId : newMessage.senderId;

          set((state) => {
            // If chat is open, append and clear unread; otherwise bump count
            const isActive = state.selectedUser?._id === otherUserId;
            const isFromOther = newMessage.senderId !== authUser._id;
            const nextUnread = { ...state.unreadCounts };
            if (isActive) {
              nextUnread[otherUserId] = 0;
            } else if (isFromOther) {
              nextUnread[otherUserId] = (nextUnread[otherUserId] || 0) + 1;
            }
            persistUnreadCounts(nextUnread);

            // Move the conversation to the top when a message arrives
            const { users: nextUsers, order } = bumpUserToTop(state.users, otherUserId);

            return {
              unreadCounts: nextUnread,
              users: nextUsers,
              conversationOrder: order,
              messages: isActive ? [...state.messages, newMessage] : state.messages,
            };
          });
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

  // UI helpers for search and unread badges
  setUserSearchTerm: (term) => set({ userSearchTerm: term }),
  setMessageSearchTerm: (term) => set({ messageSearchTerm: term, searchResultIndex: 0 }),
  setSearchResultIndex: (index) => set({ searchResultIndex: index }),
  setPendingMessageId: (messageId) => set({ pendingMessageId: messageId }),
  clearUnread: (userId) =>
    set((state) => ({
      unreadCounts: (() => {
        const next = { ...state.unreadCounts, [userId]: 0 };
        persistUnreadCounts(next);
        return next;
      })(),
    })),

  // Switch active chat and clear its unread count
  setSelectedUser: (selectedUser) => {
    // Keep a single subscription alive; only change selectedUser
    set((state) => ({
      selectedUser,
      unreadCounts: (() => {
        if (!selectedUser) return state.unreadCounts;
        const next = { ...state.unreadCounts, [selectedUser._id]: 0 };
        persistUnreadCounts(next);
        return next;
      })(),
    }));
    // Attempt to ensure subscription exists if socket is connected
    const client = useAuth.getState().socket;
    if (client && client.connected && !get().messageSubscription) {
      // trigger subscription
      get().subscribeToMessages();
    }
  },

  getMediaUrl: (mediaId) => get().mediaCache[mediaId] || null,

  prefetchMedia: async (msgs) => {
    const { mediaCache } = get();
    const toFetch = (msgs || []).filter(
      (m) => m?.mediaId && !mediaCache[m.mediaId]
    );

    for (const m of toFetch) {
      try {
        const url = await downloadAndDecryptImage(
          m.mediaId,
          m.encryptedKey,
          m.iv,
          m.hash
        );
        set((state) => ({
          mediaCache: { ...state.mediaCache, [m.mediaId]: url },
        }));
      } catch (e) {
        console.error("Failed to fetch media", m.mediaId, e);
      }
    }
  },
}));
