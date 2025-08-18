<template>
  <div id="userProfilePage">
    <h2>个人设置</h2>
    <a-form :model="form" layout="vertical" @finish="onSubmit" style="max-width: 520px">
      <a-form-item label="头像">
        <a-upload
          name="file"
          :show-upload-list="false"
          :before-upload="beforeUpload"
          :custom-request="doUpload"
        >
          <a-avatar :size="80" :src="form.userAvatar" style="cursor: pointer" />
          <div style="margin-top: 8px">
            <a-button>上传头像</a-button>
          </div>
        </a-upload>
      </a-form-item>
      <a-form-item label="昵称">
        <a-input v-model:value="form.userName" placeholder="请输入昵称" />
      </a-form-item>
      <a-form-item label="个人简介">
        <a-textarea v-model:value="form.userProfile" :rows="4" placeholder="一句话介绍你自己" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="saving">保存</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/request'
import { getLoginUser, updateUser } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

interface ProfileForm {
  userName?: string
  userAvatar?: string
  userProfile?: string
}

const form = reactive<ProfileForm>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})
const saving = ref(false)

onMounted(async () => {
  const res = await getLoginUser()
  if (res.data.code === 0 && res.data.data) {
    const u = res.data.data
    form.userName = u.userName || ''
    form.userAvatar = u.userAvatar || ''
    form.userProfile = u.userProfile || ''
  }
})

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('仅支持图片文件')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片需小于 2MB')
    return false
  }
  return true
}

const doUpload = async (options: any) => {
  const { file, onSuccess, onError } = options
  try {
    const formData = new FormData()
    formData.append('file', file as File)
    const resp = await request.post('/user/upload/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const url = resp.data?.data
    if (url) {
      form.userAvatar = url
      message.success('头像上传成功')
      // 立即保存到后端并同步到全局用户信息
      const saveResp = await request.post('/user/update/my', {
        userAvatar: form.userAvatar,
        userName: form.userName,
        userProfile: form.userProfile,
      })
      if (saveResp.data?.code === 0) {
        loginUserStore.setLoginUser({
          ...loginUserStore.loginUser,
          userAvatar: form.userAvatar,
        })
      }
      onSuccess?.(resp, file)
    } else {
      throw new Error('上传失败')
    }
  } catch (e) {
    console.error(e)
    message.error('上传失败')
    onError?.(e)
  }
}

const onSubmit = async () => {
  saving.value = true
  try {
    // 后端提供了管理员更新接口，这里沿用同结构的数据给自助更新接口
    const resp = await request.post('/user/update/my', {
      userName: form.userName,
      userAvatar: form.userAvatar,
      userProfile: form.userProfile,
    })
    if (resp.data?.code === 0) {
      message.success('保存成功')
      // 同步到全局登录用户
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userName: form.userName,
        userAvatar: form.userAvatar,
        userProfile: form.userProfile,
      })
    } else {
      message.error(resp.data?.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
#userProfilePage {
  max-width: 960px;
  margin: 24px auto;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
}
</style>
