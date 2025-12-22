import express from 'express';
import { register, login, logout, profile, authed } from '../controllers/auth.js';
import { Gatekeeper } from '../middleware/auth.js';

const router = express.Router();

router.post('/register', register);
router.post('/login', login);
router.post('/logout', logout);
router.put('/profile', Gatekeeper, profile);
router.get('/pulse', Gatekeeper, authed); // to check if user is authenticated

export default router;