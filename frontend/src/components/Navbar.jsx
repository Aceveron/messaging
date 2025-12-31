import { Link } from "react-router-dom";
import { useAuth } from "../store/useAuth";
import { useChat } from "../store/useChat";
import { useMessageSearch } from "../hooks/useMessageSearch";
import { axiosInstance } from "../lib/axios";
import { LogOut, User, Menu, Loader2, Search } from "lucide-react";
import { useEffect, useRef, useState } from "react";

const Navbar = () => {
  const { logout, authUser } = useAuth();
  const { userSearchTerm, setUserSearchTerm, selectedUser, users, getUsers, setSelectedUser } = useChat();
  const {
    setMessageSearchTerm,
    resultCount,
    currentResultNumber,
    isSearching,
    setPendingMessageId,
    highlightText,
  } = useMessageSearch();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const inputRef = useRef(null);
  const [showSearch, setShowSearch] = useState(true);
  const [globalResults, setGlobalResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    if (!users?.length) {
      getUsers();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const handler = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  useEffect(() => {
    if (showSearch && inputRef.current) {
      inputRef.current.focus();
    }
  }, [showSearch]);
  return (
    <header
      className="border-b border-base-300 fixed w-full top-0 z-40 
    backdrop-blur-lg bg-base-100/80"
    >
      <div className="container mx-auto px-4 h-16">
        <div className="flex items-center justify-between h-full">
          <div className="flex items-center">
            <Link to="/" className="flex items-center gap-2.5 hover:opacity-80 transition-all">
              <div className="size-9 rounded-lg" />
            </Link>
          </div>

          <div className="flex-1 px-4 max-w-xl">
            {!showSearch ? (
              <button
                type="button"
                className="btn btn-ghost btn-sm btn-circle"
                onClick={() => {
                  setShowSearch(true);
                  setTimeout(() => inputRef.current?.focus(), 0);
                }}
                title="Search"
              >
                <Search size={18} />
              </button>
            ) : (
              <div className="relative">
                <input
                  ref={inputRef}
                  type="text"
                  value={userSearchTerm}
                  onChange={async (e) => {
                    const val = e.target.value;
                    setUserSearchTerm(val);
                    setMessageSearchTerm(val);
                    setShowDropdown(!!val.trim());

                    if (!val.trim()) {
                      setGlobalResults([]);
                      return;
                    }

                    setSearchLoading(true);
                    try {
                      const queries = users || [];
                      const results = await Promise.all(
                        queries.map(async (user) => {
                          try {
                            const res = await axiosInstance.get(`/messages/${user._id}`);
                            const matches = (res.data || []).filter((m) =>
                              (m.text || "").toLowerCase().includes(val.toLowerCase())
                            );
                            return { user, matches };
                          } catch {
                            return { user, matches: [] };
                          }
                        })
                      );
                      setGlobalResults(results.filter((r) => r.matches.length > 0));
                    } finally {
                      setSearchLoading(false);
                    }
                  }}
                  placeholder="Search chats or people"
                  className="w-full input input-sm input-bordered bg-base-200/70 text-sm pr-24"
                />
                {selectedUser && isSearching && resultCount > 0 && (
                  <span className="absolute right-12 top-1/2 -translate-y-1/2 text-xs text-base-content/70">
                    {currentResultNumber} / {resultCount}
                  </span>
                )}

                {/* Cancel search */}
                <button
                  type="button"
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-base-content/70 hover:text-base-content"
                  onClick={() => {
                    setUserSearchTerm("");
                    setMessageSearchTerm("");
                    setShowDropdown(false);
                    setGlobalResults([]);
                    setShowSearch(false);
                  }}
                >
                  Cancel
                </button>

                {showDropdown && (
                  <div className="absolute mt-2 w-full rounded-lg border border-base-400 bg-base-200 shadow-xl z-50 max-h-96 overflow-y-auto">
                    {searchLoading && (
                      <div className="p-3 flex items-center gap-2 text-sm text-base-content/70">
                        <Loader2 className="w-4 h-4 animate-spin" /> Searching all chats...
                      </div>
                    )}
                    {!searchLoading && globalResults.length === 0 && (
                      <div className="p-3 text-sm text-base-content/70">No matches found</div>
                    )}
                    {!searchLoading &&
                      globalResults.map(({ user, matches }) => (
                        <div key={user._id} className="border-b border-base-400 last:border-none">
                          <div className="px-3 py-2 text-xs font-semibold text-base-content/70">
                            {user.fullname || user.username} — {matches.length} match{matches.length > 1 ? "es" : ""}
                          </div>
                          <div className="flex flex-col">
                            {matches.slice(0, 5).map((m) => (
                              <button
                                key={m._id}
                                className="px-3 py-2 text-left hover:bg-base-300/50 text-sm"
                                onClick={() => {
                                  setSelectedUser(user);
                                  setMessageSearchTerm(userSearchTerm);
                                  setPendingMessageId(m._id);
                                  setShowDropdown(false);
                                  setShowSearch(false);
                                }}
                              >
                                <div className="truncate text-base-content">{highlightText(m.text || "(media)")}</div>
                                <div className="text-[11px] text-base-content/70">{new Date(m.createdAt).toLocaleString()}</div>
                              </button>
                            ))}
                          </div>
                        </div>
                      ))}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="relative flex items-center gap-2">
            <button
              type="button"
              className="inline-flex items-center justify-center rounded-md p-2 text-white hover:bg-white/10 focus:outline-none"
              aria-label="Open menu"
              onClick={() => setMenuOpen((o) => !o)}
            >
              <Menu className="w-5 h-5" />
            </button>

            {/* Desktop buttons removed; using dropdown only */}

            {authUser && menuOpen && (
              <div
                ref={menuRef}
                className="absolute right-0 top-12 w-40 rounded-lg border border-white/10 bg-base-200/95 shadow-xl backdrop-blur"
              >
                <div className="py-1 text-sm text-white/90">
                  <Link
                    to="/profile"
                    className="flex items-center gap-2 px-4 py-2 hover:bg-white/10"
                    onClick={() => setMenuOpen(false)}
                  >
                    <User className="w-4 h-4" />
                    Profile
                  </Link>
                  <button
                    className="w-full flex items-center gap-2 px-4 py-2 text-left hover:bg-white/10"
                    onClick={() => {
                      setMenuOpen(false);
                      logout();
                    }}
                  >
                    <LogOut className="w-4 h-4" />
                    Logout
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
export default Navbar;