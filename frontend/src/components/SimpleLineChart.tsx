import { MetricPoint } from '../api/client';

type Props = {
  data: MetricPoint[];
  metric: 'processed' | 'errors';
  color: string;
};

export default function SimpleLineChart({ data, metric, color }: Props) {
  if (data.length === 0) {
    return <div className="chart-empty">No data</div>;
  }

  const width = 360;
  const height = 120;
  const values = data.map((point) => point[metric]);
  const max = Math.max(...values, 1);

  const points = values
    .map((value, index) => {
      const x = (index / Math.max(values.length - 1, 1)) * width;
      const y = height - (value / max) * (height - 10) - 5;
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <svg className="chart" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none">
      <polyline fill="none" stroke={color} strokeWidth="3" points={points} />
    </svg>
  );
}
