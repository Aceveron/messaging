import { useRef, useState, useEffect } from "react";
import { useChat } from "../store/useChat";
import { Image, Send, X } from "lucide-react";
import toast from "react-hot-toast";

const MessageInput = () => {
  const [text, setText] = useState("");
  const [imagePreview, setImagePreview] = useState(null);
  const fileInputRef = useRef(null);
  const textareaRef = useRef(null);
  const { sendMessage } = useChat();

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "40px"; // slightly larger base height
    }
  }, []);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file.type.startsWith("image/")) {
      toast.error("Please select an image file");
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setImagePreview(reader.result);
    };
    reader.readAsDataURL(file);
  };

  const removeImage = () => {
    setImagePreview(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleTextChange = (e) => {
    setText(e.target.value);
    // Auto-grow textarea
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 200) + "px";
    }
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!text.trim() && !imagePreview) return;

      const textToSend = text.trim();
      const imageToSend = imagePreview;

      // Clear form immediately for better UX
      setText("");
      setImagePreview(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      if (textareaRef.current) {
        textareaRef.current.style.height = "auto";
      }

      try {
      await sendMessage({
          text: textToSend,
          image: imageToSend,
      });
    } catch (error) {
      console.error("Failed to send message:", error);
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
            >
              <Image className="size-5 mt-4" />
            </button>
            <button
              type="submit"
              className="btn btn-ghost btn-sm mt-3"
              disabled={!text.trim() && !imagePreview}
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
