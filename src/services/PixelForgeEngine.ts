export interface ProjectLayer {
  id: string;
  type: 'text' | 'image';
  content: string;
  color: string;
  opacity: number;
}

export class PixelForgeEngine {
  static createDefaultLayer(content: string = 'PixelForge'): ProjectLayer {
    return {
      id: `layer-${Date.now()}`,
      type: 'text',
      content,
      color: '#ffffff',
      opacity: 1,
    };
  }
}
