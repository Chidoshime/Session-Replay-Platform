export interface SessionReplayConfig {
  /** URL бэкенда для отправки событий */
  backendUrl: string;
  
  /** API ключ для аутентификации (опционально) */
  apiKey?: string;
  
  /** Размер буфера событий перед отправкой */
  bufferSize?: number;
  
  /** Интервал авто-отправки в миллисекундах */
  flushIntervalMs?: number;
  
  /** Включить запись консоли */
  recordConsole?: boolean;
  
  /** Включить запись сети */
  recordNetwork?: boolean;
  
  /** Маскировать ввод текста (пароли и т.д.) */
  maskAllInputs?: boolean;
  
  /** Селекторы элементов для маскировки */
  maskInputOptions?: MaskInputOptions;
  
  /** Блокировка записи определенных селекторов */
  blockClass?: string | RegExp;
  
  /** Игнорирование определенных селекторов */
  ignoreClass?: string | RegExp;
  
  /** Callback для логирования ошибок */
  onError?: (error: Error) => void;
}

export interface MaskInputOptions {
  password?: boolean;
  email?: boolean;
  tel?: boolean;
  creditCard?: boolean;
}

export interface SessionEvent {
  timestamp: number;
  type: string;
  data: any;
  url?: string;
  viewportWidth?: number;
  viewportHeight?: number;
}

export interface SessionMetadata {
  userAgent?: string;
  ip?: string;
  [key: string]: string | undefined;
}
