import { useEffect, useMemo, useState } from 'react';
import { apiClient, DlqEvent } from '../api/client';

type Props = {
  token: string;
  actor: string;
};

export default function DlqPage({ token, actor }: Props) {
  const [rows, setRows] = useState<DlqEvent[]>([]);
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [expandedPayloads, setExpandedPayloads] = useState<Record<string, boolean>>({});
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const selectedIds = useMemo(
    () => Object.entries(selected).filter(([, checked]) => checked).map(([id]) => id),
    [selected]
  );

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.getDlq(token, 200);
      setRows(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const replay = async () => {
    const result = await apiClient.replayDlq(token, selectedIds, actor);
    setStatus(`Replayed ${result.count} messages.`);
    setSelected({});
    await load();
  };

  const togglePayload = (id: string) => {
    setExpandedPayloads((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const formatPayload = (payloadJson: string) => {
    try {
      return JSON.stringify(JSON.parse(payloadJson), null, 2);
    } catch {
      return payloadJson;
    }
  };

  return (
    <div className="panel">
      <div className="toolbar">
        <button disabled={selectedIds.length === 0} onClick={() => void replay()}>
          Replay Selected ({selectedIds.length})
        </button>
        <button onClick={() => void load()}>Refresh</button>
        <span className={`toolbar-note ${error ? 'is-danger' : ''}`}>
          {loading ? 'Loading DLQ events...' : error ? `Failed to load DLQ: ${error}` : `${rows.length} records`}
        </span>
        <span>{status}</span>
      </div>

      <table>
        <thead>
          <tr>
            <th></th>
            <th>Event ID</th>
            <th>Reason</th>
            <th>Payload</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && !loading && !error && (
            <tr>
              <td colSpan={5}>
                <div className="table-empty-state">
                  No DLQ events right now. Messages that fail processing will appear here.
                </div>
              </td>
            </tr>
          )}
          {rows.map((row) => {
            const isExpanded = Boolean(expandedPayloads[row.id]);
            return (
              <tr key={row.id}>
                <td>
                  <input
                    type="checkbox"
                    checked={Boolean(selected[row.id])}
                    onChange={(e) => setSelected((prev) => ({ ...prev, [row.id]: e.target.checked }))}
                  />
                </td>
                <td>{row.eventId}</td>
                <td>{row.reason}</td>
                <td>
                  {row.payloadJson ? (
                    <div className="payload-cell">
                      <button className="ghost-button" onClick={() => togglePayload(row.id)}>
                        {isExpanded ? 'Hide payload' : 'Show payload'}
                      </button>
                      {isExpanded && (
                        <pre className="payload-pre">{formatPayload(row.payloadJson)}</pre>
                      )}
                    </div>
                  ) : (
                    <span className="muted">No payload</span>
                  )}
                </td>
                <td>{new Date(row.createdAt).toLocaleString()}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
