/**
 * Media Encryption Utilities
 * 
 * Provides client-side encryption/decryption for images using AES-256-GCM.
 * Implements WhatsApp-style media security with:
 * - Random key generation per image
 * - AES-256-GCM encryption
 * - SHA-256 hash verification
 * - Web Crypto API (native, fast)
 */

/**
 * Generates a random AES-256 key
 * @returns {Promise<CryptoKey>} AES-GCM key
 */
export const generateEncryptionKey = async () => {
  return await window.crypto.subtle.generateKey(
    {
      name: "AES-GCM",
      length: 256, // 256-bit key
    },
    true, // extractable (so we can export it)
    ["encrypt", "decrypt"]
  );
};

/**
 * Generates a random 96-bit IV (Initialization Vector)
 * @returns {Uint8Array} Random IV
 */
export const generateIV = () => {
  return window.crypto.getRandomValues(new Uint8Array(12)); // 96 bits for GCM
};

/**
 * Encrypts data using AES-256-GCM
 * @param {ArrayBuffer} data - Data to encrypt
 * @param {CryptoKey} key - AES key
 * @param {Uint8Array} iv - Initialization vector
 * @returns {Promise<ArrayBuffer>} Encrypted data with auth tag
 */
export const encryptData = async (data, key, iv) => {
  return await window.crypto.subtle.encrypt(
    {
      name: "AES-GCM",
      iv: iv,
      tagLength: 128, // 128-bit authentication tag
    },
    key,
    data
  );
};

/**
 * Decrypts data using AES-256-GCM
 * @param {ArrayBuffer} encryptedData - Data to decrypt (includes auth tag)
 * @param {CryptoKey} key - AES key
 * @param {Uint8Array} iv - Initialization vector
 * @returns {Promise<ArrayBuffer>} Decrypted data
 * @throws {Error} If decryption fails or auth tag is invalid
 */
export const decryptData = async (encryptedData, key, iv) => {
  return await window.crypto.subtle.decrypt(
    {
      name: "AES-GCM",
      iv: iv,
      tagLength: 128,
    },
    key,
    encryptedData
  );
};

/**
 * Exports a CryptoKey to raw bytes (ArrayBuffer)
 * @param {CryptoKey} key - Key to export
 * @returns {Promise<ArrayBuffer>} Raw key bytes
 */
export const exportKey = async (key) => {
  return await window.crypto.subtle.exportKey("raw", key);
};

/**
 * Imports raw key bytes into a CryptoKey
 * @param {ArrayBuffer} keyData - Raw key bytes
 * @returns {Promise<CryptoKey>} Imported key
 */
export const importKey = async (keyData) => {
  return await window.crypto.subtle.importKey(
    "raw",
    keyData,
    {
      name: "AES-GCM",
      length: 256,
    },
    true,
    ["encrypt", "decrypt"]
  );
};

/**
 * Computes SHA-256 hash of data
 * @param {ArrayBuffer} data - Data to hash
 * @returns {Promise<string>} Hex-encoded hash
 */
export const computeHash = async (data) => {
  const hashBuffer = await window.crypto.subtle.digest("SHA-256", data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
};

/**
 * Converts ArrayBuffer to Base64 string
 * @param {ArrayBuffer} buffer - Buffer to encode
 * @returns {string} Base64 string
 */
export const arrayBufferToBase64 = (buffer) => {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window.btoa(binary);
};

/**
 * Converts Base64 string to ArrayBuffer
 * @param {string} base64 - Base64 string
 * @returns {ArrayBuffer} Decoded buffer
 */
export const base64ToArrayBuffer = (base64) => {
  const binary = window.atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
};

/**
 * Compresses an image file before encryption
 * Uses canvas API to reduce size (WebP or JPEG at 85% quality)
 * @param {File} imageFile - Image file to compress
 * @param {number} maxWidth - Maximum width (default: 1920)
 * @param {number} quality - Quality 0-1 (default: 0.85)
 * @returns {Promise<Blob>} Compressed image blob
 */
export const compressImage = async (imageFile, maxWidth = 1920, quality = 0.85) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onload = (e) => {
      const img = new Image();
      
      img.onload = () => {
        // Calculate dimensions
        let width = img.width;
        let height = img.height;
        
        if (width > maxWidth) {
          height = (height * maxWidth) / width;
          width = maxWidth;
        }
        
        // Create canvas
        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        
        // Draw and compress
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, width, height);
        
        // Convert to blob (try WebP, fallback to JPEG)
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob);
            } else {
              reject(new Error("Failed to compress image"));
            }
          },
          "image/webp", // WebP has better compression
          quality
        );
      };
      
      img.onerror = () => reject(new Error("Failed to load image"));
      img.src = e.target.result;
    };
    
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(imageFile);
  });
};

/**
 * Complete encryption workflow for an image
 * 1. Compress image
 * 2. Generate key and IV
 * 3. Encrypt compressed image
 * 4. Compute hash
 * @param {File} imageFile - Image to encrypt
 * @returns {Promise<Object>} { encryptedBlob, key (base64), iv (base64), hash, mimeType, fileSize }
 */
export const encryptImage = async (imageFile) => {
  try {
    // Step 1: Compress image
    const compressedBlob = await compressImage(imageFile);
    console.log(`Compressed: ${imageFile.size} → ${compressedBlob.size} bytes`);
    
    // Step 2: Generate encryption key and IV
    const key = await generateEncryptionKey();
    const iv = generateIV();
    
    // Step 3: Convert blob to ArrayBuffer
    const arrayBuffer = await compressedBlob.arrayBuffer();
    
    // Step 4: Encrypt
    const encryptedData = await encryptData(arrayBuffer, key, iv);
    
    // Step 5: Compute hash of encrypted data
    const hash = await computeHash(encryptedData);
    
    // Step 6: Export key to raw bytes
    const rawKey = await exportKey(key);
    
    // Step 7: Convert to base64 for storage
    const keyBase64 = arrayBufferToBase64(rawKey);
    const ivBase64 = arrayBufferToBase64(iv);
    
    // Step 8: Create encrypted blob
    const encryptedBlob = new Blob([encryptedData], { type: "application/octet-stream" });
    
    return {
      encryptedBlob,
      key: keyBase64,
      iv: ivBase64,
      hash,
      mimeType: compressedBlob.type || "image/webp",
      originalSize: imageFile.size,
      encryptedSize: encryptedBlob.size,
    };
  } catch (error) {
    console.error("Encryption failed:", error);
    throw new Error("Failed to encrypt image: " + error.message);
  }
};

/**
 * Complete decryption workflow for an encrypted image
 * @param {ArrayBuffer} encryptedData - Encrypted image data
 * @param {string} keyBase64 - Base64-encoded key
 * @param {string} ivBase64 - Base64-encoded IV
 * @param {string} expectedHash - Expected SHA-256 hash for verification
 * @returns {Promise<Blob>} Decrypted image blob
 * @throws {Error} If hash doesn't match or decryption fails
 */
export const decryptImage = async (encryptedData, keyBase64, ivBase64, expectedHash) => {
  try {
    // Step 1: Verify hash
    const actualHash = await computeHash(encryptedData);
    if (actualHash !== expectedHash) {
      throw new Error("Hash mismatch - data may be corrupted or tampered");
    }
    
    // Step 2: Convert base64 to ArrayBuffer
    const keyBuffer = base64ToArrayBuffer(keyBase64);
    const ivBuffer = base64ToArrayBuffer(ivBase64);
    
    // Step 3: Import key
    const key = await importKey(keyBuffer);
    const iv = new Uint8Array(ivBuffer);
    
    // Step 4: Decrypt
    const decryptedData = await decryptData(encryptedData, key, iv);
    
    // Step 5: Create blob
    const blob = new Blob([decryptedData], { type: "image/webp" });
    
    return blob;
  } catch (error) {
    console.error("Decryption failed:", error);
    throw new Error("Failed to decrypt image: " + error.message);
  }
};

/**
 * Creates an object URL from a decrypted blob for display
 * @param {Blob} blob - Decrypted image blob
 * @returns {string} Object URL
 */
export const createImageURL = (blob) => {
  return URL.createObjectURL(blob);
};

/**
 * Revokes an object URL to free memory
 * @param {string} url - Object URL to revoke
 */
export const revokeImageURL = (url) => {
  URL.revokeObjectURL(url);
};
