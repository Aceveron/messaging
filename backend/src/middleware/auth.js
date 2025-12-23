import jwt from 'jsonwebtoken';
import User from '../schemas/user.js';

// Gatekeeper middleware to protect routes
export const Gatekeeper = async (req, res, next) => { /** next is a callback to proceed to the next middleware 
    which is Profile in {router.put('/profile', Gatekeeper, Profile);}   */
  try {
    const token = req.cookies.token;
    if (!token) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET); // decode token to get userId
    if (!decoded) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const user = await User.findById(decoded.userId).select('-password');
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    req.user = user; // attach user to request object
    next(); // proceed to the next middleware or route handler
  } catch (error) {
    console.log("Error in Gatekeeper middleware:", error.message);
    res.status(500).json({ message: 'Server error' });
  }
};