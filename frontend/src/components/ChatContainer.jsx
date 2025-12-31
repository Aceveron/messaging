import { useChat } from "../store/useChat";
import { useEffect, useRef } from "react";

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
  } = useChat();
  const { authUser } = useAuth();
  const messageEndRef = useRef(null);

  useEffect(() => {
    if (!selectedUser?._id) return;

    getMessages(selectedUser._id);
    subscribeToMessages();

    return () => unsubscribeFromMessages();
    // We intentionally exclude functions to avoid effect churn; they are stable from zustand.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedUser?._id]);

  useEffect(() => {
    if (messageEndRef.current && messages) {
      messageEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
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

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((message) => (
          <div
            key={message._id}
            className={`chat ${message.senderId === authUser._id ? "chat-end" : "chat-start"}`}
            ref={messageEndRef}
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
              {message.image && (
                <img
                  src={message.image}
                  alt="Attachment"
                  className="sm:max-w-50 rounded-md"
                />
              )}
              {message.text && (
                <div className="flex items-end gap-2">
                  <p className="whitespace-pre-line wrap-break-words leading-relaxed">{message.text}</p>
                  <span className="text-[11px] opacity-60 leading-tight">{formatMessageTime(message.createdAt)}</span>
                </div>
              )}
              {!message.text && (
                <span className="text-[11px] opacity-60 self-end">{formatMessageTime(message.createdAt)}</span>
              )}
            </div>
          </div>
        ))}
      </div>

      <MessageInput />
    </div>
  );
};
export default ChatContainer;
