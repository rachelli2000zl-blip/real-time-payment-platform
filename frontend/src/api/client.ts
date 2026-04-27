export type MetricPoint = {
  minute: string;
  processed: number;
  errors: number;
};

export type SummaryResponse = {
  throughputPerMinute: number;
  errorRate: number;
  dlqDepth: number;
  approximateLagSeconds: number;
  latestProcessedTime: string | null;
  series: MetricPoint[];
};

export type ProcessingError = {
  id: string;
  eventId: string;
  stage: string;
  errorMessage: string;
  stack: string;
  attempts: number;
  createdAt: string;
  lastAttemptAt: string;
};

export type DlqEvent = {
  id: string;
  eventId: string;
  payloadJson: string;
  reason: string;
  createdAt: string;
};

class ApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private async request<T>(path: string, token: string, init: RequestInit = {}): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        ...(init.headers ?? {})
      }
    });

    if (!response.ok) {
      throw new Error(`API request failed: ${response.status}`);
    }

    return (await response.json()) as T;
  }

  getSummary(token: string) {
    return this.request<SummaryResponse>('/summary', token);
  }

  getErrors(token: string, params: { eventId?: string; stage?: string; limit?: number }) {
    const search = new URLSearchParams();
    if (params.eventId) search.set('eventId', params.eventId);
    if (params.stage) search.set('stage', params.stage);
    if (params.limit) search.set('limit', String(params.limit));
    return this.request<ProcessingError[]>(`/errors?${search.toString()}`, token);
  }

  getDlq(token: string, limit = 100) {
    return this.request<DlqEvent[]>(`/dlq?limit=${limit}`, token);
  }

  replayDlq(token: string, ids: string[], actor: string) {
    return this.request<{ count: number; status: string }>('/dlq/replay', token, {
      method: 'POST',
      body: JSON.stringify({ ids, actor })
    });
  }

  getSchemas(token: string) {
    return this.request<Array<{ name: string; version: number; path: string }>>('/schemas', token);
  }

  getConfig(token: string) {
    return this.request<Record<string, unknown>>('/config', token);
  }
}

export const apiClient = new ApiClient(import.meta.env.VITE_CONTROL_PLANE_URL ?? 'http://localhost:8081');
