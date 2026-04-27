import { useEffect, useMemo, useState } from 'react';
import { apiClient, DlqEvent } from '../api/client';

type Props = {
  token: string;
  actor: string;
};

export default function DlqPage({ token, actor }: Props) {
  const [rows, setRows] = useState<DlqEvent[]>([]);
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [status, setStatus] = useState('');

  const selectedIds = useMemo(
    () => Object.entries(selected).filter(([, checked]) => checked).map(([id]) => id),
    [selected]
  );

  const load = async () => {
    const data = await apiClient.getDlq(token, 200);
    setRows(data);
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

  return (
    <div className="panel">
      <div className="toolbar">
        <button disabled={selectedIds.length === 0} onClick={() => void replay()}>
          Replay Selected ({selectedIds.length})
        </button>
        <button onClick={() => void load()}>Refresh</button>
        <span>{status}</span>
      </div>

      <table>
        <thead>
          <tr>
            <th></th>
            <th>Event ID</th>
            <th>Reason</th>
            <th>Created</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
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
              <td>{new Date(row.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
