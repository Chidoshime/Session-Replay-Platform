import { SessionReplayConfig } from './types';
import SessionRecorder from './recorder';

let instance: SessionRecorder | null = null;

/**
 * Инициализация Session Replay SDK
 * @param config Конфигурация SDK
 * @returns Экземпляр рекордера
 */
export function init(config: SessionReplayConfig): SessionRecorder {
  if (instance) {
    console.warn('[SessionReplay] SDK already initialized. Use getInstance() to get the existing instance.');
    return instance;
  }

  instance = new SessionRecorder(config);
  return instance;
}

/**
 * Получение текущего экземпляра SDK
 */
export function getInstance(): SessionRecorder | null {
  return instance;
}

/**
 * Быстрый старт записи сессии
 * @param backendUrl URL бэкенда
 */
export function startRecording(backendUrl: string): SessionRecorder {
  const recorder = init({ backendUrl });
  recorder.start();
  return recorder;
}

export { SessionRecorder };
export type { SessionReplayConfig, SessionEvent, SessionMetadata, MaskInputOptions } from './types';
