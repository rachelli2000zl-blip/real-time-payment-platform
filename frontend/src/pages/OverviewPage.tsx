import { useEffect, useState } from 'react';
import { apiClient, SummaryResponse } from '../api/client';
import SimpleLineChart from '../components/SimpleLineChart';

type Props = {
  token: string;
};

type MetricTone = 'healthy' | 'warning' | 'danger' | 'muted';

function throughputTone(value: number): MetricTone {
  return value > 0 ? 'healthy' : 'muted';
}

function errorRateTone(value: number): MetricTone {
  if (value <= 0) {
    return 'healthy';
  }
  return value >= 0.02 ? 'danger' : 'warning';
}

function dlqTone(value: number): MetricTone {
  if (value <= 0) {
    return 'healthy';
  }
  return value >= 10 ? 'danger' : 'warning';
}

function lagTone(value: number): MetricTone {
  if (!Number.isFinite(value)) {
    return 'warning';
  }
  if (value >= 300) {
    return 'danger';
  }
  if (value >= 120) {
    return 'warning';
  }
  return 'healthy';
}

function toneLabel(tone: MetricTone): string {
  switch (tone) {
    case 'healthy':
      return 'Healthy';
    case 'warning':
      return 'Warning';
    case 'danger':
      return 'Critical';
    default:
      return 'Idle';
  }
}

export default function OverviewPage({ token }: Props) {
  const [summary, setSummary] = useState<SummaryResponse | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const next = await apiClient.getSummary(token);
        if (active) {
          setError('');
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

  const throughputStatus = throughputTone(summary.throughputPerMinute);
  const errorStatus = errorRateTone(summary.errorRate);
  const dlqStatus = dlqTone(summary.dlqDepth);
  const lagStatus = lagTone(summary.approximateLagSeconds);
  const hasKnownLag = Number.isFinite(summary.approximateLagSeconds);

  return (
    <div className="grid">
      <section className={`card metric-card status-${throughputStatus}`}>
        <div className="metric-header">
          <h3>Throughput / min</h3>
          <span className={`status-pill status-${throughputStatus}`}>{toneLabel(throughputStatus)}</span>
        </div>
        <strong>{summary.throughputPerMinute.toFixed(2)}</strong>
        <p className="metric-caption">Healthy throughput means events are actively being processed.</p>
      </section>
      <section className={`card metric-card status-${errorStatus}`}>
        <div className="metric-header">
          <h3>Error rate</h3>
          <span className={`status-pill status-${errorStatus}`}>{toneLabel(errorStatus)}</span>
        </div>
        <strong>{(summary.errorRate * 100).toFixed(2)}%</strong>
        <p className="metric-caption">Any non-zero error rate should be investigated quickly.</p>
      </section>
      <section className={`card metric-card status-${dlqStatus}`}>
        <div className="metric-header">
          <h3>DLQ depth</h3>
          <span className={`status-pill status-${dlqStatus}`}>{toneLabel(dlqStatus)}</span>
        </div>
        <strong>{summary.dlqDepth}</strong>
        <p className="metric-caption">Messages in DLQ indicate failed processing that needs replay or triage.</p>
      </section>
      <section className={`card metric-card status-${lagStatus}`}>
        <div className="metric-header">
          <h3>Approx lag (s)</h3>
          <span className={`status-pill status-${lagStatus}`}>{toneLabel(lagStatus)}</span>
        </div>
        <strong>{hasKnownLag ? summary.approximateLagSeconds : 'Unknown'}</strong>
        <p className="metric-caption">High or unknown lag means downstream processing is falling behind.</p>
      </section>

      <section className="panel span-2">
        <h3>Processed events (last 15m)</h3>
        <SimpleLineChart
          data={summary.series}
          metric="processed"
          color="#0b7a75"
          emptyMessage="No processed events yet. Start sending events to see live metrics."
        />
      </section>

      <section className="panel span-2">
        <h3>Errors (last 15m)</h3>
        <SimpleLineChart
          data={summary.series}
          metric="errors"
          color="#b22222"
          emptyMessage="No error events in this time window. Your pipeline is running clean."
        />
      </section>
    </div>
  );
}
