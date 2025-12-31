import { useChat } from "../store/useChat";
import { useEffect, useRef } from "react";
import { useMessageSearch } from "../hooks/useMessageSearch";

import ChatHeader from "./ChatHeader";
import MessageInput from "./MessageInput";
import MessageSkeleton from "./skeletons/MessageSkeleton";
import { useAuth } from "../store/useAuth";
import { formatMessageTime } from "../lib/utils";

const ChatContainer = () => {
  const {
    messages,
    getMessages,
    isMessagesLoading,
    selectedUser,
    subscribeToMessages,
    unsubscribeFromMessages,
    prefetchMedia,
    getMediaUrl,
  } = useChat();
  const { authUser } = useAuth();
  const messageEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const { highlightText, isActiveResult, searchResultRef } = useMessageSearch();

  const scrollToBottom = () => {
    // Force scroll after paint completes
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        const container = scrollContainerRef.current;
        if (container) {
          container.scrollTop = container.scrollHeight;
          console.log('Scrolled to bottom. Height:', container.scrollHeight, 'Current:', container.scrollTop);
        }
      });
    });
  };

  useEffect(() => {
    if (!selectedUser?._id) return;

    getMessages(selectedUser._id);
    subscribeToMessages();

    return () => unsubscribeFromMessages();
    // We intentionally exclude functions to avoid effect churn; they are stable from zustand.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedUser?._id]);

  // Scroll to bottom when messages finish loading
  useEffect(() => {
    if (!isMessagesLoading && messages?.length > 0) {
      scrollToBottom();
    }
  }, [isMessagesLoading, messages?.length]);

  useEffect(() => {
    if (messages?.length) {
      prefetchMedia(messages);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messages]);

  if (isMessagesLoading) {
    return (
      <div className="flex-1 flex flex-col overflow-auto">
        <ChatHeader />
        <MessageSkeleton />
        <MessageInput />
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col overflow-auto">
      <ChatHeader />

      <div ref={scrollContainerRef} className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((message) => (
          <div
            key={message._id}
            className={`chat ${message.senderId === authUser._id ? "chat-end" : "chat-start"} ${isActiveResult(message._id) ? "bg-base-300/50 p-2 rounded" : ""}`}
            ref={isActiveResult(message._id) ? searchResultRef : null}
          >
            <div className=" chat-image avatar">
              <div className="size-10 rounded-full border">
                <img
                  src={
                    message.senderId === authUser._id
                      ? authUser.profilePic || "/noprofile.png"
                      : selectedUser.profilePic || "/noprofile.png"
                  }
                  alt="profile pic"
                />
              </div>
            </div>
            <div className="chat-bubble flex flex-col gap-1">
              {(() => {
                const mediaUrl = message.mediaId
                  ? getMediaUrl(message.mediaId)
                  : null;
                const previewUrl = message.previewUrl || null;
                const src = mediaUrl || previewUrl;
                return src ? (
                  <img
                    src={src}
                    alt="Attachment"
                    className="sm:max-w-50 rounded-md"
                  />
                ) : null;
              })()}
              {message.text && (
                <div className="flex items-end gap-2">
                  <p className="whitespace-pre-line wrap-break-words leading-relaxed">
                    {highlightText(message.text)}
                  </p>
                  <span className="text-[11px] opacity-60 leading-tight">{formatMessageTime(message.createdAt)}</span>
                </div>
              )}
              {!message.text && (
                <span className="text-[11px] opacity-60 self-end">{formatMessageTime(message.createdAt)}</span>
              )}
            </div>
          </div>
        ))}
        {/* Sentinel to keep view anchored to latest message */}
        <div ref={messageEndRef} />
      </div>

      <MessageInput />
    </div>
  );
};
export default ChatContainer;
