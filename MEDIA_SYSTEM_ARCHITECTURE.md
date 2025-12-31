# Self-Hosted Encrypted Media System Architecture

## System Overview
A privacy-first, WhatsApp-style media handling system with client-side encryption, no third-party dependencies, and automatic cleanup.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Browser)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  1. User Selects Image                                           │
│     ↓                                                             │
│  2. Compress Image (WebP/JPEG quality 85%)                       │
│     ↓                                                             │
│  3. Generate Random 256-bit AES Key + 96-bit IV                  │
│     ↓                                                             │
│  4. Encrypt Image → AES-256-GCM                                  │
│     ↓                                                             │
│  5. Compute SHA-256 Hash                                         │
│     ↓                                                             │
│  6. Upload Encrypted Blob via REST (/api/media/upload)          │
│     │                                                             │
│     └─────────────────────────────────────────────────┐          │
│                                                        │          │
│  ┌─────────────────────────────────────────────────────┘         │
│  │                                                                │
│  7. Receive mediaId from server                                  │
│     ↓                                                             │
│  8. Store metadata in message:                                   │
│     {                                                             │
│       mediaId: "uuid",                                            │
│       encryptedKey: base64(encrypted_aes_key),                   │
│       iv: base64(iv),                                             │
│       hash: "sha256_hex"                                          │
│     }                                                             │
│     ↓                                                             │
│  9. Send Message via WebSocket                                   │
│                                                                   │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                       SERVER (Spring Boot)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  UPLOAD FLOW:                                                    │
│  ────────────                                                    │
│  1. Receive encrypted blob                                       │
│  2. Generate unique mediaId (UUID)                               │
│  3. Save encrypted blob to disk:                                 │
│     /media-storage/{mediaId}.enc                                 │
│  4. Store metadata in MongoDB:                                   │
│     {                                                             │
│       mediaId, uploaderId, fileSize,                             │
│       mimeType, createdAt, expiresAt                             │
│     }                                                             │
│  5. Return mediaId to client                                     │
│                                                                   │
│  DOWNLOAD FLOW:                                                  │
│  ────────────                                                    │
│  1. Client requests: GET /api/media/{mediaId}?token=jwt          │
│  2. Validate token (30-second TTL)                               │
│  3. Check user has access (sent/received the message)            │
│  4. Stream encrypted blob from disk                              │
│  5. Client decrypts using stored key + IV                        │
│                                                                   │
│  CLEANUP JOB (Scheduled):                                        │
│  ─────────────────────────                                       │
│  1. Run every 6 hours                                            │
│  2. Find media where expiresAt < now                             │
│  3. Delete file from disk                                        │
│  4. Delete metadata from database                                │
│                                                                   │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                    STORAGE (Local Disk)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  /media-storage/                                                 │
│    ├── a1b2c3d4-uuid.enc  (encrypted blob)                       │
│    ├── e5f6g7h8-uuid.enc                                         │
│    └── ...                                                       │
│                                                                   │
│  MongoDB: media collection                                       │
│    ├── { mediaId, uploaderId, fileSize, ... }                    │
│    └── ...                                                       │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Media Flow Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│                         UPLOAD FLOW                                │
└────────────────────────────────────────────────────────────────────┘

[User Selects Image]
        │
        ↓
[Compress: canvas API → WebP/JPEG 85% quality]
        │
        ↓
[Generate Crypto Keys]
    • AES-256 key (random 256 bits)
    • IV (random 96 bits)
        │
        ↓
[Encrypt: AES-256-GCM]
    plainImage → encryptedBlob + authTag
        │
        ↓
[Compute Hash]
    SHA-256(encryptedBlob)
        │
        ↓
[HTTP POST /api/media/upload]
    Content-Type: application/octet-stream
    Body: encryptedBlob
        │
        ↓
[Server: Save to disk]
    /media-storage/{uuid}.enc
        │
        ↓
[Server: Store metadata in DB]
    { mediaId, uploaderId, fileSize, mimeType, createdAt, expiresAt }
        │
        ↓
[Server Response]
    { "mediaId": "uuid" }
        │
        ↓
[Client: Prepare message]
    {
      text: "...",
      mediaId: "uuid",
      encryptedKey: base64(aes_key),
      iv: base64(iv),
      hash: "sha256_hex"
    }
        │
        ↓
[Send via WebSocket]


┌────────────────────────────────────────────────────────────────────┐
│                        DOWNLOAD FLOW                               │
└────────────────────────────────────────────────────────────────────┘

[Message Received with mediaId]
        │
        ↓
[Generate Download Token]
    JWT: { mediaId, userId, exp: 30s }
        │
        ↓
[HTTP GET /api/media/{mediaId}?token=jwt]
        │
        ↓
[Server: Validate Token]
    • Check signature
    • Check expiry (30s)
    • Verify user access
        │
        ↓
[Server: Stream encrypted blob]
    Read /media-storage/{mediaId}.enc
        │
        ↓
[Client: Receive encrypted blob]
        │
        ↓
[Extract keys from message]
    • encryptedKey (base64 → ArrayBuffer)
    • iv (base64 → ArrayBuffer)
        │
        ↓
[Decrypt: AES-256-GCM]
    encryptedBlob → plainImage
        │
        ↓
[Verify Hash]
    SHA-256(encryptedBlob) === stored hash
        │
        ↓
[Display Image]
    <img src={objectURL} />
```

## Database Schema

### Messages Collection (MongoDB)

```javascript
{
  _id: ObjectId("..."),
  text: String,              // Optional message text
  senderId: String,          // User who sent the message
  receiverId: String,        // User who receives the message
  
  // Media metadata (null if no media)
  mediaId: String,           // UUID of encrypted media file
  encryptedKey: String,      // Base64-encoded AES key (encrypted with recipient's public key if E2E)
  iv: String,                // Base64-encoded IV for AES-GCM
  hash: String,              // SHA-256 hash of encrypted blob
  mimeType: String,          // Original MIME type (image/jpeg, image/png, etc.)
  fileSize: Number,          // Size of encrypted blob in bytes
  
  createdAt: ISODate,
  updatedAt: ISODate
}
```

### Media Collection (MongoDB)

```javascript
{
  _id: ObjectId("..."),
  mediaId: String,           // UUID - primary identifier (indexed, unique)
  uploaderId: String,        // User who uploaded the media
  fileName: String,          // Original filename (optional)
  mimeType: String,          // MIME type
  fileSize: Number,          // Size in bytes
  storagePath: String,       // Relative path: media-storage/{mediaId}.enc
  
  createdAt: ISODate,        // When uploaded
  expiresAt: ISODate,        // TTL for automatic deletion (default: 30 days)
  
  // Access control
  accessedBy: [String],      // Array of userIds who accessed this media
  lastAccessedAt: ISODate,   // Last download time
}
```

### Indexes

```javascript
// Messages
db.messages.createIndex({ senderId: 1, receiverId: 1 });
db.messages.createIndex({ createdAt: -1 });
db.messages.createIndex({ mediaId: 1 }, { sparse: true });

// Media
db.media.createIndex({ mediaId: 1 }, { unique: true });
db.media.createIndex({ uploaderId: 1 });
db.media.createIndex({ expiresAt: 1 }); // For TTL-based cleanup
db.media.createIndex({ createdAt: -1 });
```

## Security Considerations

### ✅ CORRECT Approach

1. **Client-Side Encryption**
   - Generate random keys in the browser using `crypto.getRandomValues()`
   - Use Web Crypto API for AES-256-GCM encryption
   - Never send unencrypted images to the server

2. **Key Management**
   - Each image gets a unique AES key
   - Keys are stored in message metadata (base64-encoded)
   - For true E2E: encrypt the AES key with recipient's public key

3. **Token-Based Downloads**
   - Generate short-lived JWT (30s expiry)
   - Include mediaId and userId in token
   - Validate token on every download request

4. **Access Control**
   - Only sender and receiver can access media
   - Check message ownership before generating download token
   - Log all access attempts

5. **Hash Verification**
   - Compute SHA-256 of encrypted blob
   - Store hash in message metadata
   - Verify on download to detect tampering

### ❌ COMMON MISTAKES TO AVOID

1. **DON'T store unencrypted images**
   - ❌ `image.jpg` → ✅ `uuid.enc`
   
2. **DON'T use predictable filenames**
   - ❌ `user123_image.jpg` → ✅ random UUID
   
3. **DON'T allow direct file access**
   - ❌ `https://server.com/media/image.jpg`
   - ✅ `https://server.com/api/media/{id}?token=...`

4. **DON'T skip compression**
   - Encrypt large files = storage waste
   - Compress before encryption

5. **DON'T reuse IVs**
   - Generate new IV for every encryption
   - IV collision breaks GCM security

6. **DON'T forget cleanup**
   - Media accumulates quickly
   - Implement TTL-based deletion

7. **DON'T trust client-provided hash**
   - Always recompute on server for audit logs
   - But don't decrypt to verify content

8. **DON'T use weak RNG**
   - ❌ `Math.random()`
   - ✅ `crypto.getRandomValues()`

## Why This Beats Cloudinary

| Feature | Cloudinary | Self-Hosted Encrypted |
|---------|-----------|----------------------|
| **Privacy** | ❌ Third-party can view images | ✅ Server never sees plaintext |
| **Compliance** | ❌ Data leaves your infrastructure | ✅ GDPR/HIPAA compliant |
| **Cost** | ❌ Pay per GB + transformations | ✅ Only disk + bandwidth costs |
| **Control** | ❌ Vendor lock-in, ToS changes | ✅ Full control over storage |
| **Security** | ❌ Cloudinary has decryption keys | ✅ Only sender/receiver can decrypt |
| **Audit** | ❌ Limited access logs | ✅ Full audit trail |
| **Uptime** | ❌ Dependent on third party | ✅ You control availability |
| **Data Residency** | ❌ Data stored in Cloudinary's region | ✅ Data never leaves your servers |

### Use Case Comparison

**When to Use Cloudinary:**
- Public content (blogs, marketing)
- Need CDN edge caching
- Want image transformations (resize, crop, filters)
- Don't care about privacy

**When to Use Self-Hosted Encrypted:**
- Private messaging (WhatsApp, Signal)
- Healthcare/financial apps (HIPAA/PCI compliance)
- Government/military applications
- Any scenario where data privacy is critical
- Want to avoid vendor lock-in
- Need true end-to-end encryption

### Real-World Example: WhatsApp
WhatsApp uses this exact approach:
1. Client encrypts media before upload
2. Server stores encrypted blobs
3. Server never has decryption keys
4. Media expires after 30 days
5. Download requires authentication token
6. Meta (WhatsApp's owner) **cannot** view your images

Your implementation will follow the same security model.

## Performance Characteristics

| Operation | Time | Notes |
|-----------|------|-------|
| Image Compression | ~200-500ms | 1MB image → 150KB WebP |
| AES-256-GCM Encryption | ~50-100ms | Web Crypto API (native) |
| Upload (1MB) | ~500ms-2s | Depends on bandwidth |
| Download (1MB) | ~500ms-2s | Depends on bandwidth |
| Decryption | ~50-100ms | Web Crypto API (native) |
| Hash Verification | ~30-50ms | SHA-256 computation |

**Total Latency (Upload + Send):** ~1-3 seconds for 1MB image
**Total Latency (Receive + Decrypt):** ~1-2 seconds for 1MB image

This is comparable to WhatsApp's performance.

## Storage Requirements

**Assumptions:**
- Average image: 150KB (after compression)
- 1000 active users
- 10 images/user/day
- 30-day retention

**Daily Storage:** 1000 × 10 × 150KB = 1.5GB/day
**Monthly Storage:** 1.5GB × 30 = 45GB/month

**Disk Requirements:**
- SSD recommended (faster I/O)
- RAID 1 for redundancy
- ~100GB for 1000 users/month

Compare to Cloudinary:
- 45GB × $0.10/GB = **$4.50/month** (Cloudinary pricing)
- Self-hosted: **$0** (you own the disk)

## Deployment Checklist

- [ ] Configure media storage path in `application.properties`
- [ ] Ensure directory has write permissions
- [ ] Set up scheduled cleanup job (cron)
- [ ] Configure JWT secret for download tokens
- [ ] Set media TTL (default: 30 days)
- [ ] Monitor disk usage
- [ ] Set up backup strategy for encrypted media
- [ ] Configure CORS for media upload endpoint
- [ ] Test encryption/decryption in different browsers
- [ ] Load test with concurrent uploads

## Next Steps

1. Implement frontend crypto utilities
2. Create backend media storage service
3. Update Message entity to store metadata
4. Remove Cloudinary dependencies
5. Test end-to-end flow
6. Monitor performance and storage usage
