import React, { useRef, useState } from 'react';

export interface FloatingPanelProps {
  panelTag: string;
  dragHandleTag: string;
  className?: string;
  initialWidth?: number;
  initialHeight?: number;
  minWidth?: number;
  minHeight?: number;
  onDismissSwipeDown?: () => void;
  children: React.ReactNode;
}

export const FloatingPanelContainer: React.FC<FloatingPanelProps> = ({
  panelTag,
  dragHandleTag,
  className = '',
  initialWidth = 340,
  initialHeight = 240,
  minWidth = 210,
  minHeight = 110,
  onDismissSwipeDown,
  children,
}) => {
  const [pos, setPos] = useState({ x: 10, y: 210 });
  const [size, setSize] = useState({ w: initialWidth, h: initialHeight });
  const dragOrigin = useRef<{ x: number; y: number; startX: number; startY: number } | null>(null);

  const onDragPointerDown = (e: React.PointerEvent) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    dragOrigin.current = { x: pos.x, y: pos.y, startX: e.clientX, startY: e.clientY };
  };

  const onDragPointerMove = (e: React.PointerEvent) => {
    if (!dragOrigin.current) return;
    const dx = e.clientX - dragOrigin.current.startX;
    const dy = e.clientY - dragOrigin.current.startY;
    setPos({ x: dragOrigin.current.x + dx, y: dragOrigin.current.y + dy });
  };

  const onDragPointerUp = (e: React.PointerEvent) => {
    if (dragOrigin.current && onDismissSwipeDown) {
      const dy = e.clientY - dragOrigin.current.startY;
      if (dy > 64) onDismissSwipeDown();
    }
    dragOrigin.current = null;
  };

  const startResize = (corner: 'tl' | 'tr' | 'bl' | 'br') => (e: React.PointerEvent) => {
    e.stopPropagation();
    const sx = e.clientX;
    const sy = e.clientY;
    const sw = size.w;
    const sh = size.h;
    const spx = pos.x;
    const spy = pos.y;

    const onMove = (ev: PointerEvent) => {
      const dx = ev.clientX - sx;
      const dy = ev.clientY - sy;
      let nw = sw;
      let nh = sh;
      let nx = spx;
      let ny = spy;
      if (corner === 'br' || corner === 'tr') nw = Math.max(minWidth, sw + dx);
      if (corner === 'bl' || corner === 'tl') {
        nw = Math.max(minWidth, sw - dx);
        nx = spx + (sw - nw);
      }
      if (corner === 'br' || corner === 'bl') nh = Math.max(minHeight, sh + dy);
      if (corner === 'tr' || corner === 'tl') {
        nh = Math.max(minHeight, sh - dy);
        ny = spy + (sh - nh);
      }
      setSize({ w: nw, h: nh });
      setPos({ x: nx, y: ny });
    };
    const onUp = () => {
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
    };
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
  };

  return (
    <section
      data-testid={panelTag}
      className={`pf-floating-panel ${className}`}
      style={{ left: pos.x, top: pos.y, width: size.w, height: size.h }}
    >
      <div
        data-testid={dragHandleTag}
        className="pf-drag-strip"
        onPointerDown={onDragPointerDown}
        onPointerMove={onDragPointerMove}
        onPointerUp={onDragPointerUp}
      >
        <div className="pf-drag-pill" />
      </div>
      <div style={{ flex: 1, overflowY: 'auto', minHeight: 0 }}>{children}</div>
      <div onPointerDown={startResize('tl')} style={{ position: 'absolute', left: 0, top: 0, width: 16, height: 16, cursor: 'nwse-resize' }} />
      <div onPointerDown={startResize('tr')} style={{ position: 'absolute', right: 0, top: 0, width: 16, height: 16, cursor: 'nesw-resize' }} />
      <div onPointerDown={startResize('bl')} style={{ position: 'absolute', left: 0, bottom: 0, width: 16, height: 16, cursor: 'nesw-resize' }} />
      <div onPointerDown={startResize('br')} style={{ position: 'absolute', right: 0, bottom: 0, width: 16, height: 16, cursor: 'nwse-resize' }} />
    </section>
  );
};
