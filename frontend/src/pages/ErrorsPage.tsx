import { useEffect, useState } from 'react';
import { apiClient, ProcessingError } from '../api/client';

type Props = {
  token: string;
};

type BadgeTone = 'healthy' | 'warning' | 'danger' | 'neutral';

function stageClassName(stage: string): string {
  const normalized = stage.toLowerCase();
  if (normalized.includes('ingestion')) {
    return 'stage-ingestion';
  }
  if (normalized.includes('consumer') || normalized.includes('kinesis')) {
    return 'stage-consumer';
  }
  if (normalized.includes('retry')) {
    return 'stage-retry';
  }
  return 'stage-generic';
}

function classifyErrorType(message: string): { label: string; tone: BadgeTone } {
  const normalized = message.toLowerCase();
  if (normalized.includes('validation') || normalized.includes('schema')) {
    return { label: 'Validation', tone: 'warning' };
  }
  if (normalized.includes('timeout') || normalized.includes('throttle') || normalized.includes('rate')) {
    return { label: 'Timeout/Throttle', tone: 'warning' };
  }
  if (normalized.includes('auth') || normalized.includes('denied') || normalized.includes('forbidden')) {
    return { label: 'Auth/Permission', tone: 'danger' };
  }
  if (normalized.includes('network') || normalized.includes('connection')) {
    return { label: 'Connectivity', tone: 'warning' };
  }
  return { label: 'Runtime', tone: 'danger' };
}

export default function ErrorsPage({ token }: Props) {
  const [eventId, setEventId] = useState('');
  const [stage, setStage] = useState('');
  const [errors, setErrors] = useState<ProcessingError[]>([]);
  const [selected, setSelected] = useState<ProcessingError | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.getErrors(token, { eventId, stage, limit: 100 });
      setErrors(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div className="panel">
      <div className="toolbar">
        <input value={eventId} onChange={(e) => setEventId(e.target.value)} placeholder="Filter by eventId" />
        <input value={stage} onChange={(e) => setStage(e.target.value)} placeholder="Filter by stage" />
        <button onClick={() => void load()}>Apply</button>
        <span className={`toolbar-note ${error ? 'is-danger' : ''}`}>
          {loading ? 'Loading errors...' : error ? `Failed to load errors: ${error}` : `${errors.length} records`}
        </span>
      </div>

      <table>
        <thead>
          <tr>
            <th>Event ID</th>
            <th>Stage</th>
            <th>Attempts</th>
            <th>Message</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {errors.length === 0 && !loading && !error && (
            <tr>
              <td colSpan={5}>
                <div className="table-empty-state">
                  No processing errors found. Your pipeline looks healthy.
                </div>
              </td>
            </tr>
          )}
          {errors.map((row) => {
            const errorType = classifyErrorType(row.errorMessage);
            return (
              <tr key={row.id} onClick={() => setSelected(row)}>
                <td>{row.eventId}</td>
                <td>
                  <span className={`badge badge-stage ${stageClassName(row.stage)}`}>{row.stage}</span>
                </td>
                <td>{row.attempts}</td>
                <td>
                  <div className="error-message-cell">
                    <span className={`badge badge-${errorType.tone}`}>{errorType.label}</span>
                    <span className="error-message-text">{row.errorMessage}</span>
                  </div>
                </td>
                <td>{new Date(row.createdAt).toLocaleString()}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {selected && (
        <aside className="drawer">
          <h3>Error detail</h3>
          <p><strong>Event:</strong> {selected.eventId}</p>
          <p><strong>Stage:</strong> {selected.stage}</p>
          <p><strong>Attempts:</strong> {selected.attempts}</p>
          <pre>{selected.stack}</pre>
          <button onClick={() => setSelected(null)}>Close</button>
        </aside>
      )}
    </div>
  );
}
