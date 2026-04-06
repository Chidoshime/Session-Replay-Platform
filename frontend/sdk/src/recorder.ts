import rrweb from 'rrweb';
import type { eventWithTime } from '@rrweb/types';
import { SessionReplayConfig, SessionEvent, SessionMetadata } from '../types';

export class SessionRecorder {
  private config: Required<SessionReplayConfig>;
  private eventBuffer: SessionEvent[] = [];
  private sessionId: string;
  private flushTimer: ReturnType<typeof setInterval> | null = null;
  private startTime: number;
  private isRecording: boolean = false;

  constructor(config: SessionReplayConfig) {
    this.config = {
      backendUrl: config.backendUrl,
      apiKey: config.apiKey || '',
      bufferSize: config.bufferSize || 100,
      flushIntervalMs: config.flushIntervalMs || 5000,
      recordConsole: config.recordConsole ?? false,
      recordNetwork: config.recordNetwork ?? false,
      maskAllInputs: config.maskAllInputs ?? true,
      maskInputOptions: config.maskInputOptions || {
        password: true,
        email: true,
        tel: true,
        creditCard: true,
      },
      blockClass: config.blockClass || 'sr-block',
      ignoreClass: config.ignoreClass || 'sr-ignore',
      onError: config.onError || console.error,
    };

    this.sessionId = this.generateSessionId();
    this.startTime = Date.now();
  }

  /**
   * Запуск записи сессии
   */
  public start(): void {
    if (this.isRecording) {
      return;
    }

    this.isRecording = true;
    this.eventBuffer = [];
    this.startTime = Date.now();

    // Настраиваем rrweb для записи DOM событий
    rrweb.record({
      emit: (event: eventWithTime) => {
        this.addEvent({
          timestamp: event.timestamp - this.startTime,
          type: event.type.toString(),
          data: event.data,
          url: window.location.href,
          viewportWidth: window.innerWidth,
          viewportHeight: window.innerHeight,
        });
      },
      maskAllInputs: this.config.maskAllInputs,
      maskInputOptions: this.config.maskInputOptions,
      blockClass: this.config.blockClass,
      ignoreClass: this.config.ignoreClass,
    });

    // Запускаем периодическую отправку событий
    this.startFlushTimer();

    // Отслеживаем уход со страницы для отправки оставшихся событий
    window.addEventListener('beforeunload', () => this.flush());
    window.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.flush();
      }
    });

    console.log('[SessionReplay] Recording started for session:', this.sessionId);
  }

  /**
   * Остановка записи сессии
   */
  public stop(): void {
    if (!this.isRecording) {
      return;
    }

    this.isRecording = false;
    
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
    }

    // Отправляем сигнал о завершении сессии
    this.sendSessionEnd();
    
    // Отправляем оставшиеся события
    this.flush();

    console.log('[SessionReplay] Recording stopped for session:', this.sessionId);
  }

  /**
   * Добавление события в буфер
   */
  private addEvent(event: SessionEvent): void {
    this.eventBuffer.push(event);

    // Если буфер заполнен, отправляем события
    if (this.eventBuffer.length >= this.config.bufferSize) {
      this.flush();
    }
  }

  /**
   * Отправка накопленных событий на сервер
   */
  private async flush(): Promise<void> {
    if (this.eventBuffer.length === 0 || !this.isRecording) {
      return;
    }

    const eventsToSend = [...this.eventBuffer];
    this.eventBuffer = [];

    try {
      await this.sendEvents(eventsToSend);
    } catch (error) {
      console.error('[SessionReplay] Failed to flush events:', error);
      // Возвращаем события обратно в буфер при ошибке
      this.eventBuffer = [...eventsToSend, ...this.eventBuffer];
    }
  }

  /**
   * Отправка событий на бэкенд
   */
  private async sendEvents(events: SessionEvent[]): Promise<void> {
    const metadata: SessionMetadata = {
      userAgent: navigator.userAgent,
    };

    const payload = {
      sessionId: this.sessionId,
      events,
      metadata,
    };

    const response = await fetch(`${this.config.backendUrl}/api/v1/sessions/events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(this.config.apiKey && { 'X-API-Key': this.config.apiKey }),
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    console.log(`[SessionReplay] Sent ${events.length} events`);
  }

  /**
   * Отправка сигнала о завершении сессии
   */
  private async sendSessionEnd(): Promise<void> {
    try {
      await fetch(`${this.config.backendUrl}/api/v1/sessions/${this.sessionId}/end`, {
        method: 'POST',
        headers: {
          ...(this.config.apiKey && { 'X-API-Key': this.config.apiKey }),
        },
      });
    } catch (error) {
      console.warn('[SessionReplay] Failed to send session end signal:', error);
    }
  }

  /**
   * Запуск таймера авто-отправки
   */
  private startFlushTimer(): void {
    this.flushTimer = setInterval(() => {
      this.flush();
    }, this.config.flushIntervalMs);
  }

  /**
   * Генерация уникального ID сессии
   */
  private generateSessionId(): string {
    return `sess_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Получение текущего ID сессии
   */
  public getSessionId(): string {
    return this.sessionId;
  }

  /**
   * Добавление пользовательского события (например, действие пользователя)
   */
  public addCustomEvent(type: string, data: any): void {
    this.addEvent({
      timestamp: Date.now() - this.startTime,
      type: `custom:${type}`,
      data,
      url: window.location.href,
      viewportWidth: window.innerWidth,
      viewportHeight: window.innerHeight,
    });
  }
}

export default SessionRecorder;
