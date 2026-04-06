import React, { useEffect, useRef, useState } from 'react';
import rrwebPlayer from 'rrweb-player';
import type { eventWithTime } from '@rrweb/types';

export interface SessionPlayerProps {
  /** URL бэкенда для получения событий */
  backendUrl: string;
  
  /** ID сессии для воспроизведения */
  sessionId: string;
  
  /** Ширина плеера */
  width?: number | string;
  
  /** Высота плеера */
  height?: number | string;
  
  /** Автоматическое воспроизведение */
  autoPlay?: boolean;
  
  /** Скорость воспроизведения */
  speed?: number;
  
  /** Показывать контролы */
  showControls?: boolean;
  
  /** API ключ (опционально) */
  apiKey?: string;
  
  /** Callback при загрузке событий */
  onLoaded?: (events: eventWithTime[]) => void;
  
  /** Callback при ошибке загрузки */
  onError?: (error: Error) => void;
}

/**
 * React компонент для воспроизведения сессий
 */
export const SessionPlayer: React.FC<SessionPlayerProps> = ({
  backendUrl,
  sessionId,
  width = '100%',
  height = 600,
  autoPlay = false,
  speed = 1,
  showControls = true,
  apiKey,
  onLoaded,
  onError,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<rrwebPlayer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [events, setEvents] = useState<eventWithTime[]>([]);

  // Загрузка событий сессии
  useEffect(() => {
    const loadEvents = async () => {
      try {
        setLoading(true);
        setError(null);

        const response = await fetch(
          `${backendUrl}/api/v1/sessions/${sessionId}/events`,
          {
            headers: {
              ...(apiKey && { 'X-API-Key': apiKey }),
            },
          }
        );

        if (!response.ok) {
          throw new Error(`Failed to load session: ${response.status}`);
        }

        const sessionEvents = await response.json();
        
        // Преобразуем наши события в формат rrweb
        const rrwebEvents: eventWithTime[] = sessionEvents.map((e: any) => ({
          timestamp: e.timestamp,
          type: parseInt(e.eventType, 10),
          data: e.data,
        }));

        setEvents(rrwebEvents);
        onLoaded?.(rrwebEvents);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Unknown error';
        setError(errorMessage);
        onError?.(err instanceof Error ? err : new Error(errorMessage));
      } finally {
        setLoading(false);
      }
    };

    loadEvents();
  }, [backendUrl, sessionId, apiKey, onLoaded, onError]);

  // Инициализация плеера
  useEffect(() => {
    if (!containerRef.current || events.length === 0 || !playerRef.current) {
      return;
    }

    // Очищаем предыдущий плеер
    containerRef.current.innerHTML = '';

    // Создаем новый плеер
    playerRef.current = new rrwebPlayer({
      target: containerRef.current,
      props: {
        events,
        width: typeof width === 'number' ? width : parseInt(width as string, 10) || 800,
        height: typeof height === 'number' ? height : parseInt(height as string, 10) || 600,
        speed,
        autoPlay,
        showControls,
      },
    });

    return () => {
      if (playerRef.current) {
        playerRef.current.destroy();
        playerRef.current = null;
      }
    };
  }, [events, width, height, speed, autoPlay, showControls]);

  // Обновление настроек плеера
  useEffect(() => {
    if (playerRef.current) {
      playerRef.current.setSpeed(speed);
    }
  }, [speed]);

  if (loading) {
    return (
      <div
        style={{
          width: typeof width === 'string' ? width : `${width}px`,
          height: typeof height === 'string' ? height : `${height}px`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#f5f5f5',
        }}
      >
        Loading session...
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          width: typeof width === 'string' ? width : `${width}px`,
          height: typeof height === 'string' ? height : `${height}px`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#ffebee',
          color: '#c62828',
        }}
      >
        Error: {error}
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      style={{
        width: typeof width === 'string' ? width : `${width}px`,
        height: typeof height === 'string' ? height : `${height}px`,
      }}
    />
  );
};

export default SessionPlayer;
