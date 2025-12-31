/**
 * Media API Utilities
 * 
 * Handles upload/download of encrypted media files with token-based authentication.
 */

import { axiosInstance } from "./axios";
import { encryptImage, decryptImage, createImageURL } from "./mediaEncryption";

/**
 * Uploads an encrypted image to the server
 * @param {File} imageFile - Image file to upload
 * @returns {Promise<Object>} { mediaId, encryptedKey, iv, hash, mimeType, fileSize }
 */
export const uploadEncryptedImage = async (imageFile) => {
  try {
    // Step 1: Encrypt the image
    const { encryptedBlob, key, iv, hash, mimeType, encryptedSize } = await encryptImage(imageFile);
    
    console.log("Encrypted image ready for upload:", {
      originalSize: imageFile.size,
      encryptedSize,
      hash: hash.substring(0, 16) + "...",
    });
    
    // Step 2: Upload encrypted blob
    const formData = new FormData();
    formData.append("file", encryptedBlob, "encrypted.bin");
    formData.append("mimeType", mimeType);
    formData.append("hash", hash);
    
    const response = await axiosInstance.post("/media/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    
    // Step 3: Return metadata to store in message
    return {
      mediaId: response.data.mediaId,
      encryptedKey: key, // base64
      iv: iv, // base64
      hash: hash,
      mimeType: mimeType,
      fileSize: encryptedSize,
    };
  } catch (error) {
    console.error("Upload failed:", error);
    throw new Error("Failed to upload image: " + (error.response?.data?.message || error.message));
  }
};

/**
 * Generates a download token for a specific media file
 * @param {string} mediaId - Media ID to download
 * @returns {Promise<string>} Download token (JWT)
 */
export const generateDownloadToken = async (mediaId) => {
  try {
    const response = await axiosInstance.post("/media/token", { mediaId });
    return response.data.token;
  } catch (error) {
    console.error("Token generation failed:", error);
    throw new Error("Failed to generate download token");
  }
};

/**
 * Downloads and decrypts an image
 * @param {string} mediaId - Media ID
 * @param {string} encryptedKey - Base64-encoded encryption key
 * @param {string} iv - Base64-encoded IV
 * @param {string} hash - Expected SHA-256 hash
 * @returns {Promise<string>} Object URL of decrypted image
 */
export const downloadAndDecryptImage = async (mediaId, encryptedKey, iv, hash) => {
  try {
    // Step 1: Generate download token
    const token = await generateDownloadToken(mediaId);
    
    // Step 2: Download encrypted blob
    const response = await axiosInstance.get(`/media/${mediaId}`, {
      params: { token },
      responseType: "arraybuffer",
    });
    
    console.log("Downloaded encrypted image:", {
      size: response.data.byteLength,
      mediaId,
    });
    
    // Step 3: Decrypt
    const decryptedBlob = await decryptImage(response.data, encryptedKey, iv, hash);
    
    // Step 4: Create object URL
    const imageUrl = createImageURL(decryptedBlob);
    
    return imageUrl;
  } catch (error) {
    console.error("Download/decrypt failed:", error);
    throw new Error("Failed to load image: " + (error.response?.data?.message || error.message));
  }
};

/**
 * Checks if media is available (for UI loading states)
 * @param {string} mediaId - Media ID to check
 * @returns {Promise<boolean>} True if media exists
 */
export const checkMediaExists = async (mediaId) => {
  try {
    const response = await axiosInstance.head(`/media/${mediaId}`);
    return response.status === 200;
  } catch {
    return false;
  }
};

/**
 * Deletes media (admin/owner only)
 * @param {string} mediaId - Media ID to delete
 * @returns {Promise<void>}
 */
export const deleteMedia = async (mediaId) => {
  try {
    await axiosInstance.delete(`/media/${mediaId}`);
    console.log("Media deleted:", mediaId);
  } catch (error) {
    console.error("Delete failed:", error);
    throw new Error("Failed to delete media");
  }
};
