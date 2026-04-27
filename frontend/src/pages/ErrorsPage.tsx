import { useEffect, useState } from 'react';
import { apiClient, ProcessingError } from '../api/client';

type Props = {
  token: string;
};

export default function ErrorsPage({ token }: Props) {
  const [eventId, setEventId] = useState('');
  const [stage, setStage] = useState('');
  const [errors, setErrors] = useState<ProcessingError[]>([]);
  const [selected, setSelected] = useState<ProcessingError | null>(null);

  const load = async () => {
    const data = await apiClient.getErrors(token, { eventId, stage, limit: 100 });
    setErrors(data);
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
          {errors.map((row) => (
            <tr key={row.id} onClick={() => setSelected(row)}>
              <td>{row.eventId}</td>
              <td>{row.stage}</td>
              <td>{row.attempts}</td>
              <td>{row.errorMessage}</td>
              <td>{new Date(row.createdAt).toLocaleString()}</td>
            </tr>
          ))}
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
