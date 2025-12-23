import jwt from 'jsonwebtoken';

export const generateToken = (userId, res) => {
    const token = jwt.sign({userId }, process.env.JWT_SECRET, {
        expiresIn: '1d',
    });
// sending token as cookie
    res.cookie('token', token, {
        maxAge: 24 * 60 * 60 * 1000, // 1 day
        httpOnly: true, // prevents attacks like (XSS) cross site scripting attacks
        sameSite: 'strict', // CSRF protection
        secure: process.env.NODE_ENV !== 'development', // set to true in production
    });

    return token;
}