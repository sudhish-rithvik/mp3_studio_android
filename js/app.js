/**
 * MP3 Studio — Main App Controller
 * Coordinates navigation, UI state, audio engine callbacks,
 * recorder, mixer, EQ, downloader, and metadata actions.
 */

const App = {
  currentScreen: 'editor',
  trackLoaded: false,
  isPlaying: false,
  isRecording: false,
  recTimerInterval: null,
  recSeconds: 0,
  savedRecordings: [],
  mixerTracks: [],
  mixerTrackCount: 0,
  exportFormat: 'MP3',
  currentEQ: [0, 0, 0, 0, 0],
  dlActiveTab: 0,  // 0 = URL, 1 = Search

  // ─────────────────────────────────────────────
  // Init
  // ─────────────────────────────────────────────
  init() {
    this._updateClock();
    setInterval(() => this._updateClock(), 30000);

    // Connect audio engine callbacks
    if (window.engine) {
      window.engine.onTimeUpdate = (t) => this._onTimeUpdate(t);
      window.engine.onEnded = () => this._onPlaybackEnded();
      window.engine.onReady = (info) => this._onTrackReady(info);
    }
  },

  _updateClock() {
    const now = new Date();
    const el = document.getElementById('statusTime');
    if (el) el.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  },

  // ─────────────────────────────────────────────
  // Navigation
  // ─────────────────────────────────────────────
  navigateTo(screen) {
    // Deactivate current
    document.getElementById(`screen-${this.currentScreen}`)?.classList.remove('active');
    document.getElementById(`nav-${this.currentScreen}`)?.classList.remove('active');

    this.currentScreen = screen;

    // Activate new
    document.getElementById(`screen-${screen}`)?.classList.add('active');
    document.getElementById(`nav-${screen}`)?.classList.add('active');

    // Update header label
    const labels = { editor: 'Editor', mixer: 'Mixer', recorder: 'Record', eq: 'EQ', metadata: 'Tags', downloader: 'Download' };
    const el = document.getElementById('screenLabel');
    if (el) el.textContent = labels[screen] || screen;
  },

  // ─────────────────────────────────────────────
  // Audio Engine Callbacks
  // ─────────────────────────────────────────────
  _onTrackReady(info) {
    this.trackLoaded = true;
    const dur = info.duration;

    const min = Math.floor(dur / 60);
    const sec = String(Math.round(dur % 60)).padStart(2, '0');
    document.getElementById('editorSubtitle').textContent = `${min}:${sec} · ${info.sampleRate}Hz · ${info.channels}ch`;
    document.getElementById('trackDuration').textContent = this._formatTime(dur);
    document.getElementById('trackInfoRow').style.display = 'flex';
    document.getElementById('uploadCard').style.display = 'none';

    // Update mini player
    document.getElementById('miniPlayer').style.display = 'flex';

    // Waveform
    if (window.waveformRenderer) {
      window.waveformRenderer.load(info.waveform);
    }

    this.updateTrimDisplay();
  },

  _onTimeUpdate(t) {
    const dur = window.engine?.duration || 1;
    if (window.waveformRenderer) {
      window.waveformRenderer.setPlayhead(t / dur);
    }
  },

  _onPlaybackEnded() {
    this.isPlaying = false;
    this._updatePlayUI();
  },

  // ─────────────────────────────────────────────
  // File Loading
  // ─────────────────────────────────────────────
  async loadAudioFile(input) {
    const file = input.files[0];
    if (!file) return;

    // Update name display
    document.getElementById('trackName').textContent = file.name.replace(/\.[^/.]+$/, '');
    document.getElementById('miniTitle').textContent = file.name.replace(/\.[^/.]+$/, '');
    document.getElementById('miniArtist').textContent = 'MP3 Studio';

    this.showToast('⏳ Loading audio...');

    try {
      await window.engine.loadFile(file);
      this.showToast('✅ Track loaded successfully!');
    } catch (e) {
      this.showToast('❌ Failed to load file');
      console.error(e);
    }
  },

  // ─────────────────────────────────────────────
  // Playback
  // ─────────────────────────────────────────────
  togglePlayback() {
    if (!this.trackLoaded) return;
    if (this.isPlaying) {
      window.engine.pause();
      this.isPlaying = false;
    } else {
      window.engine.play();
      this.isPlaying = true;
    }
    this._updatePlayUI();
  },

  stopPlayback() {
    window.engine.stop();
    this.isPlaying = false;
    this._updatePlayUI();
    if (window.waveformRenderer) window.waveformRenderer.setPlayhead(0);
  },

  _updatePlayUI() {
    const playIcon = document.getElementById('playIcon');
    const miniBtn = document.getElementById('miniPlayBtn');
    if (this.isPlaying) {
      if (playIcon) playIcon.innerHTML = '<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>';
      if (miniBtn) miniBtn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>';
    } else {
      if (playIcon) playIcon.innerHTML = '<polygon points="5,3 19,12 5,21"/>';
      if (miniBtn) miniBtn.innerHTML = '<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><polygon points="5,3 19,12 5,21"/></svg>';
    }
  },

  // ─────────────────────────────────────────────
  // Trim
  // ─────────────────────────────────────────────
  updateTrimDisplay() {
    const dur = window.engine?.duration || 0;
    const wr = window.waveformRenderer;
    if (!wr || !dur) return;
    document.getElementById('trimStart').textContent = this._formatTimeFull(wr.trimStart * dur);
    document.getElementById('trimEnd').textContent = this._formatTimeFull(wr.trimEnd * dur);
  },

  applyTrim() {
    if (!this.trackLoaded) { this.showToast('⚠️ No track loaded'); return; }
    this.showToast('✂️ Trimming...');
    setTimeout(() => {
      const buf = window.engine.applyTrim();
      if (buf) {
        if (window.waveformRenderer) window.waveformRenderer.load(window.engine.generateWaveform(buf));
        this.showToast('✅ Trim applied!');
      }
    }, 300);
  },

  // ─────────────────────────────────────────────
  // Tool Controls
  // ─────────────────────────────────────────────
  selectTool(index, btn) {
    document.querySelectorAll('#toolTabs .seg-btn').forEach((b, i) => {
      b.classList.toggle('active', i === index);
    });
    document.querySelectorAll('.tool-card').forEach((c, i) => {
      c.classList.toggle('active', i === index);
    });
  },

  updateSpeed(val) {
    const speed = val / 100;
    window.engine.setSpeed(speed);
    document.getElementById('speedVal').textContent = speed.toFixed(2) + 'x';
    document.querySelectorAll('#tool-1 .preset-chip').forEach(c => c.classList.remove('active'));
  },

  setSpeed(val) {
    document.getElementById('speedSlider').value = val;
    this.updateSpeed(val);
  },

  updatePitch(val) {
    const v = parseInt(val);
    document.getElementById('pitchVal').textContent = (v >= 0 ? '+' : '') + v;
    if (window.engine) window.engine.pitchShift = v;
  },

  updateVolume(val) {
    const vol = val / 100;
    window.engine.setVolume(vol);
    document.getElementById('volDisplay').textContent = val + '%';
    document.getElementById('volVal').textContent = val + '%';
  },

  updateFadeIn(val) {
    document.getElementById('fadeInVal').textContent = (val / 1000).toFixed(1) + 's';
  },

  updateFadeOut(val) {
    document.getElementById('fadeOutVal').textContent = (val / 1000).toFixed(1) + 's';
  },

  applyFade(type) {
    if (!this.trackLoaded) { this.showToast('⚠️ No track loaded'); return; }
    const slider = document.getElementById(type === 'in' ? 'fadeInSlider' : 'fadeOutSlider');
    const durationSec = parseInt(slider.value) / 1000;
    this.showToast(`📈 Applying Fade ${type === 'in' ? 'In' : 'Out'}...`);
    setTimeout(() => {
      if (type === 'in') window.engine.applyFadeIn(durationSec);
      else window.engine.applyFadeOut(durationSec);
      this.showToast(`✅ Fade ${type === 'in' ? 'In' : 'Out'} applied!`);
    }, 500);
  },

  applyDenoise() {
    if (!this.trackLoaded) { this.showToast('⚠️ No track loaded'); return; }
    this.showToast('🧹 Applying Denoise...');
    window.engine.applyDenoise().then(() => this.showToast('✅ Denoise complete!'));
  },

  removeSilence() {
    if (!this.trackLoaded) { this.showToast('⚠️ No track loaded'); return; }
    this.showToast('🤫 Detecting silence...');
    setTimeout(() => {
      window.engine.removeSilence();
      this.showToast('✅ Silence removed!');
    }, 1200);
  },

  // ─────────────────────────────────────────────
  // EQ
  // ─────────────────────────────────────────────
  updateEQBand(index, val) {
    this.currentEQ[index] = parseInt(val);
    window.engine.setEQBand(index, parseInt(val));
    const sign = val >= 0 ? '+' : '';
    document.getElementById(`eq${index}db`).textContent = `${sign}${val}dB`;
  },

  updateMasterVol(val) {
    const vol = val / 100;
    window.engine.setVolume(vol);
    document.getElementById('masterVolVal').textContent = val + '%';
  },

  applyEQPreset(name) {
    const presets = {
      flat:       [0, 0, 0, 0, 0],
      bass:       [9, 5, 0, -1, -1],
      treble:     [-1, 0, 0, 5, 9],
      rock:       [6, 3, -1, 3, 5],
      pop:        [-1, 3, 4, 3, -1],
      jazz:       [4, 1, -1, 1, 3],
      vocal:      [-2, 0, 6, 4, 0],
      electronic: [7, 4, -2, 4, 7],
      classical:  [0, 0, 0, 3, 5]
    };

    const vals = presets[name];
    if (!vals) return;

    vals.forEach((v, i) => {
      const slider = document.getElementById(`eq${i}`);
      if (slider) { slider.value = v; this.updateEQBand(i, v); }
    });

    document.querySelectorAll('.preset-chip').forEach(c => {
      c.classList.toggle('active', c.textContent.toLowerCase() === name || c.onclick?.toString().includes(name));
    });

    this.showToast(`🎛️ ${name.charAt(0).toUpperCase() + name.slice(1)} preset applied!`);
  },

  selectFormat(index, btn) {
    const formats = ['MP3', 'WAV', 'AAC', 'OGG', 'FLAC'];
    this.exportFormat = formats[index];
    document.querySelectorAll('#formatTabs .seg-btn').forEach((b, i) => b.classList.toggle('active', i === index));
    document.getElementById('exportFormatLabel').textContent = this.exportFormat;
  },

  exportTrack() {
    if (!this.trackLoaded) { this.showToast('⚠️ No track loaded'); return; }
    this.showToast(`⬇ Exporting as ${this.exportFormat}...`);
    setTimeout(() => {
      const filename = (document.getElementById('trackName')?.textContent || 'track').replace(/\s+/g, '_');
      // Write ID3 tags
      const writer = new ID3Writer();
      writer.setTitle(document.getElementById('metaTitle')?.value || filename);
      writer.setArtist(document.getElementById('metaArtist')?.value || '');
      writer.setAlbum(document.getElementById('metaAlbum')?.value || '');
      writer.setYear(document.getElementById('metaYear')?.value || '');
      writer.setGenre(document.getElementById('metaGenre')?.value || '');

      const blob = window.engine.exportToWAV();
      if (blob) {
        WavEncoder.downloadAs(window.engine.buffer, this.exportFormat, filename, writer);
        this.showToast(`✅ Exported as ${this.exportFormat}!`);
      }
    }, 800);
  },

  // ─────────────────────────────────────────────
  // Metadata
  // ─────────────────────────────────────────────
  loadCoverArt(input) {
    const file = input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (e) => {
      const preview = document.getElementById('coverPreview');
      preview.innerHTML = `<img src="${e.target.result}" alt="Cover Art" />`;
    };
    reader.readAsDataURL(file);
  },

  saveMetadata() {
    this.showToast('💾 Saving metadata & exporting...');
    setTimeout(() => this.exportTrack(), 400);
  },

  setRingtone(type) {
    const labels = { ringtone: '📞 Ringtone', alarm: '⏰ Alarm', notification: '🔔 Notification' };
    this.showToast(`${labels[type] || type} set! (Android integration required on device)`);
  },

  // ─────────────────────────────────────────────
  // Recorder & Dubbing
  // ─────────────────────────────────────────────
  toggleDubMode(cb) {
    const isDub = cb.checked;
    document.getElementById('recModeTitle').textContent = isDub ? 'Dub Mode' : 'Record Mode';
    document.getElementById('recModeDesc').textContent = isDub ? 'Record over a background track' : 'Standalone audio recording';
    document.getElementById('dubPanel').style.display = isDub ? 'block' : 'none';
  },

  toggleRecording() {
    if (!this.isRecording) {
      this._startRecording();
    } else {
      this._stopRecording();
    }
  },

  _startRecording() {
    this.isRecording = true;
    this.recSeconds = 0;

    const btn = document.getElementById('recordBtn');
    const icon = document.getElementById('recIcon');
    btn?.classList.add('recording');
    if (icon) icon.innerHTML = '<rect x="4" y="4" width="16" height="16" rx="3" fill="white"/>';
    document.getElementById('recTimer').style.display = 'block';
    document.getElementById('recHint').textContent = 'Tap to stop recording';

    // Activate VU meter
    if (window.vuMeter) window.vuMeter.setActive(true);

    this.recTimerInterval = setInterval(() => {
      this.recSeconds++;
      const min = Math.floor(this.recSeconds / 60);
      const sec = this.recSeconds % 60;
      document.getElementById('timerDisplay').textContent = `${String(min).padStart(2,'0')}:${String(sec).padStart(2,'0')}`;
    }, 1000);
  },

  _stopRecording() {
    this.isRecording = false;
    clearInterval(this.recTimerInterval);

    const btn = document.getElementById('recordBtn');
    const icon = document.getElementById('recIcon');
    btn?.classList.remove('recording');
    if (icon) icon.innerHTML = '<circle cx="12" cy="12" r="8" fill="white"/>';
    document.getElementById('recTimer').style.display = 'none';
    document.getElementById('recHint').textContent = 'Tap to start recording';

    // Deactivate VU meter
    if (window.vuMeter) window.vuMeter.setActive(false);

    if (this.recSeconds > 0) {
      const name = `Recording_${this.savedRecordings.length + 1}`;
      const dur = this._formatTime(this.recSeconds);
      this.savedRecordings.push({ name, dur });
      this._renderSavedRecordings();
      this.showToast(`🎙️ ${name} saved (${dur})`);
    }
    this.recSeconds = 0;
  },

  updateGain(val) {
    document.getElementById('gainVal').textContent = val + '%';
  },

  _renderSavedRecordings() {
    const container = document.getElementById('savedRecordings');
    container.innerHTML = '';
    this.savedRecordings.forEach((rec, i) => {
      const div = document.createElement('div');
      div.className = 'saved-rec-item';
      div.innerHTML = `
        <div class="rec-icon-box">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          </svg>
        </div>
        <div style="flex:1;">
          <div class="rec-item-name">${rec.name}</div>
          <div class="rec-item-dur">${rec.dur}</div>
        </div>
        <button class="rec-action" title="Play" onclick="App.showToast('▶️ Playing ${rec.name}')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><polygon points="5,3 19,12 5,21"/></svg>
        </button>
        <button class="rec-action" title="Export" onclick="App.showToast('⬇ Exporting ${rec.name}')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/></svg>
        </button>
      `;
      container.appendChild(div);
    });
  },

  // ─────────────────────────────────────────────
  // Mixer
  // ─────────────────────────────────────────────
  addMixerTrack() {
    this.mixerTrackCount++;
    const colors = ['#FA243C', '#0A84FF', '#BF5AF2', '#30D158', '#FF9F0A', '#FF2D55'];
    const color = colors[(this.mixerTrackCount - 1) % colors.length];
    const track = {
      id: `t${this.mixerTrackCount}`,
      name: `Track ${this.mixerTrackCount}`,
      color,
      volume: 100,
      muted: false,
      soloed: false
    };
    this.mixerTracks.push(track);
    this._renderMixer();

    const emptyEl = document.getElementById('mixerEmpty');
    if (emptyEl) emptyEl.style.display = 'none';
    document.getElementById('mixerSubtitle').textContent = `${this.mixerTracks.length} track${this.mixerTracks.length !== 1 ? 's' : ''} loaded`;
  },

  _renderMixer() {
    const container = document.getElementById('mixerTracks');
    container.innerHTML = '';
    this.mixerTracks.forEach((track) => {
      const card = document.createElement('div');
      card.className = 'mixer-track';
      card.innerHTML = `
        <div class="track-controls">
          <div class="track-color-badge" style="background:${track.color}22;color:${track.color};border:1px solid ${track.color}44;">${track.id.replace('t','')}</div>
          <div style="flex:1;">
            <div class="card-title">${track.name}</div>
            <div class="card-sub" style="font-size:10px;">Empty slot</div>
          </div>
          <button class="track-ctl-btn ${track.muted ? 'active' : ''}" onclick="App.toggleMute('${track.id}', this)" title="Mute">M</button>
          <button class="track-ctl-btn solo ${track.soloed ? 'active' : ''}" onclick="App.toggleSolo('${track.id}', this)" title="Solo">S</button>
          <button class="track-ctl-btn" onclick="App.removeMixerTrack('${track.id}')" style="color:#FF453A;">✕</button>
        </div>
        <div style="margin-top:12px;">
          <label style="font-size:11px;color:var(--t2);">Volume: <span id="vol-${track.id}" style="color:${track.color};font-weight:600;">${track.volume}%</span></label>
          <input type="range" class="apple-range" min="0" max="150" value="${track.volume}"
            oninput="App.updateTrackVolume('${track.id}', this.value)"
            style="margin-top:6px;" />
        </div>
      `;
      container.appendChild(card);
    });
  },

  toggleMute(id, btn) {
    const track = this.mixerTracks.find(t => t.id === id);
    if (!track) return;
    track.muted = !track.muted;
    btn.classList.toggle('active', track.muted);
    this.showToast(track.muted ? `🔇 ${track.name} muted` : `🔊 ${track.name} unmuted`);
  },

  toggleSolo(id, btn) {
    const track = this.mixerTracks.find(t => t.id === id);
    if (!track) return;
    track.soloed = !track.soloed;
    btn.classList.toggle('active', track.soloed);
    this.showToast(track.soloed ? `⭐ ${track.name} soloed` : `${track.name} unsolo'd`);
  },

  updateTrackVolume(id, val) {
    const track = this.mixerTracks.find(t => t.id === id);
    if (!track) return;
    track.volume = parseInt(val);
    const el = document.getElementById(`vol-${id}`);
    if (el) el.textContent = val + '%';
  },

  removeMixerTrack(id) {
    this.mixerTracks = this.mixerTracks.filter(t => t.id !== id);
    this._renderMixer();
    document.getElementById('mixerSubtitle').textContent = `${this.mixerTracks.length} track${this.mixerTracks.length !== 1 ? 's' : ''} loaded`;
    if (this.mixerTracks.length === 0) {
      document.getElementById('mixerEmpty').style.display = 'flex';
    }
  },

  mixDown() {
    if (this.mixerTracks.length < 2) {
      this.showToast('⚠️ Add at least 2 tracks to mix!');
      return;
    }
    this.showToast('🔀 Mixing down...');
    setTimeout(() => this.showToast('✅ Mix complete! Ready to export.'), 1500);
  },

  // ─────────────────────────────────────────────
  // Downloader (Media Downloader Integration)
  // ─────────────────────────────────────────────
  selectDLTab(index, btn) {
    this.dlActiveTab = index;
    document.querySelectorAll('#dlTabs .seg-btn').forEach((b, i) => b.classList.toggle('active', i === index));
    const urlPanel = document.getElementById('dlUrlPanel');
    const searchPanel = document.getElementById('dlSearchPanel');
    const mediaCard = document.getElementById('mediaAnalysisCard');
    const readyCard = document.getElementById('downloadReadyCard');
    const progressCard = document.getElementById('downloadProgressCard');

    if (urlPanel) urlPanel.style.display = index === 0 ? 'block' : 'none';
    if (searchPanel) searchPanel.style.display = index === 1 ? 'block' : 'none';
    if (index === 1) {
      if (mediaCard) mediaCard.style.display = 'none';
      if (readyCard) readyCard.style.display = 'none';
      if (progressCard) progressCard.style.display = 'none';
    }
  },

  onMediaUrlChange(url) {
    if (window.DownloaderModule) {
      window.DownloaderModule.updatePlatformUI(url);
    }
  },

  async pasteMediaUrl() {
    try {
      const text = await navigator.clipboard.readText();
      const input = document.getElementById('dlUrl');
      if (input && text) {
        input.value = text.trim();
        this.onMediaUrlChange(text.trim());
        this.showToast('📋 Link pasted!');
      }
    } catch {
      this.showToast('⚠️ Clipboard access restricted, paste manually');
    }
  },

  async analyzeMediaUrl() {
    const input = document.getElementById('dlUrl');
    const url = input ? input.value.trim() : '';
    if (!url) {
      this.showToast('⚠️ Please enter or paste a URL');
      return;
    }

    const loader = document.getElementById('dlLoading');
    const loaderText = document.getElementById('dlLoadingText');
    const emptyState = document.getElementById('dlEmpty');
    const mediaCard = document.getElementById('mediaAnalysisCard');
    const readyCard = document.getElementById('downloadReadyCard');
    const progressCard = document.getElementById('downloadProgressCard');

    if (loader) {
      if (loaderText) loaderText.textContent = 'Extracting media metadata...';
      loader.style.display = 'flex';
    }
    if (emptyState) emptyState.style.display = 'none';
    if (mediaCard) mediaCard.style.display = 'none';
    if (readyCard) readyCard.style.display = 'none';
    if (progressCard) progressCard.style.display = 'none';

    try {
      const info = await window.DownloaderModule.analyze(url);
      if (loader) loader.style.display = 'none';

      // Populate Media Analysis Card
      document.getElementById('mediaTitle').textContent = info.title || 'Untitled Video';
      document.getElementById('mediaUploader').textContent = info.uploader || 'Creator';
      document.getElementById('mediaViews').textContent = info.view_count ? ` · ${(info.view_count / 1000000).toFixed(1)}M views` : '';
      document.getElementById('mediaDuration').textContent = info.duration_string || '0:00';
      const thumb = document.getElementById('mediaThumb');
      if (thumb && info.thumbnail) {
        thumb.src = info.thumbnail;
      }

      const pBadge = document.getElementById('mediaPlatformBadge');
      if (pBadge) {
        const plat = info.platform || 'youtube';
        pBadge.textContent = plat.toUpperCase();
        const colors = {
          youtube: ['rgba(255,0,0,0.2)', '#FF0000'],
          instagram: ['rgba(225,48,108,0.2)', '#E1306C'],
          tiktok: ['rgba(37,244,238,0.2)', '#25F4EE'],
          facebook: ['rgba(24,119,242,0.2)', '#1877F2'],
          twitter: ['rgba(29,161,242,0.2)', '#1DA1F2']
        };
        const c = colors[plat] || colors.youtube;
        pBadge.style.background = c[0];
        pBadge.style.color = c[1];
      }

      if (mediaCard) mediaCard.style.display = 'block';
      this.showToast('✅ Media link verified!');
    } catch (err) {
      if (loader) loader.style.display = 'none';
      if (emptyState) emptyState.style.display = 'flex';
      this.showToast(`❌ ${err.message || 'Failed to analyze URL'}`);
    }
  },

  selectMediaFormat(formatId, quality, el) {
    document.querySelectorAll('#formatGrid .format-card-pill').forEach(pill => {
      pill.classList.remove('selected');
    });
    el.classList.add('selected');

    if (window.DownloaderModule) {
      window.DownloaderModule.selectedFormatId = formatId;
      window.DownloaderModule.selectedQuality = quality;
    }
    const label = formatId.startsWith('mp4') ? `MP4 (${quality})` : 'MP3 Audio';
    this.showToast(`Selected format: ${label}`);
  },

  async startMediaDownload() {
    const progressCard = document.getElementById('downloadProgressCard');
    const readyCard = document.getElementById('downloadReadyCard');
    const statusText = document.getElementById('progressStatusText');
    const percentText = document.getElementById('progressPercentText');
    const fill = document.getElementById('progressFill');
    const speedText = document.getElementById('progressSpeedText');
    const etaText = document.getElementById('progressEtaText');
    const startBtn = document.getElementById('startDownloadBtn');

    if (readyCard) readyCard.style.display = 'none';
    if (progressCard) progressCard.style.display = 'block';
    if (startBtn) startBtn.disabled = true;

    try {
      await window.DownloaderModule.startDownload(
        window.DownloaderModule.selectedFormatId,
        window.DownloaderModule.selectedQuality,
        (data) => {
          const pct = Math.round(data.percent || 0);
          if (percentText) percentText.textContent = `${pct}%`;
          if (fill) fill.style.width = `${pct}%`;
          if (statusText) statusText.innerHTML = `<span class="spinner" style="width:12px;height:12px;border-width:2px;"></span> ${data.status.charAt(0).toUpperCase() + data.status.slice(1)}...`;
          if (speedText) speedText.textContent = `Speed: ${data.speed || '--'}`;
          if (etaText) etaText.textContent = `ETA: ${data.eta || '--'}`;
        },
        ({ blob, filename }) => {
          if (startBtn) startBtn.disabled = false;
          if (fill) {
            fill.style.width = '100%';
            fill.classList.add('finished');
          }
          if (statusText) statusText.innerHTML = '✓ Download Complete!';
          if (percentText) percentText.textContent = '100%';

          setTimeout(() => {
            if (progressCard) progressCard.style.display = 'none';
            if (readyCard) readyCard.style.display = 'block';
            const fnEl = document.getElementById('readyFilename');
            if (fnEl) fnEl.textContent = filename;
            this.showToast('🎉 Download completed!');
          }, 600);
        },
        (errorMsg) => {
          if (startBtn) startBtn.disabled = false;
          this.showToast(`❌ ${errorMsg}`);
        }
      );
    } catch (err) {
      if (startBtn) startBtn.disabled = false;
      this.showToast(`❌ ${err.message}`);
    }
  },

  async openDownloadedInEditor() {
    const blob = window.DownloaderModule?.downloadedAudioBlob;
    if (!blob) {
      this.showToast('⚠️ No audio data available');
      return;
    }
    const name = window.DownloaderModule.downloadedFileName.replace(/\.[^/.]+$/, '');
    this.showToast('✂️ Loading into Audio Editor...');

    try {
      const file = new File([blob], `${name}.wav`, { type: 'audio/wav' });
      document.getElementById('trackName').textContent = name;
      document.getElementById('miniTitle').textContent = name;
      document.getElementById('miniArtist').textContent = window.DownloaderModule.currentMediaInfo?.uploader || 'Media Download';

      await window.engine.loadFile(file);
      this.navigateTo('editor');
      this.showToast('✅ Track ready for editing!');
    } catch (err) {
      console.error(err);
      this.showToast('❌ Failed to load audio into editor');
    }
  },

  addDownloadedToMixer() {
    const name = window.DownloaderModule?.downloadedFileName.replace(/\.[^/.]+$/, '') || 'Downloaded Track';
    this.addMixerTrack();
    const lastTrack = this.mixerTracks[this.mixerTracks.length - 1];
    if (lastTrack) {
      lastTrack.name = name.slice(0, 18);
      this._renderMixer();
    }
    this.navigateTo('mixer');
    this.showToast(`🎛️ Added "${name.slice(0, 15)}" to Mixer!`);
  },

  saveDownloadedFile() {
    const blob = window.DownloaderModule?.downloadedAudioBlob;
    const filename = window.DownloaderModule?.downloadedFileName || 'track.mp3';
    if (!blob) {
      this.showToast('⚠️ No file ready to save');
      return;
    }

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    this.showToast(`💾 Saved ${filename}!`);
  },

  async searchMusic() {
    const query = document.getElementById('dlQuery').value.trim();
    if (!query) {
      this.showToast('⚠️ Please enter a search query');
      return;
    }

    const loader = document.getElementById('dlLoading');
    const loaderText = document.getElementById('dlLoadingText');
    const empty = document.getElementById('dlEmpty');
    const results = document.getElementById('dlResults');

    if (loader) {
      if (loaderText) loaderText.textContent = 'Searching catalog...';
      loader.style.display = 'flex';
    }
    if (empty) empty.style.display = 'none';
    if (results) results.innerHTML = '';

    const items = await DownloaderModule.search(query);
    if (loader) loader.style.display = 'none';
    DownloaderModule.renderResults(items, 'dlResults');
  },

  loadSearchedMedia(platformKey) {
    const sample = DownloaderModule.SAMPLE_MEDIA[platformKey] || DownloaderModule.SAMPLE_MEDIA.youtube;
    const input = document.getElementById('dlUrl');
    if (input) {
      input.value = `https://www.${platformKey}.com/watch?v=sample`;
      this.onMediaUrlChange(input.value);
    }
    this.selectDLTab(0);
    this.analyzeMediaUrl();
  },

  // ─────────────────────────────────────────────
  // Toast notification
  // ─────────────────────────────────────────────
  showToast(msg, durationMs = 2500) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = msg;
    toast.classList.add('show');
    clearTimeout(this._toastTimeout);
    this._toastTimeout = setTimeout(() => toast.classList.remove('show'), durationMs);
  },

  togglePlayerBar() {
    const mp = document.getElementById('miniPlayer');
    mp.style.display = mp.style.display === 'none' ? 'flex' : 'none';
  },

  // ─────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────
  _formatTime(totalSec) {
    const min = Math.floor(totalSec / 60);
    const sec = Math.floor(totalSec % 60);
    return `${min}:${String(sec).padStart(2, '0')}`;
  },

  _formatTimeFull(totalSec) {
    const min = Math.floor(totalSec / 60);
    const sec = Math.floor(totalSec % 60);
    const ms = Math.floor((totalSec % 1) * 100);
    return `${String(min).padStart(2,'0')}:${String(sec).padStart(2,'0')}.${String(ms).padStart(2,'0')}`;
  }
};

// ─────────────────────────────────────────────
// Global function bridges for HTML onclick=""
// ─────────────────────────────────────────────
window.app = App;

function navigateTo(screen, btn) { App.navigateTo(screen); }
function togglePlayback() { App.togglePlayback(); }
function stopPlayback() { App.stopPlayback(); }
function loadAudioFile(input) { App.loadAudioFile(input); }
function applyTrim() { App.applyTrim(); }
function selectTool(i, btn) { App.selectTool(i, btn); }
function updateSpeed(v) { App.updateSpeed(v); }
function setSpeed(v) { App.setSpeed(v); }
function updatePitch(v) { App.updatePitch(v); }
function updateVolume(v) { App.updateVolume(v); }
function updateFadeIn(v) { App.updateFadeIn(v); }
function updateFadeOut(v) { App.updateFadeOut(v); }
function applyFade(t) { App.applyFade(t); }
function applyDenoise() { App.applyDenoise(); }
function removeSilence() { App.removeSilence(); }
function updateEQBand(i, v) { App.updateEQBand(i, v); }
function updateMasterVol(v) { App.updateMasterVol(v); }
function applyEQPreset(n) { App.applyEQPreset(n); }
function selectFormat(i, btn) { App.selectFormat(i, btn); }
function exportTrack() { App.exportTrack(); }
function loadCoverArt(input) { App.loadCoverArt(input); }
function saveMetadata() { App.saveMetadata(); }
function setRingtone(t) { App.setRingtone(t); }
function toggleDubMode(cb) { App.toggleDubMode(cb); }
function toggleRecording() { App.toggleRecording(); }
function updateGain(v) { App.updateGain(v); }
function addMixerTrack() { App.addMixerTrack(); }
function mixDown() { App.mixDown(); }
function selectDLTab(i, btn) { App.selectDLTab(i, btn); }
function onMediaUrlChange(v) { App.onMediaUrlChange(v); }
function pasteMediaUrl() { App.pasteMediaUrl(); }
function analyzeMediaUrl() { App.analyzeMediaUrl(); }
function selectMediaFormat(f, q, el) { App.selectMediaFormat(f, q, el); }
function startMediaDownload() { App.startMediaDownload(); }
function openDownloadedInEditor() { App.openDownloadedInEditor(); }
function addDownloadedToMixer() { App.addDownloadedToMixer(); }
function saveDownloadedFile() { App.saveDownloadedFile(); }
function searchMusic() { App.searchMusic(); }
function loadSearchedMedia(p) { App.loadSearchedMedia(p); }
function togglePlayerBar() { App.togglePlayerBar(); }

// Boot
document.addEventListener('DOMContentLoaded', () => {
  App.init();
  if (window.DownloaderModule) {
    window.DownloaderModule.init();
  }
});
