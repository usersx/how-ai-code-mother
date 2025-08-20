<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" class="code-gen-type-tag" color="blue">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
          :disabled="!isOwner"
          ghost
          @click="downloadCode"
          :loading="downloading"
          type="primary"
        >
          <template #icon>
            <DownloadOutlined/>
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 工作流进度（Agent 模式） -->
        <div v-if="workflow.visible" class="workflow-panel">
          <div class="workflow-header">
            <span class="workflow-title">工作流进度</span>
            <span class="workflow-status" v-if="workflow.completed">已完成</span>
          </div>
          <a-steps direction="vertical" :current="Math.max(workflow.current - 1, 0)">
            <a-step
              v-for="s in workflow.steps"
              :key="s.number"
              :title="s.title"
              :status="s.status"
            />
          </a-steps>
        </div>
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button :loading="loadingHistory" size="small" type="link" @click="loadMoreHistory">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button
              v-if="isOwner && previewUrl"
              :class="{ 'edit-mode-active': isEditMode }"
              :danger="isEditMode"
              type="link"
              @click="toggleEditMode"
              style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined/>
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <!-- 代码文件 Tabs 展示 -->
        <div class="code-tabs" v-if="codeFiles.length">
          <a-tabs v-model:activeKey="activeCodeTab">
            <a-tab-pane v-for="file in codeFiles" :key="file.filename" :tab="file.filename">
              <div class="code-tab-toolbar">
                <a-button size="small" @click="copyCode(file.content)">复制</a-button>
              </div>
              <pre class="hljs code-pre"><code v-html="highlightCode(file.content, file.language)"></code></pre>
            </a-tab-pane>
          </a-tabs>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {message} from 'ant-design-vue'
import {useLoginUserStore} from '@/stores/loginUser'
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById,
} from '@/api/appController'
import {listAppChatHistory} from '@/api/chatHistoryController'
import {CodeGenTypeEnum, formatCodeGenType} from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import aiAvatar from '@/assets/aiAvatar.png'
import {API_BASE_URL, getStaticPreviewUrl} from '@/config/env'
import {type ElementInfo, VisualEditor} from '@/utils/visualEditor'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const appId = ref<any>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 代码文件 Tabs 数据
interface CodeFile {
  filename: string
  language: string
  content: string
}
const codeFiles = ref<CodeFile[]>([])
const activeCodeTab = ref<string>('')
// 工具输出缓冲区（处理流式分片的代码块）
const toolOutputBuffer = ref('')
// 工作流事件缓冲（SSE 可能分片，需等到“\n\n”完整块再解析）
const workflowBuffer = ref('')

// 工作流渲染状态
type StepStatus = 'wait' | 'process' | 'finish' | 'error'
interface WorkflowStep { number: number; title: string; status: StepStatus }
const workflow = ref({
  visible: false,
  current: 0,
  steps: [] as WorkflowStep[],
  completed: false,
})

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            createTime: chat.createTime,
          }))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({id: id as unknown as number})
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数（开启 agent 模式）
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
      agent: 'true',
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 新一轮生成前，重置代码 Tabs 与缓冲区
    codeFiles.value = []
    activeCodeTab.value = ''
    toolOutputBuffer.value = ''

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content: string = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          // 将原始分片先放入工作流缓冲，提取并移除所有完整的 workflow 事件块
          workflowBuffer.value += content
          const cleaned = extractWorkflowFromBuffer()
          if (cleaned) {
            fullContent += cleaned
            // 仅将“非工作流事件”的内容用于代码块解析
            toolOutputBuffer.value += cleaned
            extractToolCodeBlocksFromBuffer()
            messages.value[aiMessageIndex].content = fullContent
            messages.value[aiMessageIndex].loading = false
            scrollToBottom()
          } else {
            // 仅收到工作流事件时，保持 loading 态，避免出现“空白但不转圈”的错觉
            messages.value[aiMessageIndex].loading = true
          }
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}
// 从缓冲区提取完整的 workflow 事件块（event: xxx + data: {...}\n\n），并返回剩余的“非事件”文本
const extractWorkflowFromBuffer = (): string => {
  let output = ''
  let s = workflowBuffer.value
  while (true) {
    const eventPos = s.indexOf('event:')
    if (eventPos === -1) {
      // 没有事件起始，全部属于普通文本
      output += s
      s = ''
      break
    }
    // 先输出 event 前的普通文本
    if (eventPos > 0) {
      output += s.slice(0, eventPos)
      s = s.slice(eventPos)
    }
    // 查找事件块结束（按 \n\n 分隔）
    const endPos = s.indexOf('\n\n')
    if (endPos === -1) {
      // 事件块不完整，保留在缓冲中等待下个分片
      break
    }
    const block = s.slice(0, endPos + 2)
    s = s.slice(endPos + 2)
    // 解析事件名与 data JSON
    try {
      const lines = block.split('\n')
      const eventLine = lines.find(l => l.startsWith('event:')) || ''
      const dataLine = lines.find(l => l.startsWith('data:')) || ''
      const eventName = eventLine.replace('event:', '').trim()
      const jsonText = dataLine.replace('data:', '').trim().replace(/[“”]/g, '"')
      const payload = JSON.parse(jsonText)
      handleWorkflowEvent(eventName, payload)
    } catch {
      // 解析失败，作为普通文本回退
      output += block
    }
  }
  workflowBuffer.value = s
  return output
}

interface WorkflowPayload { currentStep?: string; step?: string; stepNumber?: number; number?: number; message?: string }
const handleWorkflowEvent = (eventName: string, payload: WorkflowPayload) => {
  if (!workflow.value.visible && (eventName === 'workflow_start' || eventName === 'step_completed')) {
    workflow.value.visible = true
  }
  if (eventName === 'workflow_start') {
    workflow.value.current = 0
    workflow.value.steps = []
    workflow.value.completed = false
    return
  }
  if (eventName === 'step_completed') {
    const title = payload?.currentStep || payload?.step || '步骤'
    const num = Number(payload?.stepNumber || payload?.number || workflow.value.steps.length + 1)
    const idx = workflow.value.steps.findIndex(s => s.number === num)
    if (idx === -1) {
      workflow.value.steps.push({ number: num, title, status: 'finish' })
    } else {
      workflow.value.steps[idx].title = title
      workflow.value.steps[idx].status = 'finish'
    }
    workflow.value.current = Math.max(workflow.value.current, num)
    // 将之前步骤设为完成、之后为等待
    workflow.value.steps = workflow.value.steps
      .sort((a, b) => a.number - b.number)
      .map(s => ({ ...s, status: s.number <= workflow.value.current ? 'finish' as StepStatus : s.status || 'wait' }))
    return
  }
  if (eventName === 'workflow_completed') {
    workflow.value.completed = true
    // 所有步骤标记完成
    workflow.value.steps = workflow.value.steps.map(s => ({ ...s, status: 'finish' }))
    return
  }
  if (eventName === 'workflow_error') {
    const num = Number(payload?.stepNumber || workflow.value.current)
    const idx = workflow.value.steps.findIndex(s => s.number === num)
    if (idx !== -1) workflow.value.steps[idx].status = 'error'
  }
}
// 从缓冲区提取“工具调用”的代码块，填充到 codeFiles Tabs
const extractToolCodeBlocksFromBuffer = () => {
  // 形如：
  // [工具调用] 写入文件 path/to/file.js\n```js\n...code...\n```
  // 注意：流式分片可能不完整，这里仅在完整出现成对 ``` 时才提取
  let changed = false
  while (true) {
    const startIdx = toolOutputBuffer.value.indexOf('[工具调用]')
    if (startIdx === -1) break
    const fenceStart = toolOutputBuffer.value.indexOf('```', startIdx)
    if (fenceStart === -1) break
    const fenceEnd = toolOutputBuffer.value.indexOf('```', fenceStart + 3)
    if (fenceEnd === -1) break

    const block = toolOutputBuffer.value.substring(startIdx, fenceEnd + 3)
    // 解析文件名、语言、内容
    // 标题行可能为：[工具调用] 写入文件 相对路径
    const headerLineEnd = block.indexOf('\n')
    const header = headerLineEnd !== -1 ? block.substring(0, headerLineEnd) : block
    const filenameMatch = header.match(/\[工具调用\][^\n]*\s+([^`\s]+)$/)

    const langLineStart = block.indexOf('```') + 3
    const langLineEnd = block.indexOf('\n', langLineStart)
    const language = langLineEnd !== -1 ? block.substring(langLineStart, langLineEnd).trim() : ''
    const codeStart = langLineEnd + 1
    const codeEnd = block.lastIndexOf('```')
    const code = codeStart > 0 && codeEnd > codeStart ? block.substring(codeStart, codeEnd) : ''

    if (filenameMatch && code) {
      const filename = filenameMatch[1]
      upsertCodeFile({ filename, language, content: code })
      changed = true
    }

    // 从缓冲中移除已处理的块，避免重复解析
    toolOutputBuffer.value = toolOutputBuffer.value.slice(0, startIdx) + toolOutputBuffer.value.slice(fenceEnd + 3)
  }
  if (changed && !activeCodeTab.value && codeFiles.value.length) {
    activeCodeTab.value = codeFiles.value[0].filename
  }
}

const upsertCodeFile = (file: CodeFile) => {
  const idx = codeFiles.value.findIndex(f => f.filename === file.filename)
  if (idx >= 0) {
    codeFiles.value[idx] = file
  } else {
    codeFiles.value.push(file)
  }
}

const escapeHtml = (code: string) =>
  code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const highlightCode = (code: string, language: string) => {
  try {
    if (language) {
      return hljs.highlight(code, { language, ignoreIllegals: true }).value
    }
    return hljs.highlightAuto(code).value
  } catch {
    return escapeHtml(code)
  }
}

const copyCode = async (code: string) => {
  try {
    await navigator.clipboard.writeText(code)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')

      // 部署成功后，延迟刷新应用信息以获取最新的封面
      setTimeout(async () => {
        await fetchAppInfo()
        // 通知其他页面刷新应用列表
        window.dispatchEvent(new CustomEvent('app-deployed', {
          detail: { appId: appId.value as number }
        }))
      }, 2000) // 延迟2秒，等待截图生成完成
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      // 通知首页刷新“我的作品”
      window.dispatchEvent(new CustomEvent('app-deleted'))
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.workflow-panel {
  padding: 12px 16px 0 16px;
  border-bottom: 1px solid #f0f0f0;
}
.workflow-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.workflow-title { font-weight: 600; color: #1a1a1a; }
.workflow-status { color: #52c41a; font-size: 12px; }

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.code-tabs {
  padding: 0 16px 8px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.code-tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.code-pre {
  background: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 12px;
  max-height: 280px;
  overflow: auto;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
