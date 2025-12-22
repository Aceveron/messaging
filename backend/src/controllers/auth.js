import User from '../models/user.js';
import bcrypt from 'bcryptjs';
import { generateToken } from '../lib/utils.js';
import cloudinary from '../lib/cloudinary.js';

// register
export const register = async (req, res) => {
  const { fullname, email, password } = req.body;
  try {

    if (!fullname || !email || !password) {
      return  res.status(400).json({ 
        message: 'Please provide all required fields'
      });
    }
    // hash password
    if (password.length < 6) {
      return res.status(400).json({ 
        message: 'Password must be at least 6 characters long'
      });
    }

    const user = await User.findOne({ email });
    if (user) {
      return res.status(400).json({ 
        message: 'Email already exists'
      });
    }

    const salt = await bcrypt.genSalt(10);
    const hashedPassword = await bcrypt.hash(password, salt);
    const newUser = new User({
      fullname,
      email,
      password: hashedPassword,
    });

    if (newUser) {
      // generate jwt token
      generateToken(newUser._id, res);
      await newUser.save();

      res.status(201).json({
        _id: newUser._id,
        fullname: newUser.fullname,
        email: newUser.email,
        profilePic: newUser.profilePic,
      });
    } else {
      res.status(400).json({ 
        message: 'Invalid user data'
      });
    }

  } catch (error) {
    console.log("Error in register controller:", error.message);
    res.status(500).json({ 
      message: 'Server error'
    });
  }

};

// login
export const login = async (req, res) => {
  const { email, password } = req.body;
  try {
    const user = await User.findOne({ email });
    if (!user) {
      return res.status(400).json({ 
        message: 'Invalid credentials'
      });
    }

    const isPasswordValid = await bcrypt.compare(password, user.password);
    if (!isPasswordValid) {
      return res.status(400).json({ 
        message: 'Invalid credentials'
      });
    }

    generateToken(user._id, res);
    res.status(200).json({
      _id: user._id,
      fullname: user.fullname,
      email: user.email,
      profilePic: user.profilePic,
    });

  } catch (error) {
    console.log("Error in login controller:", error.message);
    res.status(500).json({ 
      message: 'Server error'
    });
  }
};

//logout
export const logout = (req, res) => {
  try {
    res.cookie('token', '', { maxAge: 0 });
    res.status(200).json({ message: 'Logged out successfully' });
     
  } catch (error) {
    console.log("Error in logout controller:", error.message);
    res.status(500).json({ 
      message: 'Server error'
    });
  }
};

// profile
export const profile = async (req, res) => {
  try {
    const {profilePic} = req.body;
    const UserId = req.user._id;

    if (!profilePic) {
      return res.status(400).json({ 
        message: 'Please provide a profile picture'
      });
    }

    const uploadedProfilePic = await cloudinary.uploader.upload(profilePic);
    const updatedUser = await User.findByIdAndUpdate(
      UserId,
      { profilePic: uploadedProfilePic.secure_url },
      { new: true }
    );
    res.status(200).json(updatedUser);

  } catch (error) {
    console.log("Error in Profile controller:", error.message);
    res.status(500).json({ 
      message: 'Server error'
    });
  }
};

// authed - check if user is authenticated and determine either to take them to profile or login page when refreshing page
export const authed = (req, res) => {
  try {
    if (!req.user) {
      return res.status(401).json({
        authenticated: false,
        message: "Not authenticated",
      });
    }

    res.status(200).json({
      authenticated: true,
      user: req.user,
    });

  } catch (error) {
    console.log("Error in authed controller:", error.message);
    res.status(500).json({
      message: "Server error",
    });
  }
};
