import { X, Menu, Phone, Video, Info, Search, ChevronUp, ChevronDown } from "lucide-react";
import { useAuth } from "../store/useAuth";
import { useChat } from "../store/useChat";
import { useState } from "react";
import { useMessageSearch } from "../hooks/useMessageSearch";

const ChatHeader = () => {
  const { selectedUser } = useChat();
  const { onlineUsers } = useAuth();
  const {
    messageSearchTerm,
    setMessageSearchTerm,
    filteredMessages,
    handlePrevious,
    handleNext,
    currentResultNumber,
    resultCount,
  } = useMessageSearch();
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchActive, setSearchActive] = useState(false);

  if (!selectedUser) return null;

  return (
    <div className="border-b border-base-300 p-2.5">
      <div className="flex items-center justify-between gap-3">
        {/* Left: Avatar & User Info or Search */}
        {searchActive ? (
          <div className="flex items-center gap-2 flex-1">
            <input
              type="text"
              placeholder="Search messages..."
              value={messageSearchTerm}
              onChange={(e) => setMessageSearchTerm(e.target.value)}
              autoFocus
              className="input input-bordered input-sm flex-1 max-w-xs"
            />
            {messageSearchTerm && filteredMessages.length > 0 && (
              <div className="flex items-center gap-1">
                <span className="text-xs font-semibold">
                  {currentResultNumber} / {resultCount}
                </span>
                <button
                  onClick={handlePrevious}
                  className="btn btn-ghost btn-xs btn-circle"
                  title="Previous result"
                >
                  <ChevronUp size={16} />
                </button>
                <button
                  onClick={handleNext}
                  className="btn btn-ghost btn-xs btn-circle"
                  title="Next result"
                >
                  <ChevronDown size={16} />
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-3">
            {/* Avatar */}
            <div className="avatar">
              <div className="size-10 rounded-full relative">
                <img src={selectedUser.profilePic || "/noprofile.png"} alt={selectedUser.fullname} />
              </div>
            </div>

            {/* User info */}
            <div>
              <h3 className="font-medium">{selectedUser.fullname}</h3>
              <p className="text-sm text-base-content/70">
                {onlineUsers.includes(selectedUser._id) ? "Online" : "Offline"}
              </p>
            </div>
          </div>
        )}

        {/* Right: Menu & Close Search button */}
        <div className="flex items-center gap-2">
          {/* Menu button */}
          <div className="relative">
            <button
              onClick={() => {
                setMenuOpen(!menuOpen);
                setSearchActive(false);
              }}
              className="btn btn-ghost btn-sm btn-circle"
              title="Call, Video, or Info"
            >
              <Menu size={20} />
            </button>

            {menuOpen && (
              <div className="absolute right-0 top-12 bg-base-100 border border-base-300 rounded-lg shadow-lg z-50 w-48">
                <button
                  onClick={() => {
                    setSearchActive(true);
                    setMenuOpen(false);
                  }}
                  className="w-full text-left px-4 py-2 hover:bg-base-200 flex items-center gap-2 first:rounded-t-lg"
                >
                  <Search size={16} />
                  <span>Search</span>
                </button>
                <button
                  onClick={() => {
                    console.log("Initiating voice call with", selectedUser.username);
                    setMenuOpen(false);
                  }}
                  className="w-full text-left px-4 py-2 hover:bg-base-200 flex items-center gap-2"
                >
                  <Phone size={16} />
                  <span>Voice Call</span>
                </button>
                <button
                  onClick={() => {
                    console.log("Initiating video call with", selectedUser.username);
                    setMenuOpen(false);
                  }}
                  className="w-full text-left px-4 py-2 hover:bg-base-200 flex items-center gap-2"
                >
                  <Video size={16} />
                  <span>Video Call</span>
                </button>
                <button
                  onClick={() => {
                    console.log("Showing info for", selectedUser.username);
                    setMenuOpen(false);
                  }}
                  className="w-full text-left px-4 py-2 hover:bg-base-200 flex items-center gap-2 last:rounded-b-lg"
                >
                  <Info size={16} />
                  <span>User Info</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
export default ChatHeader;
