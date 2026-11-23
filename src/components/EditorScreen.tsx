import React, { useState } from 'react';
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

export type ActiveTab = 'NONE' | 'TEXT' | 'COLOR' | 'FONT' | 'ROTATE' | 'ADJUST' | 'CROP' | 'CORNER_PIN' | 'FILTER' | 'THREE_D' | 'EMOJI';

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
  const [showSettings, setShowSettings] = useState(false);
  const [showProjectsDrawer, setShowProjectsDrawer] = useState(false);
  const [showLayersDrawer, setShowLayersDrawer] = useState(false);
  const [showExportDialog, setShowExportDialog] = useState(false);
  const [isDarkTheme, setIsDarkTheme] = useState(true);

  return (
    <div className="pf-editor-shell">
      {!creatorView && (
        <header className="pf-topbar">
          <div className="pf-topbar-row">
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <button type="button" className="pf-touch-btn" onClick={() => setActiveTab('TEXT')} data-testid="top_add_button">
                <img src="../assets/icons/ic_add.png" alt="Add Layer" className="pf-icon" />
              </button>
              <button type="button" className="pf-touch-btn" data-testid="top_undo_button">
                <img src="../assets/icons/ic_undo.png" alt="Undo" className="pf-icon" />
              </button>
              <button type="button" className="pf-touch-btn" data-testid="top_redo_button">
                <img src="../assets/icons/ic_redo.png" alt="Redo" className="pf-icon" />
              </button>
            </div>
            <span style={{ fontSize: 13, fontWeight: 700 }}>{projectTitle}</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <button type="button" className="pf-touch-btn" onClick={() => setShowExportDialog(true)} data-testid="top_save_button">
                <img src="../assets/icons/ic_save.png" alt="Save" className="pf-icon" />
              </button>
              <button type="button" className="pf-touch-btn" data-testid="top_share_button">
                <img src="../assets/icons/ic_share.png" alt="Share" className="pf-icon" />
              </button>
              <button type="button" className="pf-touch-btn" onClick={() => setShowSettings(true)} data-testid="three_dot_menu_button">
                <img src="../assets/icons/ic_more_vert.png" alt="Menu" className="pf-icon" />
              </button>
            </div>
          </div>

          <div className="pf-topbar-context">
            <button type="button" className="pf-tool-chip" onClick={() => setActiveTab('NONE')} data-testid="top_tool_delete">
              <img src="../assets/icons/ic_delete.png" alt="Delete" className="pf-icon pf-icon-sm" />
              <span>Delete</span>
            </button>
            <button type="button" className={`pf-tool-chip ${activeTab === 'CORNER_PIN' ? 'active' : ''}`} onClick={() => setActiveTab('CORNER_PIN')} data-testid="top_tool_corner_pin">
              <img src="../assets/icons/ic_corner_pin.png" alt="Corner Pin" className="pf-icon pf-icon-sm" />
              <span>Corner Pin</span>
            </button>
            <button type="button" className={`pf-tool-chip ${activeTab === 'THREE_D' ? 'active' : ''}`} onClick={() => setActiveTab('THREE_D')} data-testid="top_tool_3d">
              <img src="../assets/icons/ic_3d.png" alt="3D" className="pf-icon pf-icon-sm" />
              <span>3D</span>
            </button>
            <button type="button" className={`pf-tool-chip ${activeTab === 'FILTER' ? 'active' : ''}`} onClick={() => setActiveTab('FILTER')} data-testid="top_tool_filter">
              <img src="../assets/icons/ic_filter.png" alt="Filter" className="pf-icon pf-icon-sm" />
              <span>Filter</span>
            </button>
            <button type="button" className={`pf-tool-chip ${activeTab === 'CROP' ? 'active' : ''}`} onClick={() => setActiveTab('CROP')} data-testid="top_tool_crop">
              <img src="../assets/icons/ic_crop.png" alt="Crop" className="pf-icon pf-icon-sm" />
              <span>Crop</span>
            </button>
          </div>
        </header>
      )}

      <main className="pf-editor-stage" data-testid="editor_canvas_viewport">
        {!creatorView && (
          <>
            <button type="button" className={`pf-floating-corner-btn ${isZoomMode ? 'active' : ''}`} style={{ right: 14, top: 14 }} onClick={() => setIsZoomMode((v) => !v)} data-testid="top_zoom_button">
              <img src="../assets/icons/ic_zoom.png" alt="Zoom" className="pf-icon pf-icon-sm" />
              <span>Zoom</span>
            </button>
            <button type="button" className="pf-floating-corner-btn" style={{ right: 14, top: 74 }} onClick={() => setShowLayersDrawer(true)} data-testid="top_layers_button">
              <img src="../assets/icons/ic_layers.png" alt="Layers" className="pf-icon pf-icon-sm" />
              <span>Layers</span>
            </button>
          </>
        )}

        <div className={`pf-canvas-frame ${creatorView ? '' : 'pf-checkerboard'}`} style={creatorView ? { background: '#00ff00' } : undefined}>
          <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 28, color: layerColor, opacity }}>
            {layerText}
          </div>
        </div>

        {activeTab !== 'NONE' && activeTab !== 'THREE_D' && (
          <FloatingPanelContainer panelTag="tool_drawer_container" dragHandleTag="tool_drawer_drag_handle" onDismissSwipeDown={() => setActiveTab('NONE')}>
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
                onToggleAddPointMode={() => setPins((c) => c + 1)}
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
                onRotate90={() => {}}
                onFlipHorizontal={() => {}}
                onFlipVertical={() => {}}
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
            onUndo={() => {}}
            onRedo={() => {}}
            onCenterLayer={() => {}}
            onResetAll={() => {}}
          />
        )}
      </main>

      {!creatorView && (
        <nav className="pf-bottom-dock">
          <button type="button" className={`pf-dock-btn ${activeTab === 'TEXT' ? 'active' : ''}`} onClick={() => setActiveTab('TEXT')} data-testid="bottom_tool_edit_text">
            <img src="../assets/icons/ic_edit.png" alt="Edit" className="pf-icon pf-icon-sm" />
            <span>Edit</span>
          </button>
          <button type="button" className={`pf-dock-btn ${activeTab === 'COLOR' ? 'active' : ''}`} onClick={() => setActiveTab('COLOR')} data-testid="bottom_tool_color">
            <img src="../assets/icons/ic_color.png" alt="Color" className="pf-icon pf-icon-sm" />
            <span>Color</span>
          </button>
          <button type="button" className={`pf-dock-btn ${activeTab === 'FONT' ? 'active' : ''}`} onClick={() => setActiveTab('FONT')} data-testid="bottom_tool_font">
            <img src="../assets/icons/ic_font.png" alt="Font" className="pf-icon pf-icon-sm" />
            <span>Font</span>
          </button>
          <button type="button" className={`pf-dock-btn ${activeTab === 'ROTATE' ? 'active' : ''}`} onClick={() => setActiveTab('ROTATE')} data-testid="bottom_tool_rotate">
            <img src="../assets/icons/ic_rotate.png" alt="Rotate" className="pf-icon pf-icon-sm" />
            <span>Rotate</span>
          </button>
          <button type="button" className={`pf-dock-btn ${activeTab === 'ADJUST' ? 'active' : ''}`} onClick={() => setActiveTab('ADJUST')} data-testid="bottom_tool_adjust">
            <img src="../assets/icons/ic_adjust.png" alt="Adjust" className="pf-icon pf-icon-sm" />
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
          onExport={() => setShowExportDialog(false)}
          onDismiss={() => setShowExportDialog(false)}
        />
      )}
    </div>
  );
};
