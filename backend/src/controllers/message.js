import User from '../schemas/user.js';
import Message from '../schemas/message.js';

 // fetch users for sidebar but not the logged in user
export const SidebarUsers = async (req, res) => {
    try {
        const loggedInUserId = req.user._id;
        const filteredUsers = await User.find({ 
            _id: { $ne: loggedInUserId } // exclude logged in user
            // ne = not equal 
        })
        .select('-password');
        res.status(200).json(filteredUsers);

    } catch (error) {
        console.error("Error fetching sidebar users:", error.message);
        res.status(500).json({ message: "Server Error" });
    }
};

// get messages between two users
export const getDM = async (req, res) => {
    try {
        const DmId = req.params;
        const MeId = req.user._id;

        // find all messages where sender is either the logged in user or the other user
        const messages = await Message.find({
            $or: [
                { senderId: MeId, receiverId: DmId },
                { senderId: DmId, receiverId: MeId }
            ]
        });

        res.status(200).json(messages);

    } catch (error) {
        console.error("Error fetching direct messages:", error.message);
        res.status(500).json({ message: "Server Error" });
    }
};

// send messages
export const DM = async (req, res) => {
    try {
        const { text, image } = req.body;
        const DMid = req.params;
        const senderId = req.user._id;

        let imageUrl;
        if (image) {
            // upload image to cloudinary
            const uploadResponse = await cloudinary.uploader.upload(image);
            imageUrl = uploadResponse.secure_url;
        }

        const newMessage = new Message({
            text,
            senderId,
            receiverId: DMid,
            image: imageUrl
        });

        await newMessage.save();

        // realtime via socket.io

        res.status(201).json(newMessage);

    } catch (error) {
        console.error("Error sending direct message:", error.message);
        res.status(500).json({ message: "Server Error" });
    }
};