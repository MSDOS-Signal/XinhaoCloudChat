<template>
  <div class="chat-layout">
    <TechBackground />
    <!-- 左侧导航栏 (Desktop) -->
    <div class="sidebar hidden-mobile">
      <div class="logo-area">
        <el-avatar 
          :size="48" 
          :src="getAvatarUrl(userStore.user?.avatar)" 
          class="user-avatar" 
          @click="showProfile = true"
        />
      </div>
      <div class="nav-menu">
        <div 
          class="nav-item" 
          :class="{ active: currentTab === 'chat' }"
          @click="currentTab = 'chat'"
        >
          <div class="icon-wrapper">
             <el-icon><ChatDotRound /></el-icon>
             <div class="badge" v-if="unreadCounts.chat > 0">{{ unreadCounts.chat > 99 ? '99+' : unreadCounts.chat }}</div>
          </div>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentTab === 'contacts' }"
          @click="currentTab = 'contacts'"
        >
          <div class="icon-wrapper">
             <el-icon><User /></el-icon>
             <div class="badge" v-if="unreadCounts.contacts > 0">{{ unreadCounts.contacts > 99 ? '99+' : unreadCounts.contacts }}</div>
          </div>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentTab === 'moments' }"
          @click="currentTab = 'moments'"
        >
          <div class="icon-wrapper">
             <el-icon><Star /></el-icon>
             <div class="badge" v-if="unreadCounts.moments > 0">{{ unreadCounts.moments > 99 ? '99+' : unreadCounts.moments }}</div>
          </div>
        </div>
        <div 
          class="nav-item" 
          :class="{ active: currentTab === 'settings' }"
          @click="showSettings = true"
        >
          <el-icon><Setting /></el-icon>
        </div>
      </div>
      <div class="bottom-actions">
        <div class="nav-item theme-switch" @click="themeStore.toggleTheme">
          <el-icon v-if="themeStore.isDark"><Moon /></el-icon>
          <el-icon v-else><Sunny /></el-icon>
        </div>
        <div class="nav-item logout" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </div>
      </div>
    </div>
    
    <!-- 二级侧边栏 (聊天列表/联系人列表) -->
    <div class="sub-sidebar" :class="{ 'hidden-mobile': isMobileChatOpen }">
      <div class="search-header">
        <div class="search-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <input type="text" v-model="searchQuery" placeholder="搜索..." class="search-input" @keydown.enter="handleGlobalSearch" />
        </div>
        <div class="add-btn" @click="showAddFriend = true">
          <el-icon><Plus /></el-icon>
        </div>
      </div>

      <!-- 聊天列表 -->
      <div v-if="currentTab === 'chat'" class="list-container">
        <!-- New Group Button in Chat List -->
         <div class="list-item create-group-item" @click="showCreateGroup = true" style="justify-content: center; color: var(--accent-color);">
             <el-icon><Plus /></el-icon> <span style="margin-left: 5px;">发起群聊</span>
         </div>

        <div 
          class="list-item"
          :class="{ active: currentChatUser && currentChatUser.id === 6 }"
          @click="startChat({ id: 6, nickname: '炘灏科技', username: 'ai_assistant', avatar: '/uploads/ai_avatar.jpg' })"
        >
          <div class="item-avatar">
            <el-avatar :size="40" shape="square" :src="getAvatarUrl('/uploads/ai_avatar.jpg')" />
          </div>
          <div class="item-content">
            <div class="item-top">
              <span class="item-title">炘灏科技</span>
              <span class="item-time"></span>
            </div>
            <div class="item-subtitle">欢迎来到炘灏科技</div>
          </div>
        </div>
        
        <!-- Chat Sessions (Unified) -->
        <div 
          v-for="item in sortedChatList" 
          :key="item.type + '-' + item.id"
          class="list-item"
          :class="{ active: (item.type === 'group' && currentChatGroup && currentChatGroup.id === item.id) || (item.type === 'friend' && currentChatUser && currentChatUser.id === item.id), pinned: item.pinned }"
          @click="item.type === 'group' ? startGroupChat(item.data) : startChat(item.data)"
          @contextmenu.prevent="showChatContextMenu($event, item)"
        >
          <div class="item-avatar">
             <div class="icon-wrapper">
                <el-avatar :size="40" :src="getAvatarUrl(item.avatar)" :icon="item.type === 'group' ? 'UserFilled' : ''" />
                <div class="badge" v-if="item.unread > 0">{{ item.unread > 99 ? '99+' : item.unread }}</div>
             </div>
          </div>
          <div class="item-content">
            <div class="item-top">
              <span class="item-title">{{ item.name }}</span>
              <span class="item-time" v-if="item.lastActiveTime">{{ formatTime(item.lastActiveTime) }}</span>
            </div>
            <div class="item-subtitle" v-if="item.type === 'group'">{{ item.data.members.length }}人</div>
            <div class="item-subtitle" v-else>{{ item.signature || '暂无签名' }}</div>
          </div>
          <div v-if="item.pinned" class="pin-icon" style="font-size: 12px; color: var(--accent-color); margin-left: 5px;"><el-icon><Top /></el-icon></div>
        </div>
      </div>

      <!-- 联系人列表 -->
      <div v-else-if="currentTab === 'contacts'" class="list-container">
        <!-- New Friends / Requests -->
        <div v-if="friendRequests.length > 0" class="section-title">新朋友</div>
        <div v-for="req in friendRequests" :key="'friend-'+req.id" class="list-item request-item">
            <div class="item-avatar">
                <el-avatar :size="36" :src="getAvatarUrl(req.sender.avatar)" />
            </div>
            <div class="item-content">
                <div class="item-title">{{ req.sender.nickname || req.sender.username }}</div>
                <div class="item-subtitle" v-if="req.reason" style="font-size: 11px; color: var(--text-secondary);">{{ req.reason }}</div>
                <div class="request-actions">
                    <el-button type="success" circle size="small" @click="handleAccept(req.id)"><el-icon><Check /></el-icon></el-button>
                    <el-button type="danger" circle size="small" @click="handleReject(req.id)"><el-icon><Close /></el-icon></el-button>
                </div>
            </div>
        </div>

        <!-- Group Invitations -->
        <div v-if="groupRequests.length > 0" class="section-title">群通知</div>
        <div v-for="req in groupRequests" :key="'group-'+req.id" class="list-item request-item" style="background-color: rgba(64, 158, 255, 0.1);">
            <div class="item-avatar">
                <el-avatar :size="36" :src="getAvatarUrl(req.group.avatar)" />
            </div>
            <div class="item-content">
                <div class="item-title">{{ req.group.name }}</div>
                <div class="item-subtitle" style="font-size: 11px; color: var(--text-secondary);">{{ req.reason }}</div>
                <div class="request-actions">
                    <el-button type="success" circle size="small" @click="handleAcceptGroup(req.id)"><el-icon><Check /></el-icon></el-button>
                    <el-button type="danger" circle size="small" @click="handleRejectGroup(req.id)"><el-icon><Close /></el-icon></el-button>
                </div>
            </div>
        </div>

        <div class="section-title">我的好友</div>
        <div v-if="friendList.length === 0" class="empty-state">
          <span>暂无联系人</span>
        </div>
        <div v-else v-for="friend in friendList.filter(f => {
            if (!searchQuery.trim()) return true
            const q = searchQuery.toLowerCase()
            return (f.nickname && f.nickname.toLowerCase().includes(q)) || (f.username && f.username.toLowerCase().includes(q))
        })" :key="friend.id" class="list-item" @click="startChat(friend)" @contextmenu.prevent="showContactContextMenu($event, friend)">
            <div class="item-avatar">
                <el-avatar :size="40" :src="getAvatarUrl(friend.avatar)" />
            </div>
            <div class="item-content">
                <div class="item-title">{{ friend.nickname || friend.username }}</div>
                <div class="item-subtitle">{{ friend.signature || '暂无签名' }}</div>
            </div>
        </div>
      </div>

      <!-- 云聊空间 -->
      <div v-else-if="currentTab === 'moments'" class="list-container moments-container">
        <div class="moments-header">
            <el-icon class="show-mobile" @click="currentTab = 'chat'" style="margin-right: 10px; cursor: pointer;"><ArrowLeft /></el-icon>
            <span>云聊空间</span>
            <el-button circle type="primary" @click="showMomentsPublish = true"><el-icon><Camera /></el-icon></el-button>
        </div>
        
        <div class="moments-list-scroll">
            <div v-if="momentsList.length === 0" class="empty-state">
              <span>暂无动态</span>
            </div>
            <div v-else v-for="moment in momentsList" :key="moment.id" class="moment-card">
                <div class="moment-header">
                    <el-avatar :size="40" :src="getAvatarUrl(moment.user.avatar)" />
                    <div class="moment-user">
                        <span class="nickname">{{ moment.user.nickname }}</span>
                        <span class="time">{{ new Date(moment.createTime).toLocaleString() }}</span>
                    </div>
                    <div class="moment-actions" v-if="moment.user.id === userStore.user.id" style="margin-left: auto;">
                        <el-button link type="danger" size="small" @click="handleDeleteMoment(moment)">删除</el-button>
                    </div>
                </div>
                <div class="moment-content">{{ moment.content }}</div>
                <div class="moment-images" v-if="moment.images && moment.images.length">
                    <el-image 
                        v-for="(img, idx) in moment.images" 
                        :key="idx" 
                        :src="getImageUrl(img)" 
                        :preview-src-list="moment.images.map(i => getImageUrl(i))" 
                        fit="cover" 
                        class="moment-img" 
                    />
                </div>
                
                <!-- Likes & Comments Actions -->
                <div class="moment-footer">
                    <div class="action-bar">
                        <div class="action-btn" @click="handleLike(moment)" :class="{ active: moment.likedUserIds && moment.likedUserIds.includes(userStore.user.id) }">
                            <el-icon><Star /></el-icon>
                            <span>{{ moment.likedUserIds ? moment.likedUserIds.length : 0 }}</span>
                        </div>
                        <div class="action-btn" @click="commentInputs[moment.id] = commentInputs[moment.id] === undefined ? '' : commentInputs[moment.id]">
                            <el-icon><Comment /></el-icon>
                            <span>{{ moment.comments ? moment.comments.length : 0 }}</span>
                        </div>
                    </div>
                    
                    <!-- Comments List -->
                    <div class="comments-section" v-if="moment.comments && moment.comments.length">
                        <div v-for="comment in moment.comments" :key="comment.id" class="comment-item" @click="prepareReply(moment, comment.user)">
                            <span class="comment-user">{{ comment.user.nickname }}</span>
                            <span v-if="comment.replyToUser" class="reply-text"> 回复 <span class="comment-user">{{ comment.replyToUser.nickname }}</span></span>
                            <span class="comment-content">: {{ comment.content }}</span>
                        </div>
                    </div>
                    
                    <!-- Comment Input -->
                    <div class="comment-input-area" v-if="commentInputs[moment.id] !== undefined">
                        <el-input 
                            v-model="commentInputs[moment.id]" 
                            :placeholder="replyTargets[moment.id] ? `回复 ${replyTargets[moment.id].nickname}...` : '评论...'"
                            size="small"
                            @keydown.enter="handleComment(moment)"
                        >
                            <template #append>
                                <el-button 
                                    @click="handleComment(moment)" 
                                    class="moment-send-btn"
                                    :style="{ 
                                        backgroundColor: themeStore.isDark ? '#10a37f' : '#000000', 
                                        color: '#ffffff',
                                        border: 'none'
                                    }"
                                >发送</el-button>
                            </template>
                        </el-input>
                        <div v-if="replyTargets[moment.id]" style="font-size: 10px; color: var(--text-secondary); margin-top: 2px; cursor: pointer;" @click="replyTargets[moment.id] = null">
                            取消回复
                        </div>
                    </div>
                </div>
            </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="main-area" :class="{ 'mobile-open': isMobileChatOpen }">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="header-left">
            <el-button v-if="isMobileChatOpen" icon="ArrowLeft" circle text class="back-btn show-mobile" @click="isMobileChatOpen = false" />
            <div class="header-info">
            <h3 v-if="currentChatGroup">{{ currentChatGroup.name }} ({{ currentChatGroup.members.length }})</h3>
            <h3 v-else>{{ currentChatUser ? (currentChatUser.nickname || currentChatUser.username) : '炘灏科技' }}</h3>
            <span class="tag" v-if="!currentChatUser && !currentChatGroup">官方团队</span>
            </div>
        </div>
        <div class="header-actions">
          <el-button v-if="currentChatGroup" circle text @click="showGroupInfo = true; selectedFriendsForGroup = []"><el-icon><MoreFilled /></el-icon></el-button>
          <el-button v-else circle text @click="showSettings = true"><el-icon><MoreFilled /></el-icon></el-button>
        </div>
      </div>

      <!-- 消息记录 -->
      <div class="messages-container" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="[msg.direction, { 'recall-row': msg.isRecall }]">
          <!-- Received Avatar -->
          <el-avatar 
            v-if="msg.direction === 'received' && !msg.isRecall" 
            :size="36" 
            shape="square" 
            :src="currentChatGroup ? getAvatarUrl(msg.sender?.avatar) : (currentChatUser ? getAvatarUrl(currentChatUser.avatar) : getAvatarUrl('/uploads/ai_avatar.jpg'))" 
            class="msg-avatar received-avatar" 
          />
          
          <!-- Content Bubble -->
          <div class="message-content" :class="{ 'recall-container': msg.isRecall }" @contextmenu.prevent="showContextMenu($event, index)">
            <div v-if="currentChatGroup && msg.direction === 'received' && !msg.isRecall" style="font-size: 10px; color: var(--text-secondary); margin-bottom: 2px;">{{ msg.sender?.nickname || msg.sender?.username }}</div>
            
            <div v-if="msg.isRecall" class="recall-tip">
                {{ msg.sender.id === userStore.user.id ? '你' : (msg.sender.nickname || msg.sender.username) }} 撤回了一条消息
            </div>
            
            <div v-else>
                <!-- Reply Context -->
                <div v-if="msg.replyToMessage" class="reply-context">
                    <span class="reply-user">{{ msg.replyToMessage.sender.nickname || msg.replyToMessage.sender.username }}:</span>
                    <span class="reply-content">{{ msg.replyToMessage.type === 'image' ? '[图片]' : msg.replyToMessage.content }}</span>
                </div>

                <div v-if="msg.image" class="image-bubble">
                    <el-image :src="getImageUrl(msg.image)" :preview-src-list="[getImageUrl(msg.image)]" fit="cover" class="msg-image" />
                </div>
                <div v-else-if="msg.contentType === 'video'" class="video-bubble">
                    <video :src="getImageUrl(msg.content)" controls class="msg-video" style="max-width: 250px; max-height: 250px; border-radius: 8px;" preload="metadata" playsinline></video>
                </div>
                <div v-else-if="msg.contentType === 'audio'" class="audio-bubble">
                    <audio :src="getImageUrl(msg.content)" controls class="msg-audio" style="max-width: 250px; height: 40px;" preload="metadata"></audio>
                </div>
                <div v-else-if="msg.contentType === 'file'" class="file-bubble">
                    <a :href="getFileUrl(msg.content)" target="_blank" class="file-link" :download="getFileName(msg.content)">
                        <el-icon><Folder /></el-icon>
                        <span>{{ getFileName(msg.content) }}</span>
                    </a>
                </div>
                <!-- Markdown Rendering for Text Messages -->
                <div v-else class="bubble markdown-body" v-html="md.render(msg.content)"></div>
                
                <div class="msg-footer">
                    <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                </div>
                
                <!-- Message Actions Menu (Context Menu) -->
                 <div class="msg-context-menu" v-if="contextMenuVisible === index" :style="{ top: contextMenuY + 'px', left: contextMenuX + 'px' }">
                    <div class="action-item" @click="handleCopyMessage(msg)" v-if="!msg.image && msg.contentType !== 'audio' && msg.contentType !== 'video' && msg.contentType !== 'file'">复制</div>
                    <div class="action-item" @click="handleReplyMessage(msg)">回复</div>
                    <div class="action-item" v-if="!msg.isRecall && ((msg.direction === 'sent' && new Date() - new Date(msg.createTime) < 120000) || (currentChatGroup && currentChatGroup.owner.id === userStore.user.id))" @click="handleRecall(msg)">撤回</div>
                    <div class="action-item" @click="handleDeleteMessageLocal(index)">删除</div>
                 </div>
                 
                 <!-- Trigger Area for Context Menu - REMOVED, bind directly to content -->
                 <!-- <div class="action-trigger-area" @contextmenu.prevent="showContextMenu($event, index)"></div> -->
            </div>
          </div>

          <!-- Sent Avatar -->
          <el-avatar 
            v-if="msg.direction === 'sent' && !msg.isRecall" 
            :size="36" 
            shape="square" 
            :src="getAvatarUrl(userStore.user?.avatar)" 
            class="msg-avatar sent-avatar" 
            @click="showProfile = true"
          />
        </div>
      </div>

      <!-- 输入框区域 -->
      <div class="input-section">
        <!-- Reply Target Indicator -->
        <div v-if="replyMessageTarget" class="reply-target-bar">
            <span>回复 {{ replyMessageTarget.sender.nickname || replyMessageTarget.sender.username }}: {{ replyMessageTarget.content }}</span>
            <el-icon class="close-btn" @click="replyMessageTarget = null"><Close /></el-icon>
        </div>
        
        <div class="toolbar">
          <el-popover placement="top" :width="300" trigger="click" :disabled="isMeMuted">
            <template #reference>
              <div class="tool-icon" :class="{ disabled: isMeMuted }"><el-icon><Emoji /></el-icon></div>
            </template>
            <div class="emoji-grid">
                <span v-for="e in emojiList" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
            </div>
          </el-popover>
          
          <el-tooltip content="发送文件" placement="top" :show-after="500">
            <div class="tool-icon" :class="{ disabled: isMeMuted }" @click="handleFileUpload"><el-icon><Folder /></el-icon></div>
          </el-tooltip>
          
          <el-tooltip content="发送图片" placement="top" :show-after="500">
            <div class="tool-icon" :class="{ disabled: isMeMuted }" @click="handleImageUpload"><el-icon><Picture /></el-icon></div>
          </el-tooltip>
          
          <el-tooltip content="发送视频" placement="top" :show-after="500">
            <div class="tool-icon" :class="{ disabled: isMeMuted }" @click="handleVideoUpload"><el-icon><VideoCamera /></el-icon></div>
          </el-tooltip>
          
          <el-tooltip content="发送语音" placement="top" :show-after="500">
            <div class="tool-icon" :class="{ disabled: isMeMuted }" @click="handleVoiceClick"><el-icon><Microphone /></el-icon></div>
          </el-tooltip>
        </div>
        
        <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" />
        <input type="file" ref="imageInput" style="display: none" accept="image/*" @change="handleImageChange" />
        <input type="file" ref="videoInput" style="display: none" accept="video/*" @change="handleVideoChange" />

        <textarea 
          v-model="inputMessage" 
          class="message-input" 
          :placeholder="isMeMuted ? '您已被禁言...' : '发送消息...'"
          :disabled="isMeMuted"
          @keydown.enter.prevent="handleSend"
        ></textarea>

        <div class="input-footer">
          <span class="hint hidden-mobile">按 Enter 发送</span>
          <button class="send-btn" @click="handleSend" :disabled="!inputMessage.trim()">
            发送
          </button>
        </div>
      </div>
    </div>

    <!-- Chat List Context Menu -->
    <div class="msg-context-menu" v-if="chatContextMenuVisible" :style="{ top: chatContextMenuY + 'px', left: chatContextMenuX + 'px' }">
        <div class="action-item" v-if="selectedChatItem?.type === 'group' || selectedChatItem?.type === 'friend'" @click="handlePinChat">{{ selectedChatItem?.pinned ? '取消置顶' : '置顶聊天' }}</div>
    </div>
    
    <!-- Friend List Context Menu (Separate) -->
    <div class="msg-context-menu" v-if="contactContextMenuVisible" :style="{ top: contactContextMenuY + 'px', left: contactContextMenuX + 'px' }">
        <div class="action-item" @click="handleDeleteContact" style="color: #ef4444;">删除好友</div>
    </div>

    <!-- 底部导航栏 (Mobile) -->
    <div class="bottom-nav show-mobile" v-if="!isMobileChatOpen">
        <div class="nav-item" :class="{ active: currentTab === 'chat' }" @click="currentTab = 'chat'">
            <div class="icon-wrapper">
                <el-icon><ChatDotRound /></el-icon>
                <div class="badge" v-if="unreadCounts.chat > 0">{{ unreadCounts.chat > 99 ? '99+' : unreadCounts.chat }}</div>
            </div>
            <span>聊天</span>
        </div>
        <div class="nav-item" :class="{ active: currentTab === 'contacts' }" @click="currentTab = 'contacts'">
            <div class="icon-wrapper">
                <el-icon><User /></el-icon>
                <div class="badge" v-if="unreadCounts.contacts > 0">{{ unreadCounts.contacts > 99 ? '99+' : unreadCounts.contacts }}</div>
            </div>
            <span>通讯录</span>
        </div>
        <div class="nav-item" :class="{ active: currentTab === 'moments' }" @click="currentTab = 'moments'">
            <div class="icon-wrapper">
                <el-icon><Star /></el-icon>
                <div class="badge" v-if="unreadCounts.moments > 0">{{ unreadCounts.moments > 99 ? '99+' : unreadCounts.moments }}</div>
            </div>
            <span>云聊空间</span>
        </div>
        <div class="nav-item" :class="{ active: currentTab === 'settings' }" @click="showSettings = true">
            <el-avatar :size="24" :src="userStore.user?.avatar" />
            <span>我</span>
        </div>
    </div>

    <!-- 个人资料弹窗 -->
    <el-dialog v-model="showProfile" title="个人资料" width="400px" class="custom-dialog">
      <div class="profile-content">
        <div class="profile-header">
          <el-avatar :size="80" :src="getAvatarUrl(editForm.avatar) || getAvatarUrl(userStore.user?.avatar)" class="profile-avatar" />
          <el-button link type="primary" @click="triggerAvatarUpload">更换头像</el-button>
          <input type="file" ref="avatarInput" style="display: none" accept="image/*" @change="handleAvatarChange" />
        </div>
        <el-form :model="editForm" label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="editForm.nickname" />
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input v-model="editForm.signature" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="editForm.gender">
              <el-radio :value="1">男</el-radio>
              <el-radio :value="2">女</el-radio>
              <el-radio :value="0">保密</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="danger" @click="handleLogout" style="float: left">退出登录</el-button>
          <el-button @click="showProfile = false">取消</el-button>
          <el-button type="primary" @click="handleSaveProfile" :loading="saving">保存</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 设置弹窗 -->
    <el-dialog v-model="showSettings" title="设置" width="400px" class="custom-dialog">
        <div class="settings-list">
            <div class="setting-item">
                <span>深色模式</span>
                <el-switch v-model="themeStore.isDark" />
            </div>
            <div class="setting-item">
                <span>消息通知</span>
                <el-switch v-model="notifications" />
            </div>
            <div class="setting-item">
                <span>允许被搜索</span>
                <el-switch v-model="searchable" @change="handleUpdateSettings" />
            </div>
             <div class="setting-item" @click="showProfile = true; showSettings = false">
                <span>编辑个人资料</span>
                <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="setting-item logout-item" @click="handleLogout">
                <span>退出登录</span>
                <el-icon><SwitchButton /></el-icon>
            </div>
        </div>
    </el-dialog>

    <!-- 添加好友弹窗 -->
    <el-dialog v-model="showAddFriend" title="添加好友" width="400px" class="custom-dialog">
        <el-input v-model="addFriendInput" placeholder="输入用户名搜索" :prefix-icon="Search" @keyup.enter="handleAddFriend" />
        <div class="search-results-list" v-if="searchResults.length > 0">
            <div v-for="user in searchResults" :key="user.id" class="user-item">
                <el-avatar :src="user.avatar" :size="36" />
                <div class="user-info">
                    <span class="nickname">{{ user.nickname || user.username }}</span>
                    <span class="username">ID: {{ user.username }}</span>
                </div>
                <el-button type="primary" size="small" @click="prepareSendRequest(user)">添加</el-button>
            </div>
        </div>
        <div v-else-if="addFriendInput && !searching && searchResults.length === 0" class="search-result">
            <div class="empty-state" style="padding: 20px 0;">
                <p>未找到用户 "{{ addFriendInput }}"</p>
            </div>
        </div>
    </el-dialog>

    <!-- 申请理由弹窗 -->
    <el-dialog v-model="showRequestReason" title="验证申请" width="400px" class="custom-dialog">
        <el-input v-model="requestReason" placeholder="请输入申请理由" />
        <template #footer>
            <el-button @click="showRequestReason = false">取消</el-button>
            <el-button type="primary" @click="confirmSendRequest">发送</el-button>
        </template>
    </el-dialog>

    <!-- 发布朋友圈弹窗 -->
    <el-dialog v-model="showMomentsPublish" title="发布动态" width="500px" class="custom-dialog">
        <el-input v-model="momentForm.content" type="textarea" :rows="4" placeholder="分享新鲜事..." />
        <div class="upload-area">
            <div class="image-preview" v-for="(img, idx) in momentForm.images" :key="idx">
                <el-image :src="img" fit="cover" class="preview-img" />
                <div class="remove-img" @click="momentForm.images.splice(idx, 1)"><el-icon><Close /></el-icon></div>
            </div>
            <div class="upload-btn" @click="triggerMomentImageUpload" v-if="momentForm.images.length < 9">
                <el-icon><Plus /></el-icon>
            </div>
        </div>
        <input type="file" ref="momentImageInput" style="display: none" accept="image/*" @change="handleMomentImageChange" multiple />
        <template #footer>
            <el-button @click="showMomentsPublish = false">取消</el-button>
            <el-button type="primary" @click="handlePublishMoment" :loading="saving">发布</el-button>
        </template>
    </el-dialog>

    <!-- 创建群聊弹窗 -->
    <el-dialog v-model="showCreateGroup" title="发起群聊" width="500px" class="custom-dialog">
        <el-input v-model="groupName" placeholder="群名称" style="margin-bottom: 20px;" />
        <div class="friend-selector" style="max-height: 300px; overflow-y: auto;">
            <div class="section-title">选择好友 (至少2人)</div>
            <el-checkbox-group v-model="selectedFriendsForGroup">
                <div v-for="friend in friendList" :key="friend.id" class="friend-select-item" style="padding: 10px; display: flex; align-items: center; border-bottom: 1px solid var(--border-color);">
                    <el-checkbox :label="friend.id" style="margin-right: 10px; width: 20px; height: 20px;" />
                    <el-avatar :size="30" :src="getAvatarUrl(friend.avatar)" style="margin-right: 10px;" />
                    <span class="dialog-friend-name">{{ friend.nickname || friend.username }}</span>
                </div>
            </el-checkbox-group>
        </div>
        <template #footer>
            <el-button @click="showCreateGroup = false">取消</el-button>
            <el-button type="primary" @click="handleCreateGroup">创建 ({{ selectedFriendsForGroup.length }})</el-button>
        </template>
    </el-dialog>

    <!-- 群聊信息/邀请弹窗 -->
    <el-dialog v-model="showGroupInfo" title="群组信息" width="500px" class="custom-dialog">
        <div v-if="currentChatGroup">
            <div class="group-info-header" style="text-align: center; margin-bottom: 20px;">
                 <el-avatar :size="80" :src="getAvatarUrl(editGroupForm.avatar || currentChatGroup.avatar)" style="margin-bottom: 10px;" />
                 <div v-if="currentChatGroup.owner.id === userStore.user.id" style="margin-bottom: 10px;">
                    <el-button link type="primary" @click="triggerGroupAvatarUpload">更换头像</el-button>
                    <input type="file" ref="groupAvatarInput" style="display: none" accept="image/*" @change="handleGroupAvatarChange" />
                 </div>
                 
                 <div v-if="currentChatGroup.owner.id === userStore.user.id">
                     <el-input v-model="editGroupForm.name" placeholder="群名称" style="margin-bottom: 10px;" />
                     <el-input v-model="editGroupForm.description" type="textarea" :rows="2" placeholder="群介绍" style="margin-bottom: 10px;" />
                     <el-button type="primary" size="small" @click="handleUpdateGroup">保存修改</el-button>
                 </div>
                 <div v-else>
                     <h3>{{ currentChatGroup.name }}</h3>
                     <p v-if="currentChatGroup.description" style="color: var(--text-secondary); font-size: 13px;">{{ currentChatGroup.description }}</p>
                 </div>
            </div>

            <p><strong>成员数:</strong> {{ currentChatGroup.members.length }}</p>
            <div class="divider" style="margin: 20px 0; border-bottom: 1px solid var(--border-color);"></div>
            
            <!-- Member List with Management -->
            <div class="section-title">群成员</div>
            <div class="group-members-list" style="max-height: 200px; overflow-y: auto;">
                <div v-for="member in currentChatGroup.members" :key="member.id" class="group-member-item" style="padding: 10px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border-color);">
                    <div style="display: flex; align-items: center;">
                        <el-avatar :size="30" :src="getAvatarUrl(member.avatar)" style="margin-right: 10px;" />
                        <span class="dialog-friend-name">{{ member.nickname || member.username }}</span>
                        <span v-if="member.id === currentChatGroup.owner.id" class="tag" style="margin-left: 5px; font-size: 10px; padding: 2px 4px;">群主</span>
                    </div>
                    
                    <div v-if="currentChatGroup.owner.id === userStore.user.id && member.id !== userStore.user.id" class="member-actions">
                         <div v-if="currentChatGroup.mutedMembers && currentChatGroup.mutedMembers[member.id] && new Date(currentChatGroup.mutedMembers[member.id]) > new Date()">
                            <el-button link type="warning" size="small" @click="handleMuteMember(member, 0)">解除</el-button>
                         </div>
                         <div v-else>
                             <el-dropdown trigger="click" @command="(cmd) => handleMuteMember(member, cmd)">
                                <el-button link type="warning" size="small">禁言</el-button>
                                <template #dropdown>
                                  <el-dropdown-menu>
                                    <el-dropdown-item command="10">10分钟</el-dropdown-item>
                                    <el-dropdown-item command="60">1小时</el-dropdown-item>
                                    <el-dropdown-item command="1440">1天</el-dropdown-item>
                                  </el-dropdown-menu>
                                </template>
                              </el-dropdown>
                         </div>
                          <el-button type="danger" link size="small" @click="handleKickMember(member)">踢出</el-button>
                    </div>
                </div>
            </div>

            <div class="divider" style="margin: 20px 0; border-bottom: 1px solid var(--border-color);"></div>
            <div class="section-title">邀请新成员</div>
             <div class="friend-selector" style="max-height: 200px; overflow-y: auto;">
                <el-checkbox-group v-model="selectedFriendsForGroup">
                    <div v-for="friend in friendList.filter(f => !currentChatGroup.members.some(m => m.id === f.id))" :key="friend.id" class="friend-select-item" style="padding: 10px; display: flex; align-items: center;">
                        <el-checkbox :label="friend.id" style="margin-right: 10px; width: 20px; height: 20px;" />
                        <el-avatar :size="30" :src="getAvatarUrl(friend.avatar)" style="margin-right: 10px;" />
                        <span class="dialog-friend-name">{{ friend.nickname || friend.username }}</span>
                    </div>
                </el-checkbox-group>
                <div v-if="friendList.filter(f => !currentChatGroup.members.some(m => m.id === f.id)).length === 0" style="padding: 20px; text-align: center; color: var(--text-secondary);">
                    没有可邀请的好友
                </div>
            </div>
            
            <div v-if="currentChatGroup.owner.id === userStore.user.id" style="margin-top: 20px; text-align: center;">
                 <el-button type="danger" plain style="width: 100%;" @click="handleDeleteGroup">解散群聊</el-button>
            </div>
        </div>
        <template #footer>
            <el-button @click="showGroupInfo = false">关闭</el-button>
            <el-button type="primary" @click="handleInviteMembers" :disabled="selectedFriendsForGroup.length === 0">邀请</el-button>
        </template>
    </el-dialog>

    <!-- 语音录制弹窗 -->
    <el-dialog v-model="showVoiceRecorder" title="录制语音" width="300px" class="custom-dialog" :close-on-click-modal="false">
        <div style="text-align: center; padding: 20px;">
            <div v-if="!isRecording">
                <el-button type="primary" circle size="large" @click="startRecording" style="width: 80px; height: 80px; font-size: 30px;"><el-icon><Microphone /></el-icon></el-button>
                <p style="margin-top: 10px;">点击开始录制</p>
            </div>
            <div v-else>
                <el-button type="danger" circle size="large" @click="stopRecording" style="width: 80px; height: 80px; font-size: 30px; animation: pulse 1s infinite;"><el-icon><VideoCamera /></el-icon></el-button>
                <p style="margin-top: 10px;">录制中... 点击停止</p>
            </div>
        </div>
        <template #footer>
            <el-button @click="cancelRecording">取消</el-button>
            <el-button type="primary" @click="sendVoice" :disabled="!recordedBlob">发送</el-button>
        </template>
    </el-dialog>

    <!-- File Upload Inputs (Hidden) -->
    <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" />
    <input type="file" ref="imageInput" style="display: none" accept="image/*" @change="handleImageChange" />

    <!-- 头像裁剪弹窗 -->
    <el-dialog v-model="showCropper" title="编辑头像" width="600px" class="custom-dialog">
        <div class="cropper-container" style="height: 400px;">
            <vue-cropper
                ref="cropper"
                :src="cropperImg"
                :aspect-ratio="1"
                preview=".preview"
                :view-mode="1"
                :guides="true"
                :background="false"
                drag-mode="move"
            />
        </div>
        <div class="cropper-actions" style="margin-top: 20px; display: flex; gap: 10px; justify-content: center;">
            <el-button-group>
                <el-button @click="cropper.zoom(0.1)"><el-icon><ZoomIn /></el-icon></el-button>
                <el-button @click="cropper.zoom(-0.1)"><el-icon><ZoomOut /></el-icon></el-button>
                <el-button @click="cropper.rotate(90)"><el-icon><Refresh /></el-icon></el-button>
            </el-button-group>
        </div>
        <template #footer>
            <el-button @click="showCropper = false">取消</el-button>
            <el-button type="primary" @click="handleCropUpload">确认上传</el-button>
        </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, watch, computed } from 'vue'
import { useUserStore } from '../store/user'
import { useThemeStore } from '../store/theme'
import { useRouter } from 'vue-router'
import { updateUser, searchUsers, uploadFile, sendFriendRequest as apiSendFriendRequest, getFriendRequests, acceptFriendRequest, rejectFriendRequest, getFriends, deleteFriend, updateSettings, publishMoment, getMoments, likeMoment, commentMoment, deleteMoment, sendMessage, recallMessage, getMessages, createGroup, getMyGroups, inviteGroupMembers, updateGroup, kickMember, muteMember, unmuteMember, deleteGroup, getGroupRequests, acceptGroupRequest, rejectGroupRequest, getUnreadCounts, markRead, pinGroup, pinFriend, sendAiMessage, default as api } from '../api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import TechBackground from '../components/TechBackground.vue'
import VueCropper from 'vue-cropperjs'
import 'cropperjs/dist/cropper.css'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css' // Use a dark theme for code blocks

const md = new MarkdownIt({
    html: false, // Disable HTML tags in source
    xhtmlOut: false, // Use '/' to close single tags (<br />).
    breaks: true, // Convert '\n' in paragraphs to <br>
    langPrefix: 'language-',  // CSS language prefix for fenced blocks. Can be
    linkify: true, // Autoconvert URL-like text to links
    typographer: true,
    quotes: '“”‘’',
    highlight: function (str, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return '<pre class="hljs"><code>' +
                    hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
                    '</code></pre>';
            } catch (__) { }
        }
        return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
    }
})

// Element Plus Icons
import { ChatDotRound, User, Setting, SwitchButton, Search, Plus, MoreFilled, Folder, Picture, Moon, Sunny, ArrowRight, ArrowLeft, Star, Check, Close, Camera, Location, Comment, Delete, ZoomIn, ZoomOut, Refresh, Upload, UserFilled, RefreshLeft, Top, VideoCamera, Microphone } from '@element-plus/icons-vue'
const Emoji = ChatDotRound 

const userStore = useUserStore()
const themeStore = useThemeStore()
const router = useRouter()
const currentTab = ref('chat')
const inputMessage = ref('')
const messagesContainer = ref(null)
const showProfile = ref(false)
const showSettings = ref(false)
const showAddFriend = ref(false)
const showRequestReason = ref(false)
const showCropper = ref(false)
const showCreateGroup = ref(false)
const showGroupInfo = ref(false)
const cropperImg = ref('')
const cropper = ref(null)
const requestReason = ref('')
const currentReceiver = ref(null)
const showMomentsPublish = ref(false)
const saving = ref(false)
const notifications = ref(true)
const searchable = ref(userStore.user?.searchable !== false)
const searchQuery = ref('')
const addFriendInput = ref('')
const fileInput = ref(null)
const imageInput = ref(null)
const videoInput = ref(null)
const avatarInput = ref(null)
const momentImageInput = ref(null)
const searchResults = ref([])
const searching = ref(false)
const friendRequests = ref([])
const groupRequests = ref([])
const friendList = ref([])
const momentsList = ref([])
const groupList = ref([])
const selectedFriendsForGroup = ref([])
const groupName = ref('')
const currentChatGroup = ref(null)
const editGroupForm = reactive({
    name: '',
    description: '',
    avatar: ''
})
const groupAvatarInput = ref(null)

watch(currentChatGroup, (newVal) => {
    if (newVal) {
        editGroupForm.name = newVal.name
        editGroupForm.description = newVal.description || ''
        editGroupForm.avatar = newVal.avatar
    }
})

const momentForm = reactive({
    content: '',
    images: []
})
const commentInputs = reactive({}) // Map of momentId -> comment content
const replyTargets = reactive({}) // Map of momentId -> user object (who we are replying to)
const unreadCounts = reactive({
    chat: 0,
    contacts: 0,
    moments: 0,
    bySender: {},
    byGroup: {}
})

let pollInterval = null

const showVoiceRecorder = ref(false)
const isRecording = ref(false)
const recordedBlob = ref(null)
let mediaRecorder = null
let audioChunks = []

const isMeMuted = ref(false)

watch([currentChatGroup, () => userStore.user], () => {
    checkMuteStatus()
}, { deep: true })

// Polling for mute status check (as websocket might not cover it immediately if not broadcasted)
// Actually we can just check when messages arrive or group updates.
// But we need to check periodally if mute expires.
const checkMuteStatus = () => {
    if (!currentChatGroup.value || !userStore.user) {
        isMeMuted.value = false
        return
    }
    const myId = userStore.user.id
    if (currentChatGroup.value.mutedMembers && currentChatGroup.value.mutedMembers[myId]) {
        const unmuteTime = new Date(currentChatGroup.value.mutedMembers[myId])
        if (unmuteTime > new Date()) {
            isMeMuted.value = true
        } else {
            isMeMuted.value = false
        }
    } else {
        isMeMuted.value = false
    }
}

// Mobile State
const isMobileChatOpen = ref(false)

const messages = ref([
  { type: 'received', content: '欢迎来到炘灏科技！这里是您的专属AI助手体验空间。' },
  { type: 'received', content: '您可以在这里体验极致流畅的即时通讯功能。' }
])

// Edit Form
const editForm = reactive({
  nickname: userStore.user?.nickname || '',
  signature: userStore.user?.signature || '',
  gender: userStore.user?.gender || 0,
  avatar: userStore.user?.avatar || ''
})

watch(() => userStore.user, (newVal) => {
    if(newVal) {
        editForm.nickname = newVal.nickname
        editForm.signature = newVal.signature
        editForm.gender = newVal.gender
        editForm.avatar = newVal.avatar
    }
}, { deep: true })

const sortedChatList = computed(() => {
    const list = []
    // Add groups
    groupList.value.forEach(g => {
        list.push({
            type: 'group',
            id: g.id,
            name: g.name,
            avatar: g.avatar,
            pinned: g.pinned || false,
            lastActiveTime: g.lastActiveTime || g.createTime,
            data: g,
            unread: unreadCounts.byGroup[g.id] || 0
        })
    })
    // Add friends
    friendList.value.forEach(f => {
        list.push({
            type: 'friend',
            id: f.id,
            name: f.nickname || f.username, // Fallback to username if nickname is missing
            avatar: f.avatar,
            pinned: f.pinned || false,
            lastActiveTime: f.lastActiveTime || f.createTime,
            data: f,
            unread: unreadCounts.bySender[f.id] || 0,
            signature: f.signature
        })
    })
    
    // Sort
    const sorted = list.sort((a, b) => {
        if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
        const t1 = new Date(a.lastActiveTime).getTime()
        const t2 = new Date(b.lastActiveTime).getTime()
        return t2 - t1
    })

    // Filter by search query (Fuzzy search)
    if (searchQuery.value && searchQuery.value.trim()) {
        const query = searchQuery.value.toLowerCase()
        return sorted.filter(item => {
            const name = item.name ? item.name.toLowerCase() : ''
            const username = item.data.username ? item.data.username.toLowerCase() : ''
            return name.includes(query) || username.includes(query)
        })
    }
    
    return sorted
})

const chatContextMenuVisible = ref(false)
const chatContextMenuX = ref(0)
const chatContextMenuY = ref(0)
const selectedChatItem = ref(null)

const contactContextMenuVisible = ref(false)
const contactContextMenuX = ref(0)
const contactContextMenuY = ref(0)
const selectedContactItem = ref(null)

const showChatContextMenu = (event, item) => {
    selectedChatItem.value = item
    chatContextMenuX.value = event.clientX
    chatContextMenuY.value = event.clientY
    chatContextMenuVisible.value = true
    contactContextMenuVisible.value = false
    contextMenuVisible.value = -1 
}

const showContactContextMenu = (event, friend) => {
    selectedContactItem.value = friend
    contactContextMenuX.value = event.clientX
    contactContextMenuY.value = event.clientY
    contactContextMenuVisible.value = true
    chatContextMenuVisible.value = false
    contextMenuVisible.value = -1
}

const handlePinChat = async () => {
    if (!selectedChatItem.value) return
    const item = selectedChatItem.value
    const newPinned = !item.pinned
    
    try {
        if (item.type === 'group') {
            await pinGroup(item.id, newPinned)
            const g = groupList.value.find(x => x.id === item.id)
            if(g) g.pinned = newPinned
        } else {
            await pinFriend(item.id, newPinned)
            const f = friendList.value.find(x => x.id === item.id)
            if(f) f.pinned = newPinned
        }
        chatContextMenuVisible.value = false
        ElMessage.success(newPinned ? '已置顶' : '已取消置顶')
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handlePinContact = async () => {
    if (!selectedContactItem.value) return
    const friend = selectedContactItem.value
    const newPinned = !friend.pinned
    
    try {
        await pinFriend(friend.id, newPinned)
        const f = friendList.value.find(x => x.id === friend.id)
        if(f) f.pinned = newPinned
        contactContextMenuVisible.value = false
        ElMessage.success(newPinned ? '已置顶' : '已取消置顶')
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleDeleteContact = async () => {
    if (!selectedContactItem.value) return
    const friend = selectedContactItem.value
    
    try {
        await ElMessageBox.confirm(`确定要删除好友 ${friend.nickname || friend.username} 吗？删除后将无法发送消息。`, '警告', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'error'
        })
        
        await deleteFriend(friend.id)
        
        // Update local state
        friendList.value = friendList.value.filter(f => f.id !== friend.id)
        if (currentChatUser.value && currentChatUser.value.id === friend.id) {
            currentChatUser.value = null
            currentTab.value = 'chat'
        }
        
        ElMessage.success('好友已删除')
        contactContextMenuVisible.value = false
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('操作失败: ' + (e.response?.data || e.message))
    }
}

const handleDeleteFriend = async () => {
    // Legacy/Unused
}

// Global click to close
window.addEventListener('click', () => {
    chatContextMenuVisible.value = false
    contactContextMenuVisible.value = false
})

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleChatClick = () => {
    // Default official chat
    currentChatUser.value = null 
    currentChatGroup.value = null
    messages.value = [
        { type: 'received', content: '欢迎来到炘灏科技！这里是您的专属AI助手体验空间。' },
        { type: 'received', content: '您可以在这里体验极致流畅的即时通讯功能。' }
    ]
    isMobileChatOpen.value = true
    scrollToBottom()
}

const handleGlobalSearch = async () => {
    // Legacy method - kept if we want to trigger external search on Enter
    // But now we filter locally first.
    // If we want to add new friends via search, we use the Add Friend dialog.
    if (currentTab.value === 'contacts' && friendList.value.filter(f => (f.nickname && f.nickname.toLowerCase().includes(searchQuery.value.toLowerCase())) || (f.username && f.username.toLowerCase().includes(searchQuery.value.toLowerCase()))).length === 0) {
        // Maybe open add friend dialog with this query?
        // addFriendInput.value = searchQuery.value
        // showAddFriend.value = true
        // handleAddFriend()
    }
}

const loadMessages = async () => {
    try {
        let res
        if (currentChatGroup.value) {
            res = await getMessages(null, currentChatGroup.value.id)
        } else if (currentChatUser.value) {
            res = await getMessages(currentChatUser.value.id, null)
        } else {
            return
        }
        
        // Helper to infer type for legacy messages
        const inferType = (m) => {
            if (m.type && m.type !== 'text') return m.type
            if (m.content.startsWith('[文件]')) return 'file'
            if (m.content.match(/\.(mp4|webm|ogg)$/i) || m.content.includes('/uploads/') && m.content.endsWith('.mp4')) return 'video'
            if (m.content.match(/\.(mp3|wav|ogg)$/i)) return 'audio'
            return 'text'
        }

        // Transform messages
        messages.value = res.data.map(m => ({
            id: m.id,
            type: m.sender.id === userStore.user.id ? 'sent' : 'received',
            content: m.type === 'recall' ? '撤回了一条消息' : m.content,
            image: m.type === 'image' ? m.content : null,
            // Use inferred type for rendering logic (file, video, etc.) stored in msg.type property for v-if
            // Note: msg.type used in template is 'sent'/'received'. We need a separate property for content type.
            // Wait, template uses: :class="msg.type" (sent/received) AND v-if="msg.type === 'video'" (content type).
            // This is a conflict in the existing code! 
            // The existing code: 
            // <div ... class="message-row" :class="msg.type"> (sent/received)
            // <div v-else-if="msg.type === 'video'" ...>
            // This is definitely a bug I introduced or existed. 
            // 'msg.type' is overloaded. 
            // Let's fix this by using a separate property 'contentType' or similar, OR fixing the mapping.
            // In the map above: type: m.sender.id ... ? 'sent' : 'received' OVERWRITES the DB type!
            
            // Fix: Store direction in 'direction' or keep 'type' as direction and use 'contentType' for content.
            // BUT the template expects 'msg.type' to be sent/received for styling.
            // AND 'msg.type' to be 'video' for content. This is impossible.
            
            // Let's check template again:
            // <div ... :class="msg.type"> -> uses 'sent'/'received'
            // <div v-if="msg.image">
            // <div v-else-if="msg.type === 'video'"> -> This checks 'sent'/'received' === 'video' => FALSE.
            
            // CORRECT FIX: Separate direction and content type.
            direction: m.sender.id === userStore.user.id ? 'sent' : 'received',
            contentType: inferType(m), // Use this for content rendering
            
            sender: m.sender,
            createTime: m.createTime,
            isRecall: m.type === 'recall',
            replyToMessage: m.replyToMessage
        }))
        scrollToBottom()
    } catch (e) {
        console.error('Load messages failed', e)
    }
}

const handleRecall = async (msg) => {
    try {
        await recallMessage(msg.id)
        // Optimistic update
        msg.type = 'recall'
        msg.content = '撤回了一条消息'
        msg.isRecall = true
    } catch (e) {
        ElMessage.error(e.response?.data || '撤回失败')
    }
}

const replyMessageTarget = ref(null)
const handleReplyMessage = (msg) => {
    replyMessageTarget.value = msg
    // inputMessage.value = `回复 ${msg.sender.nickname}: ` // Optional visual cue
}

const handleDeleteMessageLocal = (index) => {
    messages.value.splice(index, 1)
}

const formatTime = (timeStr) => {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content) return

  // Optimistic update
  const tempId = 'temp-' + Date.now()
  const tempMsg = {
    id: tempId, // Add temp ID
    direction: 'sent', // Changed from type
    contentType: 'text', // Added
    content: content,
    sender: userStore.user,
    createTime: new Date().toISOString()
  }
  
  messages.value.push(tempMsg)
  
  const tempContent = inputMessage.value
  inputMessage.value = ''
  scrollToBottom()
  
  try {
      let res
      if (currentChatGroup.value) {
          res = await sendMessage({ groupId: currentChatGroup.value.id, content: tempContent, type: 'text', replyToMessageId: replyMessageTarget.value?.id })
      } else if (currentChatUser.value) {
          res = await sendMessage({ receiverId: currentChatUser.value.id, content: tempContent, type: 'text', replyToMessageId: replyMessageTarget.value?.id })
      } else {
          // Official chat - AI Chat
          // Send to AI service
          // We don't wait for response here as it will come via WebSocket stream
          await sendAiMessage(tempContent)
          replyMessageTarget.value = null
          return
      }

      if (res && res.data) {
          // Find the temp message again (it might have moved if other messages arrived, though unlikely in JS single thread)
          const targetMsg = messages.value.find(m => m.id === tempId)
          if (targetMsg) {
               Object.assign(targetMsg, {
                  id: res.data.id,
                  createTime: res.data.createTime,
                  sender: res.data.sender
              })
          }
      }

      replyMessageTarget.value = null // Clear reply target
  } catch (e) {
      ElMessage.error(e.response?.data || '发送失败')
      // Mark temp message as failed?
      const targetMsg = messages.value.find(m => m.id === tempId)
      if (targetMsg) {
          messages.value = messages.value.filter(m => m.id !== tempId) // Remove it
      }
  }
}

// Group Logic
const handleCreateGroup = async () => {
    if (selectedFriendsForGroup.value.length < 2) { // Owner + 2 = 3
        ElMessage.warning('请至少选择2位好友')
        return
    }
    if (!groupName.value.trim()) {
        ElMessage.warning('请输入群名称')
        return
    }
    
    try {
        await createGroup(groupName.value, selectedFriendsForGroup.value)
        ElMessage.success('群聊创建成功')
        showCreateGroup.value = false
        selectedFriendsForGroup.value = []
        groupName.value = ''
        fetchData() // Reload groups
    } catch (e) {
        ElMessage.error(e.response?.data || '创建失败')
    }
}

const startGroupChat = (group) => {
    currentChatUser.value = null
    currentChatGroup.value = group
    
    // Clear unread for this group
    if (unreadCounts.byGroup[group.id] > 0) {
        unreadCounts.chat = Math.max(0, unreadCounts.chat - unreadCounts.byGroup[group.id])
        unreadCounts.byGroup[group.id] = 0
    }

    loadMessages()
    currentTab.value = 'chat'
    isMobileChatOpen.value = true
}

const handleInviteMembers = async () => {
    if (!currentChatGroup.value) return
    if (selectedFriendsForGroup.value.length === 0) return
    
    try {
        await inviteGroupMembers(currentChatGroup.value.id, selectedFriendsForGroup.value)
        ElMessage.success('邀请成功')
        // showGroupInfo.value = false // Keep open to see result or continue editing
        selectedFriendsForGroup.value = []
        fetchData() // Refresh group members
    } catch (e) {
        ElMessage.error('邀请失败')
    }
}

const handleUpdateGroup = async () => {
    if (!currentChatGroup.value) return
    try {
        await updateGroup(currentChatGroup.value.id, editGroupForm)
        ElMessage.success('群资料已更新')
        // Update local currentChatGroup immediately for UI feedback
        currentChatGroup.value.name = editGroupForm.name
        currentChatGroup.value.description = editGroupForm.description
        currentChatGroup.value.avatar = editGroupForm.avatar
        // Also refresh group list to update sidebar avatar
        fetchData()
    } catch (e) {
        ElMessage.error('更新失败')
    }
}

const handleKickMember = async (member) => {
    try {
        await ElMessageBox.confirm(`确定要将 ${member.nickname || member.username} 移出群聊吗？`, '提示', { type: 'warning' })
        await kickMember(currentChatGroup.value.id, member.id)
        ElMessage.success('已移出群聊')
        currentChatGroup.value.members = currentChatGroup.value.members.filter(m => m.id !== member.id)
        fetchData()
    } catch (e) {
        if(e !== 'cancel') ElMessage.error('操作失败')
    }
}

const handleMuteMember = async (member, minutes) => {
    const min = parseInt(minutes)
    if (min === 0) {
        try {
            await unmuteMember(currentChatGroup.value.id, member.id)
            ElMessage.success(`已解除 ${member.nickname || member.username} 的禁言`)
        } catch(e) { ElMessage.error('操作失败') }
    } else {
        try {
            await muteMember(currentChatGroup.value.id, member.id, min)
            let timeStr = min + '分钟'
            if(min === 60) timeStr = '1小时'
            if(min === 1440) timeStr = '1天'
            ElMessage.success(`已禁言 ${member.nickname || member.username} ${timeStr}`)
        } catch(e) { ElMessage.error('操作失败') }
    }
}

const triggerGroupAvatarUpload = () => {
    groupAvatarInput.value.click()
}

const handleGroupAvatarChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    try {
        const { data } = await uploadFile(formData)
        const newAvatarUrl = data.url || data // handle response format
        console.log('Uploaded new group avatar:', newAvatarUrl)
        editGroupForm.avatar = newAvatarUrl
        // Also update currentChatGroup avatar immediately for preview in main chat
        if (currentChatGroup.value) {
            currentChatGroup.value.avatar = newAvatarUrl
        }
        ElMessage.success('头像上传成功，请点击保存修改')
    } catch (e) {
        console.error('Group avatar upload error:', e)
        ElMessage.error('头像上传失败')
    }
    event.target.value = ''
}

const handleDeleteGroup = async () => {
    try {
        await ElMessageBox.confirm('确定要解散该群聊吗？解散后所有成员将被移出，聊天记录可能无法恢复。', '警告', {
            confirmButtonText: '解散',
            cancelButtonText: '取消',
            type: 'error'
        })
        await deleteGroup(currentChatGroup.value.id)
        ElMessage.success('群聊已解散')
        showGroupInfo.value = false
        currentChatGroup.value = null
        currentTab.value = 'chat'
        fetchData()
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('操作失败')
    }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const triggerAvatarUpload = () => {
    avatarInput.value.click()
}

const handleAvatarChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // Read file for cropper
    const reader = new FileReader()
    reader.onload = (e) => {
        cropperImg.value = e.target.result
        showCropper.value = true
        // Reset input value so same file can be selected again
        event.target.value = ''
    }
    reader.readAsDataURL(file)
}

const handleCropUpload = () => {
    if (!cropper.value) return
    
    cropper.value.getCroppedCanvas().toBlob(async (blob) => {
        const formData = new FormData()
        // Use a generic name or original name if preserved
        formData.append('file', blob, 'avatar.png')
        
        try {
            const { data } = await uploadFile(formData)
            editForm.avatar = data.url
            // Update immediately? Or wait for save?
            // User requested "edit/adjust avatar", usually implies immediate update or preview.
            // Here we update the form preview.
            showCropper.value = false
            ElMessage.success('头像已裁剪并上传，请点击保存')
        } catch (error) {
            ElMessage.error('头像上传失败')
        }
    })
}

const getFileUrl = (content) => {
    // Format: [文件] name (url)
    // Regex extract url inside last parentheses
    const match = content.match(/\((.*?)\)$/)
    if (match && match[1]) {
        return getImageUrl(match[1])
    }
    // Fallback if no match (maybe legacy format or direct url)
    if (content.startsWith('http') || content.startsWith('/')) return getImageUrl(content)
    return '#'
}

const getFileName = (content) => {
    // Format: [文件] name (url)
    // Remove [文件] and (url)
    const name = content.replace(/^\[文件\]\s*/, '').replace(/\s*\(.*?\)$/, '')
    if (!name.trim()) return '未知文件'
    return name
}

const handleFileUpload = () => {
    if (isMeMuted.value) return
    fileInput.value.click()
}

const handleImageUpload = () => {
    if (isMeMuted.value) return
    imageInput.value.click()
}

const handleVideoUpload = () => {
    if (isMeMuted.value) return
    videoInput.value.click()
}

const handleVideoChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    const formData = new FormData()
    formData.append('file', file)
    try {
        const { data } = await uploadFile(formData)
        const videoUrl = data.url || data
        if (currentChatGroup.value) {
            await sendMessage({ groupId: currentChatGroup.value.id, content: videoUrl, type: 'video' })
        } else if (currentChatUser.value) {
            await sendMessage({ receiverId: currentChatUser.value.id, content: videoUrl, type: 'video' })
        }
        ElMessage.success('视频发送成功')
    } catch (e) {
        ElMessage.error('视频上传失败')
    }
    event.target.value = ''
}

const startRecording = async () => {
    // Check if mediaDevices is supported (it might be undefined in insecure contexts)
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        ElMessageBox.alert(
            '您的浏览器环境不支持媒体访问。这通常是因为：\n1. 未使用 HTTPS 协议\n2. 浏览器版本过低\n3. 禁用了相关权限',
            '不支持录音',
            { confirmButtonText: '知道了', type: 'error' }
        )
        showVoiceRecorder.value = false
        return
    }

    if (window.location.protocol !== 'https:' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
        try {
            await ElMessageBox.confirm(
                '您的连接不是 HTTPS，浏览器可能会阻止麦克风访问。是否仍要尝试？(建议使用 localhost 或配置 HTTPS)',
                '非安全连接',
                { confirmButtonText: '尝试', cancelButtonText: '取消', type: 'warning' }
            )
        } catch (e) {
            showVoiceRecorder.value = false
            return
        }
    }

    try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
        mediaRecorder = new MediaRecorder(stream)
        audioChunks = []
        mediaRecorder.ondataavailable = (event) => {
            audioChunks.push(event.data)
        }
        mediaRecorder.onstop = () => {
            recordedBlob.value = new Blob(audioChunks, { type: 'audio/webm' })
            stream.getTracks().forEach(track => track.stop())
        }
        mediaRecorder.start()
        isRecording.value = true
    } catch (e) {
        console.error(e)
        ElMessage.error('无法访问麦克风: ' + (e.message || '权限被拒绝或环境不支持'))
    }
}

const stopRecording = () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        mediaRecorder.stop()
        isRecording.value = false
    }
}

const cancelRecording = () => {
    stopRecording()
    recordedBlob.value = null
    showVoiceRecorder.value = false
}

const sendVoice = async () => {
    if (!recordedBlob.value) return
    const formData = new FormData()
    formData.append('file', recordedBlob.value, 'voice.webm')
    try {
        const { data } = await uploadFile(formData)
        const audioUrl = data.url || data
        if (currentChatGroup.value) {
            await sendMessage({ groupId: currentChatGroup.value.id, content: audioUrl, type: 'audio' })
        } else if (currentChatUser.value) {
            await sendMessage({ receiverId: currentChatUser.value.id, content: audioUrl, type: 'audio' })
        }
        ElMessage.success('语音发送成功')
        showVoiceRecorder.value = false
        recordedBlob.value = null
    } catch (e) {
        ElMessage.error('发送失败')
    }
}

const handleVoiceClick = () => {
    if (isMeMuted.value) return
    showVoiceRecorder.value = true
}

const handleFileChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    const formData = new FormData()
    formData.append('file', file)
    
    try {
        const { data } = await uploadFile(formData)
        const fileUrl = data.url || data // Handle object or string response
        const content = `[文件] ${file.name} (${fileUrl})`
        
        if (currentChatGroup.value) {
            await sendMessage({ groupId: currentChatGroup.value.id, content, type: 'file' })
        } else if (currentChatUser.value) {
            await sendMessage({ receiverId: currentChatUser.value.id, content, type: 'file' })
        }
        ElMessage.success('文件发送成功')
    } catch (e) {
        ElMessage.error('文件上传失败')
    }
    event.target.value = ''
}

const handleImageChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    const formData = new FormData()
    formData.append('file', file)
    
    try {
        const { data } = await uploadFile(formData)
        const imageUrl = data.url || data // Handle object or string response
        
        if (currentChatGroup.value) {
            await sendMessage({ groupId: currentChatGroup.value.id, content: imageUrl, type: 'image' })
        } else if (currentChatUser.value) {
            await sendMessage({ receiverId: currentChatUser.value.id, content: imageUrl, type: 'image' })
        }
        ElMessage.success('图片发送成功')
    } catch (e) {
        ElMessage.error('图片上传失败')
    }
    event.target.value = ''
}

const handleSaveProfile = async () => {
    saving.value = true
    try {
        const { data } = await updateUser(userStore.user.id, editForm)
        // Check if data contains token (LoginResponse) or just User
        // UpdateUser returns User entity directly usually
        userStore.login(data) 
        ElMessage.success('个人资料已更新')
        showProfile.value = false
    } catch (error) {
        ElMessage.error('更新失败: ' + (error.response?.data || error.message))
    } finally {
        saving.value = false
    }
}

const emojiList = ['😀', '😁', '😂', '🤣', '😃', '😄', '😅', '😆', '😉', '😊', '😋', '😎', '😍', '😘', '😗', '😙', '😚', '🙂', '🤗', '🤩', '🤔', '🤨', '😐', '😑', '😶', '🙄', '😏', '😣', '😥', '😮', '🤐', '😯', '😪', '😫', '😴', '😌', '😛', '😜', '😝', '🤤', '😒', '😓', '😔', '😕', '🙃', '🤑', '😲', '☹️', '🙁', '😖', '😞', '😟', '😤', '😢', '😭', '😦', '😧', '😨', '😩', '🤯', '😬', '😰', '😱', '🥵', '🥶', '😳', '🤪', '😵', '😡', '😠', '🤬', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '😇', '🤠', '🤡', '🥳', '🥴', '🥺', '🤥', '🤫', '🤭', '🧐', '🤓', '😈', '👿', '👹', '👺', '💀', '👻', '👽', '🤖', '💩', '😺', '😸', '😹', '😻', '😼', '😽', '🙀', '😿', '😾']

const insertEmoji = (emoji) => {
    inputMessage.value += emoji
}

const handleUpdateSettings = async () => {
    try {
        await updateSettings(userStore.user.id, { searchable: searchable.value })
        // Update local user store
        const updatedUser = { ...userStore.user, searchable: searchable.value }
        userStore.login(updatedUser)
        ElMessage.success('设置已更新')
    } catch (e) {
        ElMessage.error('设置更新失败')
        searchable.value = !searchable.value // revert
    }
}

// Data Fetching
const fetchData = async () => {
    try {
        const [reqs, friends, moments, groups, groupReqs] = await Promise.all([
            getFriendRequests(),
            getFriends(),
            getMoments(),
            getMyGroups(),
            getGroupRequests()
        ])
        friendRequests.value = reqs.data
        
        // Sort friends by name (Pinyin)
        friendList.value = friends.data.sort((a, b) => {
            const nameA = a.nickname || a.username
            const nameB = b.nickname || b.username
            return nameA.localeCompare(nameB, 'zh-CN')
        })
        
        momentsList.value = moments.data
        groupList.value = groups.data
        groupRequests.value = groupReqs.data
    } catch (e) {
        console.error('Failed to fetch data', e)
    }
}

watch(currentTab, async (newTab) => {
    if (newTab === 'contacts' || newTab === 'moments') {
        fetchData()
    }
    
    if (userStore.user) {
        if (newTab === 'moments') {
            try {
                await markRead(userStore.user.id, 'moments')
                unreadCounts.moments = 0
            } catch (e) {}
        } else if (newTab === 'chat') {
            try {
                await markRead(userStore.user.id, 'chat')
                unreadCounts.chat = 0
            } catch (e) {}
        }
    }
})

// Watch search input for real-time search
let searchTimeout
watch(addFriendInput, (newVal) => {
    if (searchTimeout) clearTimeout(searchTimeout)
    if (!newVal.trim()) {
        searchResults.value = []
        return
    }
    searchTimeout = setTimeout(() => {
        handleAddFriend()
    }, 500)
})

// Friend Logic
const prepareSendRequest = (user) => {
    currentReceiver.value = user
    requestReason.value = `我是${userStore.user.nickname || userStore.user.username}`
    showRequestReason.value = true
}

const confirmSendRequest = async () => {
    if (!currentReceiver.value) return
    try {
        await apiSendFriendRequest(currentReceiver.value.id, requestReason.value)
        ElMessage.success(`已向 ${currentReceiver.value.nickname} 发送好友请求`)
        showRequestReason.value = false
        showAddFriend.value = false
    } catch (e) {
        ElMessage.error(e.response?.data || '请求发送失败')
    }
}

const handleAddFriend = async () => {
    if (!addFriendInput.value.trim()) return
    searching.value = true
    try {
        const { data } = await searchUsers(addFriendInput.value)
        searchResults.value = data
    } catch (e) {
        // Silent error for real-time search or show minimal feedback
    } finally {
        searching.value = false
    }
}

const getAvatarUrl = (url) => {
    // Check if url is valid string and not empty
    if (!url || url === '' || url === 'null' || url === 'undefined') {
        return 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
    }
    if (url.startsWith('http')) return url
    // If it's a relative path, prepend base URL (assuming API base URL)
    // api.defaults.baseURL includes '/api', so we need just the host part
    const baseURL = api.defaults.baseURL.replace('/api', '')
    
    // Ensure url starts with / if not
    const cleanUrl = url.startsWith('/') ? url : '/' + url
    return `${baseURL}${cleanUrl}`
}

const getImageUrl = (url) => {
    if (!url) return ''
    if (url.startsWith('http')) return url
    // Remove /api if present in url to avoid double /api/api if base has it, 
    // BUT usually url from DB is like /uploads/xxx.
    // api.defaults.baseURL is like http://localhost:8080/api
    // We want http://localhost:8080/uploads/xxx
    // So we strip /api from baseURL.
    const baseURL = api.defaults.baseURL.replace('/api', '')
    
    // Ensure url starts with / if not
    const cleanUrl = url.startsWith('/') ? url : '/' + url
    return `${baseURL}${cleanUrl}`
}

const currentChatUser = ref(null)

const startChat = (friend) => {
    currentChatUser.value = friend
    currentChatGroup.value = null
    
    // Clear unread for this friend
    if (unreadCounts.bySender[friend.id] > 0) {
        unreadCounts.chat = Math.max(0, unreadCounts.chat - unreadCounts.bySender[friend.id])
        unreadCounts.bySender[friend.id] = 0
    }

    loadMessages()
    
    currentTab.value = 'chat'
    isMobileChatOpen.value = true
}


const handleAccept = async (requestId) => {
    try {
        await acceptFriendRequest(requestId)
        ElMessage.success('已添加好友')
        await fetchData() // Refresh all lists
        
        // Find the friend request to get user info 
        // Note: request might be gone after fetchData, but we can try to find it in the old list or rely on fetch
        // Actually fetchData updates friendRequests so we can't find it there.
        // But the new friend is in friendList.
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleReject = async (requestId) => {
    try {
        await rejectFriendRequest(requestId)
        ElMessage.success('已拒绝')
        await fetchData()
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleAcceptGroup = async (requestId) => {
    try {
        await acceptGroupRequest(requestId)
        ElMessage.success('已加入群聊')
        await fetchData()
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleRejectGroup = async (requestId) => {
    try {
        await rejectGroupRequest(requestId)
        ElMessage.success('已拒绝')
        await fetchData()
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

// Moments Logic
const triggerMomentImageUpload = () => {
    momentImageInput.value.click()
}

const handleMomentImageChange = async (event) => {
    const files = event.target.files
    if (!files.length) return
    
    // Upload each file
    for (let i = 0; i < files.length; i++) {
        const formData = new FormData()
        formData.append('file', files[i])
        try {
            const { data } = await uploadFile(formData)
            momentForm.images.push(data.url)
        } catch (e) {
            ElMessage.error('图片上传失败')
        }
    }
}

const handleLike = async (moment) => {
    try {
        await likeMoment(moment.id)
        // Optimistic update
        const userId = userStore.user.id
        const index = moment.likedUserIds.indexOf(userId)
        if (index > -1) {
            moment.likedUserIds.splice(index, 1)
        } else {
            moment.likedUserIds.push(userId)
        }
    } catch (e) {
        ElMessage.error('操作失败')
    }
}

const handleComment = async (moment) => {
    const content = commentInputs[moment.id]
    if (!content || !content.trim()) return
    
    const replyTo = replyTargets[moment.id]
    
    try {
        await commentMoment(moment.id, content, replyTo ? replyTo.id : null)
        ElMessage.success('评论成功')
        commentInputs[moment.id] = ''
        replyTargets[moment.id] = null // Reset reply target
        
        // Optimistic update
        moment.comments.push({
            user: userStore.user,
            replyToUser: replyTo,
            content: content,
            createTime: new Date().toISOString()
        })
    } catch (e) {
        ElMessage.error(e.response?.data || '评论失败')
    }
}

const prepareReply = (moment, user) => {
    // if (user.id === userStore.user.id) return // Don't reply to self
    replyTargets[moment.id] = user
    if (commentInputs[moment.id] === undefined) commentInputs[moment.id] = ''
}

const handleDeleteMoment = async (moment) => {
    try {
        await ElMessageBox.confirm('确定要删除这条动态吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await deleteMoment(moment.id)
        ElMessage.success('删除成功')
        momentsList.value = momentsList.value.filter(m => m.id !== moment.id)
    } catch (e) {
        if (e !== 'cancel') ElMessage.error('删除失败')
    }
}

const handlePublishMoment = async () => {
    if (!momentForm.content && momentForm.images.length === 0) return
    saving.value = true
    try {
        await publishMoment(momentForm)
        ElMessage.success('发布成功')
        showMomentsPublish.value = false
        momentForm.content = ''
        momentForm.images = []
        fetchData()
    } catch (e) {
        ElMessage.error('发布失败')
    } finally {
        saving.value = false
    }
}

const stompClient = ref(null)

const connectWebSocket = () => {
    if (!userStore.isAuthenticated) return
    
    // api.defaults.baseURL includes '/api', we need root for ws
    const wsUrl = api.defaults.baseURL.replace('/api', '/ws')
    // Or just construct it
    // const wsUrl = 'http://localhost:8080/ws' // Dynamic preferred
    
    const client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: {
            Authorization: `Bearer ${userStore.token}`
        },
        debug: (str) => {
            // console.log(str) 
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    })

    client.onConnect = (frame) => {
        // Subscribe to user queue
        client.subscribe('/user/queue/messages', (message) => {
            const msg = JSON.parse(message.body)
            handleIncomingMessage(msg)
        })

        // Subscribe to friend updates (e.g. new friend added)
        client.subscribe('/user/queue/friends', (message) => {
            const msg = JSON.parse(message.body)
            if (msg.type === 'new_friend' && msg.friend) {
                // Check if already exists to avoid duplicates
                if (!friendList.value.some(f => f.id === msg.friend.id)) {
                    friendList.value.push(msg.friend)
                    
                    // Remove from friendRequests if present (for the acceptor side)
                    const reqIndex = friendRequests.value.findIndex(r => r.sender.id === msg.friend.id)
                    if (reqIndex > -1) {
                        friendRequests.value.splice(reqIndex, 1)
                    }

                    ElMessage.success(`您已添加 ${msg.friend.nickname || msg.friend.username} 为好友`)
                }
            } else if (msg.type === 'friend_deleted' && msg.friendId) {
                // Handle deletion
                friendList.value = friendList.value.filter(f => f.id !== msg.friendId)
                
                // If currently chatting with this friend, close chat or show alert
                if (currentChatUser.value && currentChatUser.value.id === msg.friendId) {
                    ElMessage.warning('对方已将您删除好友')
                }
            } else if (msg.type === 'group_invite' && msg.request) {
                // Add to group requests list
                if (!groupRequests.value.some(r => r.id === msg.request.id)) {
                    groupRequests.value.push(msg.request)
                    if (currentTab.value === 'contacts') {
                        // Maybe show red dot
                    }
                    unreadCounts.contacts++
                    ElMessage.info('收到新的群聊邀请')
                }
            }
        })
        
        // Subscribe to group topics
        groupList.value.forEach(group => {
            client.subscribe(`/topic/group/${group.id}`, (message) => {
                const msg = JSON.parse(message.body)
                handleIncomingMessage(msg)
            })
            // Subscribe to group events (mute, update, etc)
            client.subscribe(`/topic/group/${group.id}/events`, (message) => {
                const event = JSON.parse(message.body)
                handleGroupEvent(event)
            })
        })
        
        // Subscribe to moment comments
        client.subscribe('/topic/moments', (message) => {
             // Refresh moments if a new comment/like/post occurs
             if (currentTab.value === 'moments') {
                 fetchData()
             } else {
                 // Increment unread count for moments if not on moments tab
                 unreadCounts.moments++
             }
        })
    }
    
    client.activate()
    stompClient.value = client
}

const handleGroupEvent = (event) => {
    if (!currentChatGroup.value || currentChatGroup.value.id !== event.groupId) return
    
    if (event.type === 'mute') {
        if (!currentChatGroup.value.mutedMembers) currentChatGroup.value.mutedMembers = {}
        if (event.duration > 0) {
            currentChatGroup.value.mutedMembers[event.userId] = new Date(Date.now() + event.duration * 60000).toISOString()
        } else {
             delete currentChatGroup.value.mutedMembers[event.userId]
        }
        // Force update UI
        checkMuteStatus()
    } else if (event.type === 'update') {
        // Handle group info update
         if (event.avatar) currentChatGroup.value.avatar = event.avatar
         if (event.name) currentChatGroup.value.name = event.name
         if (event.description) currentChatGroup.value.description = event.description
    } else if (event.type === 'kick') {
        // If I am kicked?
        if (event.userId === userStore.user.id) {
            ElMessage.error('你已被移出该群聊')
            currentChatGroup.value = null
            currentTab.value = 'chat'
            fetchData()
        } else {
            // Remove member from local list
            currentChatGroup.value.members = currentChatGroup.value.members.filter(m => m.id !== event.userId)
        }
    }
}

const handleIncomingMessage = async (msg) => {
    // 0. Handle Stream (AI)
    if (msg.type === 'stream') {
        const msgId = msg.id || msg.messageId
        const aiSenderId = msg.senderId || 6 // AI user ID is usually 6
        
        // Only process if we are currently chatting with AI
        // AI chat is identified by currentChatUser.id === 1 (or whatever AI ID is)
        if (!currentChatUser.value || currentChatUser.value.id !== aiSenderId) {
            // Not in AI chat, maybe show a red dot or something?
            // For stream chunks, we probably just ignore them if not in the chat, 
            // as we can't easily "notify" for every character.
            // But we should notify for the FIRST chunk if we want to show red dot.
            // For simplicity: Ignore stream chunks if not in AI chat.
            // The "full" message will eventually be saved in DB and fetched when we open the chat.
            return 
        }

        // Find if we already have this message
        const existingMsg = messages.value.find(m => m.id === msgId)
        if (existingMsg) {
            // Append content
            if (msg.content) {
                // If it was empty placeholder, replace content, else append
                if (!existingMsg.content) existingMsg.content = msg.content
                else existingMsg.content += msg.content
                
                // Scroll to bottom
                nextTick(() => {
                    if (messagesContainer.value) {
                         messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
                    }
                })
            }
        } else {
            // New AI message starting
            messages.value.push({
                id: msgId,
                direction: 'received',
                contentType: 'text',
                content: msg.content || '',
                sender: { id: aiSenderId, nickname: '炘灏科技', avatar: '/uploads/ai_avatar.jpg' }, // Use correct avatar
                createTime: new Date().toISOString(),
                isRecall: false
            })
            scrollToBottom()
        }
        return
    }

    // 1. If it's a recall message
    if (msg.type === 'recall') {
        // Update local message
        const existingMsg = messages.value.find(m => m.id === msg.id)
        if (existingMsg) {
            existingMsg.type = 'recall'
            existingMsg.content = '撤回了一条消息'
            existingMsg.isRecall = true
        }
        return
    }

    // 2. Determine if it belongs to current chat
    const isCurrentChat = 
        (currentChatUser.value && msg.sender?.id === currentChatUser.value.id && !msg.group) || // From friend
        (currentChatUser.value && msg.receiver?.id === currentChatUser.value.id && msg.sender?.id === userStore.user.id && !msg.group) || // From me (echo)
        (currentChatGroup.value && msg.group?.id === currentChatGroup.value.id) // Group chat

    // Infer content type for incoming message
    let contentType = 'text'
    if (msg.type && msg.type !== 'text') contentType = msg.type
    else if (msg.content.startsWith('[文件]')) contentType = 'file'
    else if (msg.content.match(/\.(mp4|webm|ogg)$/i) || msg.content.includes('/uploads/') && msg.content.endsWith('.mp4')) contentType = 'video'
    else if (msg.content.match(/\.(mp3|wav|ogg)$/i)) contentType = 'audio'

    if (isCurrentChat) {
        // Avoid duplicate if optimistic update already added it (check by temporary ID or content/time heuristics if needed)
        // For simplicity, we might just append if not exists, or replace optimistic one.
        // Since backend sends back full message with ID, we can replace or append.
        // Simplest: Check if last message is similar (sent by me, same content, recent) and replace it, or just push.
        
        // Filter out duplicate echo from self
        // Check if we have a temp message that matches this real message
        // Or if we have a real message with same ID already
        const existingRealMsg = messages.value.find(m => m.id === msg.id)
        if (existingRealMsg) return // Already processed via API response or duplicate WS frame
        
        // Check for optimistic message match (same content, sent by me, recent, temp ID)
        // We use reverse search to find the latest matching temp message
        let matchIndex = -1
        for (let i = messages.value.length - 1; i >= 0; i--) {
            const m = messages.value[i]
            if (typeof m.id === 'string' && m.id.startsWith('temp-') && 
                m.direction === 'sent' && 
                m.content === msg.content && 
                msg.sender.id === userStore.user.id) {
                matchIndex = i
                break
            }
        }
        
        if (matchIndex > -1) {
            // Update the optimistic message with real data
            Object.assign(messages.value[matchIndex], {
                id: msg.id,
                createTime: msg.createTime,
                direction: msg.sender.id === userStore.user.id ? 'sent' : 'received',
                contentType: contentType,
                sender: msg.sender,
                replyToMessage: msg.replyToMessage
            })
        } else {
             // Not a duplicate of optimistic msg, so append it
             // BUT wait, if it's sent by me and I didn't find a temp msg, maybe I just sent it from another tab?
             // Or the temp msg was already updated by API response?
             // If API response updated it, it has a real ID now.
             // So 'existingRealMsg' check above handles that.
             
             messages.value.push({
                id: msg.id,
                direction: msg.sender.id === userStore.user.id ? 'sent' : 'received',
                contentType: contentType,
                content: msg.content,
                image: msg.type === 'image' ? msg.content : null,
                sender: msg.sender,
                createTime: msg.createTime,
                replyToMessage: msg.replyToMessage,
                isRecall: false
            })
            scrollToBottom()
        }
        
        // Mark as read immediately if it's an incoming message in current chat
        if (msg.sender.id !== userStore.user.id) {
             try {
                await markRead(userStore.user.id, 'chat')
                // Don't increment unread counts
             } catch (e) {}
        }

    } else {
        // 3. Notification (Red Dot)
        if (msg.sender?.id && msg.sender.id !== userStore.user.id) { // Only notify if not from self (e.g. sent from another device)
            if (msg.group) {
                 const groupId = msg.group.id
                 if (groupId) {
                     unreadCounts.byGroup[groupId] = (unreadCounts.byGroup[groupId] || 0) + 1
                 }
            } else {
                const senderId = msg.sender.id
                if (senderId) {
                    unreadCounts.bySender[senderId] = (unreadCounts.bySender[senderId] || 0) + 1
                }
            }
            unreadCounts.chat++
            ElMessage.info(`收到新消息: ${msg.sender.nickname || '未知用户'}`)
        }
    }
}

// ... existing polling code ...
// We can keep polling as fallback or remove it.
// Let's keep polling for friend requests, but remove message polling if WebSocket is active.

// Modified startPolling
const startPolling = () => {
    pollInterval = setInterval(async () => {
        if (!userStore.user) return

        try {
            const { data } = await getUnreadCounts(userStore.user.id)
            unreadCounts.chat = data.chat
            unreadCounts.contacts = data.contacts
            unreadCounts.moments = data.moments
            unreadCounts.bySender = data.chatBySender || {}
            unreadCounts.byGroup = data.chatByGroup || {}
            
            // Refresh friend requests list if on contacts tab
            if (currentTab.value === 'contacts') {
                 const reqs = await getFriendRequests()
                 friendRequests.value = reqs.data
            }
        } catch (e) {}
        
    }, 5000) 
}

const contextMenuVisible = ref(null)
const contextMenuX = ref(0)
const contextMenuY = ref(0)

const showContextMenu = (event, index) => {
    event.preventDefault() // Prevent default browser context menu
    event.stopPropagation()
    contextMenuVisible.value = index
    
    // Use fixed positioning relative to viewport for more reliable placement
    // But our menu is absolute inside relative parent.
    // Let's use offsetX/Y but clamp it or adjust.
    // Actually, let's just use mouse clientX/Y if we change menu to fixed?
    // No, keep it simple. Just use offsetX/Y and ensure it's visible.
    
    // If we want it to appear right under the mouse:
    contextMenuX.value = event.offsetX
    contextMenuY.value = event.offsetY
    
    chatContextMenuVisible.value = false
    contactContextMenuVisible.value = false
}

const handleCopyMessage = async (msg) => {
    try {
        await navigator.clipboard.writeText(msg.content)
        ElMessage.success('已复制')
    } catch (e) {
        ElMessage.error('复制失败')
    }
    contextMenuVisible.value = null
}

const closeContextMenu = () => {
    contextMenuVisible.value = null
}

onMounted(() => {
  document.addEventListener('click', closeContextMenu)
  scrollToBottom()
  fetchData().then(() => {
      connectWebSocket()
      if (userStore.user) {
          getUnreadCounts(userStore.user.id).then(res => {
              if (res.data) {
                  unreadCounts.chat = res.data.chat
                  unreadCounts.contacts = res.data.contacts
                  unreadCounts.moments = res.data.moments
                  unreadCounts.bySender = res.data.chatBySender || {}
                  unreadCounts.byGroup = res.data.chatByGroup || {}
              }
          })
      }
  })
  startPolling()
})

import { onUnmounted } from 'vue'
onUnmounted(() => {
    document.removeEventListener('click', closeContextMenu)
    if (pollInterval) clearInterval(pollInterval)
    if (stompClient.value) stompClient.value.deactivate()
})
</script>

<style scoped>
/* Existing styles... */
/* Recall Message Centering */
.recall-container {
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    max-width: 100%; /* Override max-width: 70% */
}

.recall-tip {
    font-size: 12px;
    color: var(--text-secondary);
    background-color: var(--bg-hover);
    padding: 4px 8px;
    border-radius: 4px;
    text-align: center;
    margin: 5px 0;
    width: auto;
}

.reply-context {
    font-size: 12px;
    color: var(--text-secondary);
    background-color: rgba(0,0,0,0.05);
    padding: 4px;
    border-radius: 4px;
    margin-bottom: 4px;
    border-left: 2px solid var(--accent-color);
}

.msg-time {
    font-size: 10px;
    color: var(--text-secondary);
    float: right;
    margin-top: 4px;
    margin-left: 8px;
}

.reply-target-bar {
    background-color: var(--bg-hover);
    padding: 8px 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    border-bottom: 1px solid var(--border-color);
}

.msg-actions {
    display: flex;
    flex-direction: column;
}

.action-item {
    padding: 8px 12px;
    cursor: pointer;
    font-size: 13px;
}

.action-item:hover {
    background-color: var(--bg-hover);
}

/* Make bubbles relative for absolute positioning if needed */
.bubble {
    position: relative;
}

/* ... */
.icon-wrapper {
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
}

.badge {
    position: absolute;
    top: -5px;
    right: -8px;
    background-color: #ff4d4f;
    color: white;
    font-size: 10px;
    height: 16px;
    min-width: 16px;
    border-radius: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 0 4px;
}
/* ... */
.cropper-container {
    width: 100%;
    height: 400px;
    background-color: #f0f0f0;
}
/* Context Menu */
.msg-context-menu {
    position: absolute;
    background: var(--bg-card); /* Should be solid color from theme */
    background-color: var(--bg-secondary); /* Ensure solid background */
    border: 1px solid var(--border-color);
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.2);
    border-radius: 4px;
    padding: 5px 0;
    z-index: 100;
    min-width: 80px;
}

.action-trigger-area {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10;
}

/* Bind context menu to message content */
.message-content {
    max-width: 70%;
    display: flex;
    flex-direction: column;
    position: relative; /* Context menu positioning context */
}

/* Adjust timestamp position */
.msg-footer {
    display: flex;
    justify-content: flex-end; /* Default right aligned */
    margin-top: 2px;
}

.received .msg-footer {
    justify-content: flex-start; /* Align time to left for received messages */
    padding-left: 5px;
}

.sent .msg-footer {
    justify-content: flex-end; /* Align time to right for sent messages */
    padding-right: 5px;
}

/* Emoji Grid */
.emoji-grid {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 5px;
    max-height: 200px;
    overflow-y: auto;
    padding: 5px;
}

.emoji-item {
    font-size: 20px;
    cursor: pointer;
    text-align: center;
    padding: 5px;
    border-radius: 4px;
}

.emoji-item:hover {
    background-color: var(--bg-hover);
}

/* Contacts Section */
.section-title {
    font-size: 12px;
    color: var(--text-secondary);
    padding: 10px 12px;
    font-weight: 600;
}

.request-item {
    background-color: rgba(16, 163, 127, 0.1);
}

.request-actions {
    display: flex;
    gap: 8px;
    margin-top: 4px;
}

/* Moments Styles */
.moments-container {
    padding: 0;
}

.moments-header {
    padding: 15px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid var(--border-color);
    font-weight: 600;
    position: sticky;
    top: 0;
    background-color: var(--bg-glass);
    backdrop-filter: blur(10px);
    z-index: 5;
}

.moment-card {
    padding: 15px;
    border-bottom: 1px solid var(--border-color);
}

.moment-header {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
}

.moment-user {
    display: flex;
    flex-direction: column;
}

.moment-user .nickname {
    font-weight: 600;
    color: var(--accent-color);
    font-size: 14px;
}

.moment-user .time {
    font-size: 11px;
    color: var(--text-secondary);
}

.moment-content {
    font-size: 14px;
    margin-bottom: 10px;
    white-space: pre-wrap;
    line-height: 1.5;
}

.moment-images {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 5px;
}

.moment-img {
    width: 100%;
    aspect-ratio: 1;
    border-radius: 4px;
    cursor: pointer;
}

.moment-footer {
    margin-top: 10px;
}

.action-bar {
    display: flex;
    gap: 20px;
    padding: 5px 0;
}

.action-btn {
    display: flex;
    align-items: center;
    gap: 5px;
    cursor: pointer;
    color: var(--text-secondary);
    font-size: 13px;
}

.action-btn:hover, .action-btn.active {
    color: var(--accent-color);
}

.comments-section {
    background-color: var(--bg-hover);
    padding: 8px;
    border-radius: 4px;
    margin-top: 5px;
    font-size: 13px;
}

.comment-item {
    margin-bottom: 4px;
}

.comment-user {
    color: var(--accent-color);
    font-weight: 500;
    margin-right: 5px;
}

.comment-content {
    color: var(--text-primary);
}

.comment-input-area {
    margin-top: 8px;
}

/* Moment Send Button Override */
:deep(.moment-send-btn) {
    border-radius: 0 4px 4px 0;
    height: 100%;
    margin: 0 !important;
}

:deep(.el-input-group__append) {
    padding: 0;
    border: none;
    background-color: transparent;
}

/* Upload Area in Dialog */
.upload-area {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 15px;
}

.upload-btn {
    width: 80px;
    height: 80px;
    border: 1px dashed var(--border-color);
    border-radius: 4px;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    color: var(--text-secondary);
}

.upload-btn:hover {
    border-color: var(--accent-color);
    color: var(--accent-color);
}

.image-preview {
    width: 80px;
    height: 80px;
    position: relative;
    border-radius: 4px;
    overflow: hidden;
}

.preview-img {
    width: 100%;
    height: 100%;
}

.remove-img {
    position: absolute;
    top: 0;
    right: 0;
    background: rgba(0,0,0,0.5);
    color: white;
    padding: 2px;
    cursor: pointer;
}

.chat-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: transparent; /* Changed from var(--bg-primary) to show TechBackground */
  color: var(--text-primary);
  overflow: hidden;
  transition: background-color 0.3s, color 0.3s;
  position: relative;
  z-index: 1;
}

/* Ensure sub-components have semi-transparent backgrounds to show the network */
 .sidebar, .sub-sidebar, .main-area, .chat-header, .input-section {
     background-color: var(--bg-glass);
     backdrop-filter: blur(10px);
 }
 
 /* User List in Dialog */
.user-item {
    display: flex;
    align-items: center;
    padding: 10px;
    border-bottom: 1px solid var(--border-color);
    gap: 12px;
}

.user-info {
    flex: 1;
    display: flex;
    flex-direction: column;
}

.user-info .nickname {
    font-weight: 500;
    color: var(--text-primary);
}

.user-info .username {
    font-size: 12px;
    color: var(--text-secondary);
}

/* 1. 一级侧边栏 (图标导航) */
.sidebar {
  width: 68px;
  /* background-color: var(--bg-primary); Removed for transparency */
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  flex-shrink: 0;
  z-index: 10;
}

.logo-area {
  margin-bottom: 32px;
}

.user-avatar {
  border: 2px solid transparent;
  transition: all 0.3s ease;
  cursor: pointer;
}

.user-avatar:hover {
  border-color: var(--accent-color);
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  align-items: center;
}

.nav-item {
  width: 44px;
  height: 44px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 12px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
}

.nav-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background-color: var(--accent-color);
  color: #ffffff;
  box-shadow: 0 4px 12px var(--shadow-color);
}

.nav-item .el-icon {
  font-size: 22px;
}

.bottom-actions {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.logout:hover {
  color: #ef4444;
  background-color: rgba(239, 68, 68, 0.1);
}

/* 2. 二级侧边栏 (列表) */
.sub-sidebar {
  width: 280px;
  /* background-color: var(--bg-secondary); Removed */
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.search-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-wrapper {
  flex: 1;
  background-color: var(--input-bg);
  border-radius: 6px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid transparent;
  transition: border-color 0.2s;
}

.search-wrapper:focus-within {
  border-color: var(--accent-color);
}

.search-icon {
  color: var(--text-secondary);
  font-size: 14px;
  margin-right: 8px;
}

.search-input {
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  width: 100%;
  font-size: 13px;
}

.add-btn {
  width: 32px;
  height: 32px;
  background-color: var(--input-bg);
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.add-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.list-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}

.list-item {
  display: flex;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.list-item:hover {
  background-color: var(--bg-tertiary);
}

.list-item.active {
  background-color: var(--bg-hover);
  background-color: var(--bg-tertiary); /* Fix for light mode visibility */
}

.list-item.pinned {
    background-color: rgba(0, 0, 0, 0.03);
}

.dark .list-item.pinned {
    background-color: rgba(255, 255, 255, 0.03);
}


.item-avatar {
  margin-right: 12px;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-title {
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
}

.item-time {
  font-size: 11px;
  color: var(--text-secondary);
}

.item-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
}

/* 3. 主聊天区域 */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  /* background-color: var(--bg-primary); Removed */
  position: relative;
}

.chat-header {
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  /* background-color: var(--bg-primary); Removed */
}

.header-left {
    display: flex;
    align-items: center;
    gap: 10px;
}

.header-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag {
  font-size: 10px;
  background-color: rgba(16, 163, 127, 0.15);
  color: var(--accent-color);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 500;
  margin-left: 8px;
}

.header-actions .el-button {
  color: var(--text-secondary);
}

.header-actions .el-button:hover {
  color: var(--text-primary);
  background-color: var(--bg-tertiary);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-row {
  display: flex;
  gap: 12px;
  max-width: 80%;
  align-items: flex-end; 
}

.message-row.recall-row {
    align-self: center;
    max-width: 100%;
    justify-content: center;
    margin: 10px 0;
}

.message-row.received {
  align-self: flex-start;
}

.message-row.sent {
  align-self: flex-end;
  justify-content: flex-end; 
}

.msg-avatar {
  flex-shrink: 0;
  cursor: pointer;
}

.bubble {
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  position: relative;
  word-wrap: break-word;
}

.image-bubble {
    border-radius: 12px;
    overflow: hidden;
    max-width: 200px;
    border: 1px solid var(--border-color);
}

.msg-image {
    display: block;
    width: 100%;
    height: auto;
}

/* Markdown Styles */
.markdown-body {
    font-size: 14px;
    line-height: 1.6;
    word-wrap: break-word;
}

.markdown-body p {
    margin: 0 0 8px 0;
}
.markdown-body p:last-child {
    margin-bottom: 0;
}
.markdown-body code {
    background-color: rgba(0, 0, 0, 0.1);
    padding: 2px 4px;
    border-radius: 4px;
    font-family: monospace;
}
.markdown-body pre {
    background-color: #282c34;
    color: #abb2bf;
    padding: 10px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;
}
.markdown-body pre code {
    background-color: transparent;
    padding: 0;
    color: inherit;
}
.markdown-body ul, .markdown-body ol {
    margin: 8px 0;
    padding-left: 20px;
}
.markdown-body blockquote {
    margin: 8px 0;
    padding-left: 10px;
    border-left: 3px solid var(--accent-color);
    color: var(--text-secondary);
}
.markdown-body a {
    color: var(--accent-color);
    text-decoration: none;
}
.markdown-body a:hover {
    text-decoration: underline;
}

/* Adjust bubble color for code blocks visibility */
.received .bubble {
  background-color: var(--bubble-received);
  color: var(--bubble-text-received);
  border-bottom-left-radius: 2px;
}

.sent .bubble {
  background-color: var(--bubble-sent);
  color: var(--bubble-text-sent);
  border-bottom-right-radius: 2px;
}

.sent .markdown-body code {
    background-color: rgba(255, 255, 255, 0.2);
}

.sent .markdown-body a {
    color: #ffffff;
    text-decoration: underline;
}

/* 4. 输入框区域 */
.input-section {
  padding: 20px 24px;
  /* background-color: var(--bg-primary); Removed */
  border-top: 1px solid var(--border-color);
}

.toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.tool-icon {
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 18px;
  transition: color 0.2s;
  display: flex;
  align-items: center;
}

.tool-icon:hover {
  color: var(--text-primary);
}

.message-input {
  width: 100%;
  height: 80px;
  background-color: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

.message-input::placeholder {
  color: var(--text-secondary);
}

.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.send-btn {
  background-color: var(--accent-color);
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:hover {
  background-color: var(--accent-hover);
}

.send-btn:disabled {
  background-color: var(--bg-tertiary);
  color: var(--text-secondary);
  cursor: not-allowed;
}

/* Profile Dialog Styles */
.profile-content {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.profile-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;
}

.profile-avatar {
    margin-bottom: 10px;
    border: 2px solid var(--border-color);
}

:deep(.custom-dialog) {
    background-color: var(--bg-secondary);
    border-radius: 12px;
    max-width: 90vw;
}

:deep(.custom-dialog .el-dialog__title) {
    color: var(--text-primary);
}

:deep(.custom-dialog .el-dialog__body) {
    padding-top: 10px;
}

:deep(.el-form-item__label) {
    color: var(--text-primary);
}

:deep(.el-input__wrapper), :deep(.el-textarea__inner) {
    background-color: var(--input-bg);
    box-shadow: 0 0 0 1px var(--border-color) inset;
}

:deep(.el-input__inner), :deep(.el-textarea__inner) {
    color: var(--text-primary);
}

/* Settings List */
.settings-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    background-color: var(--input-bg);
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-primary);
}

.setting-item:hover {
    background-color: var(--bg-tertiary);
}

.logout-item {
    color: #ef4444;
}

/* Mobile Adaptation */
.bottom-nav {
    display: none;
}

@media (min-width: 769px) {
    .hidden-mobile {
        display: flex !important;
    }

    .show-mobile {
        display: none !important;
    }
}

@media (max-width: 768px) {
    .hidden-mobile {
        display: none !important;
    }

    .show-mobile {
        display: flex !important;
    }

    .sidebar {
        display: none;
    }

    .sub-sidebar {
        width: 100%;
        border-right: none;
        flex: 1;
    }

    .main-area {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        z-index: 20;
        transform: translateX(100%);
        transition: transform 0.3s ease-in-out;
        background-color: var(--bg-primary); /* Ensure solid background for chat */
    }

    .main-area.mobile-open {
        transform: translateX(0);
    }

    .chat-layout {
        flex-direction: column;
    }

    .bottom-nav {
        height: 60px;
        background-color: var(--bg-secondary);
        border-top: 1px solid var(--border-color);
        display: flex;
        justify-content: space-around;
        align-items: center;
        flex-shrink: 0;
        padding-bottom: env(safe-area-inset-bottom);
    }

    .bottom-nav .nav-item {
        flex-direction: column;
        gap: 4px;
        font-size: 10px;
        color: var(--text-secondary);
        background: transparent;
        height: auto;
    }

    .bottom-nav .nav-item.active {
        color: var(--accent-color);
        background: transparent;
        box-shadow: none;
    }

    .bottom-nav .el-icon {
        font-size: 20px;
    }

    .chat-header {
        padding: 0 16px;
    }

    .back-btn {
        margin-right: 8px;
    }
    
    .input-section {
        padding: 10px 16px;
        padding-bottom: calc(10px + env(safe-area-inset-bottom));
    }
    
    .message-input {
        height: 50px;
    }
}
</style>

<style>
/* Global overrides for Element Plus components in dark mode */
html.dark .el-message-box,
html.dark .el-dialog {
    background-color: #1e1e1e;
    border-color: #333;
    --el-text-color-primary: #e5e5e5;
    --el-text-color-regular: #cfcfcf;
    color: #e5e5e5;
}
html.dark .el-message-box__title,
html.dark .el-dialog__title {
    color: #e5e5e5;
}
html.dark .el-message-box__content,
html.dark .el-dialog__body {
    color: #cfcfcf;
}
html.dark .el-message-box__close,
html.dark .el-dialog__headerbtn .el-dialog__close {
    color: #999;
}
html.dark .el-message-box__close:hover,
html.dark .el-dialog__headerbtn .el-dialog__close:hover {
    color: #fff;
}
.dialog-friend-name {
    color: var(--text-primary);
    font-size: 14px !important; /* Force font size to prevent 0px inheritance */
    line-height: 1.5;
    display: inline-block;
    width: auto;
    height: auto;
    flex: 1; /* Ensure it takes available space */
}
html.dark .dialog-friend-name {
    color: #e5e5e5;
}
/* Override Element Plus Checkbox label hiding */
.friend-select-item .el-checkbox__label {
    display: none;
}
/* Ensure container has font size */
.friend-select-item {
    font-size: 14px;
}
</style>
