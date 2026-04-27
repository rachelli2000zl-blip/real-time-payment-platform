import { useEffect, useState } from 'react';
import { apiClient } from '../api/client';

type Props = {
  token: string;
};

export default function ConfigPage({ token }: Props) {
  const [schemas, setSchemas] = useState<Array<{ name: string; version: number; path: string }>>([]);
  const [config, setConfig] = useState<Record<string, unknown>>({});

  useEffect(() => {
    const load = async () => {
      const [schemaRows, configRow] = await Promise.all([
        apiClient.getSchemas(token),
        apiClient.getConfig(token)
      ]);
      setSchemas(schemaRows);
      setConfig(configRow);
    };

    void load();
  }, [token]);

  return (
    <div className="grid">
      <section className="panel">
        <h3>Schema Versions</h3>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Version</th>
              <th>Path</th>
            </tr>
          </thead>
          <tbody>
            {schemas.map((schema) => (
              <tr key={`${schema.name}-${schema.version}`}>
                <td>{schema.name}</td>
                <td>{schema.version}</td>
                <td>{schema.path}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="panel">
        <h3>Pipeline Settings</h3>
        <pre>{JSON.stringify(config, null, 2)}</pre>
      </section>
    </div>
  );
}
