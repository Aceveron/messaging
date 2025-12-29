export function formatMessageTime(date) {
  try {
    // Handle null/undefined gracefully
    if (!date) return new Date().toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false });

    // Accept both epoch millis and ISO strings
    const value = typeof date === "number" ? date : date;
    const d = new Date(value);

    // If invalid date, fall back to current time
    if (isNaN(d.getTime())) {
      return new Date().toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false });
    }

    return d.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    });
  } catch {
    return new Date().toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit", hour12: false });
  }
}
