<template>
  <div class="banner-image-cropper">
    <div class="banner-image-cropper__preview">
      <img v-if="displayUrl" :src="displayUrl" alt="轮播图预览">
      <div v-else class="banner-image-cropper__empty">请选择本地图片并完成裁剪</div>
    </div>
    <el-upload
      action="#"
      accept="image/jpeg,image/png"
      :auto-upload="false"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-change="handleFileChange"
    >
      <el-button size="small" type="primary" icon="el-icon-upload2">选择并裁剪</el-button>
    </el-upload>
    <div class="banner-image-cropper__hint">输出尺寸：1120×550，仅支持JPG、PNG，最大5MB</div>

    <el-dialog
      title="裁剪轮播图"
      :visible.sync="cropOpen"
      width="900px"
      append-to-body
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <div class="banner-image-cropper__canvas">
        <vue-cropper
          v-if="sourceUrl"
          ref="cropper"
          :img="sourceUrl"
          :auto-crop="true"
          :fixed="true"
          :fixed-number="[112, 55]"
          :fixed-box="true"
          :can-move-box="false"
          output-type="jpeg"
        />
      </div>
      <div class="banner-image-cropper__tools">
        <el-button size="small" icon="el-icon-plus" @click="changeScale(1)">放大</el-button>
        <el-button size="small" icon="el-icon-minus" @click="changeScale(-1)">缩小</el-button>
        <el-button size="small" icon="el-icon-refresh-left" @click="rotateLeft">左转</el-button>
        <el-button size="small" icon="el-icon-refresh-right" @click="rotateRight">右转</el-button>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cropOpen = false">取 消</el-button>
        <el-button type="primary" :loading="processing" @click="confirmCrop">确认裁剪</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { VueCropper } from 'vue-cropper'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024

export default {
  name: 'BannerImageCropper',
  components: { VueCropper },
  props: {
    value: { type: String, default: '' }
  },
  data() {
    return {
      cropOpen: false,
      processing: false,
      sourceUrl: '',
      resultPreviewUrl: ''
    }
  },
  computed: {
    displayUrl() {
      return this.resultPreviewUrl || this.value
    }
  },
  beforeDestroy() {
    this.releaseSourceUrl()
    this.releaseResultUrl()
  },
  methods: {
    beforeUpload(file) {
      return this.validateFile(file)
    },
    handleFileChange(file) {
      const raw = file && file.raw ? file.raw : file
      if (!this.validateFile(raw)) return
      this.releaseSourceUrl()
      this.sourceUrl = URL.createObjectURL(raw)
      this.cropOpen = true
    },
    validateFile(file) {
      const allowed = file && (file.type === 'image/jpeg' || file.type === 'image/png')
      if (!allowed) {
        this.$modal.msgError('请选择JPG或PNG图片')
        return false
      }
      if (!file.size || file.size > MAX_IMAGE_BYTES) {
        this.$modal.msgError('轮播图图片不能超过5MB')
        return false
      }
      return true
    },
    changeScale(step) {
      if (this.$refs.cropper) this.$refs.cropper.changeScale(step)
    },
    rotateLeft() {
      if (this.$refs.cropper) this.$refs.cropper.rotateLeft()
    },
    rotateRight() {
      if (this.$refs.cropper) this.$refs.cropper.rotateRight()
    },
    confirmCrop() {
      if (!this.$refs.cropper || this.processing) return
      this.processing = true
      this.$refs.cropper.getCropBlob(blob => this.normalizeCrop(blob))
    },
    normalizeCrop(blob) {
      const croppedUrl = URL.createObjectURL(blob)
      const image = new Image()
      image.onload = () => {
        const canvas = document.createElement('canvas')
        canvas.width = 1120
        canvas.height = 550
        const context = canvas.getContext('2d')
        context.drawImage(image, 0, 0, canvas.width, canvas.height)
        canvas.toBlob(result => {
          URL.revokeObjectURL(croppedUrl)
          this.processing = false
          if (!result) {
            this.$modal.msgError('轮播图裁剪失败，请重试')
            return
          }
          this.releaseResultUrl()
          this.resultPreviewUrl = URL.createObjectURL(result)
          this.$emit('change', { blob: result, previewUrl: this.resultPreviewUrl })
          this.cropOpen = false
        }, 'image/jpeg', 0.9)
      }
      image.onerror = () => {
        URL.revokeObjectURL(croppedUrl)
        this.processing = false
        this.$modal.msgError('轮播图裁剪失败，请重新选择图片')
      }
      image.src = croppedUrl
    },
    handleDialogClosed() {
      this.processing = false
      this.releaseSourceUrl()
    },
    reset() {
      this.cropOpen = false
      this.processing = false
      this.releaseSourceUrl()
      this.releaseResultUrl()
    },
    releaseSourceUrl() {
      if (this.sourceUrl) URL.revokeObjectURL(this.sourceUrl)
      this.sourceUrl = ''
    },
    releaseResultUrl() {
      if (this.resultPreviewUrl) URL.revokeObjectURL(this.resultPreviewUrl)
      this.resultPreviewUrl = ''
    }
  }
}
</script>

<style scoped lang="scss">
.banner-image-cropper__preview {
  width: 100%;
  max-width: 620px;
  aspect-ratio: 112 / 55;
  margin-bottom: 12px;
  overflow: hidden;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #f5f7fa;
}

.banner-image-cropper__preview img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-image-cropper__empty {
  display: flex;
  min-height: 180px;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.banner-image-cropper__hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.banner-image-cropper__canvas {
  height: 420px;
}

.banner-image-cropper__tools {
  margin-top: 12px;
  text-align: center;
}
</style>
