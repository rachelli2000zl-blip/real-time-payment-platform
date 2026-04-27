import { useEffect, useState } from 'react';
import { apiClient, SummaryResponse } from '../api/client';
import SimpleLineChart from '../components/SimpleLineChart';

type Props = {
  token: string;
};

export default function OverviewPage({ token }: Props) {
  const [summary, setSummary] = useState<SummaryResponse | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const next = await apiClient.getSummary(token);
        if (active) {
          setSummary(next);
        }
      } catch (e) {
        if (active) {
          setError((e as Error).message);
        }
      }
    };

    load();
    const handle = setInterval(load, 15000);
    return () => {
      active = false;
      clearInterval(handle);
    };
  }, [token]);

  if (error) {
    return <div className="panel">Failed to load summary: {error}</div>;
  }

  if (!summary) {
    return <div className="panel">Loading summary...</div>;
  }

  return (
    <div className="grid">
      <section className="card">
        <h3>Throughput / min</h3>
        <strong>{summary.throughputPerMinute.toFixed(2)}</strong>
      </section>
      <section className="card">
        <h3>Error rate</h3>
        <strong>{(summary.errorRate * 100).toFixed(2)}%</strong>
      </section>
      <section className="card">
        <h3>DLQ depth</h3>
        <strong>{summary.dlqDepth}</strong>
      </section>
      <section className="card">
        <h3>Approx lag (s)</h3>
        <strong>{summary.approximateLagSeconds}</strong>
      </section>

      <section className="panel span-2">
        <h3>Processed events (last 15m)</h3>
        <SimpleLineChart data={summary.series} metric="processed" color="#0b7a75" />
      </section>

      <section className="panel span-2">
        <h3>Errors (last 15m)</h3>
        <SimpleLineChart data={summary.series} metric="errors" color="#b22222" />
      </section>
    </div>
  );
}
