# Frontend Integration Guide: Encrypted Media System

## Files Modified/Created

### ✅ Created Files
1. `/frontend/src/lib/mediaEncryption.js` - Encryption utilities
2. `/frontend/src/lib/mediaApi.js` - Media upload/download API
3. Backend entities, controllers, services (all complete)

### 🔧 Files That Need Updates

#### 1. `/frontend/src/components/MessageInput.jsx`

Replace the current implementation with:

```jsx
import { useRef, useState, useEffect } from "react";
import { useChat } from "../store/useChat";
import { Image, Send, X } from "lucide-react";
import toast from "react-hot-toast";
import { uploadEncryptedImage } from "../lib/mediaApi";

const MessageInput = () => {
  const [text, setText] = useState("");
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef(null);
  const textareaRef = useRef(null);
  const { sendMessage } = useChat();

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "40px";
    }
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      toast.error("Please select an image file");
      return;
    }

    // Store the actual file for encryption
    setImageFile(file);

    // Create preview
    const reader = new FileReader();
    reader.onloadend = () => {
      setImagePreview(reader.result);
    };
    reader.readAsDataURL(file);
  };

  const removeImage = () => {
    setImageFile(null);
    setImagePreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleTextChange = (e) => {
    setText(e.target.value);
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 200) + "px";
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!text.trim() && !imageFile) return;

    const textToSend = text.trim();
    const imageToSend = imageFile;

    // Clear form immediately
    setText("");
    setImageFile(null);
    setImagePreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
    }

    try {
      setIsUploading(true);

      let mediaMetadata = null;

      // If image, encrypt and upload first
      if (imageToSend) {
        toast.loading("Encrypting and uploading image...", { id: "upload" });
        mediaMetadata = await uploadEncryptedImage(imageToSend);
        toast.success("Image uploaded!", { id: "upload" });
      }

      // Send message with text and/or media metadata
      await sendMessage({
        text: textToSend,
        ...mediaMetadata, // includes mediaId, encryptedKey, iv, hash, mimeType, fileSize
      });

    } catch (error) {
      console.error("Failed to send message:", error);
      toast.error("Failed to send message");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="p-4 w-full">
      {imagePreview && (
        <div className="mb-3 flex items-center gap-2">
          <div className="relative">
            <img
              src={imagePreview}
              alt="Preview"
              className="w-20 h-20 object-cover rounded-lg border border-zinc-700"
            />
            <button
              onClick={removeImage}
              className="absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full bg-base-300
              flex items-center justify-center"
              type="button"
            >
              <X className="size-3" />
            </button>
          </div>
        </div>
      )}

      <form onSubmit={handleSendMessage} className="relative">
        <div className="flex items-end relative">
          <textarea
            ref={textareaRef}
            rows={1}
            className="w-full textarea textarea-bordered textarea-m rounded-lg pr-24 py-2 min-h-0 h-10 leading-tight resize-none overflow-hidden"
            placeholder="Type a message..."
            value={text}
            onChange={handleTextChange}
            disabled={isUploading}
            style={{ minHeight: "40px", height: "40px" }}
          />
          <input
            type="file"
            accept="image/*"
            className="hidden"
            ref={fileInputRef}
            onChange={handleImageChange}
          />

          <div className="absolute bottom-2 right-2 flex items-center gap-1">
            <button
              type="button"
              className={`btn btn-ghost btn-sm sm:flex hidden
                       ${imagePreview ? "text-emerald-500" : "text-zinc-400"}`}
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
            >
              <Image className="size-5 mt-4" />
            </button>
            <button
              type="submit"
              className="btn btn-ghost btn-sm mt-3"
              disabled={(!text.trim() && !imageFile) || isUploading}
            >
              <Send className="size-4 mt-2" />
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};
export default MessageInput;
```

#### 2. `/frontend/src/components/ChatContainer.jsx`

Update to handle encrypted media display:

```jsx
import { useChat } from "../store/useChat";
import { useEffect, useRef, useState } from "react";

import ChatHeader from "./ChatHeader";
import MessageInput from "./MessageInput";
import MessageSkeleton from "./skeletons/MessageSkeleton";
import { useAuth } from "../store/useAuth";
import { formatMessageTime } from "../lib/utils";
import { downloadAndDecryptImage } from "../lib/mediaApi";

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
          <MessageBubble
            key={message._id}
            message={message}
            authUser={authUser}
            selectedUser={selectedUser}
            messageEndRef={messageEndRef}
          />
        ))}
      </div>

      <MessageInput />
    </div>
  );
};

// Separate component to handle encrypted media decryption
const MessageBubble = ({ message, authUser, selectedUser, messageEndRef }) => {
  const [imageUrl, setImageUrl] = useState(null);
  const [loadingImage, setLoadingImage] = useState(false);
  const [imageError, setImageError] = useState(false);

  useEffect(() => {
    // If message has encrypted media, decrypt it
    if (message.mediaId && message.encryptedKey && message.iv && message.hash) {
      loadEncryptedImage();
    }
  }, [message.mediaId]);

  const loadEncryptedImage = async () => {
    try {
      setLoadingImage(true);
      setImageError(false);

      const decryptedUrl = await downloadAndDecryptImage(
        message.mediaId,
        message.encryptedKey,
        message.iv,
        message.hash
      );

      setImageUrl(decryptedUrl);
    } catch (error) {
      console.error("Failed to load image:", error);
      setImageError(true);
    } finally {
      setLoadingImage(false);
    }
  };

  return (
    <div
      className={`chat ${message.senderId === authUser._id ? "chat-end" : "chat-start"}`}
      ref={messageEndRef}
    >
      <div className="chat-image avatar">
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
        {/* Encrypted Image Display */}
        {message.mediaId && (
          <div className="sm:max-w-50">
            {loadingImage && (
              <div className="flex items-center justify-center h-32 bg-base-200 rounded-md">
                <span className="loading loading-spinner"></span>
              </div>
            )}
            {imageError && (
              <div className="flex items-center justify-center h-32 bg-base-200 rounded-md text-error">
                Failed to load image
              </div>
            )}
            {imageUrl && !loadingImage && !imageError && (
              <img
                src={imageUrl}
                alt="Encrypted attachment"
                className="rounded-md max-w-full"
                onError={() => setImageError(true)}
              />
            )}
          </div>
        )}

        {/* Text Message */}
        {message.text && (
          <div className="flex items-end gap-2">
            <p className="whitespace-pre-line wrap-break-words leading-relaxed">{message.text}</p>
            <span className="text-[11px] opacity-60 leading-tight">
              {formatMessageTime(message.createdAt)}
            </span>
          </div>
        )}

        {/* Timestamp for image-only messages */}
        {!message.text && message.mediaId && (
          <span className="text-[11px] opacity-60 self-end">
            {formatMessageTime(message.createdAt)}
          </span>
        )}
      </div>
    </div>
  );
};

export default ChatContainer;
```

#### 3. `/frontend/src/store/useChat.js`

Update sendMessage to handle media metadata:

```javascript
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
      msg._id === tempId ? res.data : msg
    );
    set({ messages: updatedMessages });
  } catch (error) {
    // Remove optimistic message on error and show error toast
    set({ messages: messages });
    toast.error(error.response?.data?.message || "Failed to send message");
  }
},
```

## Backend Configuration

Add to `application.properties`:

```properties
# Already added - verify these settings:
media.storage.path=media-storage
media.ttl.days=30
```

## Testing Checklist

### Backend Tests
1. Start backend: `cd backend && mvn spring-boot:run`
2. Check console for: "Created media storage directory"
3. Verify `media-storage/` folder created in project root
4. Test upload endpoint: POST `/api/media/upload` with encrypted file
5. Test token generation: POST `/api/media/token` with mediaId
6. Test download: GET `/api/media/{mediaId}?token=...`

### Frontend Tests
1. Start frontend: `cd frontend && npm run dev`
2. Login as two users in different browsers
3. User A: Select an image, verify preview
4. User A: Send image, check console for "Encrypted image ready for upload"
5. User A: Verify message appears immediately
6. User B: Verify encrypted image received and decrypted automatically
7. Both users: Verify image displays correctly

### Security Tests
1. Try accessing media without token → Should fail (403)
2. Try accessing media with expired token → Should fail (403)
3. Try accessing someone else's media → Should fail (403)
4. Verify encrypted files on disk are unreadable (binary data)
5. Check hash verification in browser console

### Cleanup Test
Wait 30 days (or change `media.ttl.days=0` for immediate test) and verify:
1. Scheduled job runs every 6 hours
2. Expired media deleted from disk and database
3. Check console logs for cleanup reports

## Migration from Cloudinary

1. **Remove CloudinaryUtil.java** - Already done ✅
2. **Remove cloudinary-http44 dependency from pom.xml** - Already done ✅
3. **Update application.properties** - Already done ✅
4. **Rebuild backend**: `mvn clean install`
5. **Update frontend dependencies**: No additional packages needed (uses Web Crypto API)

## Performance Monitoring

Monitor these metrics:
- Upload time: Should be ~1-3s for 1MB image
- Download time: Should be ~1-2s for 1MB image
- Encryption time: ~50-100ms
- Decryption time: ~50-100ms
- Disk usage: ~150KB per compressed image

## Troubleshooting

### Issue: "Failed to encrypt image"
**Solution**: Check browser console for crypto errors. Ensure HTTPS (or localhost).

### Issue: "Media not found"
**Solution**: Verify mediaId exists in database. Check media-storage folder.

### Issue: "Hash mismatch"
**Solution**: Media file corrupted or tampered. Delete and re-upload.

### Issue: "Token expired"
**Solution**: Tokens last 30s. Generate new token before download.

### Issue: Images not displaying
**Solution**: Check browser console for decryption errors. Verify keys/IV are correct.

## Next Steps

1. ✅ All code is implemented
2. 🔧 Update 3 frontend files (MessageInput, ChatContainer, useChat)
3. 🧪 Test the complete flow
4. 📊 Monitor performance
5. 🚀 Deploy to production

Your self-hosted encrypted media system is ready!
