export interface GestureConfig {
  tapSlop: number;
  longPressMs: number;
}

export const DEFAULT_GESTURE_CONFIG: GestureConfig = {
  tapSlop: 8,
  longPressMs: 500,
};

export class TouchGestureManager {
  private startX: number = 0;
  private startY: number = 0;
  private startTime: number = 0;

  onTouchStart(x: number, y: number): void {
    this.startX = x;
    this.startY = y;
    this.startTime = Date.now();
  }

  isQuickTap(x: number, y: number, config: GestureConfig = DEFAULT_GESTURE_CONFIG): boolean {
    const dx = Math.abs(x - this.startX);
    const dy = Math.abs(y - this.startY);
    const dt = Date.now() - this.startTime;
    return dx <= config.tapSlop && dy <= config.tapSlop && dt < 400;
  }

  isLongPress(x: number, y: number, config: GestureConfig = DEFAULT_GESTURE_CONFIG): boolean {
    const dx = Math.abs(x - this.startX);
    const dy = Math.abs(y - this.startY);
    const dt = Date.now() - this.startTime;
    return dx <= config.tapSlop && dy <= config.tapSlop && dt >= config.longPressMs;
  }
}
