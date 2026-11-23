export class AudioService {
  private static audio: HTMLAudioElement | null = null;

  static playShutterSound(): void {
    try {
      if (!this.audio) {
        this.audio = new Audio('./src/assets/media/shutter_click.mp3');
      }
      this.audio.currentTime = 0;
      this.audio.play().catch(() => {
        // Fallback or muted by user browser policy
      });
    } catch {
      // Audio playback safely handled
    }
  }
}
