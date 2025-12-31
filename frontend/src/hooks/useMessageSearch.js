import { useEffect, useRef, createElement } from "react";
import { useChat } from "../store/useChat";

/**
 * Custom hook for message search functionality
 * Provides search filtering, navigation, and auto-scrolling
 */
export const useMessageSearch = () => {
  const {
    messages,
    messageSearchTerm,
    setMessageSearchTerm,
    searchResultIndex,
    setSearchResultIndex,
    pendingMessageId,
    setPendingMessageId,
  } = useChat();

  const searchResultRef = useRef(null);

  // Filter messages to count search results
  const filteredMessages = messages.filter((message) => {
    const q = messageSearchTerm.trim().toLowerCase();
    if (!q) return false;
    return (message.text || "").toLowerCase().includes(q);
  });

  const getActiveMessage = () => {
    if (messageSearchTerm && filteredMessages.length > 0) {
      return filteredMessages[searchResultIndex];
    }
    return null;
  };

  const isActiveResult = (messageId) => {
    return messageId === getActiveMessage()?._id;
  };

  const handlePrevious = () => {
    setSearchResultIndex(
      searchResultIndex === 0 ? filteredMessages.length - 1 : searchResultIndex - 1
    );
  };

  const handleNext = () => {
    setSearchResultIndex((searchResultIndex + 1) % filteredMessages.length);
  };

  // Auto-set search index: prefer pendingMessageId if present, else most recent match
  useEffect(() => {
    if (!messageSearchTerm || filteredMessages.length === 0) return;

    if (pendingMessageId) {
      const idx = filteredMessages.findIndex((m) => m._id === pendingMessageId);
      if (idx !== -1) {
        setSearchResultIndex(idx);
        setPendingMessageId(null);
        return;
      }
    }

    setSearchResultIndex(filteredMessages.length - 1);
  }, [messageSearchTerm, filteredMessages, pendingMessageId, setSearchResultIndex, setPendingMessageId]);

  // Scroll to active search result when search index changes
  useEffect(() => {
    if (messageSearchTerm && searchResultRef.current) {
      searchResultRef.current.scrollIntoView({ behavior: "smooth", block: "center" });
    }
  }, [searchResultIndex, messageSearchTerm]);

  const highlightText = (text) => {
    const q = messageSearchTerm.trim().toLowerCase();
    if (!q) return text;

    const parts = text.split(new RegExp(`(${q})`, "gi"));
    return parts.map((part, i) =>
      part.toLowerCase() === q
        ? createElement(
            "mark",
            { key: i, className: "bg-yellow-400 px-1 rounded font-semibold" },
            part
          )
        : createElement("span", { key: i }, part)
    );
  };

  return {
    // State
    messageSearchTerm,
    setMessageSearchTerm,
    searchResultIndex,
    setSearchResultIndex,
    filteredMessages,
    
    // Methods
    handlePrevious,
    handleNext,
    getActiveMessage,
    isActiveResult,
    highlightText,
    
    // Display values
    resultCount: filteredMessages.length,
    currentResultNumber: messageSearchTerm && filteredMessages.length > 0 ? searchResultIndex + 1 : 0,
    totalMessages: messages.length,
    isSearching: messageSearchTerm.trim().length > 0,
    
    // Ref for scrolling
    searchResultRef,

    // Targeting specific message
    setPendingMessageId,
  };
};
