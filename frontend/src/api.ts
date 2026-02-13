const API_BASE = '/api';

function getToken(): string | null {
  return localStorage.getItem('token');
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }

  const text = await response.text();
  if (!text) return {} as T;
  return JSON.parse(text);
}

// Auth
export interface AuthResponse {
  token: string;
  username: string;
  role: string;
  userId: number;
}

export const authApi = {
  register: (username: string, email: string, password: string) =>
    request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password }),
    }),
  login: (username: string, password: string) =>
    request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
};

// Subscriptions
export interface Subscription {
  id: number;
  sourceType: string;
  params: string;
  enabled: boolean;
  createdAt: string;
}

export const subscriptionApi = {
  list: () => request<Subscription[]>('/subscriptions'),
  create: (data: Partial<Subscription>) =>
    request<Subscription>('/subscriptions', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  update: (id: number, data: Partial<Subscription>) =>
    request<Subscription>(`/subscriptions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  delete: (id: number) =>
    request<void>(`/subscriptions/${id}`, { method: 'DELETE' }),
};

// Rules
export interface Rule {
  id: number;
  subscriptionId: number;
  keywordFilter: string;
  dedupWindowMinutes: number;
  rateLimitPerHour: number;
  priority: string;
  quietHoursStart: string | null;
  quietHoursEnd: string | null;
  createdAt: string;
}

export const ruleApi = {
  list: (subscriptionId: number) =>
    request<Rule[]>(`/rules?subscriptionId=${subscriptionId}`),
  create: (data: Partial<Rule>) =>
    request<Rule>('/rules', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  update: (id: number, data: Partial<Rule>) =>
    request<Rule>(`/rules/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  delete: (id: number) =>
    request<void>(`/rules/${id}`, { method: 'DELETE' }),
};

// Notifications
export interface NotificationDto {
  id: number;
  userId: number;
  eventId: string;
  channel: string;
  status: string;
  attempts: number;
  lastError: string | null;
  createdAt: string;
  eventTitle: string;
  eventSourceType: string;
  eventPriority: string;
  eventPayloadJson: string;
}

export const notificationApi = {
  list: (page = 0, size = 50, status?: string) => {
    let path = `/notifications?page=${page}&size=${size}`;
    if (status) path += `&status=${status}`;
    return request<NotificationDto[]>(path);
  },
  listBetween: (from: string, to: string) =>
    request<NotificationDto[]>(`/notifications?from=${from}&to=${to}`),
};

// Monitoring
export interface Stats {
  totalUsers: number;
  totalSubscriptions: number;
  totalEvents: number;
  totalNotifications: number;
}

export const monitoringApi = {
  stats: () => request<Stats>('/monitoring/stats'),
};

// SSE Stream
export function createNotificationStream(
  onNotification: (n: NotificationDto) => void,
  onError?: (e: Event) => void
): EventSource {
  const token = getToken();
  const url = `${API_BASE}/stream/notifications${token ? `?token=${token}` : ''}`;
  const eventSource = new EventSource(url);

  eventSource.addEventListener('notification', (event) => {
    try {
      const data = JSON.parse((event as MessageEvent).data);
      onNotification(data);
    } catch (e) {
      console.error('Failed to parse SSE notification:', e);
    }
  });

  eventSource.onerror = (e) => {
    console.error('SSE connection error:', e);
    onError?.(e);
  };

  return eventSource;
}
