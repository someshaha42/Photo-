import React, { useState } from 'react';
import { FloatingPanelContainer } from './FloatingPanelContainer';

export type ThreeDMode = 'MOVE' | 'ROTATE' | 'SCALE' | 'SKEW';

export interface CornerPinPanelProps {
  canUndo: boolean;
  isAddPointMode: boolean;
  puppetPinCount: number;
  onToggleAddPointMode: () => void;
  onClearPins: () => void;
  onUndo: () => void;
  onConfirm: () => void;
}

export const CornerPinToolPanel: React.FC<CornerPinPanelProps> = ({
  canUndo,
  isAddPointMode,
  puppetPinCount,
  onToggleAddPointMode,
  onClearPins,
  onUndo,
  onConfirm,
}) => (
  <div style={{ padding: '6px 12px', display: 'flex', flexDirection: 'column', gap: 8 }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <strong>Corner Pin</strong>
      <span style={{ fontSize: 12, color: 'var(--pf-on-variant)' }}>
        {isAddPointMode ? `Tap layer to pin (${puppetPinCount})` : 'Drag corners or Add Point'}
      </span>
    </div>
    <div style={{ display: 'flex', gap: 8, overflowX: 'auto' }}>
      <button type="button" className={`pf-tool-chip ${isAddPointMode ? 'active' : ''}`} onClick={onToggleAddPointMode} data-testid="corner_pin_add_point_toggle">
        <img src="../assets/icons/ic_corner_pin.png" alt="Add Point" className="pf-icon pf-icon-sm" />
        <span>Add Point</span>
      </button>
      {puppetPinCount > 0 && (
        <button type="button" className="pf-tool-chip" onClick={onClearPins} data-testid="corner_pin_clear_pins_button">
          <img src="../assets/icons/ic_close.png" alt="Clear Pins" className="pf-icon pf-icon-sm" />
          <span>Clear ({puppetPinCount})</span>
        </button>
      )}
      <button type="button" className="pf-tool-chip" disabled={!canUndo} onClick={onUndo} data-testid="corner_pin_undo_button">
        <img src="../assets/icons/ic_undo.png" alt="Undo Corner Pin" className="pf-icon pf-icon-sm" />
        <span>Undo</span>
      </button>
      <button type="button" className="pf-tool-chip" style={{ background: 'var(--pf-emerald)' }} onClick={onConfirm} data-testid="corner_pin_confirm_button">
        <img src="../assets/icons/ic_check.png" alt="Confirm Corner Pin" className="pf-icon pf-icon-sm" />
        <span>Confirm</span>
      </button>
    </div>
  </div>
);

export interface FloatingThreeDControllerProps {
  onUndo: () => void;
  onRedo: () => void;
  onCenterLayer: () => void;
  onResetAll: () => void;
}

export const FloatingThreeDControllerPanel: React.FC<FloatingThreeDControllerProps> = ({
  onUndo,
  onRedo,
  onCenterLayer,
  onResetAll,
}) => {
  const [mode, setMode] = useState<ThreeDMode>('MOVE');
  const [moveIsZ, setMoveIsZ] = useState(false);
  const [rotateAxis, setRotateAxis] = useState<'X' | 'Y' | 'Z'>('Y');
  const [scaleLinked, setScaleLinked] = useState(true);
  const [skewAxis, setSkewAxis] = useState<'X' | 'Y'>('X');

  return (
    <FloatingPanelContainer
      panelTag="three_d_floating_panel"
      dragHandleTag="three_d_drag_handle"
      className="am-3d"
      initialHeight={268}
      minWidth={210}
      minHeight={165}
    >
      <div className="pf-3d-body">
        <div className="pf-3d-left-rail">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <button type="button" className="pf-touch-btn" onClick={onUndo} data-testid="three_d_undo_button">
              <img src="../assets/icons/ic_undo.png" alt="Undo" className="pf-icon" />
            </button>
            <button type="button" className="pf-touch-btn" onClick={onRedo} data-testid="three_d_redo_button">
              <img src="../assets/icons/ic_redo.png" alt="Redo" className="pf-icon" />
            </button>
          </div>
          <button type="button" className="pf-touch-btn" onClick={onCenterLayer} title="Center / Reset 3D" onDoubleClick={onResetAll} data-testid="three_d_more_button">
            <img src="../assets/icons/ic_more_vert.png" alt="3D Options" className="pf-icon" />
          </button>
        </div>

        <div className="pf-3d-center">
          {mode === 'MOVE' && (
            <>
              <div className="pf-pill-row">
                <button type="button" className={`pf-pill-btn ${!moveIsZ ? 'active' : ''}`} onClick={() => setMoveIsZ(false)}>
                  X Y
                </button>
                <button type="button" className={`pf-pill-btn ${moveIsZ ? 'active' : ''}`} onClick={() => setMoveIsZ(true)}>
                  Z
                </button>
              </div>
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--pf-am-muted)', fontSize: 13 }}>
                {moveIsZ ? 'Swipe here to adjust Z position' : 'Swipe here to move layer'}
              </div>
            </>
          )}
          {mode === 'ROTATE' && (
            <div className="pf-pill-row">
              {(['X', 'Y', 'Z'] as const).map((ax) => (
                <button key={ax} type="button" className={`pf-pill-btn ${rotateAxis === ax ? 'active' : ''}`} onClick={() => setRotateAxis(ax)}>
                  {ax}
                </button>
              ))}
            </div>
          )}
          {mode === 'SCALE' && (
            <div className="pf-pill-row">
              <button type="button" className="pf-pill-btn active" onClick={() => setScaleLinked((v) => !v)}>
                {scaleLinked ? 'Linked Width / Height' : 'Unlinked Scale'}
              </button>
            </div>
          )}
          {mode === 'SKEW' && (
            <div className="pf-pill-row">
              <button type="button" className={`pf-pill-btn ${skewAxis === 'X' ? 'active' : ''}`} onClick={() => setSkewAxis('X')}>
                Skew X
              </button>
              <button type="button" className={`pf-pill-btn ${skewAxis === 'Y' ? 'active' : ''}`} onClick={() => setSkewAxis('Y')}>
                Skew Y
              </button>
            </div>
          )}
        </div>

        <div className="pf-3d-right-rail">
          <button type="button" className={`pf-3d-mode-btn ${mode === 'MOVE' ? 'selected' : ''}`} onClick={() => setMode('MOVE')} data-testid="three_d_mode_move">
            <img src="../assets/icons/ic_3d.png" alt="Move Mode" className="pf-icon" />
          </button>
          <button type="button" className={`pf-3d-mode-btn ${mode === 'ROTATE' ? 'selected' : ''}`} onClick={() => setMode('ROTATE')} data-testid="three_d_mode_rotate">
            <img src="../assets/icons/ic_3d_rotate.png" alt="Rotate Mode" className="pf-icon" />
          </button>
          <button type="button" className={`pf-3d-mode-btn ${mode === 'SCALE' ? 'selected' : ''}`} onClick={() => setMode('SCALE')} data-testid="three_d_mode_scale">
            <img src="../assets/icons/ic_3d_scale.png" alt="Scale Mode" className="pf-icon" />
          </button>
          <button type="button" className={`pf-3d-mode-btn ${mode === 'SKEW' ? 'selected' : ''}`} onClick={() => setMode('SKEW')} data-testid="three_d_mode_skew">
            <img src="../assets/icons/ic_3d_skew.png" alt="Skew Mode" className="pf-icon" />
          </button>
        </div>
      </div>
    </FloatingPanelContainer>
  );
};
