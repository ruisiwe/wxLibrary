const courses = require('../../services/course');
Page({
  data: { videoId: '', playUrl: '', progressSeconds: 0, duration: 0, loading: true, error: '' },
  onLoad(options) {
    this.setData({ videoId: options.videoId });
    courses.progress().then(items => {
      const progress = (items || []).find(item => String(item.videoId) === String(this.data.videoId));
      if (progress) this.setData({ progressSeconds: progress.progressSeconds || 0 });
    }).catch(() => {}).then(() => this.authorize());
  },
  onUnload() { this.persist(false); },
  authorize() {
    this.setData({ loading: true, error: '', playUrl: '' });
    courses.play(this.data.videoId).then(result => this.setData({ playUrl: result.playUrl, loading: false }))
      .catch(error => this.setData({ loading: false, error: error.message }));
  },
  timeUpdate(event) {
    const progressSeconds = Math.floor(event.detail.currentTime || 0);
    this.setData({ progressSeconds, duration: Math.floor(event.detail.duration || 0) });
    if (progressSeconds > 0 && progressSeconds % 15 === 0 && this.lastSaved !== progressSeconds) {
      this.lastSaved = progressSeconds; this.persist(false);
    }
  },
  ended() { this.persist(true); },
  persist(finished) {
    if (!this.data.videoId) return;
    courses.saveProgress(this.data.videoId, this.data.progressSeconds, finished).catch(() => {});
  }
});
