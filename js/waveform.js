/**
 * MP3 Studio — Waveform Canvas Renderer
 * Draws interactive waveform with trim handles, playhead, and selection.
 */

class WaveformRenderer {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) throw new Error(`WaveformRenderer: canvas '#${canvasId}' not found`);
    this.ctx = this.canvas.getContext('2d');
    this.waveform = new Float32Array(0);
    this.playheadFraction = 0;
    this.trimStart = 0;    // 0..1
    this.trimEnd = 1;      // 0..1
    this.dragHandle = null; // 'start' | 'end' | 'seek' | null
    this.glowPhase = 0;
    this.rafId = null;

    // Colors
    this.colors = {
      bg:         '#0D0D0C',
      grid:       'rgba(255,255,255,0.04)',
      bar:        '#3A3A3C',
      barPlayed:  'rgba(255,45,85,0.55)',
      barSelected:'#FA243C',
      selection:  'rgba(250,36,60,0.18)',
      handle:     '#FA243C',
      playhead:   '#FFFFFF',
      playheadGlow:'rgba(255,255,255,0.15)'
    };

    this._bindEvents();
    this._startLoop();
  }

  load(waveform) {
    this.waveform = waveform;
    this.trimStart = 0;
    this.trimEnd = 1;
    this.playheadFraction = 0;
  }

  setPlayhead(fraction) {
    this.playheadFraction = Math.max(0, Math.min(1, fraction));
  }

  setTrimRange(start, end) {
    this.trimStart = Math.max(0, Math.min(1, start));
    this.trimEnd = Math.max(this.trimStart + 0.001, Math.min(1, end));
  }

  // ── Render ──
  draw() {
    const { canvas, ctx } = this;
    const dpr = window.devicePixelRatio || 1;
    const W = canvas.clientWidth;
    const H = canvas.clientHeight;

    if (canvas.width !== W * dpr || canvas.height !== H * dpr) {
      canvas.width = W * dpr;
      canvas.height = H * dpr;
    }
    // Always reset to absolute DPR transform to avoid accumulation across resize
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    ctx.clearRect(0, 0, W, H);

    // Background
    ctx.fillStyle = this.colors.bg;
    ctx.fillRect(0, 0, W, H);

    const bars = this.waveform.length;
    if (bars === 0) {
      this._drawEmpty(W, H);
      return;
    }

    const centerY = H / 2;
    const barW = W / bars;
    const gap = barW * 0.22;
    const effW = Math.max(barW - gap, 1);

    // Time grid
    ctx.strokeStyle = this.colors.grid;
    ctx.lineWidth = 1;
    for (let i = 0; i <= 10; i++) {
      const x = (W / 10) * i;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, H);
      ctx.stroke();
    }

    // Center line
    ctx.strokeStyle = 'rgba(255,255,255,0.06)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(0, centerY);
    ctx.lineTo(W, centerY);
    ctx.stroke();

    // Selection overlay
    const selX = this.trimStart * W;
    const selW = (this.trimEnd - this.trimStart) * W;
    ctx.fillStyle = this.colors.selection;
    ctx.fillRect(selX, 0, selW, H);

    // Waveform bars
    for (let i = 0; i < bars; i++) {
      const x = i * barW;
      const frac = i / bars;
      const amp = Math.max(this.waveform[i], 0.03);
      const barH = amp * centerY * 0.9;

      const isSelected = frac >= this.trimStart && frac <= this.trimEnd;
      const isPlayed = frac < this.playheadFraction;

      let color;
      if (isSelected && isPlayed) {
        color = this.colors.barSelected;
      } else if (isSelected) {
        color = 'rgba(250,36,60,0.5)';
      } else if (isPlayed) {
        color = this.colors.barPlayed;
      } else {
        color = this.colors.bar;
      }

      // Upper bar
      this._roundedRect(ctx, x + gap / 2, centerY - barH, effW, barH, 2, color);
      // Mirror (lower) — draw at reduced opacity
      ctx.globalAlpha = 0.35;
      this._roundedRect(ctx, x + gap / 2, centerY, effW, barH * 0.45, 2, color);
      ctx.globalAlpha = 1;
    }

    // Trim handles
    this._drawHandle(selX, H, 'start');
    this._drawHandle(selX + selW, H, 'end');

    // Playhead glow
    const phX = this.playheadFraction * W;
    this.glowPhase += 0.05;
    const glowAlpha = 0.15 + 0.15 * Math.sin(this.glowPhase);

    const glowGrad = ctx.createLinearGradient(phX - 12, 0, phX + 12, 0);
    glowGrad.addColorStop(0, 'transparent');
    glowGrad.addColorStop(0.5, `rgba(255,255,255,${glowAlpha})`);
    glowGrad.addColorStop(1, 'transparent');
    ctx.fillStyle = glowGrad;
    ctx.fillRect(phX - 12, 0, 24, H);

    // Playhead line
    ctx.strokeStyle = this.colors.playhead;
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(phX, 0);
    ctx.lineTo(phX, H);
    ctx.stroke();

    // Playhead diamond
    ctx.fillStyle = this.colors.playhead;
    ctx.beginPath();
    ctx.moveTo(phX, 2);
    ctx.lineTo(phX + 7, 12);
    ctx.lineTo(phX, 20);
    ctx.lineTo(phX - 7, 12);
    ctx.closePath();
    ctx.fill();
  }

  _drawHandle(x, H, type) {
    const { ctx } = this;
    ctx.strokeStyle = this.colors.handle;
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, H);
    ctx.stroke();

    // Grip circle
    ctx.fillStyle = this.colors.handle;
    ctx.beginPath();
    ctx.arc(x, H / 2, 6, 0, Math.PI * 2);
    ctx.fill();

    // Arrow indicator
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    if (type === 'start') {
      ctx.moveTo(x + 3, H / 2 - 3);
      ctx.lineTo(x - 1, H / 2);
      ctx.lineTo(x + 3, H / 2 + 3);
    } else {
      ctx.moveTo(x - 3, H / 2 - 3);
      ctx.lineTo(x + 1, H / 2);
      ctx.lineTo(x - 3, H / 2 + 3);
    }
    ctx.stroke();
  }

  _drawEmpty(W, H) {
    const { ctx } = this;
    const centerY = H / 2;
    const bars = 80;
    const barW = W / bars;
    for (let i = 0; i < bars; i++) {
      const amp = 0.05 + Math.sin(i * 0.3) * 0.03;
      const barH = amp * centerY;
      ctx.fillStyle = '#3A3A3C';
      ctx.fillRect(i * barW + 1, centerY - barH, barW - 2, barH);
      ctx.fillRect(i * barW + 1, centerY, barW - 2, barH * 0.4);
    }
  }

  _roundedRect(ctx, x, y, w, h, r, color) {
    ctx.fillStyle = color;
    ctx.beginPath();
    if (ctx.roundRect) {
      ctx.roundRect(x, y, w, h, r);
    } else {
      // Polyfill for Safari < 15.4
      const clamp = Math.min(r, w / 2, h / 2);
      ctx.moveTo(x + clamp, y);
      ctx.lineTo(x + w - clamp, y);
      ctx.arcTo(x + w, y, x + w, y + clamp, clamp);
      ctx.lineTo(x + w, y + h - clamp);
      ctx.arcTo(x + w, y + h, x + w - clamp, y + h, clamp);
      ctx.lineTo(x + clamp, y + h);
      ctx.arcTo(x, y + h, x, y + h - clamp, clamp);
      ctx.lineTo(x, y + clamp);
      ctx.arcTo(x, y, x + clamp, y, clamp);
      ctx.closePath();
    }
    ctx.fill();
  }

  _startLoop() {
    const loop = () => {
      this.draw();
      this.rafId = requestAnimationFrame(loop);
    };
    loop();
  }

  // ── Pointer Events ──
  _bindEvents() {
    const c = this.canvas;
    c.addEventListener('mousedown', e => this._onDown(e));
    c.addEventListener('mousemove', e => this._onMove(e));
    c.addEventListener('mouseup', () => this._onUp());
    c.addEventListener('touchstart', e => this._onDown(e.touches[0]), { passive: true });
    c.addEventListener('touchmove', e => { e.preventDefault(); this._onMove(e.touches[0]); }, { passive: false });
    c.addEventListener('touchend', () => this._onUp());
  }

  _getFraction(e) {
    const rect = this.canvas.getBoundingClientRect();
    return Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  }

  _onDown(e) {
    const f = this._getFraction(e);
    const handleRadius = 0.03;
    if (Math.abs(f - this.trimStart) < handleRadius) {
      this.dragHandle = 'start';
    } else if (Math.abs(f - this.trimEnd) < handleRadius) {
      this.dragHandle = 'end';
    } else {
      this.dragHandle = 'seek';
      this.playheadFraction = f;
      if (window.engine) {
        window.engine.seekTo(f * window.engine.duration);
      }
    }
  }

  _onMove(e) {
    if (!this.dragHandle) return;
    const f = this._getFraction(e);
    if (this.dragHandle === 'start') {
      this.trimStart = Math.min(f, this.trimEnd - 0.01);
      this._notifyTrimChange();
    } else if (this.dragHandle === 'end') {
      this.trimEnd = Math.max(f, this.trimStart + 0.01);
      this._notifyTrimChange();
    } else if (this.dragHandle === 'seek') {
      this.playheadFraction = f;
      if (window.engine) window.engine.seekTo(f * window.engine.duration);
    }
  }

  _onUp() { this.dragHandle = null; }

  _notifyTrimChange() {
    if (window.engine) {
      window.engine.setTrimRange(
        this.trimStart * window.engine.duration,
        this.trimEnd * window.engine.duration
      );
    }
    // Update UI trim display
    if (window.app) window.app.updateTrimDisplay();
  }
}

/** VU Meter renderer for live recording */
class VUMeterRenderer {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    if (!this.canvas) throw new Error(`VUMeterRenderer: canvas '#${canvasId}' not found`);
    this.ctx = this.canvas.getContext('2d');
    this.amplitudes = new Float32Array(60).fill(0.02);
    this.isActive = false;
    this._startLoop();
  }

  setActive(active) {
    this.isActive = active;
  }

  setAmplitudes(amps) {
    this.amplitudes = amps;
  }

  draw() {
    const { canvas, ctx } = this;
    const W = canvas.clientWidth;
    const H = canvas.clientHeight;
    if (canvas.width !== W || canvas.height !== H) {
      canvas.width = W;
      canvas.height = H;
    }

    ctx.clearRect(0, 0, W, H);
    ctx.fillStyle = 'rgba(0,0,0,0.3)';
    ctx.fillRect(0, 0, W, H);

    const bars = this.amplitudes.length;
    const barW = W / bars;
    const centerY = H / 2;

    for (let i = 0; i < bars; i++) {
      const amp = this.isActive
        ? Math.min(1, this.amplitudes[i] + Math.random() * 0.08)
        : this.amplitudes[i] * 0.3;

      const barH = amp * centerY * 0.95;
      const x = i * barW;

      // Gradient red to orange
      const hue = 0 + (i / bars) * 30;
      const color = this.isActive ? `hsl(${hue}, 90%, 55%)` : '#48484A';

      ctx.fillStyle = color;
      ctx.fillRect(x + 1, centerY - barH, barW - 2, barH * 2);
    }

    // Peak hold line
    if (this.isActive) {
      const peak = Math.max(...this.amplitudes);
      const py = centerY - peak * centerY * 0.95;
      ctx.strokeStyle = 'rgba(255,45,85,0.8)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(0, py);
      ctx.lineTo(W, py);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, centerY + peak * centerY * 0.95);
      ctx.lineTo(W, centerY + peak * centerY * 0.95);
      ctx.stroke();
    }
  }

  _startLoop() {
    const loop = () => {
      this.draw();
      requestAnimationFrame(loop);
    };
    loop();
  }
}

// ── Initialize renderers ──
window.waveformRenderer = null;
window.vuMeter = null;

document.addEventListener('DOMContentLoaded', () => {
  window.waveformRenderer = new WaveformRenderer('waveformCanvas');
  window.vuMeter = new VUMeterRenderer('vuMeter');
});
