import express from 'express';
import { Gatekeeper } from '../middleware/auth.js';
import { SidebarUsers, getDM, DM } from '../controllers/message.js';

const router = express.Router();

 //show users name in sidebar
router.get("/users", Gatekeeper, SidebarUsers);

// get messages between two users
router.get("/:DmId", Gatekeeper, getDM);

// send messages
router.post("/send/:DmId", Gatekeeper, DM);

export default router;