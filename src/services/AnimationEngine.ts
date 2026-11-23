export interface UiAnimationSpecs {
  panelEnterMs: number;
  panelExitMs: number;
  overlayFadeMs: number;
  dialogScaleMs: number;
}

export const DEFAULT_ANIMATION_SPECS: UiAnimationSpecs = {
  panelEnterMs: 220,
  panelExitMs: 180,
  overlayFadeMs: 200,
  dialogScaleMs: 200,
};

export class AnimationSpecLoader {
  static getSpecs(): UiAnimationSpecs {
    return DEFAULT_ANIMATION_SPECS;
  }
}
