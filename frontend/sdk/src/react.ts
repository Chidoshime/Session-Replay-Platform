import React, { useEffect, useRef } from 'react';
import { init, SessionRecorder, SessionReplayConfig } from '@sessionreplay/sdk';

interface UseSessionReplayProps extends Omit<SessionReplayConfig, 'apiKey'> {
  /** Автоматически начинать запись при монтировании компонента */
  autoStart?: boolean;
  
  /** API ключ (опционально) */
  apiKey?: string;
}

/**
 * React хук для использования Session Replay
 */
export function useSessionReplay(props: UseSessionReplayProps = {}) {
  const { autoStart = true, ...config } = props;
  const recorderRef = useRef<SessionRecorder | null>(null);

  useEffect(() => {
    // Инициализируем SDK только один раз
    if (!recorderRef.current && config.backendUrl) {
      recorderRef.current = init({
        backendUrl: config.backendUrl,
        apiKey: config.apiKey,
        bufferSize: config.bufferSize,
        flushIntervalMs: config.flushIntervalMs,
        recordConsole: config.recordConsole,
        recordNetwork: config.recordNetwork,
        maskAllInputs: config.maskAllInputs,
        maskInputOptions: config.maskInputOptions,
        blockClass: config.blockClass,
        ignoreClass: config.ignoreClass,
        onError: config.onError,
      });

      if (autoStart) {
        recorderRef.current.start();
      }
    }

    // Очистка при размонтировании
    return () => {
      if (recorderRef.current) {
        recorderRef.current.stop();
      }
    };
  }, [autoStart, config]);

  return {
    recorder: recorderRef.current,
    sessionId: recorderRef.current?.getSessionId(),
    start: () => recorderRef.current?.start(),
    stop: () => recorderRef.current?.stop(),
    addCustomEvent: (type: string, data: any) => 
      recorderRef.current?.addCustomEvent(type, data),
  };
}

export default useSessionReplay;
