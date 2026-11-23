import React, { useState } from 'react';

export interface FilterToolPanelProps {
  blendMode: string;
  opacity: number;
  strokeEnabled: boolean;
  shadowEnabled: boolean;
  onSetBlendMode: (mode: string) => void;
  onToggleStroke: () => void;
  onToggleShadow: () => void;
  onSetOpacity: (val: number) => void;
  onLaunchEyedropper: (target: 'stroke' | 'shadow' | 'fill') => void;
}

const SWATCH_COLORS = ['#ffffff', '#000000', '#ef4444', '#f97316', '#facc15', '#22c55e', '#06b6d4', '#2d8cff', '#8b5cf6', '#ec4899'];
const BLEND_MODES = ['Normal (Default)', 'Multiply', 'Screen', 'Overlay', 'Darken', 'Lighten', 'Color Dodge', 'Soft Light'];

export const FilterToolPanel: React.FC<FilterToolPanelProps> = ({
  blendMode,
  opacity,
  strokeEnabled,
  shadowEnabled,
  onSetBlendMode,
  onToggleStroke,
  onToggleShadow,
  onSetOpacity,
  onLaunchEyedropper,
}) => {
  const [subTab, setSubTab] = useState<'BLEND' | 'SHADOW' | 'FILL'>('BLEND');

  return (
    <div style={{ padding: '6px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', gap: 8 }}>
        {(['BLEND', 'SHADOW', 'FILL'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            className={`pf-tool-chip ${subTab === tab ? 'active' : ''}`}
            style={{ background: subTab === tab ? 'var(--pf-azure)' : 'var(--pf-surface-variant)', color: 'var(--pf-on-surface)' }}
            onClick={() => setSubTab(tab)}
          >
            {tab === 'BLEND' ? 'Blend' : tab === 'SHADOW' ? 'Shadow' : 'Fill'}
          </button>
        ))}
      </div>

      {subTab === 'BLEND' && (
        <div style={{ display: 'flex', gap: 6, overflowX: 'auto' }}>
          {BLEND_MODES.map((m) => (
            <button
              key={m}
              type="button"
              className="pf-tool-chip"
              style={{ background: blendMode === m ? 'var(--pf-azure)' : 'var(--pf-surface-variant)', color: 'var(--pf-on-surface)' }}
              onClick={() => onSetBlendMode(m)}
            >
              {m}
            </button>
          ))}
        </div>
      )}

      {subTab === 'SHADOW' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Stroke</span>
            <input type="checkbox" checked={strokeEnabled} onChange={onToggleStroke} />
          </div>
          {strokeEnabled && (
            <div className="pf-swatch-row">
              <button type="button" className="pf-touch-btn" onClick={() => onLaunchEyedropper('stroke')}>
                <img src="/assets/icons/ic_eyedropper.png" alt="Pick Stroke Color" className="pf-icon pf-icon-sm" />
              </button>
              <button type="button" className="pf-touch-btn">
                <img src="/assets/icons/ic_palette.png" alt="Stroke Palette" className="pf-icon pf-icon-sm" />
              </button>
              <div style={{ display: 'flex', gap: 6, overflowX: 'auto', flex: 1 }}>
                {SWATCH_COLORS.map((c) => (
                  <button key={c} type="button" className="pf-swatch-dot" style={{ background: c }} aria-label={`Stroke ${c}`} />
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>Shadow</span>
            <input type="checkbox" checked={shadowEnabled} onChange={onToggleShadow} />
          </div>
          {shadowEnabled && (
            <div className="pf-swatch-row">
              <button type="button" className="pf-touch-btn" onClick={() => onLaunchEyedropper('shadow')}>
                <img src="/assets/icons/ic_eyedropper.png" alt="Pick Shadow Color" className="pf-icon pf-icon-sm" />
              </button>
              <button type="button" className="pf-touch-btn">
                <img src="/assets/icons/ic_palette.png" alt="Shadow Palette" className="pf-icon pf-icon-sm" />
              </button>
            </div>
          )}

          <label style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13 }}>
            <span>Opacity ({Math.round(opacity * 100)}%)</span>
            <input type="range" min={0} max={100} value={Math.round(opacity * 100)} onChange={(e) => onSetOpacity(Number(e.target.value) / 100)} />
          </label>
        </div>
      )}

      {subTab === 'FILL' && (
        <div className="pf-swatch-row">
          <button type="button" className="pf-touch-btn" onClick={() => onLaunchEyedropper('fill')}>
            <img src="/assets/icons/ic_eyedropper.png" alt="Pick Fill Color" className="pf-icon pf-icon-sm" />
          </button>
          <button type="button" className="pf-touch-btn">
            <img src="/assets/icons/ic_palette.png" alt="Fill Palette" className="pf-icon pf-icon-sm" />
          </button>
          <div style={{ display: 'flex', gap: 6, overflowX: 'auto', flex: 1 }}>
            {SWATCH_COLORS.map((c) => (
              <button key={c} type="button" className="pf-swatch-dot" style={{ background: c }} aria-label={`Fill ${c}`} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
