import React, { useState, useEffect, useRef } from 'react';
import { AudioService } from '../services/AudioService';

export interface SaveExportDialogProps {
  projectTitle: string;
  layerText: string;
  layerColor: string;
  canvasRatio: string;
  opacity: number;
  exposure: number;
  contrast: number;
  saturation: number;
  strokeEnabled: boolean;
  shadowEnabled: boolean;
  rotation?: number;
  flipH?: boolean;
  flipV?: boolean;
  onDismiss: () => void;
  onExportDone?: (format: 'png' | 'jpg') => void;
}

export const SaveExportDialog: React.FC<SaveExportDialogProps> = ({
  projectTitle,
  layerText,
  layerColor,
  canvasRatio,
  opacity,
  exposure,
  contrast,
  saturation,
  strokeEnabled,
  shadowEnabled,
  rotation = 0,
  flipH = false,
  flipV = false,
  onDismiss,
  onExportDone,
}) => {
  const [format, setFormat] = useState<'png' | 'jpg'>('png');
  const [resolution, setResolution] = useState<'1080' | '1440' | '720'>('1080');
  const [jpgBackground, setJpgBackground] = useState<'dark' | 'light'>('dark');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const previewCanvasRef = useRef<HTMLCanvasElement | null>(null);

  const getCanvasDimensions = (baseDim: number) => {
    switch (canvasRatio) {
      case '16:9':
        return { w: Math.round(baseDim * (16 / 9)), h: baseDim };
      case '9:16':
        return { w: baseDim, h: Math.round(baseDim * (16 / 9)) };
      case '4:3':
        return { w: Math.round(baseDim * (4 / 3)), h: baseDim };
      case '1:1':
      default:
        return { w: baseDim, h: baseDim };
    }
  };

  const renderToCanvas = (canvas: HTMLCanvasElement, width: number, height: number, isPreview = false) => {
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, width, height);

    if (format === 'jpg') {
      ctx.fillStyle = jpgBackground === 'dark' ? '#12161f' : '#f8f9fa';
      ctx.fillRect(0, 0, width, height);
    }

    ctx.save();

    // Color adjust filters
    const b = 1 + exposure * 0.5;
    const c = 1 + contrast * 0.5;
    const s = 1 + saturation * 0.5;
    ctx.filter = `brightness(${Math.max(0.1, b)}) contrast(${Math.max(0.1, c)}) saturate(${Math.max(0, s)})`;

    // Center and transform
    ctx.translate(width / 2, height / 2);
    ctx.rotate((rotation * Math.PI) / 180);
    ctx.scale(flipH ? -1 : 1, flipV ? -1 : 1);

    ctx.globalAlpha = Math.max(0, Math.min(1, opacity));

    const fontSize = isPreview ? Math.max(16, Math.round(width * 0.08)) : Math.max(32, Math.round(width * 0.09));
    ctx.font = `bold ${fontSize}px system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    if (shadowEnabled) {
      ctx.shadowColor = 'rgba(0,0,0,0.75)';
      ctx.shadowBlur = isPreview ? 8 : 24;
      ctx.shadowOffsetX = 0;
      ctx.shadowOffsetY = isPreview ? 3 : 8;
    }

    if (strokeEnabled) {
      ctx.lineWidth = isPreview ? 3 : 8;
      ctx.strokeStyle = '#000000';
      ctx.strokeText(layerText || 'PixelForge', 0, 0);
    }

    ctx.fillStyle = layerColor;
    ctx.fillText(layerText || 'PixelForge', 0, 0);

    ctx.restore();
  };

  useEffect(() => {
    if (previewCanvasRef.current) {
      const { w, h } = getCanvasDimensions(220);
      renderToCanvas(previewCanvasRef.current, w, h, true);
    }
  }, [layerText, layerColor, canvasRatio, opacity, exposure, contrast, saturation, strokeEnabled, shadowEnabled, rotation, flipH, flipV, format, jpgBackground]);

  const handleExport = () => {
    try {
      const baseDim = parseInt(resolution, 10);
      const { w, h } = getCanvasDimensions(baseDim);
      const exportCanvas = document.createElement('canvas');
      renderToCanvas(exportCanvas, w, h, false);

      const mimeType = format === 'png' ? 'image/png' : 'image/jpeg';
      const quality = format === 'jpg' ? 0.95 : undefined;

      exportCanvas.toBlob(
        (blob) => {
          if (!blob) {
            setStatusMessage('Export generation failed.');
            return;
          }
          const blobUrl = URL.createObjectURL(blob);
          const link = document.createElement('a');
          const cleanName = (projectTitle || 'pixelforge_project').toLowerCase().replace(/[^a-z0-9_-]/g, '_');
          link.download = `${cleanName}_${resolution}p.${format}`;
          link.href = blobUrl;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(blobUrl);

          AudioService.playShutterSound();
          setStatusMessage(`Exported ${format.toUpperCase()} (${w}×${h}px) successfully!`);
          if (onExportDone) {
            onExportDone(format);
          }
          setTimeout(() => {
            onDismiss();
          }, 900);
        },
        mimeType,
        quality
      );
    } catch {
      setStatusMessage('Error during file generation.');
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.72)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 200,
        padding: 16,
      }}
      data-testid="save_export_dialog_overlay"
    >
      <div
        style={{
          width: 'min(92vw, 420px)',
          maxHeight: '90vh',
          background: 'var(--pf-surface)',
          border: '1px solid var(--pf-outline)',
          borderRadius: 20,
          padding: 20,
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
          overflowY: 'auto',
          boxShadow: '0 20px 40px rgba(0,0,0,0.5)',
        }}
        data-testid="save_export_dialog"
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 700 }}>Export Image</h3>
          <button
            type="button"
            onClick={onDismiss}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--pf-on-variant)',
              fontSize: 20,
              cursor: 'pointer',
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        </div>

        {/* Live Canvas Preview */}
        <div
          className="pf-checkerboard"
          style={{
            width: '100%',
            height: 180,
            borderRadius: 14,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
            border: '1px solid var(--pf-outline)',
          }}
        >
          <canvas ref={previewCanvasRef} style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
        </div>

        {/* Format Selector */}
        <div>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Export Format</div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button
              type="button"
              onClick={() => setFormat('png')}
              style={{
                flex: 1,
                padding: '10px 12px',
                borderRadius: 12,
                border: format === 'png' ? '2px solid var(--pf-azure)' : '1px solid var(--pf-outline)',
                background: format === 'png' ? 'rgba(45,140,255,0.18)' : 'var(--pf-surface-variant)',
                color: 'var(--pf-on-surface)',
                fontWeight: 700,
                cursor: 'pointer',
                textAlign: 'center',
              }}
              data-testid="export_format_png"
            >
              PNG (Transparent)
            </button>
            <button
              type="button"
              onClick={() => setFormat('jpg')}
              style={{
                flex: 1,
                padding: '10px 12px',
                borderRadius: 12,
                border: format === 'jpg' ? '2px solid var(--pf-teal)' : '1px solid var(--pf-outline)',
                background: format === 'jpg' ? 'rgba(0,180,160,0.18)' : 'var(--pf-surface-variant)',
                color: 'var(--pf-on-surface)',
                fontWeight: 700,
                cursor: 'pointer',
                textAlign: 'center',
              }}
              data-testid="export_format_jpg"
            >
              JPG (Opaque)
            </button>
          </div>
        </div>

        {/* Resolution Selector */}
        <div>
          <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Resolution</div>
          <div style={{ display: 'flex', gap: 8 }}>
            {(['720', '1080', '1440'] as const).map((res) => (
              <button
                key={res}
                type="button"
                onClick={() => setResolution(res)}
                style={{
                  flex: 1,
                  padding: '8px',
                  borderRadius: 10,
                  border: resolution === res ? '2px solid var(--pf-amber)' : '1px solid var(--pf-outline)',
                  background: resolution === res ? 'rgba(255,179,0,0.18)' : 'var(--pf-surface-variant)',
                  color: 'var(--pf-on-surface)',
                  fontWeight: 600,
                  fontSize: 12,
                  cursor: 'pointer',
                }}
              >
                {res === '720' ? '720p SD' : res === '1080' ? '1080p FHD' : '1440p 2K'}
              </button>
            ))}
          </div>
        </div>

        {/* JPG Background option if JPG */}
        {format === 'jpg' && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 13 }}>
            <span>JPG Background Color:</span>
            <div style={{ display: 'flex', gap: 6 }}>
              <button
                type="button"
                onClick={() => setJpgBackground('dark')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 8,
                  border: jpgBackground === 'dark' ? '2px solid var(--pf-azure)' : '1px solid var(--pf-outline)',
                  background: '#12161f',
                  color: '#fff',
                  cursor: 'pointer',
                  fontSize: 12,
                }}
              >
                Dark
              </button>
              <button
                type="button"
                onClick={() => setJpgBackground('light')}
                style={{
                  padding: '4px 10px',
                  borderRadius: 8,
                  border: jpgBackground === 'light' ? '2px solid var(--pf-azure)' : '1px solid var(--pf-outline)',
                  background: '#f8f9fa',
                  color: '#000',
                  cursor: 'pointer',
                  fontSize: 12,
                }}
              >
                Light
              </button>
            </div>
          </div>
        )}

        {statusMessage && (
          <div
            style={{
              padding: '8px 12px',
              borderRadius: 8,
              background: 'rgba(46,125,50,0.2)',
              border: '1px solid var(--pf-emerald)',
              color: 'var(--pf-emerald)',
              fontSize: 12,
              textAlign: 'center',
              fontWeight: 600,
            }}
          >
            {statusMessage}
          </div>
        )}

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
          <button
            type="button"
            onClick={onDismiss}
            style={{
              flex: 1,
              padding: '12px',
              borderRadius: 12,
              border: '1px solid var(--pf-outline)',
              background: 'transparent',
              color: 'var(--pf-on-variant)',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleExport}
            style={{
              flex: 2,
              padding: '12px',
              borderRadius: 12,
              border: 'none',
              background: 'var(--pf-azure)',
              color: '#ffffff',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
            }}
            data-testid="execute_export_button"
          >
            <span>Download {format.toUpperCase()}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
