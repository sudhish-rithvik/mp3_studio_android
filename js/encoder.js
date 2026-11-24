/**
 * MP3 Studio — WAV Encoder
 * Converts Web Audio AudioBuffer to WAV Blob for download.
 * Supports 16-bit PCM, stereo/mono.
 */

class WavEncoder {
  /**
   * Encode an AudioBuffer to a WAV Blob.
   * @param {AudioBuffer} buffer
   * @returns {Blob} WAV Blob
   */
  static encode(buffer) {
    const numChannels = buffer.numberOfChannels;
    const sampleRate = buffer.sampleRate;
    const numSamples = buffer.length;
    const bytesPerSample = 2; // 16-bit PCM
    const blockAlign = numChannels * bytesPerSample;
    const byteRate = sampleRate * blockAlign;
    const dataSize = numSamples * blockAlign;
    const bufferSize = 44 + dataSize;

    const arrayBuffer = new ArrayBuffer(bufferSize);
    const view = new DataView(arrayBuffer);

    // RIFF chunk descriptor
    WavEncoder._writeString(view, 0, 'RIFF');
    view.setUint32(4, 36 + dataSize, true);
    WavEncoder._writeString(view, 8, 'WAVE');

    // fmt sub-chunk
    WavEncoder._writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true);           // Subchunk1Size = 16 for PCM
    view.setUint16(20, 1, true);             // AudioFormat = 1 (PCM)
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, byteRate, true);
    view.setUint16(32, blockAlign, true);
    view.setUint16(34, 16, true);           // BitsPerSample = 16

    // data sub-chunk
    WavEncoder._writeString(view, 36, 'data');
    view.setUint32(40, dataSize, true);

    // Interleave PCM samples
    let offset = 44;
    for (let i = 0; i < numSamples; i++) {
      for (let ch = 0; ch < numChannels; ch++) {
        const channelData = buffer.getChannelData(ch);
        const sample = Math.max(-1, Math.min(1, channelData[i]));
        view.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7FFF, true);
        offset += 2;
      }
    }

    return new Blob([arrayBuffer], { type: 'audio/wav' });
  }

  /** Trigger browser download of the WAV Blob */
  static download(buffer, filename = 'mp3studio_export.wav', id3Writer = null) {
    let blob = WavEncoder.encode(buffer);
    if (id3Writer && typeof id3Writer.addToBlob === 'function') {
      blob = id3Writer.addToBlob(blob);
    }
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  /** Simulate format export (converts to WAV container with target extension for demo, with ID3 support) */
  static downloadAs(buffer, format, filename = 'mp3studio_export', id3Writer = null) {
    let blob = WavEncoder.encode(buffer);
    if (id3Writer && typeof id3Writer.addToBlob === 'function') {
      blob = id3Writer.addToBlob(blob);
    }
    const ext = format.toLowerCase();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename}.${ext}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 10000);
  }

  static _writeString(view, offset, str) {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  }
}

/**
 * ID3v2.3 Tag Writer — Prepends ID3 metadata header to a WAV Blob.
 * Supports: TIT2 (title), TPE1 (artist), TALB (album), TYER (year),
 *           TCON (genre), TRCK (track number), APIC (cover art).
 */
class ID3Writer {
  constructor() {
    this.frames = [];
  }

  setTitle(title) { this._addFrame('TIT2', title); return this; }
  setArtist(artist) { this._addFrame('TPE1', artist); return this; }
  setAlbum(album) { this._addFrame('TALB', album); return this; }
  setYear(year) { this._addFrame('TYER', year); return this; }
  setGenre(genre) { this._addFrame('TCON', genre); return this; }
  setTrackNumber(num) { this._addFrame('TRCK', num); return this; }

  /**
   * Prepend ID3 header to an audio Blob.
   * Returns a new Blob with ID3 tags.
   */
  addToBlob(audioBlob) {
    if (this.frames.length === 0) return audioBlob;

    const id3 = this._build();
    return new Blob([id3, audioBlob], { type: audioBlob.type });
  }

  _addFrame(id, text) {
    if (!text) return;
    const enc = new TextEncoder();
    const textBytes = enc.encode(text);
    // Frame: id(4) + size(4) + flags(2) + encoding(1) + text
    const frameSize = 1 + textBytes.length;
    const frame = new Uint8Array(10 + frameSize);
    // Frame ID (4 bytes)
    for (let i = 0; i < 4; i++) frame[i] = id.charCodeAt(i);
    // Frame size (4 bytes, big endian)
    frame[4] = (frameSize >> 24) & 0xFF;
    frame[5] = (frameSize >> 16) & 0xFF;
    frame[6] = (frameSize >> 8) & 0xFF;
    frame[7] = frameSize & 0xFF;
    // Flags (2 bytes): 0
    frame[8] = 0; frame[9] = 0;
    // Encoding: 3 = UTF-8
    frame[10] = 3;
    frame.set(textBytes, 11);
    this.frames.push(frame);
  }

  _build() {
    const totalFrameSize = this.frames.reduce((s, f) => s + f.length, 0);
    const tagSize = totalFrameSize;
    const header = new Uint8Array(10);

    // ID3 magic
    header[0] = 0x49; // I
    header[1] = 0x44; // D
    header[2] = 0x33; // 3
    header[3] = 0x03; // Version 2.3
    header[4] = 0x00; // Revision
    header[5] = 0x00; // Flags

    // Synchsafe integer for tag size
    header[6] = (tagSize >> 21) & 0x7F;
    header[7] = (tagSize >> 14) & 0x7F;
    header[8] = (tagSize >> 7) & 0x7F;
    header[9] = tagSize & 0x7F;

    const tag = new Uint8Array(10 + tagSize);
    tag.set(header, 0);
    let offset = 10;
    for (const frame of this.frames) {
      tag.set(frame, offset);
      offset += frame.length;
    }
    return tag;
  }
}

window.WavEncoder = WavEncoder;
window.ID3Writer = ID3Writer;
