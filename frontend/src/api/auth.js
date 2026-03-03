import axios from 'axios'

// Dynamically determine the base URL based on the current window location
// This allows the frontend to work whether accessed via localhost or LAN IP
const protocol = window.location.protocol
const hostname = window.location.hostname
const port = 8080 // Backend port

const api = axios.create({
  baseURL: `${protocol}//${hostname}:${port}/api`,
  timeout: 5000
})

// Add request interceptor to include token
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
}, error => {
    return Promise.reject(error)
})

export const login = (data) => api.post('/auth/login', data)
export const register = (data) => api.post('/auth/register', data)
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const searchUsers = (keyword) => api.get('/users/search', { params: { keyword } })
export const updateSettings = (id, data) => api.put(`/users/${id}/settings`, data)
export const getUnreadCounts = (userId) => api.get(`/users/${userId}/unread`)
export const markRead = (userId, type) => api.post(`/users/${userId}/read/${type}`)
export const uploadFile = (formData) => api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
})

// Friends API
export const sendFriendRequest = (receiverId, reason) => api.post(`/friends/request/${receiverId}`, null, { params: { reason } })
export const getFriendRequests = () => api.get('/friends/requests')
export const acceptFriendRequest = (requestId) => api.post(`/friends/accept/${requestId}`)
export const rejectFriendRequest = (requestId) => api.post(`/friends/reject/${requestId}`)
export const getFriends = () => api.get('/friends')
export const deleteFriend = (friendId) => api.delete(`/friends/${friendId}`)

// Moments API
export const publishMoment = (data) => api.post('/moments', data)
export const getMoments = () => api.get('/moments')
export const likeMoment = (id) => api.post(`/moments/${id}/like`)
export const commentMoment = (id, content, replyToUserId) => api.post(`/moments/${id}/comment`, { content, replyToUserId })
export const deleteMoment = (id) => api.delete(`/moments/${id}`)

// Chat & Group API
export const sendMessage = (data) => api.post('/messages', data)
export const recallMessage = (messageId) => api.post(`/messages/${messageId}/recall`)
export const getMessages = (friendId, groupId) => api.get('/messages', { params: { friendId, groupId } })
export const createGroup = (name, memberIds) => api.post('/groups', { name, memberIds })
export const getMyGroups = () => api.get('/groups')
export const inviteGroupMembers = (groupId, memberIds) => api.post(`/groups/${groupId}/invite`, { memberIds })
export const updateGroup = (groupId, data) => api.put(`/groups/${groupId}`, data)
export const pinGroup = (groupId, pinned) => api.post(`/groups/${groupId}/pin`, { pinned })
export const pinFriend = (friendId, pinned) => api.post(`/friends/${friendId}/pin`, { pinned })
export const kickMember = (groupId, userId) => api.delete(`/groups/${groupId}/members/${userId}`)
export const muteMember = (groupId, userId, minutes) => api.post(`/groups/${groupId}/members/${userId}/mute`, { minutes })
export const unmuteMember = (groupId, userId) => api.delete(`/groups/${groupId}/members/${userId}/mute`)
export const deleteGroup = (groupId) => api.delete(`/groups/${groupId}`)
export const getGroupRequests = () => api.get('/groups/requests')
export const acceptGroupRequest = (requestId) => api.post(`/groups/requests/${requestId}/accept`)
export const rejectGroupRequest = (requestId) => api.post(`/groups/requests/${requestId}/reject`)

// AI API
export const sendAiMessage = (content) => api.post('/ai/chat', { content })

export default api
