import dotenv from 'dotenv';
dotenv.config();

import express from 'express'; // webframework for building APIs ie routes middle layers etc
import authRoutes from './routes/auth.js';
import { connectDB } from './utils/db.js';
import cookieParser from 'cookie-parser';
import messageRoutes from './routes/message.js';
import cors from 'cors';

const app = express();
const PORT = process.env.PORT;

app.use(express.json()); //middleware to parse json bodies

// middleware to parse cookies
app.use(cookieParser());
app.use(cors({
  origin: 'http://localhost:5173', // frontend origin
  credentials: true, // allow cookies to be sent
}));

app.use("/api/auth", authRoutes);
app.use("/api/message", messageRoutes);

app.listen(PORT, () => {
  console.log("Server is running on port:" + PORT);
  connectDB()
});