import React, { useState, useRef } from 'react';
import { CornerPinToolPanel, FloatingThreeDControllerPanel } from './CornerPinAnd3DPanels';
import { FilterToolPanel } from './FilterToolPanel';
import { FloatingPanelContainer } from './FloatingPanelContainer';
import { TextToolPanel } from './TextToolPanel';
import { ColorToolPanel } from './ColorToolPanel';
import { CropToolPanel } from './CropToolPanel';
import { CanvasSettingsPanel } from './CanvasSettingsPanel';
import { SettingsDialog } from './SettingsDialog';
import { MyProjectsDrawer } from './MyProjectsDrawer';
import { LayersDrawer } from './LayersDrawer';
import { SaveExportDialog } from './SaveExportDialog';
import { RotateTransformPanel } from './RotateTransformPanel';
import { AdjustToolPanel } from './AdjustToolPanel';
import { EmojiSettingsPanel } from './EmojiSettingsPanel';
import { AudioService } from '../services/AudioService';

export type ActiveTab =
  | 'NONE'
  | 'TEXT'
  | 'COLOR'
  | 'FONT'
  | 'ROTATE'
  | 'ADJUST'
  | 'CROP'
  | 'CORNER_PIN'
  | 'FILTER'
  | 'THREE_D'
  | 'EMOJI';

export interface EditorScreenProps {
  projectTitle: string;
  onNavigateBack: () => void;
}

export const EditorScreen: React.FC<EditorScreenProps> = ({ projectTitle, onNavigateBack }) => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('NONE');
  const [isZoomMode, setIsZoomMode] = useState(false);
  const [creatorView, setCreatorView] = useState(false);
  const [layerText, setLayerText] = useState('PixelForge');
  const [layerColor, setLayerColor] = useState('#ffffff');
  const [blendMode, setBlendMode] = useState('Normal (Default)');
  const [opacity, setOpacity] = useState(1);
  const [strokeOn, setStrokeOn] = useState(false);
  const [shadowOn, setShadowOn] = useState(false);
  const [pins, setPins] = useState(0);
  const [canvasRatio, setCanvasRatio] = useState('1:1');
  const [exposure, setExposure] = useState(0);
  const [contrast, setContrast] = useState(0);
  const [saturation, setSaturation] = useState(0);
  const [rotation, setRotation] = useState(0);
  const [flipH, setFlipH] = useState(false);
  const [flipV, setFlipV] = useState(false);
  const [rotate3DX, setRotate3DX] = useState(0);
  const [rotate3DY, setRotate3DY] = useState(0);
  const [scale3D, setScale3D] = useState(1);
  const [showSettings, setShowSettings] = useState(false);
  const [showProjectsDrawer, setShowProjectsDrawer] = useState(false);
  const [showLayersDrawer, setShowLayersDrawer] = useState(false);
  const [showExportDialog, setShowExportDialog] = useState(false);
  const [showAddMenu, setShowAddMenu] = useState(false);
  const [isDarkTheme, setIsDarkTheme] = useState(true);
  const [backdropImage, setBackdropImage] = useState<string | null>(null);

  const photoFileInputRef = useRef<HTMLInputElement | null>(null);

  const handlePhotoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (ev) => {
        if (ev.target?.result) {
          setBackdropImage(ev.target.result as string);
          AudioService.playShutterSound();
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const getCanvasAspectRatioStyle = () => {
    switch (canvasRatio) {
      case '16:9':
        return { aspectRatio: '16 / 9', maxWidth: '92%' };
      case '9:16':
        return { aspectRatio: '9 / 16', maxHeight: '78%' };
      case '4:3':
        return { aspectRatio: '4 / 3', maxWidth: '85%' };
      case '1:1':
      default:
        return { aspectRatio: '1 / 1', maxWidth: '82%' };
    }
  };

  const getComputedBlendMode = (): React.CSSProperties['mixBlendMode'] => {
    const lower = blendMode.toLowerCase();
    if (lower.includes('multiply')) return 'multiply';
    if (lower.includes('screen')) return 'screen';
    if (lower.includes('overlay')) return 'overlay';
    if (lower.includes('darken')) return 'darken';
    if (lower.includes('lighten')) return 'lighten';
    if (lower.includes('color dodge')) return 'color-dodge';
    return 'normal';
  };

  return (
    <div className="pf-editor-shell">
      {/* Hidden File Input for Direct Local Photo Picking */}
      <input
        type="file"
        ref={photoFileInputRef}
        accept="image/*"
        style={{ display: 'none' }}
        onChange={handlePhotoUpload}
      />

      {!creatorView && (
        <header className="pf-topbar">
          <div className="pf-topbar-row">
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <button
                type="button"
                className="pf-touch-btn"
                onClick={onNavigateBack}
                data-testid="top_back_button"
                title="Back to Projects"
              >
                <img src="/assets/icons/ic_close.png" alt="Back" className="pf-icon" />
              </button>

              <div style={{ position: 'relative' }}>
                <button
                  type="button"
                  className="pf-touch-btn"
                  onClick={() => setShowAddMenu((v) => !v)}
                  data-testid="top_add_button"
                  title="Add Layer or Photo"
                >
                  <img src="/assets/icons/ic_add.png" alt="Add Layer" className="pf-icon" />
                </button>
                {showAddMenu && (
                  <div
                    style={{
                      position: 'absolute',
                      top: '100%',
                      left: 0,
                      background: 'var(--pf-surface)',
                      border: '1px solid var(--pf-outline)',
                      borderRadius: 12,
                      padding: 6,
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 4,
                      zIndex: 120,
                      boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
                      minWidth: 140,
                    }}
                  >
                    <button
                      type="button"
                      onClick={() => {
                        setShowAddMenu(false);
                        photoFileInputRef.current?.click();
                      }}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        padding: '8px 12px',
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--pf-on-surface)',
                        borderRadius: 8,
                        cursor: 'pointer',
                        fontSize: 13,
                        textAlign: 'left',
                      }}
                    >
                      <img src="/assets/icons/ic_add_photo.png" alt="" className="pf-icon pf-icon-sm" />
                      <span>Photo</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setShowAddMenu(false);
                        setActiveTab('TEXT');
                      }}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        padding: '8px 12px',
                        background: 'transparent',
                        border: 'none',
                        color: 'var(--pf-on-surface)',
                        borderRadius: 8,
                        cursor: 'pointer',
                        fontSize: 13,
                        textAlign: 'left',
                      }}
                    >
                      <img src="/assets/icons/ic_add_text.png" alt="" className="pf-icon pf-icon-sm" />
                      <span>Text</span>
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                className="pf-touch-btn"
                onClick={() => {
                  setPins(0);
                  setRotation(0);
                  setFlipH(false);
                  setFlipV(false);
                }}
                data-testid="top_undo_button"
                title="Reset Transform"
              >
                <img src="/assets/icons/ic_undo.png" alt="Undo" className="pf-icon" />
              </button>
              <button
                type="button"
                className="pf-touch-btn"
                onClick={() => {
                  setRotation(90);
                }}
                data-testid="top_redo_button"
                title="Redo"
              >
                <img src="/assets/icons/ic_redo.png" alt="Redo" className="pf-icon" />
              </button>
            </div>

            <span style={{ fontSize: 13, fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 120 }}>
              {projectTitle}
            </span>

            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <button
                type="button"
                className="pf-touch-btn"
                onClick={() => setShowExportDialog(true)}
                data-testid="top_save_button"
                title="Export Image"
              >
                <img src="/assets/icons/ic_save.png" alt="Save" className="pf-icon" />
              </button>
              <button
                type="button"
                className="pf-touch-btn"
                onClick={() => setShowExportDialog(true)}
                data-testid="top_share_button"
                title="Share Project"
              >
                <img src="/assets/icons/ic_share.png" alt="Share" className="pf-icon" />
              </button>
              <button
                type="button"
                className="pf-touch-btn"
                onClick={() => setShowSettings(true)}
                data-testid="three_dot_menu_button"
                title="Studio Settings"
              >
                <img src="/assets/icons/ic_more_vert.png" alt="Menu" className="pf-icon" />
              </button>
            </div>
          </div>

          <div className="pf-topbar-context">
            <button
              type="button"
              className="pf-tool-chip"
              onClick={() => {
                setLayerText('');
                setBackdropImage(null);
                setActiveTab('NONE');
              }}
              data-testid="top_tool_delete"
            >
              <img src="/assets/icons/ic_delete.png" alt="Delete" className="pf-icon pf-icon-sm" />
              <span>Delete</span>
            </button>
            <button
              type="button"
              className={`pf-tool-chip ${activeTab === 'CORNER_PIN' ? 'active' : ''}`}
              onClick={() => setActiveTab(activeTab === 'CORNER_PIN' ? 'NONE' : 'CORNER_PIN')}
              data-testid="top_tool_corner_pin"
            >
              <img src="/assets/icons/ic_corner_pin.png" alt="Corner Pin" className="pf-icon pf-icon-sm" />
              <span>Corner Pin</span>
            </button>
            <button
              type="button"
              className={`pf-tool-chip ${activeTab === 'THREE_D' ? 'active' : ''}`}
              onClick={() => setActiveTab(activeTab === 'THREE_D' ? 'NONE' : 'THREE_D')}
              data-testid="top_tool_3d"
            >
              <img src="/assets/icons/ic_3d.png" alt="3D" className="pf-icon pf-icon-sm" />
              <span>3D</span>
            </button>
            <button
              type="button"
              className={`pf-tool-chip ${activeTab === 'FILTER' ? 'active' : ''}`}
              onClick={() => setActiveTab(activeTab === 'FILTER' ? 'NONE' : 'FILTER')}
              data-testid="top_tool_filter"
            >
              <img src="/assets/icons/ic_filter.png" alt="Filter" className="pf-icon pf-icon-sm" />
              <span>Filter</span>
            </button>
            <button
              type="button"
              className={`pf-tool-chip ${activeTab === 'CROP' ? 'active' : ''}`}
              onClick={() => setActiveTab(activeTab === 'CROP' ? 'NONE' : 'CROP')}
              data-testid="top_tool_crop"
            >
              <img src="/assets/icons/ic_crop.png" alt="Crop" className="pf-icon pf-icon-sm" />
              <span>Crop</span>
            </button>
          </div>
        </header>
      )}

      <main className="pf-editor-stage" data-testid="editor_canvas_viewport">
        {!creatorView && (
          <>
            <button
              type="button"
              className={`pf-floating-corner-btn ${isZoomMode ? 'active' : ''}`}
              style={{ right: 14, top: 14 }}
              onClick={() => setIsZoomMode((v) => !v)}
              data-testid="top_zoom_button"
            >
              <img src="/assets/icons/ic_zoom.png" alt="Zoom" className="pf-icon pf-icon-sm" />
              <span>Zoom</span>
            </button>
            <button
              type="button"
              className="pf-floating-corner-btn"
              style={{ right: 14, top: 74 }}
              onClick={() => setShowLayersDrawer(true)}
              data-testid="top_layers_button"
            >
              <img src="/assets/icons/ic_layers.png" alt="Layers" className="pf-icon pf-icon-sm" />
              <span>Layers</span>
            </button>
          </>
        )}

        {/* Central Canvas Viewport with Live CSS Transformations */}
        <div
          className={`pf-canvas-frame ${creatorView ? '' : 'pf-checkerboard'}`}
          style={{
            ...getCanvasAspectRatioStyle(),
            background: creatorView ? '#00ff00' : undefined,
            overflow: 'hidden',
            position: 'relative',
            perspective: 800,
          }}
        >
          {backdropImage && (
            <img
              src={backdropImage}
              alt="Backdrop"
              style={{
                position: 'absolute',
                inset: 0,
                width: '100%',
                height: '100%',
                objectFit: 'cover',
                filter: `brightness(${1 + exposure * 0.5}) contrast(${1 + contrast * 0.5}) saturate(${1 + saturation * 0.5})`,
                transform: `rotate(${rotation}deg) scaleX(${flipH ? -1 : 1}) scaleY(${flipV ? -1 : 1}) rotateX(${rotate3DX}deg) rotateY(${rotate3DY}deg) scale(${scale3D})`,
                transition: 'transform 0.15s ease-out',
              }}
            />
          )}

          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: 'clamp(20px, 7vw, 36px)',
              color: layerColor,
              opacity,
              mixBlendMode: getComputedBlendMode(),
              transform: `rotate(${rotation}deg) scaleX(${flipH ? -1 : 1}) scaleY(${flipV ? -1 : 1}) rotateX(${rotate3DX}deg) rotateY(${rotate3DY}deg) scale(${scale3D})`,
              filter: `brightness(${1 + exposure * 0.5}) contrast(${1 + contrast * 0.5}) saturate(${1 + saturation * 0.5})`,
              textShadow: shadowOn ? '0px 6px 18px rgba(0,0,0,0.85)' : undefined,
              WebkitTextStroke: strokeOn ? '2px #000000' : undefined,
              userSelect: 'none',
              padding: 16,
              textAlign: 'center',
              wordBreak: 'break-word',
              transition: 'transform 0.15s ease-out, filter 0.15s ease-out',
            }}
          >
            {layerText}
          </div>

          {/* Corner pin markers if active */}
          {pins > 0 && (
            <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
              <div style={{ position: 'absolute', top: 8, left: 8, width: 14, height: 14, borderRadius: '50%', background: 'var(--pf-amber)', border: '2px solid #fff' }} />
              <div style={{ position: 'absolute', top: 8, right: 8, width: 14, height: 14, borderRadius: '50%', background: 'var(--pf-amber)', border: '2px solid #fff' }} />
              <div style={{ position: 'absolute', bottom: 8, left: 8, width: 14, height: 14, borderRadius: '50%', background: 'var(--pf-amber)', border: '2px solid #fff' }} />
              <div style={{ position: 'absolute', bottom: 8, right: 8, width: 14, height: 14, borderRadius: '50%', background: 'var(--pf-amber)', border: '2px solid #fff' }} />
            </div>
          )}
        </div>

        {activeTab !== 'NONE' && activeTab !== 'THREE_D' && (
          <FloatingPanelContainer
            panelTag="tool_drawer_container"
            dragHandleTag="tool_drawer_drag_handle"
            onDismissSwipeDown={() => setActiveTab('NONE')}
          >
            {activeTab === 'FILTER' && (
              <FilterToolPanel
                blendMode={blendMode}
                opacity={opacity}
                strokeEnabled={strokeOn}
                shadowEnabled={shadowOn}
                onSetBlendMode={setBlendMode}
                onToggleStroke={() => setStrokeOn((v) => !v)}
                onToggleShadow={() => setShadowOn((v) => !v)}
                onSetOpacity={setOpacity}
                onLaunchEyedropper={() => {}}
              />
            )}
            {activeTab === 'CORNER_PIN' && (
              <CornerPinToolPanel
                canUndo={pins > 0}
                isAddPointMode={pins > 0}
                puppetPinCount={pins}
                onToggleAddPointMode={() => setPins((c) => (c < 4 ? c + 1 : 0))}
                onClearPins={() => setPins(0)}
                onUndo={() => setPins((c) => Math.max(0, c - 1))}
                onConfirm={() => setActiveTab('NONE')}
              />
            )}
            {activeTab === 'TEXT' && (
              <TextToolPanel
                draftText={layerText}
                onTextChange={setLayerText}
                onConfirm={() => setActiveTab('NONE')}
              />
            )}
            {activeTab === 'COLOR' && (
              <ColorToolPanel
                selectedColor={layerColor}
                onSelectColor={setLayerColor}
              />
            )}
            {activeTab === 'CROP' && (
              <CropToolPanel
                onCommitCrop={() => setActiveTab('NONE')}
              />
            )}
            {activeTab === 'FONT' && (
              <CanvasSettingsPanel
                activeRatio={canvasRatio}
                onSelectRatio={setCanvasRatio}
              />
            )}
            {activeTab === 'ROTATE' && (
              <RotateTransformPanel
                onRotate90={() => setRotation((r) => (r + 90) % 360)}
                onFlipHorizontal={() => setFlipH((f) => !f)}
                onFlipVertical={() => setFlipV((f) => !f)}
              />
            )}
            {activeTab === 'ADJUST' && (
              <AdjustToolPanel
                exposure={exposure}
                contrast={contrast}
                saturation={saturation}
                onUpdateExposure={setExposure}
                onUpdateContrast={setContrast}
                onUpdateSaturation={setSaturation}
              />
            )}
            {activeTab === 'EMOJI' && (
              <EmojiSettingsPanel
                onSelectEmoji={(em) => setLayerText((t) => t + em)}
              />
            )}
          </FloatingPanelContainer>
        )}

        {activeTab === 'THREE_D' && (
          <FloatingThreeDControllerPanel
            onUndo={() => {
              setRotate3DX(0);
              setRotate3DY(0);
            }}
            onRedo={() => {
              setRotate3DY(25);
            }}
            onCenterLayer={() => {
              setRotate3DX(0);
              setRotate3DY(0);
              setScale3D(1);
            }}
            onResetAll={() => {
              setRotate3DX(0);
              setRotate3DY(0);
              setScale3D(1);
            }}
          />
        )}
      </main>

      {!creatorView && (
        <nav className="pf-bottom-dock">
          <button
            type="button"
            className={`pf-dock-btn ${activeTab === 'TEXT' ? 'active' : ''}`}
            onClick={() => setActiveTab(activeTab === 'TEXT' ? 'NONE' : 'TEXT')}
            data-testid="bottom_tool_edit_text"
          >
            <img src="/assets/icons/ic_edit.png" alt="Edit" className="pf-icon pf-icon-sm" />
            <span>Edit</span>
          </button>
          <button
            type="button"
            className={`pf-dock-btn ${activeTab === 'COLOR' ? 'active' : ''}`}
            onClick={() => setActiveTab(activeTab === 'COLOR' ? 'NONE' : 'COLOR')}
            data-testid="bottom_tool_color"
          >
            <img src="/assets/icons/ic_color.png" alt="Color" className="pf-icon pf-icon-sm" />
            <span>Color</span>
          </button>
          <button
            type="button"
            className={`pf-dock-btn ${activeTab === 'FONT' ? 'active' : ''}`}
            onClick={() => setActiveTab(activeTab === 'FONT' ? 'NONE' : 'FONT')}
            data-testid="bottom_tool_font"
          >
            <img src="/assets/icons/ic_font.png" alt="Font" className="pf-icon pf-icon-sm" />
            <span>Font</span>
          </button>
          <button
            type="button"
            className={`pf-dock-btn ${activeTab === 'ROTATE' ? 'active' : ''}`}
            onClick={() => setActiveTab(activeTab === 'ROTATE' ? 'NONE' : 'ROTATE')}
            data-testid="bottom_tool_rotate"
          >
            <img src="/assets/icons/ic_rotate.png" alt="Rotate" className="pf-icon pf-icon-sm" />
            <span>Rotate</span>
          </button>
          <button
            type="button"
            className={`pf-dock-btn ${activeTab === 'ADJUST' ? 'active' : ''}`}
            onClick={() => setActiveTab(activeTab === 'ADJUST' ? 'NONE' : 'ADJUST')}
            data-testid="bottom_tool_adjust"
          >
            <img src="/assets/icons/ic_adjust.png" alt="Adjust" className="pf-icon pf-icon-sm" />
            <span>Adjust</span>
          </button>
        </nav>
      )}

      {showSettings && (
        <SettingsDialog
          isDarkTheme={isDarkTheme}
          onToggleTheme={setIsDarkTheme}
          onDismiss={() => setShowSettings(false)}
        />
      )}

      {showProjectsDrawer && (
        <MyProjectsDrawer
          projects={[{ id: '1', title: projectTitle, updatedAtLabel: 'Active' }]}
          onClose={() => setShowProjectsDrawer(false)}
          onOpenProject={() => {}}
        />
      )}

      {showLayersDrawer && (
        <LayersDrawer onClose={() => setShowLayersDrawer(false)} />
      )}

      {showExportDialog && (
        <SaveExportDialog
          projectTitle={projectTitle}
          layerText={layerText}
          layerColor={layerColor}
          canvasRatio={canvasRatio}
          opacity={opacity}
          exposure={exposure}
          contrast={contrast}
          saturation={saturation}
          strokeEnabled={strokeOn}
          shadowEnabled={shadowOn}
          rotation={rotation}
          flipH={flipH}
          flipV={flipV}
          onDismiss={() => setShowExportDialog(false)}
        />
      )}
    </div>
  );
};
